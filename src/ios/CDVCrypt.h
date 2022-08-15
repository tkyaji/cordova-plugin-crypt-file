//
//  CDVCrypt.h
//  CordovaLib
//
//  Created by tkyaji on 2015/07/17.
//
//

#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVViewController.h>
#import <WebKit/WebKit.h>
#import "CDVCryptURLProtocol.h"

@interface CDVCrypt : CDVPlugin

- (void)pluginInitialize;

#ifdef HASCDVUrlProtocol
#else
@property (nonatomic) NSMutableArray* stoppedTasks;
@property (nonatomic) CDVCryptURLProtocol* protocol;
#endif

@end
