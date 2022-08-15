package com.tkyaji.cordova;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebViewAssetLoader;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptCordovaPathHandler implements WebViewAssetLoader.PathHandler {
    private static final String TAG = "DecryptResource";

    public CordovaWebView webView;
    public CordovaInterface cordova;

    private final String[] INCLUDE_FILES;
    private final String[] EXCLUDE_FILES;
    private final String CRYPT_KEY;
    private final String CRYPT_IV;

    public DecryptCordovaPathHandler(CordovaWebView webView,
                                     CordovaInterface cordova,
                                     String[] INCLUDE_FILES, String[] EXCLUDE_FILES,
                                     String CRYPT_KEY, String CRYPT_IV) {
        this.webView = webView;
        this.cordova = cordova;
        this.INCLUDE_FILES = INCLUDE_FILES;
        this.EXCLUDE_FILES = EXCLUDE_FILES;
        this.CRYPT_KEY = CRYPT_KEY;
        this.CRYPT_IV = CRYPT_IV;
    }

    private boolean isCryptFiles(String uri) {
        String checkPath = uri.replace("file:///android_asset/www/", "");
        if (!this.hasMatch(checkPath, INCLUDE_FILES)) {
            return false;
        }
        if (this.hasMatch(checkPath, EXCLUDE_FILES)) {
            return false;
        }
        return true;
    }

    private boolean hasMatch(String text, String[] regexArr) {
        for (String regex : regexArr) {
            if (Pattern.compile(regex).matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public WebResourceResponse handle(@NonNull String path) {
        if (path.startsWith("+++/")) {
            String newPath = "www/" + path.replace("+++/", "").split("\\?")[0];

            try {
                CordovaResourceApi resourceApi = this.webView.getResourceApi();
                String mimeType = resourceApi.getMimeType(Uri.parse("file://" + newPath));
                InputStream is = webView.getContext().getAssets().open(newPath, AssetManager.ACCESS_STREAMING);

                if (!isCryptFiles(path)) {
                    return new WebResourceResponse(mimeType, null, is);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder strb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    strb.append(line);
                }
                br.close();

                byte[] bytes = Base64.decode(strb.toString(), Base64.DEFAULT);

                LOG.d(TAG, "decrypt: " + newPath);
                ByteArrayInputStream byteInputStream = null;
                try {
                    SecretKey skey = new SecretKeySpec(CRYPT_KEY.getBytes("UTF-8"), "AES");
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, skey, new IvParameterSpec(CRYPT_IV.getBytes("UTF-8")));

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bos.write(cipher.doFinal(bytes));
                    byteInputStream = new ByteArrayInputStream(bos.toByteArray());
                } catch (Exception ex) {
                    LOG.e(TAG, ex.getMessage());
                }

                return new WebResourceResponse(mimeType, null, byteInputStream);
            }
            catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        } else {
            return null;
        }

        return null;
    }
}
