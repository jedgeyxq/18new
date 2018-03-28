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
