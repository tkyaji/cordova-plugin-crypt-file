package com.qhng.cordova;

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
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;


public class DecryptResourceNG extends CordovaPlugin {

    private static final String TAG = "DecryptResourceNG";

    private static final String PUBLIC_PEM = "";
    private static final String _CRYPT_KEY = "";
    private static final String _CRYPT_IV = "";
    private static final String[] INCLUDE_FILES = new String[] { };
    private static final String[] EXCLUDE_FILES = new String[] { };
    private final String CRYPT_KEY;
    private final String CRYPT_IV;

    public DecryptResourceNG() throws Exception {
        PublicKey pubKey = PublicKeyReader.get(PUBLIC_PEM);
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.DECRYPT_MODE, pubKey);
        CRYPT_KEY = new String(rsa.doFinal(Base64.decode(_CRYPT_KEY, Base64.DEFAULT)));
        CRYPT_IV = new String(rsa.doFinal(Base64.decode(_CRYPT_IV, Base64.DEFAULT)));
    }

    @Override
    public Uri remapUri(Uri uri) {
        if (uri.toString().indexOf("/+++/") > -1) {
            return this.toPluginUri(uri);
        } else {
            return uri;
        }
    }

    @Override
    public CordovaResourceApi.OpenForReadResult handleOpenForRead(Uri uri) throws IOException {
        Uri oriUri = this.fromPluginUri(uri);
        String uriStr = oriUri.toString().replace("/+++/", "/").split("\\?")[0];

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
}

class PublicKeyReader {
    public static PublicKey get(String publicPemStr) throws Exception {
        byte[] keyBytes = Base64.decode(publicPemStr, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
