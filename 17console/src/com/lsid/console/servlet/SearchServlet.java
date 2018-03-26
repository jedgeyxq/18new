package com.lsid.console.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.DataFiles;

@SuppressWarnings("serial")
public class SearchServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SearchServlet() {
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
			
			result.put("data",DataFiles.search(result.get("eid").toString(), request.getParameter("day"), Integer.parseInt(request.getParameter("startrow")),
					request.getParameter("code"), request.getParameter("mobile"), request.getParameter("nick")));
			
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
