package com.XECUREVoIP.chat.ChatUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

public abstract class CryptoHelper {
    /** Nombre de curva ek&iacute;ptica. */
    public enum EcCurve {

        /** BrainpoolP256r1. */
        BRAINPOOL_P256_R1("brainpoolp256r1"); //$NON-NLS-1$

        private final String name;
        private EcCurve(final String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /** Algoritmo de huella digital. */
    public enum DigestAlgorithm {

        /** SHA-1. */
        SHA1("SHA1"), //$NON-NLS-1$

        /** SHA-256. */
        SHA256("SHA-256"), //$NON-NLS-1$

        /** SHA-384. */
        SHA384("SHA-384"), //$NON-NLS-1$

        /** SHA-512. */
        SHA512("SHA-512"); //$NON-NLS-1$

        private final String name;
        private DigestAlgorithm(final String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static final int PKCS1_LEN_1024 = 128;
    private static final int PKCS1_LEN_2048 = 256;
    private static final byte PKCS1_BLOCK_TYPE = (byte) 0x01;
    private static final byte PKCS1_FILL = (byte) 0xff;
    private static final byte PKCS1_DELIMIT = (byte) 0x00;

    /** A&ntilde;ade relleno PKCS#1 para operaciones con clave privada.
     * @param in Datos a los que se quiere a&ntilde;adir relleno PKCS#1.
     * @param keySize Tama&ntilde;o de la clave privada que operar&aacute; posteriormente con estos datos con relleno.
     * @return Datos con el relleno PKCS#1 a&ntilde;adido.
     * @throws IOException En caso de error el el tratamiento de datos. */
    public final static byte[] addPkcs1PaddingForPrivateKeyOperation(final byte[] in, final int keySize) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("Los datos de entrada no pueden ser nulos"); //$NON-NLS-1$
        }
        if (keySize != 1024 && keySize != 2048) {
            throw new IllegalArgumentException("Solo se soportan claves de 1024 o 2048 bits, y se ha indicado " + keySize); //$NON-NLS-1$
        }
        final int len = keySize == 1024 ? PKCS1_LEN_1024 : PKCS1_LEN_2048;
        if (in.length > len - 3) {
            throw new IllegalArgumentException(
                    "Los datos son demasiado grandes para el valor de clave indicado: " + in.length + " > " + len + "-3" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            );
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
        baos.write(PKCS1_DELIMIT);    // Delimitador :   00
        baos.write(PKCS1_BLOCK_TYPE); // Tipo de bloque: 01
        while (baos.size() < len - (1 + in.length)) { // Se rellena hasta dejar sitio justo para un delimitador y los datos
            baos.write(PKCS1_FILL);
        }
        baos.write(PKCS1_DELIMIT);    // Delimitador :   00
        baos.write(in);               // Datos

        return baos.toByteArray();
    }

    /** Realiza una huella digital de los datos proporcionados.
     * @param algorithm Algoritmo de huella digital que debe utilizarse.
     * @param data Datos de entrada.
     * @return Huella digital de los datos.
     * @throws IOException Si ocurre alg&uacute;n problema generando la huella
     *         digital. */
    public abstract byte[] digest(final DigestAlgorithm algorithm, final byte[] data) throws IOException;

    /** Encripta datos mediante Triple DES (modo CBC sin relleno) y con un
     * salto de (IV) de 8 bytes a cero. Si se le indica una clave de 24 bytes,
     * la utilizar&aacute;a tal cual. Si se le indica una clave de 16 bytes,
     * duplicar&aacute; los 8 primeros y los agregar&aacute; al final para
     * obtener una de 24.
     * @param data Datos a encriptar.
     * @param key Clave 3DES de cifrado.
     * @return Datos cifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         encriptado. */
    public abstract byte[] desedeEncrypt(final byte[] data, final byte[] key) throws IOException;

    /** Desencripta datos mediante Triple DES (modo CBC sin relleno) y con un
     * salto de (IV) de 8 bytes a cero. Si se le indica una clave de 24 bytes,
     * la utilizar&aacute;a tal cual. Si se le indica una clave de 16 bytes,
     * duplicar&aacute; los 8 primeros y los agregar&aacute; al final para obtener una de 24.
     * @param data Datos a desencriptar.
     * @param key Clave 3DES de descifrado.
     * @return Datos descifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         desencriptado. */
    public abstract byte[] desedeDecrypt(final byte[] data, final byte[] key) throws IOException;

    /** Encripta datos mediante DES (modo ECB sin relleno).
     * @param data Datos a encriptar.
     * @param key Clave DES de cifrado.
     * @return Datos cifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         encriptado. */
    public abstract byte[] desEncrypt(final byte[] data, final byte[] key) throws IOException;

    /** Desencripta datos mediante DES (modo ECB sin relleno).
     * @param data Datos a desencriptar.
     * @param key Clave DES de descifrado.
     * @return Datos descifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         desencriptado. */
    public abstract byte[] desDecrypt(final byte[] data, final byte[] key) throws IOException;

    /** Desencripta datos mediante AES (modo CBC sin relleno).
     * @param data Datos a encriptar.
     * @param iv Vector de inicializaci&oacute;n. Si se proporciona <code>null</code> se usar&aacute;
     *           un vector con valores aleatorios.
     * @param key Clave AES de cifrado.
     * @return Datos cifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         encriptado. */
    public abstract byte[] aesDecrypt(final byte[] data, final byte[] iv, final byte[] key) throws IOException;

    /** Encripta datos mediante AES (modo CBC sin relleno).
     * @param data Datos a encriptar.
     * @param iv Vector de inicializaci&oacute;n. Si se proporciona <code>null</code> se usar&aacute;
     *           un vector con valores aleatorios.
     * @param key Clave AES de cifrado.
     * @return Datos cifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         encriptado. */
    public abstract byte[] aesEncrypt(final byte[] data, final byte[] iv, final byte[] key) throws IOException;

    /** Desencripta datos mediante RSA.
     * @param cipheredData Datos a desencriptar.
     * @param key Clava RSA de descifrado.
     * @return Datos descifrados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         desencriptado. */
    public abstract byte[] rsaDecrypt(final byte[] cipheredData, final Key key) throws IOException;

    /** Encripta datos mediante RSA.
     * @param data Datos a encriptar.
     * @param key Clava RSA de cifrado.
     * @return Datos encriptados.
     * @throws IOException Si ocurre alg&uacute;n problema durante el
     *         encriptado. */
    public abstract byte[] rsaEncrypt(final byte[] data, final Key key) throws IOException;

    /** Genera un certificado del tipo indicado a partir de su codificaci&oacute;n.
     * @param encode Codificaci&oacute;n del certificado.
     * @return Certificado generado.
     * @throws CertificateException Si ocurre alg&uacute;n problema durante la
     *         generaci&oacute;n. */
    public abstract Certificate generateCertificate(byte[] encode) throws CertificateException;

    /** Genera un aleatorio contenido en un array de bytes.
     * @param numBytes N&uacute;mero de bytes aleatorios que generar.
     * @return Array de bytes aleatorios.
     * @throws IOException Si ocurre alg&uacute;n problema durante la
     *         generaci&oacute;n del aleatorio. */
    public abstract byte[] generateRandomBytes(int numBytes) throws IOException;

    /** Genera un par de claves de tipo curva el&iacute;ptica.
     * @param curveName Tipo de curva el&iacute;ptica a utilizar.
     * @return Par de claves generadas.
     * @throws NoSuchAlgorithmException Si el sistema no soporta la generaci&oacute;n de curvas el&iacute;pticas.
     * @throws InvalidAlgorithmParameterException Si el sistema no soporta el tipo de curva el&iacute;ptica indicada. */
    public abstract KeyPair generateEcKeyPair(final EcCurve curveName) throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException;

    /** Realiza un CMAC con AES.
     * @param data Datos (deben estar ya con el relleno adecuado).
     * @param key Clave AES.
     * @return CMAC
     * @throws NoSuchAlgorithmException Si no se encuentra un proveedor que permita realizar
     *                                  CMAC con AES.
     * @throws InvalidKeyException Si la clave proporcionada no es una clave AES v&aacute;lida. */
    public abstract byte[] doAesCmac(final byte[] data, final byte[] key) throws NoSuchAlgorithmException, InvalidKeyException;

    /** Realiza un acuerdo de claves Diffie Hellman con algoritmo de curva el&iacute;ptica.
     * @param privateKey Clave privada.
     * @param publicKey Clave p&uacute;blica.
     * @return Resultado de acuerdo de claves.
     * @throws NoSuchAlgorithmException Si no hay ning&uacute;n proveedor en el sistema que soporte el
     *                                  algoritmo <i>ECDH</i>.
     * @throws InvalidKeySpecException Si alguna de las claves es inv&aacute;lida.
     * @throws InvalidKeyException Si alguna de las claves es inv&aacute;lida. */
    public abstract byte[] doEcDh(Key privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException;

    /** Obtiene un punto en una curva el&iacute;ptica.
     * @param nonceS Aleatorio de un solo uso.
     * @param sharedSecretH Secreto compartido.
     * @param curveName Nombre de la curva.
     * @return Punto encapsulado. */
    public abstract AlgorithmParameterSpec getEcPoint(byte[] nonceS, byte[] sharedSecretH, final EcCurve curveName);
}
