package com.lsid.luck.client.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;

public class LuckBuffer implements Filter {
	private static final Map<String, Vector<String>> luckbuffer = new HashMap<String, Vector<String>>();
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain arg2)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	response.setHeader("Access-Control-Allow-Origin", "*");
    	response.setHeader("XDomainRequestAllowed","1");
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	try {
    		if (request.getParameter("eid")==null||request.getParameter("eid").trim().isEmpty()||request.getParameter("eid").equals("null")){
    			throw new Exception("eidneeded");
    		}
    		if (request.getParameter("poolid")==null||request.getParameter("poolid").trim().isEmpty()||request.getParameter("poolid").equals("null")){
    			throw new Exception("poolidneeded");
    		}
    		
//    		if (!Files.exists(RemoveLock.lucklock.resolve(request.getParameter("eid")))) {
//    			Files.createDirectories(RemoveLock.lucklock.resolve(request.getParameter("eid")));
//    		}
    		
    		AutoConfig.innerechok(response, getbuffer(request.getParameter("eid"), request.getParameter("poolid")));
    	} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
		
	}

	private synchronized String getbuffer(String eid, String poolid) throws Exception{
		String returnvalue = "";
		try {
			returnvalue = luckbuffer.get(eid+poolid).remove(0);
		}catch(Exception e) {
			String[] b=AutoConfig.innerpost(AutoConfig.config(null, "lsid.interface.luck"), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.connectimeoutinsec")), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.socketimeoutinsec")), 
					"eid",eid,"poolid",poolid).split(AutoConfig.SPLIT);
			List<String> v = Arrays.asList(b);
			if (luckbuffer.get(eid+poolid)==null){
				Vector<String> vv = new Vector<String>(v.size());
				vv.addAll(v);
				luckbuffer.put(eid+poolid, vv);
			} else {
				luckbuffer.get(eid+poolid).addAll(v);
			}
			returnvalue = luckbuffer.get(eid+poolid).remove(0);
		}
		if (!AutoConfig.config(eid, "lsid.pool"+poolid+".prizesonetime").isEmpty()){
			try{
				int prizesonetime = Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+poolid+".prizesonetime"));
				for (int i=1;i<prizesonetime;i++){
					returnvalue+=AutoConfig.SPLIT+luckbuffer.get(eid+poolid).remove(0);
				}
			}catch(Exception ex){
				//do nothing
			}
		}
		return returnvalue;
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
