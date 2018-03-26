package com.lsid.notify.servlet;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;

@SuppressWarnings("serial")
public class NotifyServlet extends HttpServlet {
	
	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public NotifyServlet() {
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
			String namespace = request.getParameter("eid");
			if (namespace==null||"null".equals(namespace.toLowerCase())||namespace.trim().isEmpty()){
				AutoConfig.innerechno(response,"namespaceneeded");
				return;
			}
			if (!namespace.matches("[a-zA-Z0-9]+")){
				AutoConfig.innerechno(response,"namespacecharnumneeded");
				return;
			}
			
			String msgtype= request.getParameter("msgtype");
			if (msgtype==null||"null".equals(msgtype.toLowerCase())||msgtype.trim().isEmpty()){
				AutoConfig.innerechno(response,"msgtypeneeded");
				return;
			}
			
			String content = request.getParameter("content");
			if (content==null||"null".equals(content.toLowerCase())||content.trim().isEmpty()){
				AutoConfig.innerechno(response,"contentneeded");
				return;
			}
			
			String result = "ok";
			for (String msgtypeone:msgtype.split(AutoConfig.SPLIT)) {
				if (!AutoConfig.config(namespace, "notify."+msgtypeone).isEmpty()) {
					byte[] writecontent = content.getBytes("UTF-8");
					for (String notifyway:AutoConfig.config(namespace, "notify."+msgtypeone).split(AutoConfig.SPLIT)) {	
						if (!AutoConfig.config(null, notifyway+"."+msgtypeone+".folder").isEmpty()&&
								!AutoConfig.config(null, AutoConfig.config(null, notifyway+"."+msgtypeone+".folder")).isEmpty()) {
							
							Files.createDirectories(Paths.get(AutoConfig.config(null, AutoConfig.config(null, notifyway+"."+msgtypeone+".folder"))).resolve("later").resolve(namespace));
							
							Files.write(Paths.get(AutoConfig.config(null, AutoConfig.config(null, notifyway+"."+msgtypeone+".folder"))).resolve("later").resolve(namespace).resolve(msgtypeone+AutoConfig.SPLIT+UUID.randomUUID().toString().replaceAll("-", "")+AutoConfig.SPLIT+writecontent.length), 
									writecontent, StandardOpenOption.CREATE_NEW);
							result+=notifyway;
						}
					}
				} else {
					throw new Exception("missing notify."+msgtypeone);
				}
			}
			AutoConfig.innerechok(response, result);
		}catch(Exception e){
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}
	

}
