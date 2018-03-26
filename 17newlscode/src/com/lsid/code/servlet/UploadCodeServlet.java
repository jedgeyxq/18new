package com.lsid.code.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.code.filter.SecureUploadCode;
import com.lsid.code.validator.CodeValidator;
import com.lsid.util.DefaultCipher;

import sun.misc.BASE64Encoder;

@SuppressWarnings("serial")
public class UploadCodeServlet extends HttpServlet {

	
	
	public static Map<String, String> tickets = new Hashtable<String, String>();
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UploadCodeServlet() {
		super();
	}

	@Override
	public void destroy(){
		
	}
	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, String>> fails = new ArrayList<Map<String, String>>(1);
		Map<String, String> fail = new HashMap<String, String>();
		fail.put("reason", "not support get");
		fails.add(fail);
		result.put("result", "fail");
		result.put("successcount", 0);
		result.put("fails", fails);
	
		PrintWriter p = response.getWriter();		
		p.write(new ObjectMapper().writeValueAsString(result));
		p.flush();
		p.close();
		p=null;
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		AutoConfig.iamrunning();
		try{
			List<Map<String, Object>> fails = new ArrayList<Map<String, Object>>(5000);
			int successcount = 0;
			String[] data = request.getParameterValues("data");
			if (data!=null){
				int datalength = data.length;
				if (datalength>5000){
					datalength = 5000;
				}
				for (int i = 0; i < datalength; i++){
					try{
						String decline = DefaultCipher.dec2((String)request.getAttribute(SecureUploadCode.CLIENTSECRET), 
								data[i]);
						if (decline.startsWith((String)request.getAttribute(SecureUploadCode.PREFIX))&&
								decline.length()>((String)request.getAttribute(SecureUploadCode.PREFIX)).length()){
							if (decline.split(",").length==(Integer)request.getAttribute(SecureUploadCode.DATALENGTH)||
									decline.split(AutoConfig.SPLIT).length==(Integer)request.getAttribute(SecureUploadCode.DATALENGTH)){
								String codeline = decline.substring(((String)request.getAttribute(SecureUploadCode.PREFIX)).length()).replaceAll(",", AutoConfig.SPLIT);
								codeline = CodeValidator.valid(request.getParameter("user"), (String)request.getAttribute(SecureUploadCode.TABLE), codeline);
								if (codeline==null){
									Map<String, Object> fail = new HashMap<String, Object>();
									fail.put("line", i+1);
									fail.put("reason", "dataerror");
									fails.add(fail);
								} else {
									try{
										String encline = DefaultCipher.enc(codeline+AutoConfig.SPLIT+AutoConfig.getremoteip(request)+AutoConfig.SPLIT+System.currentTimeMillis());
										String enc = null;
										if (codeline.contains(AutoConfig.SPLIT)){
											enc = DefaultCipher.enc(codeline.substring(0, codeline.indexOf(AutoConfig.SPLIT)));
										} else {
											enc = DefaultCipher.enc(codeline);
										}
										if ("true".equals(AutoConfig.config(request.getParameter("user"), "lsid.code.allowrepeat."+(String)request.getAttribute(SecureUploadCode.TABLE)))||
												AutoConfig.cachecodedata(request.getParameter("user"), enc, (String)request.getAttribute(SecureUploadCode.TABLE), enc, "c").isEmpty()){
											AutoConfig.cachecodedata(request.getParameter("user"), enc, (String)request.getAttribute(SecureUploadCode.TABLE), enc, "c", encline);
											successcount++;
										} else {
											Map<String, Object> fail = new HashMap<String, Object>();
											fail.put("line", i+1);
											fail.put("reason", "repeat");
											fails.add(fail);
										}
									}catch(Exception e){
										AutoConfig.log(e, "error processing "+request.getRequestURI());
										Map<String, Object> fail = new HashMap<String, Object>();
										fail.put("line", i+1);
										fail.put("reason", e.toString());
										fails.add(fail);
									}
								}
							} else {
								Map<String, Object> fail = new HashMap<String, Object>();
								fail.put("line", i+1);
								fail.put("reason", "datalengtherror");
								fails.add(fail);
							}
						} else {
							Map<String, Object> fail = new HashMap<String, Object>();
							fail.put("line", i+1);
							fail.put("reason", "contexterror");
							fails.add(fail);
						}
					}catch(Exception ex){
						AutoConfig.log(ex, "error processing "+request.getRequestURI());
						Map<String, Object> fail = new HashMap<String, Object>();
						fail.put("line", i+1);
						fail.put("reason", ex.toString());
						fails.add(fail);
					}
				}
			}
			
			String state = "success";
			if (successcount==0){
				state = "fail";
			} else if (!fails.isEmpty()){
				state = "partialfail";
			} 
			
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", state);
			result.put("successcount", successcount);
			result.put("fails", fails);
			
			if (!"success".equals(state)){
				Date current = new Date();
				
				try{
					Path logfolder = Paths.get("incoming").resolve(request.getParameter("user")).resolve(new SimpleDateFormat("yyyyMMdd").format(current));
					
					if (!Files.exists(logfolder)){
						Files.createDirectories(logfolder);
					}
					List<String> lines = new ArrayList<String>();
					lines.add(result+"=p=r=o=c=e=s=s="+current.getTime()+AutoConfig.getremoteip(request)+"=p=r=o=c=e=s=s="+new ObjectMapper().writeValueAsString(data));
					Files.write(logfolder.resolve(new SimpleDateFormat("HH").format(current)+AutoConfig.SPLIT+Thread.currentThread().getId()), lines, 
							Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				}catch(Exception e){
					AutoConfig.log(e, "error when logging =="+result+"=p=r=o=c=e=s=s="+current.getTime()+AutoConfig.getremoteip(request)+"=p=r=o=c=e=s=s="+new ObjectMapper().writeValueAsString(data));
				}
			}
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
		}finally{
			AutoConfig.iamdone();
		}
	}
	
	public static void main(String[] s) throws Exception{
		String eid="q";
		String apikey = "3c684C4a*Be6c!4r2w_9c78#8dH8fe32uFdi";
		String encryptsecret = "8uEb#Lw*R_6gHdfe";
		//List<String> lines = Files.readAllLines(Paths.get("D:\\全聚德\\全聚德-瀛厚德9.22-9.29.txt"), Charset.forName("UTF-8"));
		List<String> lines = Files.readAllLines(Paths.get("D:\\全聚德\\全聚德-三元金星_2018.2.25（生产选错日期27）(1).txt"), Charset.forName("UTF-8"));
		List<String> code=new ArrayList<String>(5000);
		for (String line:lines) {
			if (line!=null&&!line.trim().isEmpty()) {
				code.add("http://0k6.cn/"+eid+"/"+line.trim());
				if (code.size()==5000) {
	    				qjdcode(eid,apikey,encryptsecret,"repeat","repeat",code);
	    				code.clear();
	    			}
			}
        }
		if (!code.isEmpty()) {
			qjdcode(eid,apikey,encryptsecret,"repeat","repeat",code);
		}
	}
	public static void main4(String[] s) throws Exception{
		
		String eid="c";
		String apikey = "qM12yg%hfghsvxs6@24tyf*kbrpX#xdyqw";
		String encryptsecret = "9Tr!RXvrn83g$7yr";
		List<String> code=new ArrayList<String>(5000);
		for (int i=2;i<20;i++) {
			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
			code.add("http://0k6.cn/"+eid+"/testactive2017112216"+i);
		}
		lscode(eid, apikey, encryptsecret, "testb", "testl", "prod0", code);
	    	
	}
	public static void main23(String[] s) throws Exception{
		
		String eid="m";
		String apikey = "qM12yg%hfghsvxs6@24tyf*kbrpX#xdyqw";
		String encryptsecret = "9Tr!RXvrn83g$7yr";
		List<String> code=new ArrayList<String>(5000);
//		for (int i=10;i<15;i++) {
//			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
//			code.add("http://0k6.cn/"+eid+"/testactive20180111"+i);
//		}
		List<String> lines = Files.readAllLines(Paths.get("D:\\code\\m\\mjcode正式.txt"), Charset.forName("UTF-8"));
		lscode(eid, apikey, encryptsecret, "batch0","line0", "prod0", lines);
	    	
	}
	
	public static void main22(String[] s) throws Exception{
		
		String eid="m";
		String apikey = "qM12yg%hfghsvxs6@24tyf*kbrpX#xdyqw";
		String encryptsecret = "9Tr!RXvrn83g$7yr";
		List<String> code=new ArrayList<String>(5000);
		for (int i=30;i<50;i++) {
			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
			code.add("http://0k6.cn/"+eid+"/testactive20180111"+i);
		}
		lscode(eid, apikey, encryptsecret, "testb", "testl", "prod0", code);
	    	
	}
	public static void main9(String[] s) throws Exception{
		
		String eid="ls";
		String apikey = "qM12yg%Cfghsvxs6@24tXf*kbrDX#xdxqw";
		String encryptsecret = "9Tr@RXvFn83H$7yr";
		List<String> code=new ArrayList<String>(5000);
		for (int i=1;i<10;i++) {
			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
			code.add("http://0k6.cn/"+eid+"/testactive20171127"+i);
		}
		lscode(eid, apikey, encryptsecret, "testl", "testb", "testp", code);
	    	
	}
	public static void main7(String[] s) throws Exception{
		
		String eid="q";
		String apikey = "3c684C4a*Be6c!4r2w_9c78#8dH8fe32uFdi";
		String encryptsecret = "8uEb#Lw*R_6gHdfe";
		List<String> code=new ArrayList<String>(5000);
		for (int i=2;i<20;i++) {
			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
			code.add("http://0k6.cn/"+eid+"/testactive2017112216"+i);
		}
		qjdcode(eid, apikey, encryptsecret, "repeat", "repeat", code);
	    	
	}
	public static void main3(String[] s) throws Exception{
		
		String eid="lst";
		String apikey = "qM12yg%Cfghsvxs6@24tXf*kbrDX#xdxqw";
		String encryptsecret = "9Tr@RXvFn83H$7yr";
		List<String> code=new ArrayList<String>(5000);
		for (int i=20;i<30;i++) {
			//code.add("http://0k6.cn/"+eid+"/testactive201709211607"+i);
			code.add("http://0k6.cn/"+eid+"/testactive20171127"+i);
		}
		lscode(eid, apikey, encryptsecret, "testl", "testb", "testp", code);
	    	
	}

	public static void main6(String[] s) throws Exception{
		
		String eid="t1";
		String apikey = "tM12yZ%Cf_hs3xs6@24tXb*kbrew#xdxa3";
		String encryptsecret = "tTr_RXzFn28b$7rt";
		
//		List<String> code=new ArrayList<String>(5000);
//		for (int i=2;i<3;i++) {
//			code.add("http://0k6.cn/"+eid+"/testactive2017102201"+i);
//		}
	//	BufferedWriter writer = Files.newBufferedWriter(Paths.get("d:\\2.text"), StandardCharsets.UTF_8);
		List<String> lines = Files.readAllLines(Paths.get("D:\\code\\t1\\code-153"), Charset.forName("UTF-8"));		
		List<String> code=new ArrayList<String>(5000);
		for (String line:lines) {
			if (line!=null&&!line.trim().isEmpty()) {
				code.add(line.trim());
				if (code.size()==5000) {
						lscode(eid, apikey, encryptsecret, "line0", "batch0", "prod0", code);
	    				code.clear();
	    			}
			}
        }
		if (!code.isEmpty()) {
			lscode(eid, apikey, encryptsecret, "line0", "batch0", "prod0", code);
		}
	    	
	}
	
	public static void maint2(String[] s) throws Exception{
		
		String eid="t2";
		String apikey = "tM12yZ%Cf_hs3xs6@24tXb*kbrew#xdxa3";
		String encryptsecret = "tTr_RXzFn28b$7rt";
		
		List<String> code=new ArrayList<String>(5000);
//		code.add("http://0k6.cn/t2/testactive201803071001");
		for (int i=0;i<10;i++) {
			code.add("http://ha0x.cn/"+eid+"/testactive201803231341"+i);
		}
		lscode(eid, apikey, encryptsecret, "line0", "batch0", "testp", code);

	    	
	}

	public static void main2(String[] s) throws Exception{
		
		String eid="a";
		String apikey = "ftyu%csx9cse1iox@1zppm0!^axv03xr#p";
		String encryptsecret = "apxr%0xebc!nrate";
		
		List<String> code=new ArrayList<String>(5000);
		for (int i=5;i<20;i++) {
			code.add("http://0k6.cn/"+eid+"/testactive2017111117"+i);
		}
		kzwcode(eid, apikey, encryptsecret, "testl", "testb", "06", code);
	    	
	}
	public static void qjdcode(String eid, String apikey, String encryptsecret, String line, String prod, List<String> code) throws Exception {
		Form f = Form.form();
		byte[] raw = encryptsecret.getBytes("utf-8");
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		for (String co : code) {
			String data = co + "#"+prod+"#"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"#"+line;
			String encline = new BASE64Encoder().encode(cipher.doFinal(data.getBytes("UTF-8")));
			f.add("data", encline);
		}
		String params = "user=" + eid + "&active=a4&t=" + System.currentTimeMillis();
		String sign = sign(params + "&key=" + apikey);
		String res = Request.Post("http://lscode.cn/17newlscode?" + params + "&sign=" + sign).bodyForm(f.build())
				.execute().returnContent().asString();
		System.out.println("=" + res);
	}
	public static void lscode(String eid, String apikey, String encryptsecret, String line, String batch, String prod, List<String> code) throws Exception {
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
//		String res = Request.Post("http://106.75.73.75/17newlscode?" + params + "&sign=" + sign).bodyForm(f.build())
//				.execute().returnContent().asString();
		System.out.println("=" + res);
		//writer.write(res+"\r\n");
	}
	public static void kzwcode(String eid, String apikey, String encryptsecret, String line, String batch, String prod, List<String> code) throws Exception {
		 
		Form f = Form.form();
		byte[] raw = encryptsecret.getBytes("utf-8");
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		for (String co : code) {
			String data = co + "#"+prod+"#"+line+"#"+batch+"#" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			String encline = new BASE64Encoder().encode(cipher.doFinal(data.getBytes("UTF-8")));
			f.add("data", encline);
		}
		String params = "user=" + eid + "&active=a5&t=" + System.currentTimeMillis();
		String sign = sign(params + "&key=" + apikey);
		String res = Request.Post("http://lscode.cn/17newlscode?" + params + "&sign=" + sign).bodyForm(f.build())
				.execute().returnContent().asString();
		System.out.println("=" + res);
		
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
