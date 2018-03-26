package com.lsid.counter.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.cache.FileCache4Later;
import com.lsid.counter.util.CounterUtil;

@SuppressWarnings("serial")
public class IncrementServlet extends HttpServlet {
	
	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public IncrementServlet() {
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
		long amountnum = 0;
		String namespace = null;
		String tablename= null;
		String hash = null;
		String row = null;
		String column = null;
		
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
			if (row==null||"null".equals(row.toLowerCase())||row.trim().isEmpty()){
				AutoConfig.innerechno(response,"rowneeded");
				return;
			}
			if (row.contains(AutoConfig.SPLIT)){
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
			
			String amount = request.getParameter("amount");
			
			try{
				amountnum = Long.parseLong(amount);
			}catch(NumberFormatException ex){
				AutoConfig.innerechno(response, "amountnumneeded");
				return;
			}

			if (CounterUtil.needcache(namespace, tablename, column, hash, row)){
				CounterUtil.cache(namespace, tablename, column, hash, row);
			}
				
			if (amountnum!=0){
				
				FileCache4Later.later(namespace, tablename, column, hash, row, amountnum);
				CounterUtil.incrementcache(namespace, tablename, column, hash, row, amountnum);
				if (request.getParameter("more")!=null){
					Date todaydate = new Date();
					String today = new SimpleDateFormat("yyyyMMdd").format(todaydate);
					String thismonth = new SimpleDateFormat("yyyyMM").format(todaydate);
					incrementauto(namespace, tablename, column, hash, row, today, amountnum);
					incrementauto(namespace, tablename, column, hash, row, thismonth, amountnum);
				}
			}
			if (!AutoConfig.config(namespace, "lsid.sort."+tablename+"."+column).isEmpty()&&!Files.exists(CounterUtil.tosort.resolve(namespace).resolve(tablename).resolve(column))){
				if (!Files.exists(CounterUtil.tosort)){
					Files.createDirectories(CounterUtil.tosort);
				}
				//put column in the middle because column may contains "-" which can mess up the filename splitting.
				Files.write(CounterUtil.tosort.resolve(namespace+AutoConfig.SPLIT+column+AutoConfig.SPLIT+tablename), new byte[0], StandardOpenOption.CREATE);
				CounterUtil.incrementcache(namespace, tablename, column, hash, row, 0);
			}
			AutoConfig.innerechok(response, String.valueOf(CounterUtil.incrementedcache(namespace, tablename, column, hash, row)));
		}catch(Exception e){
			if (e.getMessage().contains("==lsidnotfound==")){
				try{
					if (amountnum!=0){
						FileCache4Later.later(namespace, tablename, column, hash, row, amountnum);
						CounterUtil.incrementcache(namespace, tablename, column, hash, row, amountnum);
						if (request.getParameter("more")!=null){
							Date todaydate = new Date();
							String today = new SimpleDateFormat("yyyyMMdd").format(todaydate);
							String thismonth = new SimpleDateFormat("yyyyMM").format(todaydate);
							incrementauto(namespace, tablename, column, hash, row, today, amountnum);
							incrementauto(namespace, tablename, column, hash, row, thismonth, amountnum);
						}
						AutoConfig.innerechok(response, String.valueOf(CounterUtil.incrementedcache(namespace, tablename, column, hash, row)));
					} else {
						AutoConfig.innerechok(response, "0");
					}
					return;
				}catch(Exception ex){
					AutoConfig.innerechno(response, request, ex);
					return;
				}
			}
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}
	private void incrementauto(String namespace, String tablename, String column, String hash, String row, String rowsuffix, long amountnum){
		try{
			if (CounterUtil.needcache(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix)){
				CounterUtil.cache(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix);
			}
			FileCache4Later.later(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix, amountnum);
			CounterUtil.incrementcache(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix, amountnum);
		}catch(Exception ex){
			if (ex.getMessage().contains("==lsidnotfound==")){
				try{
					FileCache4Later.later(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix, amountnum);
					CounterUtil.incrementcache(namespace, tablename, column, hash, row+AutoConfig.SPLIT_HBASE+rowsuffix, amountnum);
				}catch(Exception ex2){
					AutoConfig.log(ex2, "error incrementing "+amountnum+" to "+row+AutoConfig.SPLIT_HBASE+rowsuffix + " when not found");
				}
			}		
			AutoConfig.log(ex, "error incrementing "+amountnum+" to "+row+AutoConfig.SPLIT_HBASE+rowsuffix);
		}
	}

}
