package com.lsid.console.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;

/**
 * Servlet implementation class WebSiteServlet
 */
@WebServlet("/WebSiteServlet")
public class WebSiteServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Path datafolder = Paths.get("/data/17new/website/");
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public WebSiteServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			doPost(request,response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		try{			
			String type=request.getParameter("type");
			
			Map<String, Object> result = new HashMap<String, Object>();
			if(type.equals("dianzan")){
				if (!Files.exists(datafolder.resolve("dianzan"))){
					Files.createDirectories(datafolder.resolve("dianzan"));
				}			
				
				String typeid=request.getParameter("id");
				List ndz=new ArrayList<>();	
				System.out.println("==="+Files.exists(datafolder.resolve("dianzan").resolve("dianzan")));
				if(Files.exists(datafolder.resolve("dianzan").resolve("dianzan"))){
					List dz=Files.readAllLines(datafolder.resolve("dianzan").resolve("dianzan"),Charset.forName("UTF-8"));
					int size=dz.size();	
					String strall="";
					for(int i=0;i<size;i++){
						String []content=dz.get(i).toString().split(",");
						int cnt=Integer.valueOf(content[1]);
						if(typeid.equals(content[0])){
							cnt+=1;
						}
						strall+=content[0]+","+cnt+"\r\n";
						String str1=content[0]+","+cnt;
						ndz.add(str1);
					}				
					Files.write(datafolder.resolve("dianzan").resolve("dianzan"), strall.getBytes("UTF-8"),StandardOpenOption.TRUNCATE_EXISTING);
				}else{
					String str=typeid+",1"+"\r\n";
					String str1=typeid+",1";
					Files.write(datafolder.resolve("dianzan").resolve("dianzan"), str.getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
					ndz.add(str1);
				}
				result.put("result", "success");
				result.put("dianzan", ndz);
				AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
			}else if(type.equals("duzan")){
				List dz=Files.readAllLines(datafolder.resolve("dianzan").resolve("dianzan"),Charset.forName("UTF-8"));
				result.put("result", "success");
				result.put("dianzan", dz);
				AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
			}else if(type.equals("news")){
				String newsid=request.getParameter("newsid");
				System.out.println("type===+"+type+"==="+newsid);
				Path filepath=datafolder.resolve("news");
				String filename="/data/17new/website/news/"+newsid+".txt";
				List<String> lines=new ArrayList<String>();  
				BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF-8"));  
				String line = null;  
				while ((line = br.readLine()) != null) {  
				      lines.add(line);  
				}  
				br.close(); 
				String fname="biaoti.txt";
				String title="";
				String pretitle="";
				String nexttitle="";
				int cnt=0;
				String createdate="";
				List other=Files.readAllLines(filepath.resolve(fname),Charset.forName("UTF-8"));
				
				String strall="";
				int preid=0;
				int nextid=0;
				for(int i=0;i<other.size();i++){
					String oneline=other.get(i).toString();
					String [] bt=oneline.split(";");
					 preid=Integer.valueOf(newsid)-1;
					 nextid=Integer.valueOf(newsid)+1;
						if(newsid.equals(bt[1])){
							title=bt[0];
							createdate=bt[2];
							cnt=Integer.valueOf(bt[3])+1;
							oneline=bt[0]+";"+bt[1]+";"+bt[2]+";"+cnt;
						}
						//System.out.println("==b1="+bt[1]);
						int id=Integer.valueOf(bt[1]);
						if(preid==id){
							pretitle=bt[0];
						}
						if(nextid==id){
							nexttitle=bt[0];
						}
						strall+=oneline+"\r\n";
				}
				Files.write(filepath.resolve(fname), strall.getBytes("UTF-8"),StandardOpenOption.TRUNCATE_EXISTING);
				
				List pic =new ArrayList<>();
				List filepic=Files.readAllLines(filepath.resolve("pic.txt"),Charset.forName("UTF-8"));
				for(int i=0;i<filepic.size();i++){
					String oneline=filepic.get(i).toString();
					if(oneline.startsWith(newsid)){
						pic.add(oneline);
					}
				}
				result.put("result", "success");
				result.put("wenben", lines);
				result.put("pic", pic);
				result.put("title", title);
				result.put("createdate", createdate);
				result.put("cnt", cnt);
				result.put("pretitle", pretitle);
				result.put("nexttitle", nexttitle);
				result.put("preid", preid);
				result.put("nextid", nextid);
				AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));				
			}
		}catch(Exception e){
			e.printStackTrace();
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("result", "fail");
			result.put("reason", e.toString());		
			AutoConfig.outerecho(response, new ObjectMapper().writeValueAsString(result));
		}
	}

}
