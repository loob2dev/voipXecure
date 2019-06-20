package com.XECUREVoIP.chat.ChatUtils;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.math.ec.ECPoint;


public class ECDH_BC {
    private PrivateKey privateKey;
    private PublicKey  publicKey;
    private PublicKey  receivedPublicKey;
    private byte[]     secretKey;

    public ECDH_BC() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpgen = KeyPairGenerator.getInstance("ECDH", "BC");
        kpgen.initialize(new ECGenParameterSpec("prime192v1"), new SecureRandom());
        KeyPair pair = kpgen.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();
    }

    public byte [] getPublicKey () throws Exception
    {
        //return key.getEncoded();

        ECPublicKey eckey = (ECPublicKey)publicKey;
        return eckey.getQ().getEncoded(true);
    }

    public void loadPublicKey (byte [] data) throws Exception
    {
		/*KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
		return kf.generatePublic(new X509EncodedKeySpec(data));*/

        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");
        ECPublicKeySpec pubKey = new ECPublicKeySpec(
                params.getCurve().decodePoint(data), params);
        KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        publicKey = kf.generatePublic(pubKey);
    }

    public byte [] getPrivateKey () throws Exception
    {
        //return key.getEncoded();

        ECPrivateKey eckey = (ECPrivateKey)privateKey;
        return eckey.getD().toByteArray();
    }

    public void loadPrivateKey (byte [] data) throws Exception
    {
        //KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        //return kf.generatePrivate(new PKCS8EncodedKeySpec(data));

        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime192v1");
        ECPrivateKeySpec prvkey = new ECPrivateKeySpec(new BigInteger(data), params);
        KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        privateKey = kf.generatePrivate(prvkey);
    }

    public void doECDH () throws Exception
    {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        secretKey = ka.generateSecret();
    }

    public String encrypt(final String message) {
        String encryptedMessage = null;
        try {
            // You can use Blowfish or another symmetric algorithm but you must adjust the key size.
            final SecretKeySpec keySpec = new SecretKeySpec(secretKey, "DES");
            final Cipher cipher  = Cipher.getInstance("DES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            encryptedMessage = new String(cipher.doFinal(message.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedMessage;
    }

    public String decrypt(final String message) {
        String decryptedMessage = null;
        try {
            // You can use Blowfish or another symmetric algorithm but you must adjust the key size.
            final SecretKeySpec keySpec = new SecretKeySpec(secretKey, "DES");
            final Cipher        cipher  = Cipher.getInstance("DES/ECB/PKCS5Padding");

            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            decryptedMessage = new String(cipher.doFinal(message.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedMessage;
    }
}
