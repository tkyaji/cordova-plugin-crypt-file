package com.tkyaji.cordova;

import android.net.Uri;
import android.util.Base64;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.LOG;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class DecryptResource extends CordovaPlugin {

    private static final String TAG = "DecryptResource";

    private static final String URL_PREFIX = "http://localhost/";

    private static final String CRYPT_KEY = "";
    private static final String CRYPT_IV = "";

    private static final String[] CRYPT_FILES = {
        ".htm",
        ".html",
        ".js",
        ".css",
    };

    private String launchUri;

    @Override
    public Uri remapUri(Uri uri) {
        this.launchUri = uri.toString();
        if (!this.launchUri.toString().startsWith(URL_PREFIX)) {
            return uri;
        }
        return Uri.parse("cdvplugin://DecryptResource");
    }

    @Override
    public CordovaResourceApi.OpenForReadResult handleOpenForRead(Uri uri) throws IOException {
        String uriStr = this.tofileUri(this.launchUri);
        CordovaResourceApi.OpenForReadResult readResult =  this.webView.getResourceApi().openForRead(Uri.parse(uriStr), true);

        if (!isCryptFiles(uriStr)) {
            return readResult;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(readResult.inputStream));
        StringBuilder strb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            strb.append(line);
        }
        br.close();

        byte[] bytes = Base64.decode(strb.toString(), Base64.DEFAULT);

        LOG.d(TAG, "decrypt: " + uriStr);
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

        return new CordovaResourceApi.OpenForReadResult(
                readResult.uri, byteInputStream, readResult.mimeType, readResult.length, readResult.assetFd);
    }

    private String tofileUri(String uri) {
        if (uri.startsWith(URL_PREFIX)) {
            uri = uri.replace(URL_PREFIX, "file:///android_asset/www/");
        }
        if (uri.endsWith("/")) {
            uri += "index.html";
        }
        return uri;
    }

    private boolean isCryptFiles(String uri) {
        for (String ext: CRYPT_FILES) {
            if (uri.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
