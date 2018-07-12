package com.jedge.hm.zfb.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class Config {
	public static String HUAXIN_AUTHFINISH=null;
	public static String HUAXIN_PARTNERKEY=null;
	public static String HUAXIN_PAYNOTIFY=null;
	public static String HUAXIN_PAYRETURN=null;
	
	public static final String ZFB_AUTHBACK="http://hm.jedge.cn/hmzfb/authback";
	public static final String ZFB_PAYNOTIFY="http://hm.jedge.cn/hmzfb/paynotify";
	public static final String ZFB_PAYRETURN="http://hm.jedge.cn/hmzfb/payreturn";
	
	public static String HUAMEI_ZFBAPPID=null;
	public static String HUAMEI_ZFBPRIKEY=null;
	public static String HUAMEI_ZFBPUBKEY=null;

	static {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						String newAUTHFINISH = new String(Files.readAllBytes(Paths.get("zfbconfig/HUAXIN_AUTHFINISH")),"UTF-8");
						if (HUAXIN_AUTHFINISH==null||!HUAXIN_AUTHFINISH.equals(newAUTHFINISH)) {
							System.out.println(new Date()+" ==== changed HUAXIN_AUTHFINISH from ["+HUAXIN_AUTHFINISH+"] to ["+newAUTHFINISH+"]");
							HUAXIN_AUTHFINISH = newAUTHFINISH;
						}
						
						String newPARTNERKEY = new String(Files.readAllBytes(Paths.get("zfbconfig/HUAXIN_PARTNERKEY")),"UTF-8");
						if (HUAXIN_PARTNERKEY==null||!HUAXIN_PARTNERKEY.equals(newPARTNERKEY)) {
							System.out.println(new Date()+" ==== changed HUAXIN_PARTNERKEY from ["+HUAXIN_PARTNERKEY+"] to ["+newPARTNERKEY+"]");
							HUAXIN_PARTNERKEY = newPARTNERKEY;
						}

						String newPAYNOTIFY = new String(Files.readAllBytes(Paths.get("zfbconfig/HUAXIN_PAYNOTIFY")),"UTF-8");
						if (HUAXIN_PAYNOTIFY==null||!HUAXIN_PAYNOTIFY.equals(newPAYNOTIFY)) {
							System.out.println(new Date()+" ==== changed HUAXIN_PAYNOTIFY from ["+HUAXIN_PAYNOTIFY+"] to ["+newPAYNOTIFY+"]");
							HUAXIN_PAYNOTIFY = newPAYNOTIFY;
						}

						String newPAYRETURN = new String(Files.readAllBytes(Paths.get("zfbconfig/HUAXIN_PAYRETURN")),"UTF-8");
						if (HUAXIN_PAYRETURN==null||!HUAXIN_PAYRETURN.equals(newPAYRETURN)) {
							System.out.println(new Date()+" ==== changed HUAXIN_PAYRETURN from ["+HUAXIN_PAYRETURN+"] to ["+newPAYRETURN+"]");
							HUAXIN_PAYRETURN = newPAYRETURN;
						}

						String newZFBAPPID = new String(Files.readAllBytes(Paths.get("zfbconfig/ZFBAPPID")),"UTF-8");
						if (HUAMEI_ZFBAPPID==null||!HUAMEI_ZFBAPPID.equals(newZFBAPPID)) {
							System.out.println(new Date()+" ==== changed HUAMEI_ZFBAPPID from ["+HUAMEI_ZFBAPPID+"] to ["+newZFBAPPID+"]");
							HUAMEI_ZFBAPPID = newZFBAPPID;
						}
						
						String newZFBPRIKEY = new String(Files.readAllBytes(Paths.get("zfbconfig/ZFBPRIKEY")),"UTF-8");
						if (HUAMEI_ZFBPRIKEY==null||!HUAMEI_ZFBPRIKEY.equals(newZFBPRIKEY)) {
							System.out.println(new Date()+" ==== changed HUAMEI_ZFBPRIKEY from ["+HUAMEI_ZFBPRIKEY+"] to ["+newZFBPRIKEY+"]");
							HUAMEI_ZFBPRIKEY = newZFBPRIKEY;
						}
						
						String newZFBPUBKEY = new String(Files.readAllBytes(Paths.get("zfbconfig/ZFBPUBKEY")),"UTF-8");
						if (HUAMEI_ZFBPUBKEY==null||!HUAMEI_ZFBPUBKEY.equals(newZFBPUBKEY)) {
							System.out.println(new Date()+" ==== changed HUAMEI_ZFBPUBKEY from ["+HUAMEI_ZFBPUBKEY+"] to ["+newZFBPUBKEY+"]");
							HUAMEI_ZFBPUBKEY = newZFBPUBKEY;
						}
						Thread.sleep(20000);
					} catch (Exception e) {
						//do nothing
					}
				}
			}
			
		}).start();
	}
		
	/**
	 * 创建md5摘要,规则是:按参数名称a-z排序,遇到空值的参数不参加签名。 sign
	 */
	public static String createSign(Map<String, Object> data, String partnerKey) {
		Set<String> keySet = data.keySet();
		String[] keyArray = keySet.toArray(new String[keySet.size()]);
		Arrays.sort(keyArray);
		StringBuilder sb = new StringBuilder();
		for (String k : keyArray) {
			if ("sign".equals(k)) {
				continue;
			}
			if (data.get(k) != null) // 参数值为空，则不参与签名
				sb.append(k).append("=").append(data.get(k)).append("&");
		}
		sb.append("key=").append(partnerKey);
		String sign = MD5Encode(sb.toString(), "UTF-8").toUpperCase();
		return sign;
	}
	private static String byteArrayToHexString(byte b[]) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++)
			resultSb.append(byteToHexString(b[i]));

		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n += 256;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	public static String MD5Encode(String origin, String charsetname) {
		String resultString = null;
		try {
			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");
			if (charsetname == null || "".equals(charsetname))
				resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
			else
				resultString = byteArrayToHexString(md.digest(resultString.getBytes(charsetname)));
		} catch (Exception exception) {
		}
		return resultString;
	}

	private static final String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

}
