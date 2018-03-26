package com.lsid.console.servlet;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.DataFiles;

@SuppressWarnings("serial")
public class MapredQueryServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MapredQueryServlet() {
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
			AutoConfig.innerechok(response, DataFiles.mapredclassparams());
		}catch(Exception e){
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}

}
