package com.lsid.sort.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

public class Top100 implements Filter {
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
			
			String column = request.getParameter("col");
			if (column==null||"null".equals(column.toLowerCase())||column.trim().isEmpty()){
				AutoConfig.innerechno(response,"columnneeded");
				return;
			}
			if (column.contains(AutoConfig.SPLIT)){
				AutoConfig.innerechno(response,"columncontainsplit");
				return;
			}
			List<String> returnvalue = new ArrayList<String>(100);
			 Path sortedone = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedone");
			if (Files.exists(sortedone.resolve(namespace).resolve(tablename).resolve(column).resolve("merged"))) {
				BufferedReader br = null;
				try {
					br = Files.newBufferedReader(sortedone.resolve(namespace).resolve(tablename).resolve(column).resolve("merged"), Charset.forName("UTF-8"));
					String line = br.readLine();
					while (line!=null) {
						returnvalue.add(line);
						if (line.endsWith(AutoConfig.SPLIT+1)||returnvalue.size()>=100) {
							break;
						}
						line = br.readLine();
					}
				}finally {
					if (br!=null) {
						br.close();
					}
				}
			}
			AutoConfig.innerechok(response, new ObjectMapper().writeValueAsString(returnvalue));
			
		} catch (Exception e) {
			AutoConfig.log(e, "error processing "+request.getRequestURI());
			AutoConfig.innerechno(response, request, e);
		}
		
	}
	
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
}
