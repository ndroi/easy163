package org.ndroi.easy163.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by andro on 2020/5/3.
 */
public class Crypto
{
    private static String aes_key = "e82ckenh8dichen8";
    private static SecretKeySpec key = new SecretKeySpec(aes_key.getBytes(), "AES");
    private static Cipher decryptCipher = null;
    private static Cipher encryptCipher = null;
    static
    {
        try
        {
            decryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
            encryptCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        } catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
        } catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    public static byte[] aesDecrypt(byte[] bytes)
    {
        byte[] result = null;
        try
        {
            result = decryptCipher.doFinal(bytes);
        } catch (BadPaddingException e)
        {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] aesEncrypt(byte[] bytes)
    {
        byte[] result = null;
        try
        {
            result = encryptCipher.doFinal(bytes);
        } catch (BadPaddingException e)
        {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e)
        {
            e.printStackTrace();
        }
        return result;
    }

}
