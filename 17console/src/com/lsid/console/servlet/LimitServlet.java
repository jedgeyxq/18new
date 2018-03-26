package com.lsid.console.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

@SuppressWarnings("serial")
public class LimitServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LimitServlet() {
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
	@SuppressWarnings("unchecked")
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
			String namespace = request.getAttribute(request.getParameter("t0ken")+"eid").toString();
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
			
			String column = request.getParameter("col");
			if (column==null||"null".equals(column.toLowerCase())||column.trim().isEmpty()){
				AutoConfig.innerechno(response,"columnneeded");
				return;
			}
			if (column.contains(AutoConfig.SPLIT)){
				AutoConfig.innerechno(response,"columncontainsplit");
				return;
			}
			List<String> temp = new ArrayList<String>();
			String[] top100s = AutoConfig.config(namespace, "lsid.interface.sort").split(AutoConfig.SPLIT);
			for (String top100:top100s) {
				List<String> t = new ArrayList<String>();
				t = new ObjectMapper().readValue(AutoConfig.innerpost(top100+"t", Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.sort.connectimeoutinsec")), 
						Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.sort.socketimeoutinsec")), 
						"eid", namespace, "table", tablename, "col", column),t.getClass());
				temp.addAll(t);
			}
			Map<String, Long> kv = new HashMap<String, Long>();
			for (String t:temp) {
				if (t!=null&&!t.trim().isEmpty()) {
					String[] ts = t.split(AutoConfig.SPLIT); 
					kv.put(ts[1], Long.parseLong(ts[2]));
				}
			}
			Map<String, Long> sorted = sortMapByValue(kv);
			List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
			Set<Map.Entry<String, Long>> entries = sorted.entrySet();
			for (Map.Entry<String, Long> entry:entries){
				Map<String, Object> d = new HashMap<String, Object>();
				d.put("key", entry.getKey());
				d.put("value", entry.getValue());
				data.add(d);
			}
			
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "success");
			result.put("t0ken", request.getAttribute(request.getParameter("t0ken")));
			result.put("eid", request.getAttribute(request.getParameter("t0ken")+"eid"));
			
			result.put("data", data);
			
			result.put("lsid.black.pip_", AutoConfig.config(result.get("eid").toString(), "lsid.black.ip"));
			result.put("lsid.white.pip_", AutoConfig.config(result.get("eid").toString(), "lsid.white.ip"));
			result.put("lsid.limit.pip_.day", AutoConfig.config(result.get("eid").toString(), "lsid.limit.ip.day"));
			result.put("lsid.limit.pip_.month", AutoConfig.config(result.get("eid").toString(), "lsid.limit.ip.month"));
			
			result.put("lsid.black.puser_", AutoConfig.config(result.get("eid").toString(), "lsid.black.id"));
			result.put("lsid.white.puser_", AutoConfig.config(result.get("eid").toString(), "lsid.white.id"));
			result.put("lsid.limit.puser_.day", AutoConfig.config(result.get("eid").toString(), "lsid.limit.id.day"));
			result.put("lsid.limit.puser_.month", AutoConfig.config(result.get("eid").toString(), "lsid.limit.id.month"));
			
			result.put("lsid.black.penc_", AutoConfig.config(result.get("eid").toString(), "lsid.black.enc"));
			result.put("lsid.white.penc_", AutoConfig.config(result.get("eid").toString(), "lsid.white.enc"));
			result.put("lsid.limit.penc_.day", AutoConfig.config(result.get("eid").toString(), "lsid.limit.enc.day"));
			result.put("lsid.limit.penc_.month", AutoConfig.config(result.get("eid").toString(), "lsid.limit.enc.month"));
			
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
	
	public static Map<String, Long> sortMapByValue(Map<String, Long> oriMap) {
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        if (oriMap != null && !oriMap.isEmpty()) {
            List<Map.Entry<String, Long>> entryList = new ArrayList<Map.Entry<String, Long>>(
                oriMap.entrySet());
	        Collections.sort(entryList, new Comparator<Entry<String, Long>>(){
	
				@Override
				public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
					Entry<String, Long> me1 = (Entry<String, Long>)o1;
					Entry<String, Long> me2 = (Entry<String, Long>)o2;
			        return me2.getValue().compareTo(me1.getValue());
				}
	        	
	        });
	
	        Iterator<Map.Entry<String, Long>> iter = entryList.iterator();
	        Map.Entry<String, Long> tmpEntry = null;
	        while (iter.hasNext()) {
	            tmpEntry = iter.next();
	            sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
	        }
        }
        return sortedMap;
    }

}
