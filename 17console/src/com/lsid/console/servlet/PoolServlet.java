package com.lsid.console.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

@SuppressWarnings("serial")
public class PoolServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public PoolServlet() {
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
			
			Map<String, String> progress = new HashMap<String, String>();
			String progresstr = AutoConfig.innerpost(AutoConfig.config(null, "lsid.interface.luck"), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.connectimeoutinsec")), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.luck.socketimeoutinsec")), 
					"eid",namespace);
			progress = new ObjectMapper().readValue(progresstr,progress.getClass());
			List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
			if (!AutoConfig.config(namespace, "lsid.pool").isEmpty()) {
				String[] poolids = AutoConfig.config(namespace, "lsid.pool").split(AutoConfig.SPLIT);
				for (String poolid:poolids) {
					if (progress.get("remaining"+poolid)!=null&&progress.get("repository"+poolid)!=null&&progress.get("ratio"+poolid)!=null) {
						String[] remaining = progress.get("remaining"+poolid).split(AutoConfig.SPLIT);
						String[] repository = progress.get("repository"+poolid).split(AutoConfig.SPLIT);
						String[] ratio = progress.get("ratio"+poolid).split(AutoConfig.SPLIT);
						if (remaining.length==repository.length&&repository.length==ratio.length) {
							Map<String, Object> d = new HashMap<String, Object>();
							d.put("start", AutoConfig.config(namespace, "lsid.pool"+poolid+".active").substring(0, AutoConfig.config(namespace, "lsid.pool"+poolid+".active").indexOf(AutoConfig.SPLIT)));
							d.put("end", AutoConfig.config(namespace, "lsid.pool"+poolid+".active").substring(AutoConfig.config(namespace, "lsid.pool"+poolid+".active").indexOf(AutoConfig.SPLIT)+1));
							d.put("type", URLDecoder.decode(AutoConfig.config(namespace, "lsid.pool"+poolid+".name"),"UTF-8"));
							d.put("rule", URLDecoder.decode(AutoConfig.config(namespace, "lsid.pool"+poolid+".rule"),"UTF-8"));
							List<Map<String, String>> prizes = new ArrayList<Map<String, String>>();
							long draws = 0;
							for (int i=0;i<remaining.length;i++) {
								Map<String, String> p = new HashMap<String, String>();
								p.put("name", URLDecoder.decode(AutoConfig.config(namespace, "lsid.pool"+poolid+".prize"+i+".name"),"UTF-8"));
								BigDecimal bd = new BigDecimal(Double.parseDouble(ratio[i])*100);
								if (bd.doubleValue()<1) {
									p.put("planratio", bd.setScale(ratio[i].substring(ratio[i].indexOf(".")+1).length()-2, BigDecimal.ROUND_HALF_UP).toString()+"%");
								} else {
									p.put("planratio", bd.setScale(0, BigDecimal.ROUND_HALF_UP).toString()+"%");
								}
								p.put("remaining", remaining[i]);
								draws += Long.parseLong(repository[i])-Long.parseLong(remaining[i]);
								prizes.add(p);
							}
							for (int i=0;i<remaining.length;i++) {
								Map<String, String> p = prizes.get(prizes.size()-(remaining.length-i));
								String val = new BigDecimal((Double.parseDouble(repository[i])-Double.parseDouble(remaining[i]))/draws).setScale(6, BigDecimal.ROUND_HALF_UP).toString();
								BigDecimal bd = new BigDecimal(Double.parseDouble(val)*100);
								if (bd.doubleValue()<1) {
									p.put("realratio", bd.setScale(val.substring(val.indexOf(".")+1).length()-2, BigDecimal.ROUND_HALF_UP).toString()+"%");
								} else {
									p.put("realratio", bd.setScale(0, BigDecimal.ROUND_HALF_UP).toString()+"%");
								}
							}
							d.put("prizes", prizes);
							data.add(d);
						}
					}
				}
			}
			
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "success");
			result.put("t0ken", request.getAttribute(request.getParameter("t0ken")));
			result.put("eid", request.getAttribute(request.getParameter("t0ken")+"eid"));
			
			result.put("data", data);
			
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
