package com.lsid.console.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.DataFiles;

@SuppressWarnings("serial")
public class SysuserUpdateServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SysuserUpdateServlet() {
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
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "success");
			result.put("t0ken", request.getAttribute(request.getParameter("t0ken")));
			result.put("eid", request.getAttribute(request.getParameter("t0ken")+"eid"));
			String name = request.getParameter("name");
			if (name==null||name.trim().isEmpty()){
				throw new Exception("nameneeded");
			}
			name=URLEncoder.encode(name, "UTF-8");
			
			String id = request.getParameter("id");
			if (id==null||id.trim().isEmpty()){
				throw new Exception("idneeded");
			}
			
			String uname = request.getParameter("uname");
			if (uname==null||uname.trim().isEmpty()){
				uname = "";
			} else {
				uname=URLEncoder.encode(uname, "UTF-8");
			}
			
			String dept = request.getParameter("dept");
			if (dept==null||dept.trim().isEmpty()){
				dept = "";
			} else {
				dept=URLEncoder.encode(dept, "UTF-8");
			}
			
			String contact = request.getParameter("contact");
			if (contact==null||contact.trim().isEmpty()){
				contact = "";
			} else {
				contact=URLEncoder.encode(contact, "UTF-8");
			}
			
			String auth = request.getParameter("auth");
			if (auth==null||auth.trim().isEmpty()){
				auth = "";
			} else if (auth.contains(AutoConfig.SPLIT)){
				throw new Exception("invalidauth");
			}
			
			String newself = id+AutoConfig.SPLIT+request.getAttribute(request.getParameter("t0ken")+"self").toString().substring(0, request.getAttribute(request.getParameter("t0ken")+"self").toString().indexOf(AutoConfig.SPLIT))+
					AutoConfig.SPLIT+name+AutoConfig.SPLIT+uname+AutoConfig.SPLIT+dept+AutoConfig.SPLIT+contact+AutoConfig.SPLIT+auth+AutoConfig.SPLIT+
			request.getAttribute(request.getParameter("t0ken")+"eid").toString();
			
			DataFiles.changecontent(name, id, newself);
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
