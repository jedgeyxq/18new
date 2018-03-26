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
public class ChangeSelfServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ChangeSelfServlet() {
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
			String[] oldself = request.getAttribute(request.getParameter("t0ken")+"self").toString().split(AutoConfig.SPLIT);
			String newself = oldself[0]+AutoConfig.SPLIT+oldself[1]+AutoConfig.SPLIT+URLEncoder.encode(request.getParameter("name"),"UTF-8")+AutoConfig.SPLIT+URLEncoder.encode(request.getParameter("uname"),"UTF-8")+AutoConfig.SPLIT+
			URLEncoder.encode(request.getParameter("dept"),"UTF-8")+AutoConfig.SPLIT+URLEncoder.encode(request.getParameter("contact"),"UTF-8")+AutoConfig.SPLIT+oldself[6]+AutoConfig.SPLIT+oldself[7];
			DataFiles.changecontent(URLEncoder.encode(request.getAttribute(request.getParameter("t0ken")+"name").toString(),"UTF-8"), 
					URLEncoder.encode(request.getParameter("name"),"UTF-8"), request.getAttribute(request.getParameter("t0ken")+"password").toString(), 
					newself);
			SecureFilter.refreshself(
					SecureFilter.aesdec(request.getAttribute(request.getParameter("t0ken")).toString(),request.getAttribute(request.getParameter("t0ken")+"key").toString()), 
					request.getParameter("name"),newself);
			result.put("self", newself);
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
