package com.XECUREVoIP.security;

import android.os.Build;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {
    private String AES = "AES";
    private String m_alias = "xecurealias";

    public SecurityUtils(String strAlias){
        m_alias = strAlias;
    }

    public String decrypt(String outputString) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decodeValue = Base64.decode(outputString, Base64.DEFAULT);
        byte[] decValue = c.doFinal(decodeValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    public String encrypt(String Data) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(Data.getBytes());
        String encrypteValue= Base64.encodeToString(encVal, Base64.DEFAULT);
        return encrypteValue;
    }

    private SecretKeySpec generateKey() throws Exception {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = m_alias.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();
        SecretKeySpec secretKaySpec = new SecretKeySpec(key, "AES");

        return secretKaySpec;
    }
}
