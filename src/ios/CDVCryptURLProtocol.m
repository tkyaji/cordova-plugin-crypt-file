//
//  CDVCryptURLProtocol.m
//  CordovaLib
//
//  Created by tkyaji on 2015/07/15.
//
//

#import "CDVCryptURLProtocol.h"

#import <MobileCoreServices/MobileCoreServices.h>
#import <CommonCrypto/CommonCryptor.h>
#import <CommonCrypto/CommonDigest.h>


static NSString* const kCryptKey = @"";
static NSString* const kCryptIv = @"";

static int const kIncludeFileLength = 0;
static int const kExcludeFileLength = 0;
static NSString* const kIncludeFiles[] = { };
static NSString* const kExcludeFiles[] = { };


@implementation CDVCryptURLProtocol

+ (BOOL)canInitWithRequest:(NSURLRequest*)theRequest
{
    if ([self checkCryptFile:theRequest.URL]) {
        return YES;
    }
    
    return [super canInitWithRequest:theRequest];
}

- (void)startLoading
{
    NSURL* url = self.request.URL;
    
    if ([[self class] checkCryptFile:url]) {
        NSString *mimeType = [self getMimeType:url];
        
        NSError* error;
        NSString* content = [[NSString alloc] initWithContentsOfFile:url.path encoding:NSUTF8StringEncoding error:&error];
        if (!error) {
            NSData* data = [self decryptAES256WithKey:kCryptKey iv:kCryptIv data:content];
            [self sendResponseWithResponseCode:200 data:data mimeType:mimeType];
        }
    }
    
    [super startLoading];
}

+ (BOOL)checkCryptFile:(NSURL *)url {
    if (![url.scheme isEqual: @"file"]) {
        return NO;
    }

    NSLog(@"%@", url.path);
    
    NSString *wwwPath = [[NSBundle mainBundle].resourcePath stringByAppendingString:@"/www/"];
    NSString *checkPath = [url.path stringByReplacingOccurrencesOfString:wwwPath withString:@""];
    
    if (![self hasMatch:checkPath regexArr:kIncludeFiles length:kIncludeFileLength]) {
        return NO;
    }
    if ([self hasMatch:checkPath regexArr:kExcludeFiles length:kExcludeFileLength]) {
        return NO;
    }

    return YES;
}

+ (BOOL)hasMatch:(NSString *)text regexArr:(NSString* const [])regexArr length:(int)length {
    for (int i = 0; i < length; i++) {
        NSString* const regex = regexArr[i];
        if ([self isMatch:text pattern:regex]) {
            return YES;
        }
    }
    return NO;
}

+ (BOOL)isMatch:(NSString *)text pattern:(NSString *)pattern {
    NSError *error = nil;
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:pattern options:0 error:&error];
    if (error) {
        return NO;
    }
    if ([regex firstMatchInString:text options:0 range:NSMakeRange(0, text.length)]) {
        return YES;
    }
    return NO;
}

- (NSString*)getMimeType:(NSURL *)url
{
    NSString *fullPath = url.path;
    NSString *mimeType = nil;
    
    if (fullPath) {
        CFStringRef typeId = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (__bridge CFStringRef)[fullPath pathExtension], NULL);
        if (typeId) {
            mimeType = (__bridge_transfer NSString*)UTTypeCopyPreferredTagWithClass(typeId, kUTTagClassMIMEType);
            if (!mimeType) {
                // special case for m4a
                if ([(__bridge NSString*)typeId rangeOfString : @"m4a-audio"].location != NSNotFound) {
                    mimeType = @"audio/mp4";
                } else if ([[fullPath pathExtension] rangeOfString:@"wav"].location != NSNotFound) {
                    mimeType = @"audio/wav";
                } else if ([[fullPath pathExtension] rangeOfString:@"css"].location != NSNotFound) {
                    mimeType = @"text/css";
                }
            }
            CFRelease(typeId);
        }
    }
    return mimeType;
}

- (NSData *)decryptAES256WithKey:(NSString *)key iv:(NSString *)iv data:(NSString *)base64String {
    
    NSData *data = [[NSData alloc] initWithBase64EncodedString:base64String options:0];
    
    size_t bufferSize = [data length] + kCCBlockSizeAES128;
    void *buffer = malloc(bufferSize);
    size_t numBytesDecrypted = 0;
    
    NSData *keyData = [key dataUsingEncoding:NSUTF8StringEncoding];
    NSData *ivData = [iv dataUsingEncoding:NSUTF8StringEncoding];
    
    CCCryptorStatus status = CCCrypt(kCCDecrypt,
                                     kCCAlgorithmAES128,
                                     kCCOptionPKCS7Padding,
                                     keyData.bytes,
                                     kCCKeySizeAES256,
                                     ivData.bytes,
                                     data.bytes,
                                     data.length,
                                     buffer,
                                     bufferSize,
                                     &numBytesDecrypted);
    
    if (status == kCCSuccess) {
        return [NSData dataWithBytes:buffer length:numBytesDecrypted];
    }
    free(buffer);
    
    return nil;
}

- (NSString*)getMimeTypeFromPath:(NSString*)fullPath
{
    NSString* mimeType = nil;
    
    if (fullPath) {
        CFStringRef typeId = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, (__bridge CFStringRef)[fullPath pathExtension], NULL);
        if (typeId) {
            mimeType = (__bridge_transfer NSString*)UTTypeCopyPreferredTagWithClass(typeId, kUTTagClassMIMEType);
            if (!mimeType) {
                // special case for m4a
                if ([(__bridge NSString*)typeId rangeOfString : @"m4a-audio"].location != NSNotFound) {
                    mimeType = @"audio/mp4";
                } else if ([[fullPath pathExtension] rangeOfString:@"wav"].location != NSNotFound) {
                    mimeType = @"audio/wav";
                } else if ([[fullPath pathExtension] rangeOfString:@"css"].location != NSNotFound) {
                    mimeType = @"text/css";
                }
            }
            CFRelease(typeId);
        }
    }
    return mimeType;
}

- (void)sendResponseWithResponseCode:(NSInteger)statusCode data:(NSData*)data mimeType:(NSString*)mimeType
{
    if (mimeType == nil) {
        mimeType = @"text/plain";
    }
    
    NSHTTPURLResponse* response = [[NSHTTPURLResponse alloc] initWithURL:[[self request] URL] statusCode:statusCode HTTPVersion:@"HTTP/1.1" headerFields:@{@"Content-Type" : mimeType}];
    
    [[self client] URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
    if (data != nil) {
        [[self client] URLProtocol:self didLoadData:data];
    }
    [[self client] URLProtocolDidFinishLoading:self];
}


@end
