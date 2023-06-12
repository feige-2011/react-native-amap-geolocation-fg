#import <React/RCTEventEmitter.h>
#import <BMKLocationkit/BMKLocationComponent.h>
#import <BaiduMapAPI_Base/BMKUserLocation.h>
#import "RCTUserLocation.h"
// 引入base相关所有的头文件
#import <BaiduMapAPI_Base/BMKBaseComponent.h>
@interface BDGeolocationModule : RCTEventEmitter <RCTBridgeModule, BMKLocationManagerDelegate,BMKLocationAuthDelegate>
@property (nonatomic, strong) BMKLocationManager*locationManager;
@property (nonatomic, strong) RCTUserLocation *location;
@property (nonatomic, assign) BOOL initialized;
@end

@implementation BDGeolocationModule {
    
}

RCT_EXPORT_MODULE(BDGeolocation)

RCT_EXPORT_METHOD(setOptions:(NSDictionary *)options) {
    if (options[@"distanceFilter"]) {
        self.locationManager.distanceFilter = [options[@"distanceFilter"] doubleValue];
    }
}

-(RCTUserLocation *)location{
    if (!_location){
         _location = [RCTUserLocation new];
    }
    return _location;
}

-(BMKLocationManager *)locationManager{
    if (!_locationManager){
        //初始化实例
        self.initialized = YES;
           _locationManager = [[BMKLocationManager alloc] init];
//        //设置是否自动停止位置更新
          _locationManager.pausesLocationUpdatesAutomatically = NO;
        _locationManager.locatingWithReGeocode = YES;
        _locationManager.isNeedNewVersionReGeocode = YES;
//        //设置位置获取超时时间
         _locationManager.locationTimeout = 120;
//        //设置获取地址信息超时时间
         _locationManager.reGeocodeTimeout = 120;
            //设置返回位置的坐标系类型
            _locationManager.coordinateType = BMKLocationCoordinateTypeGCJ02;
//            //设置预期精度参数
            _locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters;
            //设置应用位置类型
            _locationManager.activityType = CLActivityTypeAutomotiveNavigation;

    }
    return _locationManager;
}

RCT_REMAP_METHOD(init, initWithKey
                 : (NSString *)key
                 : (RCTPromiseResolveBlock)resolve
                 : (RCTPromiseRejectBlock)reject){
    dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"%@",key);
        [[BMKLocationAuth new] setAgreePrivacy:YES];
        // 要使用百度地图，请先启动BaiduMapManager
        [[BMKLocationAuth sharedInstance] checkPermisionWithKey:key authDelegate:self];
        if (!self.initialized){
            self.initialized=YES;
        
        }
        //设置delegate
        self.locationManager.delegate = self;
    });
    //设置精度参数
    resolve(nil);
}

RCT_EXPORT_METHOD(start) {
    dispatch_async(dispatch_get_main_queue(), ^{
         self.initialized = NO;
        [self.locationManager setLocatingWithReGeocode:YES];
        [self.locationManager startUpdatingLocation];
  });
}

RCT_EXPORT_METHOD(stop) {
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.locationManager stopUpdatingLocation];
    });
}

- (void)BMKLocationManager:(BMKLocationManager * _Nonnull)manager didUpdateLocation:(BMKLocation * _Nullable)location orError:(NSError * _Nullable)error

{
    if (error)
    {
        NSLog(@"locError:{%ld - %@};", (long)error.code, error.localizedDescription);
    }
    
    if (location.rgcData&&!self.initialized) {//得到定位信息，添加annotation
        [self didUpdateBMKUserLocation:location];
    }
}

//sdk重写前
- (void)didUpdateBMKUserLocation:(BMKLocation *)location {
    [self sendEventWithName:@"baiduMapLocation" body: [self json:location]];
}

- (id)json:(BMKLocation *)bmk_location{
    CLLocation *location = bmk_location.location;
    
//    @interface BMKLocationReGeocode : NSObject
//
//    ///国家名字属性
//    @property(nonatomic, copy, readonly) NSString *country;
//
//    ///国家编码属性
//    @property(nonatomic, copy, readonly) NSString *countryCode;
//
//    ///省份名字属性
//    @property(nonatomic, copy, readonly) NSString *province;
//
//    ///城市名字属性
//    @property(nonatomic, copy, readonly) NSString *city;
//
//    ///区名字属性
//    @property(nonatomic, copy, readonly) NSString *district;
//
//    ///乡镇名字属性
//    @property(nonatomic, copy, readonly) NSString *town;
//
//    ///街道名字属性
//    @property(nonatomic, copy, readonly) NSString *street;
//
//    ///街道号码属性
//    @property(nonatomic, copy, readonly) NSString *streetNumber;
//
//    ///城市编码属性
//    @property(nonatomic, copy, readonly) NSString *cityCode;
//
//    ///行政区划编码属性
//    @property(nonatomic, copy, readonly) NSString *adCode;


  if (bmk_location.rgcData) {
      self.initialized = YES;
      BMKLocationReGeocode *reGeocode = bmk_location.rgcData;
      NSString * street =  [NSString stringWithFormat:@"%@  %@",reGeocode.city,reGeocode.locationDescribe? reGeocode.locationDescribe : reGeocode.street ?reGeocode.street:@""];
    return @{
      @"errorCode": @(0),
      @"accuracy" : @(location.horizontalAccuracy),
      @"latitude" : @(location.coordinate.latitude),
      @"longitude" : @(location.coordinate.longitude),
      @"altitude" : @(location.altitude),
      @"speed" : @(location.speed),
      @"heading" : @(location.course),
      @"timestamp" : @(location.timestamp.timeIntervalSince1970 * 1000),
      @"address" : street,
      @"my_address":street,
      @"poiName" : reGeocode.locationDescribe ? reGeocode.locationDescribe : @"",
      @"country" : reGeocode.country ? reGeocode.country : @"",
      @"province" : reGeocode.province ? reGeocode.province : @"",
      @"city" : reGeocode.city ? reGeocode.city : @"",
      @"cityCode" : reGeocode.cityCode ? reGeocode.cityCode : @"",
      @"district" : reGeocode.district ? reGeocode.district : @"",
      @"street" : reGeocode.street ? reGeocode.street : @"",
      @"streetNumber" : reGeocode.streetNumber ? reGeocode.streetNumber : @"",
      @"adCode" : reGeocode.adCode ? reGeocode.adCode : @"",
    };
  } else {
    return @{
      @"errorCode": @(0),
      @"accuracy" : @(location.horizontalAccuracy),
      @"latitude" : @(location.coordinate.latitude),
      @"longitude" : @(location.coordinate.longitude),
      @"altitude" : @(location.altitude),
      @"speed" : @(location.speed),
      @"direction" : @(location.course),
      @"timestamp" : @(location.timestamp.timeIntervalSince1970 * 1000),
    };
  }
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"BDGeolocation"];
}

@end
