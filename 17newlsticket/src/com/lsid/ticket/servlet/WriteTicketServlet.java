package com.lsid.ticket.servlet;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.ticket.listener.TicketExpire;

@SuppressWarnings("serial")
public class WriteTicketServlet extends HttpServlet {
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public WriteTicketServlet() {
		super();
	}

	@Override
	public void destroy(){
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
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		AutoConfig.iamrunning();
		try {
			TicketExpire.cache(request.getParameter("ticket"), request.getParameter("value"), request.getParameter("type"), false);
	    	AutoConfig.innerechok(response, "ok");
		} catch (Exception e) {
			AutoConfig.log(e,"error processing request ["+request.getRequestURI()+"] due to below exception:");
			AutoConfig.innerechno(response, request, e);
		} finally {
			AutoConfig.iamdone();
		}
	}
	
}
