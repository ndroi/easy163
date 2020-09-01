package org.ndroi.easy163.providers.utils;

import android.util.Base64;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by andro on 2020/5/6.
 */
public class MiguCrypto {

  private static String password = "00000000000000000000000000000000";
  private static String salt = "00000000";
  private static String skeyB64 = "OMYm0ulbQZgEd21abq1wQI7CnLeAY5CT4RPRLBmAzSUdBWgPHq3n" +
      "KTYBNJe9EJMMs2l2aOKPHQCl05QDDfO4wJpzwwL4IFag5u%2FAWY81MZ6SJJpD1gUEw6fVqENIQowg" +
      "0bSjZwkY61kY0EIvDNsEZ9TbqFCiy25RXb%2BaLWgcRGE%3D";
  private static Cipher aesCipher = null;

  private static void initAes() {
    int keySize = 256 / 8;
    int ivSize = 16;
    int repeat = (keySize + ivSize) / 16;
    List<byte[]> byteList = new ArrayList<>();
    byte[] ps = (password + salt).getBytes();

    try {
      MessageDigest messageDigest = MessageDigest.getInstance("md5");

      for (int i = 0; i < repeat; i++) {
        if (byteList.isEmpty()) {
          messageDigest.update(ps);
        } else {
          byte[] last = byteList.get(byteList.size() - 1);
          byte[] buffer = new byte[last.length + ps.length];
          System.arraycopy(last, 0, buffer, 0, last.length);
          System.arraycopy(ps, 0, buffer, last.length, ps.length);
          messageDigest.update(buffer);
        }
        byteList.add(messageDigest.digest());
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    byte[] skey = new byte[16 * 2];
    System.arraycopy(byteList.get(0), 0, skey, 0, 16);
    System.arraycopy(byteList.get(1), 0, skey, 16, 16);
    byte[] sIv = byteList.get(2);
    SecretKeySpec keySpec = new SecretKeySpec(skey, "AES");

    try {
      aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(sIv));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  public static String Encrypt(String text) {
    if (aesCipher == null) {
      initAes();
    }

    String result = null;

    try {
      byte[] header = ("Salted__" + salt).getBytes();
      byte[] aseEnc = aesCipher.doFinal(text.getBytes());
      byte[] data = new byte[header.length + aseEnc.length];
      System.arraycopy(header, 0, data, 0, header.length);
      System.arraycopy(aseEnc, 0, data, header.length, aseEnc.length);
      String dataB64 = Base64.encodeToString(data, Base64.NO_WRAP);
      result = "data=" + URLEncoder.encode(dataB64, "UTF-8") + "&secKey=" + skeyB64;
    } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
      return result;
  }
}