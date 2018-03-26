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
import com.lsid.console.filter.SecureFilter;
import com.lsid.console.util.DataFiles;

@SuppressWarnings("serial")
public class ChangePasswordServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ChangePasswordServlet() {
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
			if (!SecureFilter.aesdec(request.getParameter("oldpassword"), request.getAttribute(request.getParameter("t0ken")+"key").toString()).equals(
					request.getAttribute(request.getParameter("t0ken")+"password").toString())){
				throw new Exception("wrongpassword");
			}
			DataFiles.changepassword(URLEncoder.encode(request.getAttribute(request.getParameter("t0ken")+"name").toString(),"UTF-8"), 
					request.getAttribute(request.getParameter("t0ken")+"password").toString(), 
					SecureFilter.aesdec(request.getParameter("newpassword"), request.getAttribute(request.getParameter("t0ken")+"key").toString()));
			
			SecureFilter.refreshpassword(SecureFilter.aesdec(request.getAttribute(request.getParameter("t0ken")).toString(), request.getAttribute(request.getParameter("t0ken")+"key").toString()), 
					SecureFilter.aesdec(request.getParameter("newpassword"), request.getAttribute(request.getParameter("t0ken")+"key").toString()));
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
