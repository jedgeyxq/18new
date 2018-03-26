package com.lsid.cache.servlet;


import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.cache.FileCache4Later;

@SuppressWarnings("serial")
public class NoCacheServlet extends HttpServlet {
	
	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public NoCacheServlet() {
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
			
			String tablename= request.getParameter("table");
			if (tablename==null||"null".equals(tablename.toLowerCase())||tablename.trim().isEmpty()){
				AutoConfig.innerechno(response,"tablenameneeded");
				return;
			}
			if (!tablename.matches("[a-zA-Z0-9]+")){
				AutoConfig.innerechno(response,"tablenamecharnumneeded");
				return;
			}
			
			String row = request.getParameter("row");
			if (row==null||"null".equals(row.toLowerCase())||row.trim().isEmpty()){
				AutoConfig.innerechno(response,"rowneeded");
				return;
			}
			if (row.contains(AutoConfig.SPLIT)){
				AutoConfig.innerechno(response,"rowcontainsplit");
				return;
			}
			
			String column = request.getParameter("col");
			if (column==null||"null".equals(column.toLowerCase())||column.trim().isEmpty()){
				AutoConfig.innerechno(response,"columnneeded");
				return;
			}
			if (column.contains(AutoConfig.SPLIT)){ 
				AutoConfig.innerechno(response,"columncontainsplit");
				return;
			}
			
			String value = request.getParameter("value");
			if (value==null||"null".equals(value.toLowerCase())||value.trim().isEmpty()){
				value="";
			}
			
			FileCache4Later.later(namespace, tablename, column, row, row, value);
				
			AutoConfig.innerechok(response, "ok");
		}catch(Exception e){
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}

}
