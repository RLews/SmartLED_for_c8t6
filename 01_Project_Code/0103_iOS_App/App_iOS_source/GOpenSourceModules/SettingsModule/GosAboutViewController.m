//
//  AboutViewController.m
//  GBOSA
//
//  Created by Zono on 16/5/12.
//  Copyright © 2016年 Gizwits. All rights reserved.
//

#import "GosAboutViewController.h"
#import <GizWifiSDK/GizWifiSDK.h>
#import "GosCommon.h"

@interface GosAboutViewController ()

@property (nonatomic, weak) IBOutlet UILabel *appVersionLabel;
@property (nonatomic, weak) IBOutlet UILabel *SDKVersionLabel;
@property (weak, nonatomic) IBOutlet UITextView *textAboutInfo;

@end

@implementation GosAboutViewController

- (void)moveView:(CGFloat)top {
    for (NSLayoutConstraint *constraint in self.view.constraints) {
        if (constraint.firstAttribute == NSLayoutAttributeTop && constraint.secondItem == self.appVersionLabel) {
            constraint.constant += top;
            [self.view addConstraint:constraint];
        }
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationItem.title = NSLocalizedString(@"About", nil);
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithImage:[UIImage imageNamed:@"page_back_button.png"] style:UIBarButtonItemStylePlain target:self action:@selector(onBack)];
    self.appVersionLabel.text = [NSString stringWithFormat:@"%@: %@.%@", NSLocalizedString(@"Program Version", nil), [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"], [self getCompileTime]];
    self.SDKVersionLabel.text = [NSString stringWithFormat:@"%@: %@", NSLocalizedString(@"SDK Version", nil), [GizWifiSDK getVersion]];
    CGFloat top = 0;
    [self moveView:top];
    self.textAboutInfo.text = common.aboutInfo;
}

- (void)onBack {
    [GosCommon safePopViewController:self.navigationController currentViewController:self animated:YES];
}

- (NSString *)getCompileTime {
    char Date[] = __DATE__;
    char Time[] = __TIME__;
    char mon_s[20] = {0};
    static const char month_names[] = "JanFebMarAprMayJunJulAugSepOctNovDec";
    int mon = 0, day = 0, year = 0, hour = 0, minute = 0, second = 0;
    sscanf(Date, "%s %d %d", mon_s, &day, &year);
    mon = (strstr(month_names, mon_s)-month_names)/3.0+1;
    sscanf(Time, "%d:%d:%d", &hour, &minute, &second);
    return [NSString stringWithFormat:@"%02d%02d%02d",mon, day, hour];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
