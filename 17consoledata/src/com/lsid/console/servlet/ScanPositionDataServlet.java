package com.lsid.console.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.console.util.DataFiles;
import com.lsid.util.DefaultCipher;

@SuppressWarnings("serial")
public class ScanPositionDataServlet extends HttpServlet {

	@Override
	public void destroy(){
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ScanPositionDataServlet() {
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
			String[] sp = request.getParameter("sp").split(AutoConfig.SPLIT);
			String lng = sp[31];
			String lat = sp[32];
			String province = sp[26];
			String city = sp[27];
			String district = sp[28];
			String street = sp[29];
			String num = sp[30];
			String nick = URLEncoder.encode(new ObjectMapper().readTree(new String(DefaultCipher.dec(sp[19]))).get("nickname").asText(),"UTF-8");
			String head =	new ObjectMapper().readTree(new String(DefaultCipher.dec(sp[19]))).get("headimgurl").asText();
			String enc = sp[3];
			String ip = sp[24];
			String scantime = sp[16];
			String prizetime = sp[39];
			String scaninfo = "wx"+AutoConfig.SPLIT+lng+AutoConfig.SPLIT+lat+AutoConfig.SPLIT+province+AutoConfig.SPLIT+city+AutoConfig.SPLIT+district+AutoConfig.SPLIT+street+AutoConfig.SPLIT+num+AutoConfig.SPLIT+
					head+AutoConfig.SPLIT+nick+AutoConfig.SPLIT+enc+AutoConfig.SPLIT+ip+AutoConfig.SPLIT+scantime+AutoConfig.SPLIT+prizetime;
			DataFiles.scanpositiondata(request.getParameter("eid"), scaninfo);
			DataFiles.prepare4search(request.getParameter("eid"), request.getParameter("sp"));
			AutoConfig.innerechok(response, "ok");
		}catch(Exception e){
			AutoConfig.innerechno(response, request, e);
		} finally{
			AutoConfig.iamdone();
		}
	}

}
