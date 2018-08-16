package com.lsid.util;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import sun.misc.BASE64Encoder;

@SuppressWarnings("restriction")
public class Codeactive {
	public static void main(String[] s) throws Exception{
		Form f = Form.form();
		byte[] raw = DefaultCipher.dec("a8SjXEF%2B5l9HR80Za%2Fzb%2FVM8zAbzeV9lttzTcKQ%2FIbw%3D").getBytes("utf-8");
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		String line = "line0";
		String batch = "batch0";
		String prod = "prod0";
		String eid="t1";
		String apikey = DefaultCipher.dec("3GYmNwr6sjodinms6%2FoKcWbFyxtQusBvOL4OoWIjrOk5pBaQsPQYzIHw%2B%2FskjTyU");
		Vector<String> code = new Vector<String>();
		code.add("http://0k6.cn/"+eid+"/testactive201808141350");
		for (String co : code) {
			String data = co + "#"+line+"#"+batch+"#"+prod+"#" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			String encline = new BASE64Encoder().encode(cipher.doFinal(data.getBytes("UTF-8")));
			f.add("data", encline);
		}
		String params = "user=" + eid + "&active=a5&t=" + System.currentTimeMillis();
		String sign = sign(params + "&key=" + apikey);
		String res = Request.Post("http://ha0q.cn/17newlscode?" + params + "&sign=" + sign).bodyForm(f.build())
				.execute().returnContent().asString();
		System.out.println(res);
	}
	public static void main2(String[] s) throws Exception{
		
		String eid=s[0];
		String apikey = DefaultCipher.dec(s[1]);
		String encryptsecret = DefaultCipher.dec(s[2]);
		String line = s[3];
		String batch = s[4];
		String prod = s[5];
		Files.createDirectories(Paths.get("logs"));
		Path logfile = Paths.get("logs/log."+eid+"."+prod+"."+System.currentTimeMillis());
		List<String> code=new ArrayList<String>(5000);
		for (int c = 6; c<s.length;c++){
			Files.write(logfile,("activating "+s[c]+System.lineSeparator()).getBytes("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			
			List<String> all = Files.readAllLines(Paths.get("ready/"+eid+"/"+s[c]), Charset.forName("UTF-8"));
			for (int i=0;i<all.size();i++){
				code.add(all.get(i));
				if (code.size()==5000){
					lscode(logfile, eid, apikey, encryptsecret, line, batch, prod, code);
					code.clear();
					Thread.sleep(3000);
					Files.write(logfile,(i+" done"+System.lineSeparator()).getBytes("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				}
			}
			if (!code.isEmpty()){
				lscode(logfile, eid, apikey, encryptsecret, line, batch, prod, code);
				code.clear();
				Thread.sleep(3000);
				Files.write(logfile,(all.size()+" done"+System.lineSeparator()).getBytes("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			
			Files.write(logfile,(s[c]+" done"+System.lineSeparator()).getBytes("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			
		}
		
	}

	@SuppressWarnings("restriction")
	public static void lscode(Path logfile, String eid, String apikey, String encryptsecret, String line, String batch, String prod, List<String> code) throws Exception {
		Form f = Form.form();
		byte[] raw = encryptsecret.getBytes("utf-8");
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		for (String co : code) {
			String data = co + "#"+line+"#"+batch+"#"+prod+"#" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			String encline = new BASE64Encoder().encode(cipher.doFinal(data.getBytes("UTF-8")));
			f.add("data", encline);
		}
		String params = "user=" + eid + "&active=a5&t=" + System.currentTimeMillis();
		String sign = sign(params + "&key=" + apikey);
		String res = Request.Post("http://ha0q.cn/17newlscode?" + params + "&sign=" + sign).bodyForm(f.build())
				.execute().returnContent().asString();
		Files.write(logfile, (res+System.lineSeparator()).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	private static String sign(String raw) {
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

	private static String[] HexCode = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e",
			"f" };

	public static String byteToHexString(byte b) {
		int n = b;
		if (n < 0) {
			n = 256 + n;
		}
		int d1 = n / 16;
		int d2 = n % 16;
		return HexCode[d1] + HexCode[d2];
	}

	public static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result = result + byteToHexString(b[i]);
		}
		return result;
	}
}
