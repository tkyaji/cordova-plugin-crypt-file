//
//  CDVCrypt.m
//  CordovaLib
//
//  Created by tkyaji on 2015/07/17.
//
//

#import "CDVCrypt.h"

@implementation CDVCrypt

- (void)pluginInitialize
{
    #ifdef HASCDVUrlProtocol
    [NSURLProtocol registerClass:[CDVCryptURLProtocol class]];
    #else
    self.stoppedTasks = [[NSMutableArray alloc] init];
    self.protocol = [[CDVCryptURLProtocol alloc] init];
    #endif
}

#ifdef HASCDVUrlProtocol
#else
- (BOOL)overrideSchemeTask: (id <WKURLSchemeTask>)urlSchemeTask
{
    if([[self.protocol class] canInitWithRequest:urlSchemeTask.request]) {
        NSURL * url = urlSchemeTask.request.URL;
        
        if([[self.protocol class] checkCryptFile:url]) {
            NSError* error;
            
            NSString * startPath = [[NSBundle mainBundle] pathForResource:((CDVViewController *) self.viewController).wwwFolderName ofType: nil];
            NSString * filePath = [startPath stringByAppendingString:url.path];
            
            NSString* content = [[NSString alloc] initWithContentsOfFile:filePath encoding:NSUTF8StringEncoding error:&error];
            if (!error) {
                NSData* data = [self.protocol decryptContent:content];
                
                NSString * length = [NSString stringWithFormat:@"%lu", (unsigned long) [data length]];
                NSString * mimeType = [self.protocol getMimeTypeFromPath:url.path];
                NSDictionary * headersDict = [NSDictionary dictionaryWithObjectsAndKeys:length, @"Content-Length", mimeType, @"Content-Type", nil];
                
                NSHTTPURLResponse * response = [[NSHTTPURLResponse alloc] initWithURL:[urlSchemeTask.request URL] statusCode:200 HTTPVersion:@"1.1" headerFields:headersDict];
                
                // Do not use urlSchemeTask if it has been closed in stopURLSchemeTask. Otherwise the app will crash.
                @try {
                    if(self.stoppedTasks == nil || ![self.stoppedTasks containsObject:urlSchemeTask]) {
                        [urlSchemeTask didReceiveResponse:response];
                        [urlSchemeTask didReceiveData:data];
                        [urlSchemeTask didFinish];
                    } else {
                        NSLog(@"CDVCrypt Task stopped %@", startPath);
                    }
                } @catch (NSException *exception) {
                    NSLog(@"CDVCrypt send response exception: %@", exception.debugDescription);
                } @finally {
                    // Cleanup
                    [self.stoppedTasks removeObject:urlSchemeTask];
                }
            }

            return  YES;
        }
    }

    return NO;
}

- (void) stopSchemeTask: (id <WKURLSchemeTask>)urlSchemeTask {
    NSLog(@"Stop CDVCrypt %@", urlSchemeTask.debugDescription);
    [self.stoppedTasks addObject:urlSchemeTask];
}
#endif

@end
