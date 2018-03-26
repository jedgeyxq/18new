package com.lsid.counter.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.counter.util.CounterUtil;

@SuppressWarnings("serial")
public class ReadServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ReadServlet() {
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
			
			String hash = request.getParameter("hash");
			if (request.getParameter("greater")==null&&request.getParameter("total")==null){
				if (hash==null||"null".equals(hash.toLowerCase())||hash.trim().isEmpty()){
					AutoConfig.innerechno(response,"hashneeded");
					return;
				}
				if (hash.contains(AutoConfig.SPLIT)){
					AutoConfig.innerechno(response,"hashcontainsplit");
					return;
				}
			}
			
			String row = request.getParameter("row");
			if (request.getParameter("greater")==null&&request.getParameter("total")==null){
				if (row==null||"null".equals(row.toLowerCase())||row.trim().isEmpty()){
					AutoConfig.innerechno(response,"rowneeded");
					return;
				}
				if (row.contains(AutoConfig.SPLIT)){
					AutoConfig.innerechno(response,"rowcontainsplit");
					return;
				}
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
			
			if (request.getParameter("greater")==null&&request.getParameter("total")==null){
				if (CounterUtil.needcache(namespace, tablename, column, hash, row)){
					CounterUtil.cache(namespace, tablename, column, hash, row);
				}
			}
			if (request.getParameter("position")!=null){
				long p = 0;
				if (Files.exists(CounterUtil.positionfile(namespace, tablename, column, hash, row))){
					try{
						p = Long.parseLong(Files.readAllLines(CounterUtil.positionfile(namespace, tablename, column, hash, row), Charset.forName("UTF-8")).get(0));
					}catch(Exception e){
						AutoConfig.log(e, "got exception when reading position of namespace=["+namespace+"],tablename=["+tablename+"],column=["+column+"],row=["+row+"]");
					}
				}
				long t = 0;
				Path total = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedtotal").resolve(namespace).resolve(tablename).resolve(column).resolve("total");
				if (Files.exists(total)){
					try{
						t = Long.parseLong(Files.readAllLines(total, Charset.forName("UTF-8")).get(0));
					}catch(Exception e){
						AutoConfig.log(e, "got exception when reading total of namespace=["+namespace+"],tablename=["+tablename+"],column=["+column+"]");
					}
				}
				AutoConfig.innerechok(response, p+AutoConfig.SPLIT+t);
			} else if (request.getParameter("greater")!=null){
				long tocompare = 0;
				try{
					tocompare = Long.parseLong(request.getParameter("greater"));
				}catch(Exception e){
					AutoConfig.innerechno(response, "greaterlongvalue");
					return;
				}
				long g = 0;
				Path merged = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedone").resolve(namespace).resolve(tablename).resolve(column).resolve("merged");
				if (Files.exists(merged)){
					BufferedReader br = null;
					try{
						br = Files.newBufferedReader(merged, Charset.forName("UTF-8"));
						String line = br.readLine();
						while(line!=null){
							if (tocompare < Long.parseLong(line.substring(line.lastIndexOf(AutoConfig.SPLIT)+1))){
								g++;
							} else {
								break;
							}
							line = br.readLine();
						}
					}catch(Exception e){
						AutoConfig.log(e, "got exception when reading merged of namespace=["+namespace+"],tablename=["+tablename+"],column=["+column+"]");
					}finally{
						if (br!=null){
							br.close();
							br = null;
						}
					}
				}
				AutoConfig.innerechok(response, String.valueOf(g));
			} else if (request.getParameter("total")!=null){
				long d = 0;
				Path merged = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedone").resolve(namespace).resolve(tablename).resolve(column).resolve("merged");
				if (Files.exists(merged)){
					BufferedReader br = null;
					try{
						br = Files.newBufferedReader(merged, Charset.forName("UTF-8"));
						String line = br.readLine();
						while(line!=null){
							line = br.readLine();
							d++;
						}
					}catch(Exception e){
						AutoConfig.log(e, "got exception when reading merged of namespace=["+namespace+"],tablename=["+tablename+"],column=["+column+"]");
					}finally{
						if (br!=null){
							br.close();
							br = null;
						}
					}
				}
				AutoConfig.innerechok(response, String.valueOf(d));
			} else {
				AutoConfig.innerechok(response, String.valueOf(CounterUtil.incrementedcache(namespace, tablename, column, hash, row)));
			}
		}catch(Exception e){
			if (e.getMessage().contains("==lsidnotfound==")){
				AutoConfig.innerechok(response, "0");
				return;
			}
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}
	
}
