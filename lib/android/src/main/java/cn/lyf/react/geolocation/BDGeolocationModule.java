package cn.lyf.react.geolocation;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.common.BaiduMapSDKException;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

@SuppressWarnings("unused")
public class BDGeolocationModule extends ReactContextBaseJavaModule {

    protected static final String TAG = BDGeolocationModule.class.getSimpleName();

    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
    private static LocationClient mLocationClient = null;
    private static LocationClientOption mOption;
    private static LocationClientOption  DIYoption;
    private ReactApplicationContext mReactContext;
    private Object objLock;
    private BDAbstractLocationListener mListener;
    BDGeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;

        SDKInitializer.setAgreePrivacy(reactContext.getApplicationContext(), true);
        try {
            SDKInitializer.initialize(reactContext.getApplicationContext());
        } catch (BaiduMapSDKException e) {
            e.printStackTrace();
        }
        SDKInitializer.setCoordType(CoordType.GCJ02);
        LocationClient.setAgreePrivacy(true);
    }

    @NonNull
    @Override
    public String getName() {
        return "BDGeolocation";
    }



    @ReactMethod
    public void init(String key, Promise promise) throws Exception {

        /**
         * 申请文件读写、相机、位置权限
         */
        SDKInitializer.setApiKey(key);
         Handler mHandler=new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //setAgreePrivacy接口需要在LocationClient实例化之前调用
                if (mLocationClient == null) {
                    try {
                        mLocationClient = new LocationClient(
                                getReactApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //声明LocationClient类
                }
                if (mLocationClient != null){

                    addLocationOption(getDefaultLocationClientOption());
                    eventEmitter = mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
                }
                mListener = new BDAbstractLocationListener() {
                    /**
                     * 定位请求回调函数
                     *
                     */
                    @Override
                    public void onReceiveLocation(BDLocation bdLocation) {
                        if (bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation || bdLocation.getLocType() == BDLocation.TypeOffLineLocation) {// 定位成功
                            sendSuccessEvent(bdLocation);
                        } else {
                            WritableMap map = Arguments.createMap();
                            map.putInt("code", BDLocation.TypeServerError);
                            eventEmitter.emit("BDGeolocation", map);
                        }
                    }
                };
                registerListener(mListener);
            }
        };
        mHandler.post(runnable);
        promise.resolve(null);
    }




    public boolean registerListener(BDAbstractLocationListener listener) {
        boolean isSuccess = false;
        if ((mLocationClient != null) && (listener != null)) {
            mLocationClient.registerLocationListener(listener);
            isSuccess = true;
        }
        return isSuccess;
    }
    public void unregisterListener(BDAbstractLocationListener listener) {
        if ((mLocationClient != null) && (listener != null)) {
            mLocationClient.unRegisterLocationListener(listener);
        }
    }

    protected void sendSuccessEvent(BDLocation location) {
        if (location != null) {
            eventEmitter.emit("BDGeolocation", toJSON(location));
        }
    }

    protected void sendFailureEvent(BDLocation location) {
        WritableMap map = Arguments.createMap();
        map.putInt("code", BDLocation.TypeServerError);
        if (location.getLocType() == BDLocation.TypeServerError) {
            map.putString("message", "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
        } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
            map.putString("message", "网络不同导致定位失败，请检查网络是否通畅");
        } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
            map.putString("message", "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
        } else {
            map.putString("message", "定位失败");
        }
        map.putString("locationDescribe", location.getLocationDescribe());
//        sendEvent(DidFailToLocateUserWithError, map);
    }

//    protected void sendDidStopEvent() {
//        WritableMap map = Arguments.createMap();
//        map.putString("message", "停止定位");
//        sendEvent(DidStopLocatingUser, map);
//    }

//    private void sendEvent(String eventName, @Nullable WritableMap params) {
//        //此处需要添加hasActiveCatalystInstance，否则可能造成崩溃
//        //问题解决参考: https://github.com/walmartreact/react-native-orientation-listener/issues/8
//        if (mReactContext.hasActiveCatalystInstance()) {
//            mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
//                    .emit(eventName, params);
//        } else {
//        }
//    }

    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            mOption.setCoorType( "gcj02" ); // 可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            mOption.setScanSpan(500); // 可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setIsNeedAddress(true); // 可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true); // 可选，设置是否需要地址描述
            mOption.setNeedDeviceDirect(false); // 可选，设置是否需要设备方向结果
            mOption.setLocationNotify(false); // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(true); // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop
            mOption.setIsNeedLocationDescribe(true); // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation
            mOption.setIsNeedLocationPoiList(true); // 可选，默认false，设置是否需要POI结果，可以在BDLocation
            mOption.SetIgnoreCacheException(false); // 可选，默认false，设置是否收集CRASH信息，默认收集
            mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备，模糊
            mOption.setIsNeedAltitude(false); // 可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
            // 可选，设置首次定位时选择定位速度优先还是定位准确性优先，默认为速度优先
            mOption.setFirstLocType(LocationClientOption.FirstLocType.SPEED_IN_FIRST_LOC);
        }
        return mOption;
    }
    @ReactMethod
    public void addLocationOption(LocationClientOption option) {

        boolean isSuccess = false;
        if ((mLocationClient != null) && (option != null)) {
            if (mLocationClient.isStarted()) {
                mLocationClient.stop();
            }
            DIYoption = option;
            mLocationClient.setLocOption(option);
        }
    }


    @ReactMethod
    public void start() {
            if (mLocationClient != null && !mLocationClient.isStarted()) {
                registerListener(mListener);
                mLocationClient.start();
            }
    }

    @ReactMethod
    public void stop() {
            if (mLocationClient != null && mLocationClient.isStarted()) {
                unregisterListener(mListener);
                mLocationClient.stop();
            }
//        sendDidStopEvent();
    }

    @ReactMethod
    public void addListener(String name) {
    }



    @ReactMethod
    public void isStarted(Promise promise) {
        promise.resolve(mLocationClient.isStarted());
    }
    private ReadableMap toJSON(BDLocation location) {

//        result.speed = location.speed;
//        result.province = location.province;
//        result.streetNumber = location.streetNumber;
//        result.street = location.street;
//        result.cityCode = location.cityCode;
//        result.address = location.address;
//        result.city = location.city;
//        result.poiName = location.poiName;
//        result.country = location.country;
//        result.direction = location.direction;
//        result.district = location.district;
//        result.longitude = location.longitude;
//        result.latitude = location.latitude;
//        result.timestamp = location.timestamp;
//        result.direction = location.direction;
//        result.accuracy = location.accuracy;

        String city = location.getCity();
        String locationDescribe = location.getLocationDescribe();
        String street = location.getStreet();
        StringBuilder sb = new StringBuilder();
        String my_adress = sb.append(city).append(locationDescribe).toString();
        if (locationDescribe == null|| ""==locationDescribe){
            sb.append(city).append(street).toString();
        }
        WritableMap map = Arguments.createMap();
        map.putDouble("latitude", location.getLatitude());
        map.putDouble("longitude", location.getLongitude());
        map.putString("address", location.getAddrStr());
        map.putString("province", location.getProvince());
        map.putString("city", location.getCity());
        map.putString("district", location.getDistrict());
        map.putString("streetName", location.getStreet());
        map.putString("streetNumber", location.getStreetNumber());
        map.putString("country", location.getCountry());

        map.putString("address", my_adress);
        map.putString("my_address", my_adress);
        map.putString("poiName", location.getLocationDescribe());
        map.putString("cityCode", location.getCityCode());
        map.putString("district", location.getDistrict());
        map.putString("street", location.getStreet());
        map.putString("adCode", location.getAdCode());
        if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
            map.putString("describe", "gps定位成功");
        } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
            map.putString("describe", "网络定位成功");
        } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
            map.putString("describe", "离线定位成功");
        }

        return map;
    }


}
