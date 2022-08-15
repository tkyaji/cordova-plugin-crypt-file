//
//  CDVCryptURLProtocol.h
//  CordovaLib
//
//  Created by tkyaji on 2015/07/15.
//
//

#if __has_include(<Cordova/CDVURLProtocol.h>)
#import <Cordova/CDVURLProtocol.h>
#define HASCDVUrlProtocol
#endif

#ifdef HASCDVUrlProtocol
@interface CDVCryptURLProtocol : CDVURLProtocol

@end
#else
@interface CDVCryptURLProtocol : NSObject

+ (BOOL)canInitWithRequest:(NSURLRequest*)theRequest;
+ (BOOL)checkCryptFile:(NSURL *)url;

- (NSData*)decryptContent:(NSString *)content;
- (NSString*)getMimeType:(NSURL *)url;
- (NSData *)decryptAES256WithKey:(NSString *)key iv:(NSString *)iv data:(NSString *)base64String;
- (NSString*)getMimeTypeFromPath:(NSString*)fullPath;

@end
#endif
