package com.lsid.console.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

@SuppressWarnings("serial")
public class RealtimeServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RealtimeServlet() {
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
			result.put("wxscan",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "scan", "count", "scan", "t_wx")));
			result.put("wxuser",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "user", "count", "user", "t_wx")));
			result.put("wxenc",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "enc", "count", "enc", "t_wx")));
			result.put("zfbscan",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "scan", "count", "scan", "t_zfb")));
			result.put("zfbuser",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "user", "count", "user", "t_zfb")));
			result.put("zfbenc",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "enc", "count", "enc", "t_zfb")));
			
			long wxtotalscanusers = AutoConfig.incremented(result.get("eid").toString(), "preauth", "count", "preauth", "t_wx");
			long wxtotalprizeusers = AutoConfig.incremented(result.get("eid").toString(), "luckuser", "count", "luckuser", "t_wx");
			long wxtotalprizes = AutoConfig.incremented(result.get("eid").toString(), "prize", "count", "prize", "t_wx");
			long wxtotalprizetimes=AutoConfig.incremented(result.get("eid").toString(), "luck", "count", "luck", "t_wx");
			if (wxtotalprizeusers>wxtotalscanusers) {
				wxtotalprizeusers = wxtotalscanusers;
			}
			if (wxtotalprizes>wxtotalprizetimes) {
				wxtotalprizes = wxtotalprizetimes;
			}
			
			result.put("wxtodayscanusers",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "preauth", "count", "preauth"+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "t_wx")));
			result.put("wxtodayscans",String.valueOf(AutoConfig.incremented(result.get("eid").toString(), "scan", "count", "scan"+AutoConfig.SPLIT_HBASE+new SimpleDateFormat("yyyyMMdd").format(new Date()), "t_wx")));
			result.put("wxtotalscanusers",String.valueOf(wxtotalscanusers));
			result.put("wxtotalprizeusers",String.valueOf(wxtotalprizeusers));
			result.put("wxtotalprizes",String.valueOf(wxtotalprizes));
			result.put("wxtotalprizetimes",String.valueOf(wxtotalprizetimes));
			
			

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
