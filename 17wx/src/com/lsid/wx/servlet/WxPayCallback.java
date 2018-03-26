package com.lsid.wx.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.wx.util.WxPay;
import com.lsid.wx.util.Xml2Map;


@SuppressWarnings("serial")
public class WxPayCallback extends HttpServlet {
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public WxPayCallback() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		getWeChatPayReturn(request);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String result=getWeChatPayReturn(request);
		PrintWriter p = response.getWriter();		
		p.write(result);
		p.flush();
		p.close();
		p=null;	
	}
	
	public String getWeChatPayReturn(HttpServletRequest request){
	    try {
	            ServletInputStream inStream = request.getInputStream();
	            int _buffer_size = 1024;
	            if (inStream != null) {
	                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	                byte[] tempBytes = new byte[_buffer_size];
	                int count = -1;
	                while ((count = inStream.read(tempBytes, 0, _buffer_size)) != -1) {
	                    outStream.write(tempBytes, 0, count);
	                }
	                tempBytes = null;
	                outStream.flush();
	                //将流转换成字符串
	                String result = new String(outStream.toByteArray(), "UTF-8");
	                //将字符串解析成XML
	                Document doc = DocumentHelper.parseText(result);
	                //将XML格式转化成MAP格式数据
	                Map<String, Object> resultMap = Xml2Map.Dom2Map(doc);
	                String out_trade_no=resultMap.get("out_trade_no").toString();
	                String eid = out_trade_no.substring(out_trade_no.lastIndexOf(AutoConfig.SPLIT_HBASE)+1);
	                String openid=resultMap.get("openid").toString();
	                if (resultMap.get("result_code").toString().equalsIgnoreCase("SUCCESS")) { 
	                	 if (verifyWeixinNotify(eid,resultMap)) {  
	                		 //TODO update internal order status.
	                	 }	                	
	                }
	            }
	            //通知微信支付系统接收到信息
	        return "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
	        		+ "<return_msg><![CDATA[OK]]></return_msg></xml>";
	        } catch (Exception e) {
	            AutoConfig.log(e, "Failed in handling weixin pay callback due to below exception:");
	            return "fail";
	        }
	} 
	
    /** 
     * 验证签名 
     *  
     * @param map 
     * @return 
     */  
    public boolean verifyWeixinNotify(String eid, Map<String, Object> map) {  
        SortedMap<String, String> parameterMap = new TreeMap<String, String>();  
        String sign = (String) map.get("sign");  
        for (Object keyValue : map.keySet()) {  
            if (!keyValue.toString().equals("sign")) {  
                parameterMap.put(keyValue.toString(), map.get(keyValue).toString());  
            }  
        }  
        String createSign = WxPay.GetSign(eid, parameterMap);  
        if (createSign.equals(sign)) {  
            return true;  
        } else {  
            return false;  
        }
    }

}
