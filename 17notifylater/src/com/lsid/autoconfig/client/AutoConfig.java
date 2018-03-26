package com.lsid.autoconfig.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import com.lsid.util.DefaultCipher;

public class AutoConfig extends Thread implements Filter {
	public static final String SPLIT = "#";
	public static final String SPLIT_HBASE = "_";
	private static final Path autoconfigfolder = Paths.get("autoconfig");
	public static boolean isrunning = false;
	private static int shutdowninsec = -1;
	private static Map<String, Properties> config = new Hashtable<String, Properties>();
	private static int running = 0;
	public void destroy() {

    }
	
	public static boolean fromwx(String data){
		return data!=null&&!data.isEmpty()&&data.contains(SPLIT+"wx"+SPLIT);
	}
	
	public static boolean finished(String data){
		return data!=null&&!data.isEmpty()&&data.startsWith("finish"+SPLIT);
	}

	public static boolean more(String data){
		return data!=null&&!data.isEmpty()&&data.startsWith("more"+SPLIT);
	}
	
	public static boolean uuid(String data){
		return data!=null&&!data.isEmpty()&&data.startsWith("uuid"+SPLIT);
	}
	
	public static boolean info(String data){
		return data!=null&&!data.isEmpty()&&data.startsWith("info"+SPLIT);
	}
	
	public static boolean playid(String data){
		return data!=null&&!data.isEmpty()&&data.startsWith("playid"+SPLIT);
	}
	
	public static boolean playfirst(String[] data){
		return data.length==22;
	}
	
	public static boolean playluck(String[] data){
		return data.length==35;
	}
	
	public static boolean playfinish(String[] data){
		return data.length==38;
	}
	
	public static String getfrom(String[] data){
		return data[1];
	}
	
	public static String getuuid(String[] data){
		return data[17];
	}
	
	public static String dataweixininit(String eid, String enc, String encproda, String encprodna, String useragent, String scanip, String loc) throws UnsupportedEncodingException{
		return "uuid"+SPLIT+"wx"+SPLIT+eid+SPLIT+enc+SPLIT+encproda+SPLIT+encprodna+SPLIT+URLEncoder.encode(useragent, "UTF-8")+SPLIT+scanip+SPLIT+loc+SPLIT+System.currentTimeMillis();
	}
	
	public static String databeforeinfo(String data, String uuid){
		return "info"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+uuid;
	}
	
	public static String datafterinfo(String data, String encuserinfo){
		return "playid"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+System.currentTimeMillis()+SPLIT+encuserinfo;
	}
	
	public static String datafterplayid(String data, String playid){
		return "play"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+playid;
	}

	public static String datafteractivityid(String data, String activityid){
		return "play"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+activityid;
	}

	public static String datamore(String data){
		return "more"+SPLIT+data.substring(data.indexOf(SPLIT)+1);
	}

	public static String datafterpoolid(String data, String event, String useragent, String playip, String loc, String poolid) throws UnsupportedEncodingException{
		return "play"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+event+SPLIT+URLEncoder.encode(useragent, "UTF-8")+SPLIT+playip+SPLIT+loc+SPLIT+System.currentTimeMillis()+SPLIT+poolid;
	}

	public static String datarepoolid(String data, String poolid) throws UnsupportedEncodingException{
		return "play"+SPLIT+data.substring(data.indexOf(SPLIT)+1, data.lastIndexOf(SPLIT))+SPLIT+poolid;
	}

	public static String datafterluck(String data, String prizeid, String require) throws Exception{
		return "luck"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+prizeid+SPLIT+DefaultCipher.enc(require)+SPLIT+System.currentTimeMillis();
	}

	public static String datafinish(String data, String userinput) throws Exception{
		return "finish"+SPLIT+data.substring(data.indexOf(SPLIT)+1)+SPLIT+DefaultCipher.enc(userinput)+SPLIT+System.currentTimeMillis();
	}

	public static String dataupdatelastatus(String data, String status) throws Exception{
		String[] d = data.split(SPLIT);
		String prefix = data;
		if (d.length==42) {
			String temp = data.substring(0, data.lastIndexOf(SPLIT));
			prefix = temp.substring(0, temp.lastIndexOf(SPLIT));
		} 
		return prefix+SPLIT+DefaultCipher.enc(status)+SPLIT+System.currentTimeMillis();	
	}

	public static String getencuserinfo(String[] data){
		return data[19];
	}
	
	public static String getencprodainfo(String[] data){
		return data[4];
	}

	public static String getencprodnainfo(String[] data){
		return data[5];
	}

	public static String getenc(String[] data){
		return data[3];
	}
	
	public static String geteid(String[] data){
		return data[2];
	}
	
	public static long getintime(String[] data){
		return Long.parseLong(data[16]);
	}
	
	public static String getinip(String[] data){
		return data[7];
	}

	public static String getplayid(String[] data){
		return data[20];
	}

	public static long getfirstplaytime(String[] data){
		return Long.parseLong(data[33]);
	}

	public static long getlucktime(String[] data){
		return Long.parseLong(data[37]);
	}

	public static String getpoolid(String[] data){
		return data[34];
	}

	public static String getprizeid(String[] data){
		return data[35];
	}
	
	public static String getsysrequire(String[] data){
		return data[36];
	}
	
	public static synchronized void iamrunning(){
		running++;
	}
	
	public static synchronized void iamdone(){
		running--;
	}
	
	public static String config(String eid, String key){
		if (config.isEmpty()){
			init();
		}
		if (key!=null&&!key.trim().isEmpty()){
			if (eid==null||eid.trim().isEmpty()){
				eid = "default";
			}
			if (config.get(eid)==null){
				config.put(eid, new Properties());
			}
			if (config.get(eid).getProperty(key)==null){
				try{
					String value = innerpost(config(null,"captainhost")+"get",Integer.parseInt(config(null,"readcaptain.connectimeoutinsec")), Integer.parseInt(config(null, "readcaptain.socketimeoutinsec")),"eid",eid,"key", key,"sailorport",config.get("default").getProperty("myport"));
					config.get(eid).setProperty(key, value);
				}catch(Exception ex){
					if (config.get(eid).getProperty(key)==null){
						config.get(eid).setProperty(key,"");
					}
				}
			}
			if (key.toLowerCase().contains("secret")||key.toLowerCase().contains("key")){
				try{
					return DefaultCipher.dec(config.get(eid).getProperty(key));
				}catch(Exception ex){
					//do nothing
				}
			}
		}
		return config.get(eid).getProperty(key);
	}
	
	public void run(){
		while(true){
			if (config.get("default")==null||config.get("default").getProperty("captainhost")==null||
					config.get("default").getProperty("captainhost").isEmpty()) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				if (!isrunning){
					shuttingdown(shutdowninsec);
				} else {
					try {
						Thread.sleep(Integer.parseInt(config(null, "readcaptain.intervalinsec"))*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!isrunning){
						shuttingdown(shutdowninsec-Integer.parseInt(config(null, "readcaptain.intervalinsec")));
					} else {
						Set<String> eids = new HashSet<String>();
						eids.addAll(config.keySet()); 
			    		for (String eid: eids){
			    			if (isrunning){
			    				Set<Object> keys = new HashSet<Object>();
								keys.addAll(config.get(eid).keySet());  
						        for (Object key:keys){ 
			    					if (isrunning){
			    						try {
											Thread.sleep(100);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
			    						if (key.equals("myport")) {
			    							continue;
			    						}
			    						
		    							try {
											String value = innerpost(config(null,"captainhost")+"get",Integer.parseInt(config(null, "readcaptain.connectimeoutinsec")), Integer.parseInt(config(null, "readcaptain.socketimeoutinsec")),"eid",eid,"key", key.toString(),"sailorport",config.get("default").getProperty("myport"));
											config.get(eid).setProperty(key.toString(), value);
			    						} catch (Exception e) {
											log(e,"failed in refreshing "+eid+"."+key+" due to below exception:");
										}
			    					} else {
					    				shuttingdown(shutdowninsec);
					    			}
			    				}
			    				BufferedWriter bw = null;
			    				try {
			    					bw = Files.newBufferedWriter(autoconfigfolder.resolve(eid), Charset.forName("UTF-8"),StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
									config.get(eid).store(bw,"auto generated");
								} catch (IOException e) {
									log(e,"failed in persisting "+eid+" due to below exception:");
								}finally{
									if (bw!= null){
										try {
											bw.close();
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										bw = null;
									}
								}
			    			} else {
			    				shuttingdown(shutdowninsec);
			    			}
			    		}
					}
				}
			}
		}
	}
	
	private void shuttingdown(int insec){
		if (insec>0){
			try {
				Thread.sleep(insec*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		while(running>0){
			try {
				System.out.println("========"+new Date()+"========waiting till running job exit");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("========"+new Date()+"========shutdown after captain");
		System.exit(0);
	}
	
    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
    	HttpServletResponse response = (HttpServletResponse)res;
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");		
    	response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("XDomainRequestAllowed","1");
		config.get("default").setProperty("myport", String.valueOf(request.getLocalPort()));
		if (request.getRequestURI().endsWith("autoconfig")){
			if (request.getParameter("shutdowninsec")!=null){
				try{
					shutdowninsec = Integer.parseInt(request.getParameter("shutdowninsec"));
					isrunning = false;
					innerechok(response, "ok");
				}catch(Exception ex){
					innerechno(response, "shutdowninsecnotnumber");
				}
			} else if (request.getParameter("captainport")!=null&&request.getParameter("captaincontext")!=null){
				try{
					String captainhost = "http://"+getremoteip(request)+":"+request.getParameter("captainport")+request.getParameter("captaincontext")+"/";
					int connectimeoutinsec = 60;
					try{
						connectimeoutinsec=Integer.parseInt(request.getParameter("connectimeoutinsec"));
					}catch(Exception ex){
						//do nothing
					}
					int socketimeoutinsec = 60;
					try{
						socketimeoutinsec=Integer.parseInt(request.getParameter("socketimeoutinsec"));
					}catch(Exception ex){
						//do nothing
					}
					innerpost(captainhost+"get",connectimeoutinsec, socketimeoutinsec,"key","test");
					config.get("default").setProperty("captainhost", captainhost);
					isrunning = true;
					config.get("default").setProperty("readcaptain.connectimeoutinsec", String.valueOf(connectimeoutinsec));
					config.get("default").setProperty("readcaptain.socketimeoutinsec", String.valueOf(socketimeoutinsec));
					System.out.println("========"+new Date()+"========init config ["+config+"]");
					innerechok(response, "ok");
				}catch(Exception ex){
					innerechno(response, "captainunavailable");
				}
			} else {
				innerechno(response, "invalidsailorequest");
			}
		} else {
			if (isrunning){
				if (!config(null,"captainhost").isEmpty()){
					chain.doFilter(req, res);
				} else {
					innerechno(response, request, new Exception("invalidsailor"));
				}
			} else {
				innerechno(response, request, new Exception("shuttingdown"));
			}
		}
		
    }
    
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		init();
	}
	
	private static synchronized void init(){
		if (config.isEmpty()){
			try {
				Files.createDirectories(autoconfigfolder);
			} catch (IOException e) {
				log(e,"System exited due to below exception:");
				System.exit(1);
			}
			String[] configfiles = autoconfigfolder.toFile().list();
			if (configfiles!=null){
				for (String filepath:configfiles){
						Properties p = new Properties();
						InputStream is = null;
						try{
							is = Files.newInputStream(autoconfigfolder.resolve(filepath));
							p.load(is);
						}catch(Exception e){
							log(e,"error loading "+filepath);
							System.exit(1);
						} finally {
							if (is!=null){
								try {
									is.close();
								} catch (IOException e) {
									//do nothing
								}
								is = null;
							}
						}
						config.put(filepath, p);
				}
			}
			if (config.get("default")==null){
				config.put("default", new Properties());
			}
			System.out.println("========"+new Date()+"========loaded config ["+config+"]");
			if (config.get("default").getProperty("captainhost")!=null&&!config.get("default").getProperty("captainhost").isEmpty()) {
				isrunning = true;
			}
			new AutoConfig().start();
		}
	}
	
	public static String getremoteip(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)){
        	ip = "127.0.0.1";
        }
        return ip;
    }
	
	public static void innerechok(HttpServletResponse response, String content) throws IOException{
		outerecho(response, "=0=k="+content);
	}

	public static void outerecho(HttpServletResponse response, String content) throws IOException{
		PrintWriter p = response.getWriter();		
		p.write(content);
		p.flush();
		p.close();
		p=null;
	}

	public static void innerechno(HttpServletResponse response, String content) throws IOException{
		outerecho(response, "=n=0="+content);
	}

	public static void innerechno(HttpServletResponse response, HttpServletRequest request, Exception e) throws IOException{
		if (!config(null,"debug").isEmpty()){
			System.out.println("========"+new Date()+"======== error processing "+request.getRequestURI());
			e.printStackTrace();
		}
		innerechno(response, e.toString()+"("+request.getServerName()+":"+request.getServerPort()+")");
	}
	
	public static void log(Exception e, String extra){
		if (!config(null,"debug").isEmpty()){
			System.out.println("========"+new Date()+"========"+extra);
			e.printStackTrace();
		}
	}

	public static String innerpost(String url, int connectimeoutsec, int socketimeoutsec, String ...paramvalues) throws Exception{
		String returnvalue = outerpost(url, connectimeoutsec, socketimeoutsec, paramvalues);
		if (returnvalue!=null){
			if (returnvalue.startsWith("=0=k=")){
				return returnvalue.substring("=0=k=".length());
			} else if (returnvalue.startsWith("=n=0=")){
				throw new Exception(returnvalue.substring("=n=0=".length()));
			} else {
				throw new Exception(returnvalue+"responsefrom["+url+"]");
			}
		} else {
			throw new Exception("nullresponsefrom["+url+"]");
		}
	}
	
	public static String outerpost(String url, int connectimeoutsec, int socketimeoutsec, String ...paramvalues) throws Exception{
		Form f = Form.form();
		for (int i=0;i<paramvalues.length;i+=2){
			f.add(paramvalues[i], paramvalues[i+1]);
		}
		return Request.Post(url).connectTimeout(connectimeoutsec*1000).socketTimeout(socketimeoutsec*1000).bodyForm(f.build(),Charset.forName("UTF-8")).execute().returnContent().asString(Charset.forName("UTF-8"));
	}

	
	private static String postrotation(String eid, String targethostskey, int connectimeoutsec, int socketimeoutsec, String ...paramvalues) throws Exception{
		return innerpost(rotation(eid, targethostskey), connectimeoutsec, socketimeoutsec, paramvalues);
	}
	
	private static String posthash(String eid, String targethostskey, String hash, int connectimeoutsec, int socketimeoutsec, String ...paramvalues) throws Exception{
		return innerpost(hash(eid, targethostskey, hash), connectimeoutsec, socketimeoutsec, paramvalues);
	}
	
	public static String hash(String eid, String targethostskey, String hash) throws Exception{
		if (hash==null||hash.trim().isEmpty()||hash.trim().toLowerCase().equals("null")){
			throw new Exception("needhash");
		}
		String[] targethosts = config(eid, targethostskey).split(SPLIT);
		if (targethosts.length==1){
			return targethosts[0];
		}
		return targethosts[Math.abs(hash.hashCode())%targethosts.length];
	}
	
	public static String rotation(String eid, String targethostskey){
		String[] targethosts = config(eid, targethostskey).split(SPLIT);
		if (targethosts.length==1){
			return targethosts[0];
		}
		return targethosts[new Random().nextInt(targethosts.length)];
	}
	
    public static void cachecodedata(String eid, String enc, String table, String row, String col, String value) throws Exception {
    	posthash(eid, "lsid.interface.cache.code.write", enc, Integer.parseInt(config(eid,"lsid.interface.cache.code.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.code.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", enc, "table", table, "row", row, "col", col, "value", value);
    }
    
    public static String cachecodedata(String eid, String enc, String table, String row, String col) throws Exception {
    	return posthash(eid, "lsid.interface.cache.code.read", enc, Integer.parseInt(config(eid,"lsid.interface.cache.code.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.code.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", enc, "table", table, "row", row, "col", col);
    }
    
    public static void cacheuserdata(String eid, String uuid, String table, String row, String col, String value) throws Exception {
    	posthash(eid, "lsid.interface.cache.user.write", uuid, Integer.parseInt(config(eid,"lsid.interface.cache.user.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.user.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", uuid, "table", table, "row", row, "col", col, "value", value);
    }
    
    public static String cacheuserdata(String eid, String uuid, String table, String row, String col) throws Exception {
    	return posthash(eid, "lsid.interface.cache.user.read", uuid, Integer.parseInt(config(eid,"lsid.interface.cache.user.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.user.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", uuid, "table", table, "row", row, "col", col);
    }

    public static void nocache(String eid, String table, String row, String col, String value) throws Exception {
    	postrotation(null, "lsid.interface.nocache.write", Integer.parseInt(config(null,"lsid.interface.nocache.connectimeoutinsec")), Integer.parseInt(config(null,"lsid.interface.nocache.socketimeoutinsec")), "eid", eid==null?"default":eid, "table", table, "row", row, "col", col, "value", value);
    }
    
    public static long incrementcache(String eid, String hash, String table, String row, String col, int amount) throws Exception {
    	return Long.valueOf(posthash(eid, "lsid.interface.cache.increment.write", hash, Integer.parseInt(config(eid,"lsid.interface.cache.increment.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.increment.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", hash, "table", table, "row", row, "col", col, "amount", String.valueOf(amount)));
    }

    public static long incrementcachemore(String eid, String hash, String table, String row, String col, int amount) throws Exception {
    	return Long.valueOf(posthash(eid, "lsid.interface.cache.increment.write", hash, Integer.parseInt(config(eid,"lsid.interface.cache.increment.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.increment.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", hash, "table", table, "row", row, "col", col, "amount", String.valueOf(amount), "more", ""));
    }

    public static long incremented(String eid, String hash, String table, String row, String col) throws Exception {
    	return Long.valueOf(posthash(eid, "lsid.interface.cache.increment.read", hash, Integer.parseInt(config(eid,"lsid.interface.cache.increment.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.increment.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", hash, "table", table, "row", row, "col", col));
    }
    
    public static String position(String eid, String hash, String table, String row, String col) throws Exception {
    	return posthash(eid, "lsid.interface.cache.increment.read", hash, Integer.parseInt(config(eid,"lsid.interface.cache.increment.connectimeoutinsec")), Integer.parseInt(config(eid,"lsid.interface.cache.increment.socketimeoutinsec")), "eid", eid==null?"default":eid, "hash", hash, "table", table, "row", row, "col", col, "position", "");
    }
    
    public static String generateticket(String ticketvalue, String type) throws Exception{
    	String ticket = UUID.randomUUID().toString().replaceAll("-", "");
    	posthash(null, "lsid.interface.ticket.write", ticket, Integer.parseInt(config(null,"lsid.interface.ticket.connectimeoutinsec")), 
    			Integer.parseInt(config(null, "lsid.interface.ticket.socketimeoutinsec")), 
    			"ticket", ticket, "value", ticketvalue, "type", type);
		return ticket;
    }

    public static String readticket(String ticket) throws Exception{
    	String ticketvalue = posthash(null, "lsid.interface.ticket.read", ticket, Integer.parseInt(config(null,"lsid.interface.ticket.connectimeoutinsec")), 
    			Integer.parseInt(config(null, "lsid.interface.ticket.socketimeoutinsec")), 
    			"ticket", ticket);
    	if (ticketvalue.isEmpty()){
    		throw new Exception("expireticket");
    	}
    	if (ticketvalue.equals("invalidticket")){
    		throw new Exception("invalidticket");
    	}
    	return ticketvalue;
    }

}
