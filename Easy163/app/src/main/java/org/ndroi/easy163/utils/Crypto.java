package org.ndroi.easy163.utils;

import android.annotation.SuppressLint;
import com.alibaba.fastjson.JSONObject;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by andro on 2020/5/3.
 */
public class Crypto {

  private static String aes_key = "e82ckenh8dichen8";
  private static SecretKeySpec key = new SecretKeySpec(aes_key.getBytes(), "AES");
  private static Cipher decryptCipher = null;
  private static Cipher encryptCipher = null;

  static {
    try {
      decryptCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
      decryptCipher.init(Cipher.DECRYPT_MODE, key);
      encryptCipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, key);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
      e.printStackTrace();
    }
  }

  public static byte[] aesDecrypt(byte[] bytes) {
    byte[] result = null;

    try {
      result = decryptCipher.doFinal(bytes);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static byte[] aesEncrypt(byte[] bytes) {
    byte[] result = null;

    try {
      result = encryptCipher.doFinal(bytes);
    } catch (BadPaddingException | IllegalBlockSizeException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static byte[] hexStringToByteArray(String hexString) {
    int len = hexString.length();
    byte[] bytes = new byte[len / 2];

    for (int i = 0; i < len; i += 2) {
      bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) +
          Character.digit(hexString.charAt(i + 1), 16));
    }
    return bytes;
  }

  private static String ByteArrayToHexString(byte[] bytes) {
    StringBuilder hexStr = new StringBuilder();

    for (byte aByte : bytes) {
      String hex = Integer.toHexString(aByte & 0xFF).toUpperCase();

      if (hex.length() == 1) {
        hex = "0" + hex;
      }
      hexStr.append(hex);
    }
    return hexStr.toString();
  }

  public static class Request {

    public String path;
    public JSONObject json;
  }

  public static Request decryptRequestBody(String body) {
    Request request = new Request();
    byte[] encryptedBytes = hexStringToByteArray(body.substring(7));
    byte[] rawBytes = aesDecrypt(encryptedBytes);
    String text = new String(rawBytes);
    String[] parts = text.split("-36cd479b6b5-");
    request.path = parts[0];
    request.json = JSONObject.parseObject(parts[1]);
    return request;
  }

  public static String encryptRequestBody(Request request) {
    String jsonText = request.json.toString();
    String message = "nobody" + request.path + "use" + jsonText + "md5forencrypt";
    String digest = "";

    try {
      MessageDigest messageDigest = MessageDigest.getInstance("md5");
      messageDigest.update(message.getBytes());

      for (byte b : messageDigest.digest()) {
        String temp = Integer.toHexString(b & 0xff);

        if (temp.length() == 1) {
          temp = "0" + temp;
        }
        digest += temp;
      }
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    String text = request.path + "-36cd479b6b5-" + jsonText + "-36cd479b6b5-" + digest;
    String body = ByteArrayToHexString(aesEncrypt(text.getBytes()));
    body = "params=" + body;
    return body;
  }
}
