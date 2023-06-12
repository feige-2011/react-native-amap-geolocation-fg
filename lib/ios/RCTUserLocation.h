#import <BMKLocationKit/BMKLocationComponent.h>

@interface RCTUserLocation : BMKLocation

- (instancetype)initWithCLLocation:(CLLocation *)cllocation;
- (void)updateWithCLLocation:(CLLocation *)cllocation;


- (id)json;

@end
