package com.lsid.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class DefaultCipher {
	private static Map<String, Cipher> decipher = new HashMap<String, Cipher>();
	private static Map<String, Cipher> encipher = new HashMap<String, Cipher>();

	private static Cipher init(boolean enc, String key) throws Exception{
        byte[] raw = key.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        if (enc){
        	Cipher encipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            encipher.init(Cipher.ENCRYPT_MODE, skeySpec);
	        return encipher;
        } else {
        	Cipher decipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            decipher.init(Cipher.DECRYPT_MODE, skeySpec);
	    	return decipher;
        }
	}
	static{
		try {
			decipher.put("default", init(false, "oUm1!Eoi_Y75l@yh"));
			encipher.put("default", init(true, "oUm1!Eoi_Y75l@yh"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static synchronized Cipher getDecipher(String key) throws Exception{
		if (decipher.get(key)==null){
			decipher.put(key, init(false, key));
		}
		return decipher.get(key);
	}
	private static synchronized Cipher getEncipher(String key) throws Exception{
		if (encipher.get(key)==null){
			encipher.put(key, init(true, key));
		}
		return encipher.get(key);
	}
	
	public static String dec(String enc) throws Exception{
		return new String(getDecipher("default").doFinal(new BASE64Decoder().decodeBuffer(URLDecoder.decode(enc, "UTF-8"))),"UTF-8");
	}
	public static String enc(String dec) throws Exception{
		return URLEncoder.encode(new BASE64Encoder().encode(getEncipher("default").doFinal(dec.getBytes("UTF-8"))),"UTF-8");
	}
	public static String dec(String key, String enc) throws Exception{
		return new String(getDecipher(key).doFinal(new BASE64Decoder().decodeBuffer(URLDecoder.decode(enc, "UTF-8"))),"UTF-8");
	}
	public static String dec2(String key, String enc) throws Exception{
		return new String(getDecipher(key).doFinal(new BASE64Decoder().decodeBuffer(enc)),"UTF-8");
	}
	public static String enc(String key, String dec) throws Exception{
		return URLEncoder.encode(new BASE64Encoder().encode(getEncipher(key).doFinal(dec.getBytes("UTF-8"))),"UTF-8");
	}
	public static String enc2(String key, String dec) throws Exception{
		return new BASE64Encoder().encode(getEncipher(key).doFinal(dec.getBytes("UTF-8")));
	}
	public static byte[] enc(String key, byte[] raw) throws Exception{
		return getEncipher(key).doFinal(raw);
	}
	
    public static String sign(String raw){
    	MessageDigest crypt = null;
    	try {
    		crypt = MessageDigest.getInstance("MD5");
    	    crypt.reset();
    	   	crypt.update(raw.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return byteArrayToHexString(crypt.digest());
    }
    
	private static String[] HexCode = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    public static String byteToHexString(byte b)
    {
        int n = b;
        if (n < 0)
        {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HexCode[d1] + HexCode[d2];
    }

    public static String byteArrayToHexString(byte[] b)
    {
        String result = "";
        for (int i = 0; i < b.length; i++)
        {
            result = result + byteToHexString(b[i]);
        }
        return result;
    }

}
