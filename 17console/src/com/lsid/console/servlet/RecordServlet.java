package com.lsid.console.servlet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.filter.SecureFilter;
import com.lsid.console.util.DataFiles;
import com.lsid.console.util.QRCodeImageImpl;
import com.lsid.util.DefaultCipher;

import jp.sourceforge.qrcode.QRCodeDecoder;

@SuppressWarnings("serial")
public class RecordServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RecordServlet() {
		super();
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
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");		
    	response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed","1");
		AutoConfig.iamrunning();
		try{
			String code = null;
			if (ServletFileUpload.isMultipartContent((HttpServletRequest) request)) {
				DiskFileItemFactory factory = new DiskFileItemFactory();
				factory.setRepository((File) request.getServletContext().getAttribute("javax.servlet.context.tempdir"));
				ServletFileUpload upload = new ServletFileUpload(factory);
				List<FileItem> items = upload.parseRequest((HttpServletRequest) request);
				if (items != null) {
					Iterator<FileItem> iter = items.iterator();
					while (iter.hasNext()) {
						FileItem item = iter.next();
						if (!item.isFormField()&&item.getSize() < 10000000&& item.getSize() > 0) {
							String filename = UUID.randomUUID().toString().replaceAll("-", "");
							File file = Paths.get(filename).toFile();
							item.write(file);
							try { 
					        	BufferedImage bufImg = ImageIO.read(file); 
					            QRCodeDecoder decoder = new QRCodeDecoder(); 
					            String val = new String(decoder.decode(new QRCodeImageImpl(bufImg)));
					            code = val.substring(val.lastIndexOf("/")+1); 
					        }catch(Exception ex){
					    	    throw ex;
					        }finally{
					    	    Files.delete(Paths.get(filename));
					        }
						}
					}
				}
			} else {
				code = SecureFilter.aesdec(request.getParameter("enc"), request.getAttribute(request.getParameter("t0ken")+"key").toString());
			}
			if (code==null||code.trim().isEmpty()) {
				throw new Exception("invalid code");
			}
			String enc = null;
			try {
				DefaultCipher.dec(code);
				enc = code;
			}catch(Exception e) {
				enc = DefaultCipher.enc(code);
			}
			
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "success");
			result.put("t0ken", request.getAttribute(request.getParameter("t0ken")));
			result.put("eid", request.getAttribute(request.getParameter("t0ken")+"eid"));
			
			String linenum = "unknown";
			String prodnum = "unknown";
			String active = "unknown";
			try {
				String a = AutoConfig.cachecodedata(result.get("eid").toString(), enc, "a", enc, "c");
				if (a.isEmpty()) {
					String na = AutoConfig.cachecodedata(result.get("eid").toString(), enc, "na", enc, "c");
					if (!na.isEmpty()) {
						linenum=AutoConfig.config(result.get("eid").toString(), "lsid.code.linena."+DefaultCipher.dec(na).split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(result.get("eid").toString(), "lsid.code.linena.index"))]);
						prodnum=AutoConfig.config(result.get("eid").toString(), "lsid.prod.desc."+DefaultCipher.dec(na).split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(result.get("eid").toString(), "lsid.code.prodna.index"))]);
						active = "false";
					}
				} else {
					active = "true";
					linenum=AutoConfig.config(result.get("eid").toString(), "lsid.code.linea."+DefaultCipher.dec(a).split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(result.get("eid").toString(), "lsid.code.linea.index"))]);
					prodnum=AutoConfig.config(result.get("eid").toString(), "lsid.prod.desc."+DefaultCipher.dec(a).split(AutoConfig.SPLIT)[Integer.parseInt(AutoConfig.config(result.get("eid").toString(), "lsid.code.proda.index"))]);
				}
				linenum=URLDecoder.decode(linenum,"UTF-8");
				prodnum=URLDecoder.decode(prodnum,"UTF-8").substring(0,16);
			}catch(Exception e) {
				//do nothing
			}
			Map<String, Object> coderepositorydata = new HashMap<String, Object>();
			coderepositorydata.put("linenum", linenum);
			coderepositorydata.put("prodnum", prodnum);
			coderepositorydata.put("active", active);
			result.put("coderepositorydata",coderepositorydata);
			
			List<String> scanresult = new ArrayList<String>();
			try {
				String[] poolids = AutoConfig.cachecodedata(result.get("eid").toString(), enc, "once", enc, "po").split(AutoConfig.SPLIT);
				if (poolids!=null) {
					for (String poolid:poolids) {
						String[] prizes = AutoConfig.cacheuserdata(result.get("eid").toString(), enc, "once", enc+AutoConfig.SPLIT_HBASE+poolid, "pr").split(AutoConfig.SPLIT);
						if (prizes!=null) {
							for (String prizeid:prizes) {
								String row = enc+AutoConfig.SPLIT_HBASE+poolid+AutoConfig.SPLIT_HBASE+prizeid;
								String prize = AutoConfig.cacheuserdata(result.get("eid").toString(), enc, "prize", row, "p");
								if (!prize.isEmpty()) {
									scanresult.add(prize);
								}
							}
						}
					}
				}
			}catch(Exception ex) {
				//do nothing
			}
			Map<String, Long> encpoolprizelucktime = new HashMap<String, Long>();
			Map<String, Map<String, Object>> encpoolprizedata = new HashMap<String, Map<String, Object>>();
			for (String one:scanresult) {
				Map<String, Object> codeprizedataone = new HashMap<String, Object>();
				
				String[] parts = one.split("#");
   				String from = parts[1];
	   			String eid = parts[2];
	   			String activity = parts[21];
	   			String pool = parts[34];
	   			String prize = parts[35];
	   			String headimgurl = "unknown";
	   			String nick = "unknown";
	   			String ip = parts[24];
	   			String province = parts[25];
	   			String city = parts[26];
	   			String district = parts[27];
	   			String street = parts[28];
	   			String num = parts[29];
	   			String playid = parts[20];
	   			String lucktime = parts[33];
	   			
	   			try {
	   				headimgurl=String.valueOf(new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("headimgurl").asText());
	   				nick=String.valueOf(new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("nickname").asText());
	   			}catch(Exception e) {
   					//do nohting
   				}
				
				codeprizedataone.put("from", from);
				codeprizedataone.put("headimgurl", headimgurl);
				codeprizedataone.put("nick", nick);
				codeprizedataone.put("openid", playid);
				codeprizedataone.put("time", lucktime);
				codeprizedataone.put("ip", ip);
				codeprizedataone.put("addr", province+city+district+street+num);
				codeprizedataone.put("activityid", URLDecoder.decode(AutoConfig.config(eid, "lsid.activity"+activity+".name"),"UTF-8"));
				codeprizedataone.put("prizeid", URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+pool+".prize"+prize+".name"),"UTF-8"));
				codeprizedataone.put("sysreq", DefaultCipher.dec(AutoConfig.getsysrequire(parts)));
				if (parts.length>38) {
					codeprizedataone.put("userinput", DefaultCipher.dec(parts[38]));
				} else {
					codeprizedataone.put("userinput", "");
				}
				if (parts.length>40) {
					codeprizedataone.put("status", DefaultCipher.dec(parts[40]));
				} else {
					codeprizedataone.put("status", "");
				}
				encpoolprizedata.put(AutoConfig.getenc(parts)+AutoConfig.getpoolid(parts)+AutoConfig.getprizeid(parts),codeprizedataone);
				encpoolprizelucktime.put(AutoConfig.getenc(parts)+AutoConfig.getpoolid(parts)+AutoConfig.getprizeid(parts), AutoConfig.getlucktime(parts));
			}
			
			List<Map<String, Object>> codeprizedata = new ArrayList<Map<String, Object>>();
			
			Map<String, Long> sorted = DataFiles.sortMapByValue(encpoolprizelucktime);
			if (sorted!=null&&!sorted.isEmpty()) {
				for (Map.Entry<String, Long> entry : sorted.entrySet()) {
					codeprizedata.add(encpoolprizedata.get(entry.getKey()));
				}
			}
			result.put("codeprizedata",codeprizedata);
			
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
		}catch(Exception e){
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "fail");
			result.put("reason", e.toString());
			result.put("t0ken", request.getAttribute(request.getParameter("t0ken")));
			result.put("eid", request.getAttribute(request.getParameter("t0ken")+"eid"));
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
		} finally{
			AutoConfig.iamdone();
		}
	}
}
