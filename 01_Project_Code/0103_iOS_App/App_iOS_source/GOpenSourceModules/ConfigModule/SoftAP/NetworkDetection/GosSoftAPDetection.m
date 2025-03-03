//
//  GizSoftAPDetection.m
//  DeviceConfigDemo
//
//  Created by GeHaitong on 16/1/12.
//  Copyright © 2016年 gizwits. All rights reserved.
//

#import "GosSoftAPDetection.h"
#import <UIKit/UIKit.h>
#import "GosReachability.h"
#import <SystemConfiguration/CaptiveNetwork.h>

#import "GizLog.h"

typedef NS_ENUM(NSInteger, GizNetStatus) {
    GizNetStatusDisconnected,
    GizNetStatusConnecting,
    GizNetStatusConnected
};

@interface GosSoftAPDetection()

@property (assign) UIBackgroundTaskIdentifier taskid_atomic;

@property (strong) NSString *ssidPrefix_atomic;
@property (assign) id <GizSoftAPDetectionDelegate>delegate_atomic;
@property (strong) GosReachability *netReach_atomic;
@property (assign) GizNetStatus netStatus_atomic;
@property (assign) BOOL netStatusChanged_atomic;

@end

@implementation GosSoftAPDetection

- (instancetype)initWithSoftAPSSID:(NSString *)ssidPrefix delegate:(id <GizSoftAPDetectionDelegate>)delegate {
    self = [super init];
    if (self) {
        self.ssidPrefix_atomic = ssidPrefix;
        self.delegate_atomic = delegate;
        self.netReach_atomic = [GosReachability reachabilityWithHostName:@"www.gizwits.com"];
        if (![self.netReach_atomic startNotifier]) {
            GIZ_LOG_ERROR("startNotifier failed");
            return nil;
        }
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didEnterBackground) name:UIApplicationDidEnterBackgroundNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didNetStatusChanged) name:kGizReachabilityChangedNotification object:nil];
    }
    return self;
}

- (void)dealloc {
    [self.netReach_atomic stopNotifier];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:kGizReachabilityChangedNotification object:nil];
    [self endBackgrondTask];
}

/*
 * 获取当前链接的 ssid
 */
- (NSString *)ssid {
    NSArray *interfaces = (__bridge_transfer NSArray *)CNCopySupportedInterfaces();
    for (NSString *interface in interfaces) {
        NSDictionary *networkInfo = (__bridge_transfer NSDictionary *)CNCopyCurrentNetworkInfo((__bridge CFStringRef)interface);
        NSString *ssid = networkInfo[(__bridge_transfer NSString *)kCNNetworkInfoKeySSID];
        if (ssid.length > 0) {
            return ssid;
        }
    }
    return nil;
}

/*
 * 检测网络状态是否改变
 */
- (void)didNetStatusChanged {
    self.netStatusChanged_atomic = YES;
    if (GizReachableViaWiFi == self.netReach_atomic.currentReachabilityStatus) {
        if (self.netReach_atomic.isConnectingWiFi)
            self.netStatus_atomic = GizNetStatusConnecting;
        else
            self.netStatus_atomic = GizNetStatusConnected;
    } else {
        self.netStatus_atomic = GizNetStatusDisconnected;
    }
}

/*
 * 后台模式自动检测状态变化
 */
- (void)didEnterBackground {
    if (![self beginBackgroundTask]) {
        return;
    }
    
    __weak __typeof(self) weakSelf = self;
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        __strong __typeof(self) strongSelf = weakSelf;
        
        if (nil == strongSelf) {
            return;
        }
        
        // 旧版 iOS 设置超时，防止出现后台时间太短的情况
        if ([[UIApplication sharedApplication] respondsToSelector:@selector(setKeepAliveTimeout:handler:)]) {
            [[UIApplication sharedApplication] setKeepAliveTimeout:600 handler:nil];
        }

        NSTimeInterval min = 3;
        NSTimeInterval max = [UIApplication sharedApplication].backgroundTimeRemaining;
        
        if (max < 20) {
            GIZ_LOG_DEBUG("The ios system has bug?");
        }
        
        NSString *ssid = [strongSelf ssid];
        
        // 如果已经是 soft ap 模式，不重复检测
        if ((GizNotReachable == strongSelf.netReach_atomic.currentReachabilityStatus || !strongSelf.netReach_atomic.isConnectingWiFi) &&
            [ssid hasPrefix:strongSelf.ssidPrefix_atomic]) {
            if ([strongSelf.delegate_atomic didSoftAPModeDetected:ssid]) {
                [strongSelf endBackgrondTask];
                return;
            }
        }
        
        strongSelf.netStatus_atomic = GizNetStatusDisconnected;
        GIZ_LOG_DEBUG("background detect softap mode started");
        
//        int counter = 0;
        
        // 检测网络切换过程
        while ([UIApplication sharedApplication].backgroundTimeRemaining > min &&
               [UIApplication sharedApplication].backgroundTimeRemaining <= max) {
            NSString *new_ssid = [strongSelf ssid];
            
            //ssid 发生改变，清除计数器的同时，拷贝新的 ssid
            if (![ssid isEqualToString:new_ssid]) {
//                counter = 0;
                ssid = new_ssid;
            }
            
            if ([ssid hasPrefix:strongSelf.ssidPrefix_atomic]) {
                //连接中或者已连接就行
                if (strongSelf.netReach_atomic.isConnectingWiFi || strongSelf.netStatus_atomic == GizNetStatusConnected) {
                    if (strongSelf.netStatusChanged_atomic) {
                        strongSelf.netStatusChanged_atomic = NO;
                    }
                    if ([strongSelf.delegate_atomic didSoftAPModeDetected:ssid]) {
                        break;
                    }
                }
                // 状态不变，500ms内没有变化
                // 状态变为已连接状态后，500ms内没有变化
//                if (strongSelf.netStatus_atomic == GizNetStatusConnected && !strongSelf.netReach_atomic.isConnectingWiFi) {
//                    if (strongSelf.netStatusChanged_atomic) {
//                        strongSelf.netStatusChanged_atomic = NO;
//                    } else {
//                        counter++;
//                    }
//
//                    if (counter >= 5) {
//                        if ([strongSelf.delegate_atomic didSoftAPModeDetected:ssid]) {
//                            break;
//                        }
//                    }
//                }
            } else {
                strongSelf.netStatusChanged_atomic = NO;
            }

            usleep(100000);
        }
        
        GIZ_LOG_DEBUG("background detect softap mode stoped");
        [strongSelf endBackgrondTask];
    });
}

- (BOOL)beginBackgroundTask {
    if (0 != self.taskid_atomic) {
        return NO;
    }
    
    self.taskid_atomic = [[UIApplication sharedApplication] beginBackgroundTaskWithExpirationHandler:nil];
    return 0 != self.taskid_atomic;
}

- (void)endBackgrondTask {
    sleep(1);
    if (0 != self.taskid_atomic) {
        NSInteger taskid = self.taskid_atomic;
        self.taskid_atomic = 0;
        [[UIApplication sharedApplication] endBackgroundTask:taskid];
    }
}

@end
