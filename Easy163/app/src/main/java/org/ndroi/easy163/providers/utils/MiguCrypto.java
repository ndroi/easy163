package org.ndroi.easy163.providers.utils;

import android.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andro on 2020/5/6.
 */
public class MiguCrypto
{
    private static String pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBg" +
            "QC8asrfSaoOb4je+DSmKdriQJKWVJ2oDZrs3wi5W67m3LwTB9QVR+cE3XWU21Nx+YBxS0yu" +
            "n8wDcjgQvYt625ZCcgin2ro/eOkNyUOTBIbuj9CvMnhUYiR61lC1f1IGbrSYYimqBVSjpifVu" +
            "fxtx/I3exReZosTByYp4Xwpb1+WAQIDAQAB";
    private static String password = "00000000000000000000000000000000";
    private static String salt = "00000000";
    private static String skeyB64 = "OMYm0ulbQZgEd21abq1wQI7CnLeAY5CT4RPRLBmAzSUdBWgPHq3n" +
            "KTYBNJe9EJMMs2l2aOKPHQCl05QDDfO4wJpzwwL4IFag5u%2FAWY81MZ6SJJpD1gUEw6fVqENIQowg" +
            "0bSjZwkY61kY0EIvDNsEZ9TbqFCiy25RXb%2BaLWgcRGE%3D";
    private static Cipher aesCipher = null;

    private static void initAes()
    {
        int keySize = 256 / 8;
        int ivSize = 16;
        int repeat = (keySize + ivSize) / 16;
        List<byte[]> byteList = new ArrayList<>();
        byte[] ps = (password + salt).getBytes();
        try
        {
            MessageDigest messageDigest = MessageDigest.getInstance("md5");
            for (int i = 0; i < repeat; i++)
            {
                if(byteList.isEmpty())
                {
                    messageDigest.update(ps);
                    byteList.add(messageDigest.digest());
                }else
                {
                    byte[] last = byteList.get(byteList.size() - 1);
                    byte[] buffer = new byte[last.length + ps.length];
                    System.arraycopy(last, 0, buffer,0, last.length);
                    System.arraycopy(ps, 0, buffer,last.length, ps.length);
                    messageDigest.update(buffer);
                    byteList.add(messageDigest.digest());
                }
            }
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        byte[] skey = new byte[16*2];
        System.arraycopy(byteList.get(0), 0, skey,0, 16);
        System.arraycopy(byteList.get(1), 0, skey,16, 16);
        byte[] sIv = byteList.get(2);
        SecretKeySpec keySpec = new SecretKeySpec(skey, "AES");
        try
        {
            aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(sIv));
        } catch (InvalidKeyException e)
        {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
        } catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public static String Encrypt(String text)
    {
        if(aesCipher == null)
        {
            initAes();
        }
        String result = null;
        try
        {
            byte[] header = ("Salted__" + salt).getBytes();
            byte[] aseEnc = aesCipher.doFinal(text.getBytes());
            byte[] data = new byte[header.length + aseEnc.length];
            System.arraycopy(header, 0, data, 0, header.length);
            System.arraycopy(aseEnc, 0, data, header.length, aseEnc.length);
            String dataB64 = Base64.encodeToString(data, Base64.NO_WRAP);
            result = "data=" + URLEncoder.encode(dataB64, "UTF-8") + "&secKey=" + skeyB64;
        } catch (IllegalBlockSizeException e)
        {
            e.printStackTrace();
        } catch (BadPaddingException e)
        {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        return result;
    }
}