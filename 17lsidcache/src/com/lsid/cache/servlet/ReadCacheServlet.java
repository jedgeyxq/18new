package com.lsid.cache.servlet;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.cache.util.CacheUtil;

@SuppressWarnings("serial")
public class ReadCacheServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ReadCacheServlet() {
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
		String namespace = null;
		String tablename = null;
		String column = null;
		String hash = null;
		String row = null;
		try{
			namespace = request.getParameter("eid");
			if (namespace==null||"null".equals(namespace.toLowerCase())||namespace.trim().isEmpty()){
				AutoConfig.innerechno(response,"namespaceneeded");
				return;
			}
			if (!namespace.matches("[a-zA-Z0-9]+")){
				AutoConfig.innerechno(response,"namespacecharnumneeded");
				return;
			}
			
			tablename= request.getParameter("table");
			if (tablename==null||"null".equals(tablename.toLowerCase())||tablename.trim().isEmpty()){
				AutoConfig.innerechno(response,"tablenameneeded");
				return;
			}
			if (!tablename.matches("[a-zA-Z0-9]+")){
				AutoConfig.innerechno(response,"tablenamecharnumneeded");
				return;
			}
			
			hash = request.getParameter("hash");
			if (hash==null||"null".equals(hash.toLowerCase())||hash.trim().isEmpty()){
				AutoConfig.innerechno(response,"hashneeded");
				return;
			}
			if (hash.contains(AutoConfig.SPLIT)){
				AutoConfig.innerechno(response,"hashcontainsplit");
				return;
			}
			
			row = request.getParameter("row");
			String scan = request.getParameter("scan");
			if ((scan==null||"null".equals(scan.toLowerCase())||scan.trim().isEmpty())&&
					(row==null||"null".equals(row.toLowerCase())||row.trim().isEmpty())){
				AutoConfig.innerechno(response,"rowneeded");
				return;
			}
			if ((scan==null||"null".equals(scan.toLowerCase())||scan.trim().isEmpty())&&row.contains(AutoConfig.SPLIT)){
				AutoConfig.innerechno(response,"rowcontainsplit");
				return;
			}
			
			column = request.getParameter("col");
			if (column==null||"null".equals(column.toLowerCase())||column.trim().isEmpty()){
				AutoConfig.innerechno(response,"columnneeded");
				return;
			}
			if (column.contains(AutoConfig.SPLIT)){ 
				AutoConfig.innerechno(response,"columncontainsplit");
				return;
			}
			String value = "";
			if (scan==null||"null".equals(scan.toLowerCase())||scan.trim().isEmpty()){
				value = CacheUtil.readcache(namespace, tablename, column, hash, row);
			} else {
				value = new ObjectMapper().writeValueAsString(CacheUtil.scancache(namespace, tablename, column, scan, scan));
			}
			AutoConfig.innerechok(response, value);
		}catch(Exception e){
			if (e.getMessage().equals(CacheUtil.notfoundcachefile)||e.getMessage().equals(CacheUtil.notfoundcacheline)){
				try {
					CacheUtil.cache(namespace, tablename, column, hash, row);
					String value = CacheUtil.readcache(namespace, tablename, column, hash, row);
					AutoConfig.innerechok(response, value);
					return;
				} catch (Exception e1) {
					if (e1.getMessage().contains("==lsidnotfound==")){
						AutoConfig.innerechok(response, "");
						return;
					}
					AutoConfig.innerechno(response, request, e1);
					return;
				}
			}
			AutoConfig.log(e, "Error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}
	

}
