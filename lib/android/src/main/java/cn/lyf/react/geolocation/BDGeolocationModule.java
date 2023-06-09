package cn.lyf.react.geolocation;

import android.Manifest;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.mapapi.SDKInitializer;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import cn.lyf.react.geolocation.PermissionUtils;

@SuppressWarnings("unused")
public class BDGeolocationModule extends ReactContextBaseJavaModule {

    protected static final String TAG = BDGeolocationModule.class.getSimpleName();
    protected static final String WillStartLocatingUser = "WillStartLocatingUser";
    protected static final String DidStopLocatingUser = "DidStopLocatingUser";

    protected static final String DidUpdateUserHeading = "DidUpdateUserHeading";
    protected static final String DidUpdateBMKUserLocation = "DidUpdateBMKUserLocation";
    protected static final String DidFailToLocateUserWithError = "DidFailToLocateUserWithError";


    private static LocationClient mLocationClient = null;
    private static LocationClientOption option;
    private static LocationClientOption  DIYoption;
    private ReactApplicationContext mReactContext;

    BDGeolocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
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
        SDKInitializer.initialize(getReactApplicationContext().getApplicationContext());
        PermissionUtils.requestPermissions(getCurrentActivity(), 0x11, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, new PermissionUtils.OnPermissionListener() {
            @Override
            public void onPermissionGranted() throws Exception {
                if (mLocationClient == null) {
                    try {
//                        mLocationClient = new LocationClient(
//                                getReactApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                      //声明LocationClient类
                }
                if (mLocationClient != null){
                    mLocationClient.registerLocationListener(new BDAbstractLocationListener() {
                        @Override
                        public void onReceiveLocation(BDLocation bdLocation) {
                            if (bdLocation.getLocType() == bdLocation.TypeGpsLocation || bdLocation.getLocType() == bdLocation.TypeNetWorkLocation || bdLocation.getLocType() == bdLocation.TypeOffLineLocation) {// 定位成功
                                sendSuccessEvent(bdLocation);
                            } else {
                                sendFailureEvent(bdLocation);
                            }
                        }
                    });
                    setLocationOption(null);
                }
            }
            @Override
            public void onPermissionDenied(String[] deniedPermissions) {

            }
        });
    }

    protected void sendSuccessEvent(BDLocation location) {
        WritableMap map = Arguments.createMap();
        map.putDouble("latitude", location.getLatitude());
        map.putDouble("longitude", location.getLongitude());
        map.putString("address", location.getAddrStr());
        map.putString("province", location.getProvince());
        map.putString("city", location.getCity());
        map.putString("district", location.getDistrict());
        map.putString("streetName", location.getStreet());
        map.putString("streetNumber", location.getStreetNumber());
        if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
            map.putString("describe", "gps定位成功");
        } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
            map.putString("describe", "网络定位成功");
        } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
            map.putString("describe", "离线定位成功");
        }
        map.putString("locationDescribe", location.getLocationDescribe());
        sendEvent(DidUpdateBMKUserLocation, map);
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
        sendEvent(DidFailToLocateUserWithError, map);
    }

    protected void sendDidStopEvent() {
        WritableMap map = Arguments.createMap();
        map.putString("message", "停止定位");
        sendEvent(DidStopLocatingUser, map);
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        //此处需要添加hasActiveCatalystInstance，否则可能造成崩溃
        //问题解决参考: https://github.com/walmartreact/react-native-orientation-listener/issues/8
        if (mReactContext.hasActiveCatalystInstance()) {
            Log.i(TAG, "hasActiveCatalystInstance");
            mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } else {
            Log.i(TAG, "not hasActiveCatalystInstance");
        }
    }


    @ReactMethod
    public void setLocationOption(ReadableMap optionMap) {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }


    @ReactMethod
    public void start() {
        mLocationClient.start();
    }

    @ReactMethod
    public void stop() {
        mLocationClient.stop();
        sendDidStopEvent();
    }

    @ReactMethod
    public void addListener(String name) {
    }



    @ReactMethod
    public void isStarted(Promise promise) {
        promise.resolve(mLocationClient.isStarted());
    }





    private ReadableMap toJSON(BDLocation location) {
        if (location == null) {
            return null;
        }
        WritableMap map = Arguments.createMap();
//        map.putString("locationDetail", location.getLocationDetail());
//        if (location.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
//            map.putDouble("timestamp", location.getTime());
//            map.putDouble("accuracy", location.getAccuracy());
//            map.putDouble("latitude", location.getLatitude());
//            map.putDouble("longitude", location.getLongitude());
//            map.putDouble("altitude", location.getAltitude());
//            map.putDouble("speed", location.getSpeed());
//            map.putDouble("heading", location.getBearing());
//            map.putInt("locationType", location.getLocationType());
//            map.putString("coordinateType", location.getCoordType());
//            map.putInt("gpsAccuracy", location.getGpsAccuracyStatus());
//            map.putInt("trustedLevel", location.getTrustedLevel());
//            if (!location.getAddress().isEmpty()) {
//                map.putString("address", location.getAddress());
//                map.putString("description", location.getDescription());
//                map.putString("poiName", location.getPoiName());
//                map.putString("country", location.getCountry());
//                map.putString("province", location.getProvince());
//                map.putString("city", location.getCity());
//                map.putString("cityCode", location.getCityCode());
//                map.putString("district", location.getDistrict());
//                map.putString("street", location.getStreet());
//                map.putString("streetNumber", location.getStreetNum());
//                map.putString("adCode", location.getAdCode());
//            }
//        }
        return map;
    }


}
