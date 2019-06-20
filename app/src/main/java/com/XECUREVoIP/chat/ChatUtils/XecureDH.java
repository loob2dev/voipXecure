package com.XECUREVoIP.chat.ChatUtils;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

public class XecureDH {
    public PrivateKey privateKey;
    private PublicKey  publicKey;
    private PublicKey  receivedPublicKey;
    private byte[]     secretKey;

    private static final String ECDH = "ECDH"; //$NON-NLS-1$

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}



    //~ --- [METHODS] --------------------------------------------------------------------------------------------------
    public String encrypt(final String message) {
        String encryptedMessage = null;
        try {
            // You can use Blowfish or another symmetric algorithm but you must adjust the key size.
            final SecretKeySpec keySpec = new SecretKeySpec(secretKey, "DES");
            final Cipher        cipher  = Cipher.getInstance("DES/ECB/PKCS5Padding");

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

    //~ ----------------------------------------------------------------------------------------------------------------

    public void generateCommonSecretKey() {

        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.insertProviderAt(new BouncyCastleProvider(), 1);
            }

            KeyAgreement keyAgreement = null;
            try {
                keyAgreement = KeyAgreement.getInstance(ECDH, BouncyCastleProvider.PROVIDER_NAME);

            }
            catch (final NoSuchProviderException e) {
                keyAgreement = KeyAgreement.getInstance(ECDH);
            }
            keyAgreement.init(privateKey);

            keyAgreement.doPhase(receivedPublicKey, true);

            secretKey = shortenSecretKey(keyAgreement.generateSecret());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    public void generateKeys() {

        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.insertProviderAt(new BouncyCastleProvider(), 1);
            }

            ECGenParameterSpec ecParamSpec = new ECGenParameterSpec("secp224k1");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH",BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(ecParamSpec);

            KeyPair keyPair=kpg.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey  = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    public PublicKey getPublicKey() {

        return publicKey;
    }

    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * In a real life example you must serialize the public key for transferring.
     *
     * @param  publicKey
     */
    public void receivePublicKeyFrom(final PublicKey publicKey) {

        receivedPublicKey = publicKey;
    }



    //~ ----------------------------------------------------------------------------------------------------------------

    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * 1024 bit symmetric key size is so big for DES so we must shorten the key size. You can get first 8 longKey of the
     * byte array or can use a key factory
     *
     * @param   longKey
     *
     * @return
     */
    private byte[] shortenSecretKey(final byte[] longKey) {

        try {

            // Use 8 bytes (64 bits) for DES, 6 bytes (48 bits) for Blowfish
            final byte[] shortenedKey = new byte[8];

            System.arraycopy(longKey, 0, shortenedKey, 0, shortenedKey.length);

            return shortenedKey;

            // Below lines can be more secure
            // final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            // final DESKeySpec       desSpec    = new DESKeySpec(longKey);
            //
            // return keyFactory.generateSecret(desSpec).getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
