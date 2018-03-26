package com.lsid.console.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class DataFiles {
	private static Path datafolder = Paths.get("/data/17new/consoledata/");
	
	static{
		new Thread(new Runnable(){

			@Override
			public void run() {
				scanpositiondataremove();
			}
			
		}).start();
	}
	
	public static String login(String name, String password) throws IOException{
		if (password!=null&&Files.exists(datafolder.resolve("login").resolve(name.toLowerCase()+sign(password)))){
			return Files.readAllLines(datafolder.resolve("login").resolve(name.toLowerCase()+sign(password)), Charset.forName("UTF-8")).get(0);
		} else {
			return null;
		}
	}
	
	public static List<String> list(String createdby) throws IOException{
		List<String> returnvalue = new ArrayList<String>();
		String[] loginfiles = datafolder.resolve("login").toFile().list();
		if (loginfiles!=null){
			for (String loginfile:loginfiles){
				if (Files.exists(datafolder.resolve("login").resolve(loginfile))){
					String content = Files.readAllLines(datafolder.resolve("login").resolve(loginfile), Charset.forName("UTF-8")).get(0);
					if (content.contains(AutoConfig.SPLIT+createdby+AutoConfig.SPLIT)){
						returnvalue.add(content);
					}
				}
			}
		}
		return returnvalue;
	}
	
	public static void changepassword(String name, String oldpassword, String newpassword) throws IOException{
		if (!Files.exists(datafolder.resolve("login"))){
			Files.createDirectories(datafolder.resolve("login"));
		}
		String content = login(name.toLowerCase(), oldpassword);
		Files.write(datafolder.resolve("login").resolve(name.toLowerCase()+sign(newpassword)), content.getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
		Files.deleteIfExists(datafolder.resolve("login").resolve(name.toLowerCase()+sign(oldpassword)));
	}

	public static void changecontent(String oldname, String newname, String password, String content) throws IOException{
		if (!Files.exists(datafolder.resolve("login"))){
			Files.createDirectories(datafolder.resolve("login"));
		}
		if (Files.exists(datafolder.resolve("login").resolve(oldname.toLowerCase()+sign(password)))){
			if (oldname.toLowerCase().equals(newname.toLowerCase())){
				Files.write(datafolder.resolve("login").resolve(oldname.toLowerCase()+sign(password)), content.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} else {
				Files.write(datafolder.resolve("login").resolve(newname.toLowerCase()+sign(password)), content.getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
				Files.deleteIfExists(datafolder.resolve("login").resolve(oldname.toLowerCase()+sign(password)));
			}
		} else {
			Files.write(datafolder.resolve("login").resolve(newname.toLowerCase()+sign(password)), content.getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
		}
	}
	
	public static void changecontent(String name, String id, String content) throws IOException{
		String[] loginfiles = datafolder.resolve("login").toFile().list();
		if (loginfiles!=null){
			for (String loginfile:loginfiles){
				if (loginfile.startsWith(name.toLowerCase())&&Files.exists(datafolder.resolve("login").resolve(loginfile))){
					String oldcontent = Files.readAllLines(datafolder.resolve("login").resolve(loginfile), Charset.forName("UTF-8")).get(0);
					if (oldcontent.startsWith(id+AutoConfig.SPLIT)){
						Files.write(datafolder.resolve("login").resolve(loginfile), content.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
						break;
					}
				}
			}
		}
	}
	
	public static Map<String, Object> scanposition(String eid) throws IOException{
		String[] files = datafolder.resolve("position").resolve(eid).toFile().list();
		if (files==null){
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("total", 0);
			return p;
		}
		Map<String, Object> p = new HashMap<String, Object>(files.length);
		for (int i = 0; i < files.length;i++){
			try{
				p.put("data"+i,Files.readAllLines(datafolder.resolve("position").resolve(eid).resolve(files[i]),Charset.forName("UTF-8")).get(0));
			}catch(Exception e){
				//do nothing
			}
		}
		p.put("total", files.length);
		return p;
	}
	
	private static void scanpositiondataremove(){
		while(AutoConfig.isrunning){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try{
				Files.walkFileTree(datafolder.resolve("position"), new FileVisitor<Object>(){
					
					@Override
					public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}
	
					@Override
					public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}
	
					@Override
					public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
						if (AutoConfig.isrunning){
							if (!Files.isDirectory(Paths.get(file.toString()))){
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								try{
									if (System.currentTimeMillis()-Files.getLastModifiedTime(Paths.get(file.toString())).toMillis()>10000){
										Files.deleteIfExists(Paths.get(file.toString()));
									}
								}catch(Exception e){
									//do nothing
								}
							}
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
						
					}
	
					@Override
					public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException {
						if (AutoConfig.isrunning){
							return FileVisitResult.CONTINUE;
						} else {
							return FileVisitResult.TERMINATE;
						}
					}  
					  
				});
			}catch(Exception e){
				AutoConfig.log(e, "System exited due to below exception:");
				System.exit(1);
			}
		}
	}

	public static Map<String, Object> repository(String eid, String begin, String end) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		returnvalue.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		returnvalue.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		Map<String, Map<String, Object>> temp = new HashMap<String, Map<String, Object>>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			String[] files = datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("a").resolve(date).toFile().list();
			if (files!=null) {
				for (String file:files) {
					Map<String, Object> d = null;
					if (temp.get(file+date)==null) {
						d = new HashMap<String, Object>();
						data.add(d);
						temp.put(file+date, d);
					} else {
						d = temp.get(file+date);
					}
					String[] parts = file.split("_-_");
					String line = "null";
					if (parts.length>0) {
						line = AutoConfig.config(eid, "lsid.code.linea."+parts[0]).isEmpty()?parts[0]:AutoConfig.config(eid, "lsid.code.linea."+parts[0]);
					}
					String batch = "null";
					if (parts.length>1) {
						batch = AutoConfig.config(eid, "lsid.code.batcha."+parts[1]).isEmpty()?parts[1]:AutoConfig.config(eid, "lsid.code.batcha."+parts[1]);
					}
					String prod = "null";
					if (parts.length>2) {
						prod = AutoConfig.config(eid, "lsid.prod.desc."+parts[2]).isEmpty()?parts[2]:AutoConfig.config(eid, "lsid.prod.desc."+parts[2]);
					}
					d.put("linenum",line);
					d.put("batchnum",batch);
					d.put("prodnum", prod);
					if (d.get("active")==null) {
						d.put("active", Files.size(datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("a").resolve(date).resolve(file)));
					} else {
						d.put("active", Long.parseLong(d.get("active").toString())+Files.size(datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("a").resolve(date).resolve(file)));
					}
					d.put("date", date);
			
				}
			}
			files = datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("na").resolve(date).toFile().list();
			if (files!=null) {
				for (String file:files) {
					Map<String, Object> d = null;
					if (temp.get(file+date)==null) {
						d = new HashMap<String, Object>();
						data.add(d);
						temp.put(file+date, d);
					} else {
						d = temp.get(file+date);
					}
					String[] parts = file.split("_-_");
					String line = "null";
					if (parts.length>0) {
						line = AutoConfig.config(eid, "lsid.code.linea."+parts[0]).isEmpty()?parts[0]:AutoConfig.config(eid, "lsid.code.linea."+parts[0]);
					}
					String batch = "null";
					if (parts.length>1) {
						batch = AutoConfig.config(eid, "lsid.code.batcha."+parts[1]).isEmpty()?parts[1]:AutoConfig.config(eid, "lsid.code.batcha."+parts[1]);
					}
					String prod = "null";
					if (parts.length>2) {
						prod = AutoConfig.config(eid, "lsid.prod.desc."+parts[2]).isEmpty()?parts[2]:AutoConfig.config(eid, "lsid.prod.desc."+parts[2]);
					}
					d.put("linenum",line);
					d.put("batchnum",batch);
					d.put("prodnum", prod);
					if (d.get("inactive")==null) {
						d.put("inactive", Files.size(datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("na").resolve(date).resolve(file)));
					} else {
						d.put("inactive", Long.parseLong(d.get("inactive").toString())+Files.size(datafolder.resolve("hadoop").resolve(eid).resolve("repositorydata").resolve("na").resolve(date).resolve(file)));
					}
					d.put("date", date);
			
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		
		returnvalue.put("data", data);
		return returnvalue;
	}
	/*
	 finish#
	 wx#
	 qq#
	 JVx%2BnpYUfsmuoNBPFO5PXOLsPZSwz9jkit6ld%2FgLdt0%3D#
	 JVx%2BnpYUfsmuoNBPFO5PXF7jkvFyCJWbxcaylNTexgaGFIMTEL2pAfTb%2BccrFZIuN%2FE0RWQImAQz%0AuLlp1LLS8A%3D%3D#
	 #
	 Mozilla%2F5.0+%28Linux%3B+Android+7.1.1%3B+ONEPLUS+A5000+Build%2FNMF26X%3B+wv%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Version%2F4.0+Chrome%2F53.0.2785.49+Mobile+MQQBrowser%2F6.2+TBS%2F043313+Safari%2F537.36+MicroMessenger%2F6.5.10.1080+NetType%2FWIFI+Language%2Fzh_CN#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501049400656#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 1501049406496#
	 l%2FSGu5OlE7PRpyxJ7ShfJcyOOAUvxN9i3E%2BDNB%2BLCn7Nf49iIkkcBscoJdeOn4nA6pyA7ksrjZWt%0ABZN10nZmiRMSrV4qzj21%2BVYu8GkJbkg7kL4dcqdcwh1kooCQ5Q4r2X0HZaTuxu8foXPX7ft4MrK4%0Athfq0STo6XqIY0mPglD6pIYly2X8gLG6z5XM9PlhnDnxAU0yJuAmMz79254mNh2ficcLwURt3App%0AKc99iYYOZjzhlg2Vt5uUcdUNhieyJG2Q6KDxkpBG%2B0TFfq%2F%2BeepKn2gqGibjTQTxnrVfWz1LWvWF%0AP%2F%2FHcJMtYWoRFpf3cyOy44ApLnbA8wU%2BBNfYubX3NApuX%2BtWkdipvUUP64m9dCsssWVIRvhsmOOJ%0Akw%2BD#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 0#
	 null#
	 Mozilla%2F5.0+%28Windows+NT+10.0%3B+WOW64%3B+rv%3A54.0%29+Gecko%2F20100101+Firefox%2F54.0#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501049687858#
	 0#
	 0#
	 aBdgbmrx2Jia5iyYgSddnw%3D%3D#
	 1501475236765#
	 K6qiBo6RgxWyZ0AIYNQCbw%3D%3D#
	 1501476768039#
	 enc(status)#
	 1501476768039#
	 */
	public static List<Map<String, String>> search(String eid, String day, int startrow, String code, String mobile, String nick) throws Exception{
		int resultrows = 100;
		List<Map<String, String>> returnvalue = new ArrayList<Map<String, String>>(resultrows);
		if (Files.exists(datafolder.resolve("tosearch").resolve(eid).resolve(day))&&!Files.isDirectory(datafolder.resolve("tosearch").resolve(eid).resolve(day))){
			BufferedReader br = null;
			try {
				String enc = "";
				if (code!=null&&!code.trim().isEmpty()) {
					try {
						DefaultCipher.dec(code);
						enc = code;
					}catch(Exception e) {
						enc = DefaultCipher.enc(code);
					}
				}
				if (startrow<0) {
					startrow=0;
				}
				if (mobile==null||mobile.trim().isEmpty()||"null".equals(mobile.toLowerCase())) {
					mobile="";
				}
				if (nick==null||nick.trim().isEmpty()||"null".equals(nick.toLowerCase())) {
					nick="";
				}
				Map<String, Long> encpoolprizelucktime = new HashMap<String, Long>();
				Map<String, String> encpoolprizedata = new HashMap<String, String>();
				br = Files.newBufferedReader(datafolder.resolve("tosearch").resolve(eid).resolve(day), Charset.forName("UTF-8"));
				String line = br.readLine();
				while(line!=null) {
					String[] d = line.split(AutoConfig.SPLIT);
					
					boolean containsmobile = false;
					if (mobile.isEmpty()) {
						containsmobile = true;
					} else {
						try {
							containsmobile = DefaultCipher.dec(d[d.length-2]).contains(mobile);
						}catch(Exception e) {
							try {
								containsmobile = DefaultCipher.dec(d[d.length-4]).contains(mobile);
							}catch(Exception e2) {
								//do nothing
							}
						}
					}
					if ((enc.isEmpty()||AutoConfig.getenc(d).contains(enc))&&containsmobile&&
							(nick.isEmpty()||new ObjectMapper().readTree(DefaultCipher.dec(AutoConfig.getencuserinfo(d))).get("nickname").asText().contains(nick))) {
						encpoolprizedata.put(AutoConfig.getenc(d)+AutoConfig.getpoolid(d)+AutoConfig.getprizeid(d), line);
						encpoolprizelucktime.put(AutoConfig.getenc(d)+AutoConfig.getpoolid(d)+AutoConfig.getprizeid(d), AutoConfig.getlucktime(d));
					}
					line = br.readLine();
				}
				Map<String, Long> sorted = sortMapByValue(encpoolprizelucktime);
				if (sorted!=null&&!sorted.isEmpty()) {
					int i=0;
					for (Map.Entry<String, Long> entry : sorted.entrySet()) {
						if (i>=startrow&&i-startrow<resultrows) {
							String[] d = encpoolprizedata.get(entry.getKey()).split(AutoConfig.SPLIT);
							Map<String, String> t = new HashMap<String, String>();
							t.put("from", AutoConfig.getfrom(d));
							t.put("openid", AutoConfig.getplayid(d));
							t.put("enc", AutoConfig.getenc(d));
							t.put("prize", AutoConfig.config(eid, "lsid.pool"+AutoConfig.getpoolid(d)+".prize"+AutoConfig.getprizeid(d)+".name"));
							t.put("nick", new ObjectMapper().readTree(DefaultCipher.dec(AutoConfig.getencuserinfo(d))).get("nickname").asText());
							t.put("headimgurl", new ObjectMapper().readTree(DefaultCipher.dec(AutoConfig.getencuserinfo(d))).get("headimgurl").asText());
							t.put("ip", d[24]);
							t.put("addr", d[25]+d[26]);
							if (d.length>39) {
								t.put("time", d[39]);
							} else {
								t.put("time", "");
							}
							t.put("sysreq", DefaultCipher.dec(AutoConfig.getsysrequire(d)));
							if (d.length>38) {
								t.put("userinput", DefaultCipher.dec(d[38]));
							} else {
								t.put("userinput", "");
							}
							if (d.length>40) {
								t.put("status", DefaultCipher.dec(d[40]));
							} else {
								t.put("status", "");
							}
							returnvalue.add(t);
						}
						i++;
						if (i-startrow>=resultrows) {
							break;
						}
					}
				}
			} finally {
				if (br!=null) {
					br.close();
				}
			}
		}
		return returnvalue;
	}
	
	public static Map<String, Object> toactive(String eid, String begin, String end) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		returnvalue.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		returnvalue.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		Map<String, Object> d = new HashMap<String, Object>();
		d.put("linenum","linenum"+new Random().nextInt(3));
		d.put("batchnum","batchnum"+new Random().nextInt(3));
		d.put("prodnum", 0);
		int a = new Random().nextInt(5000);
		d.put("inactive", a);
		data.add(d);
		d = new HashMap<String, Object>();
		d.put("linenum","linenum"+new Random().nextInt(3));
		d.put("batchnum","batchnum"+new Random().nextInt(3));
		d.put("prodnum", 1);
		a = new Random().nextInt(5000);
		d.put("inactive", a);
		data.add(d);
		d = new HashMap<String, Object>();
		d.put("linenum","linenum"+new Random().nextInt(3));
		d.put("batchnum","batchnum"+new Random().nextInt(3));
		d.put("prodnum", 2);
		a = new Random().nextInt(5000);
		d.put("inactive", a);
		data.add(d);
		d = new HashMap<String, Object>();
		d.put("linenum","linenum"+new Random().nextInt(3));
		d.put("batchnum","batchnum"+new Random().nextInt(3));
		d.put("prodnum", 3);
		a = new Random().nextInt(5000);
		d.put("inactive", a);
		data.add(d);
		returnvalue.put("data", data);
		return returnvalue;
	}
	
	public static Map<String, Object> orders(String eid){
		Path orderfolder = datafolder.resolve("todeliver").resolve(eid);
		Map<String, Object> returnvalue = new HashMap<String, Object>(501);
		BufferedReader br = null;
		try{
			Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh","-c", "ls "+orderfolder.toString()+" -rt | head -n 500"});
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = br.readLine();
			int i = 0;
			while (line!=null){
				Path file = orderfolder.resolve(line);
				try{
					String detail = Files.readAllLines(file,Charset.forName("UTF-8")).get(0);
					String[] detailparts = detail.split(AutoConfig.SPLIT);
					String poolid=detailparts[34];
					String prizeid=detailparts[35];
					String desc = AutoConfig.config(eid, "lsid.pool"+poolid+".prize"+prizeid+".name");
					String num = AutoConfig.config(eid, "lsid.pool"+poolid+".prize"+prizeid+".amount");
					String addr = URLEncoder.encode(DefaultCipher.dec(detailparts[38]),"UTF-8");
					String time  =detailparts[39];
					String oid = line;
					
					returnvalue.put("data"+i,desc+AutoConfig.SPLIT+num+AutoConfig.SPLIT+addr+AutoConfig.SPLIT+time+AutoConfig.SPLIT+oid);
					i++;
				}catch(Exception e){
					//do nothing
				}
				line = br.readLine();
			}
			returnvalue.put("total", ordertotal(eid));
		}catch(Exception ex){
			//do nothing
		} finally {
			if (br!=null){
				try {
					br.close();
				} catch (IOException e) {
					//do nothing
				}
			}
		}
		return returnvalue;
	}

	public static long ordertotal(String eid){
		Path orderfolder = datafolder.resolve("todeliver").resolve(eid);
		BufferedReader br = null;
		try{
			Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh","-c", "ls "+orderfolder.toString()+" -l |grep \"^-\"|wc -l"});
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = br.readLine();
			while (line!=null){
				return Long.parseLong(line);
			}
		}catch(Exception ex){
			//do nothing
		} finally {
			if (br!=null){
				try {
					br.close();
				} catch (IOException e) {
					//do nothing
				}
			}
		}
		return 0;
	}
	
	public static Map<String, Object> times(String eid, String from, String province, String city, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		List<List<Long>> data = new ArrayList<List<Long>>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		returnvalue.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		returnvalue.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		Map<Long, Long> timespersons = new HashMap<Long, Long>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			Path folder = datafolder.resolve("hadoop").resolve(eid).resolve(from+"timesdata").resolve(date);
			String[] files = folder.toFile().list();
			if (files!=null){
				for (String file:files){
					String[] parts = URLDecoder.decode(file, "UTF-8").split("_");
					String p = parts[0];
					String c = parts[1];
					String gen = parts[2];
					String act = parts[3];
					String pro = parts[4];
					long times = Long.parseLong(parts[5]);
					long persons = Files.size(folder.resolve(file));
					if (("all".equals(province)||p.equals(province))&&
							("all".equals(city)||c.equals(city))&&
							("all".equals(gender)||gen.equals(gender))&&
							("all".equals(activity)||act.equals(activity))&&
							("all".equals(product)||pro.equals(product))){
						if (timespersons.get(times)==null){
							timespersons.put(times, persons);
						} else {
							timespersons.put(times, timespersons.get(times)+persons);
						}
					}
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		for (Map.Entry<Long, Long> entry:timespersons.entrySet()){
			List<Long> d = new ArrayList<Long>();
			d.add(entry.getValue());
			d.add(entry.getKey());
			data.add(d);
		}
		returnvalue.put("data", data);
		return returnvalue;
	}
	
	public static Map<String, Object> scan(String eid, String from, String pc, String begin, String end, String product) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		List<String> x = new ArrayList<String>();
		Map<String, Long> total = new HashMap<String, Long>();
		Map<String, Long> datevalues = new HashMap<String, Long>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			Path file = datafolder.resolve("hadoop").resolve(eid).resolve(from+"scandata").resolve(date);
			x.add(date);
			if (Files.exists(file)){
				List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
				for (String line:lines){
					String[] parts = line.split("_");
					if (parts.length>3) {
						String p = parts[0];
						String c = parts[1];
						String pro = parts[2];
						long times = Long.parseLong(parts[3]);
						String key = p;
						if ("c".equals(pc)){
							key = c;
						}
						key = URLDecoder.decode(key,"UTF-8");
						long value = 0;
						if ("all".equals(product)||pro.equals(product)){
							value = times;
						}
						if (total.get(key)==null){
							total.put(key, value);
						} else {
							total.put(key, total.get(key)+value);
						}
						if (datevalues.get(key+date)==null){
							datevalues.put(key+date, value);
						} else {
							datevalues.put(key+date, datevalues.get(key+date)+value);
						}
					}
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		List<List<Long>> data = new ArrayList<List<Long>>();
		Set<String> yaxis = new HashSet<String>();
		if (!total.isEmpty()){
			Map<String, Long> sortedtotal = sortMapByValue(total);
			yaxis = sortedtotal.keySet();
			for (String date:x){
				List<Long> dayvalues = new ArrayList<Long>();
				for (Map.Entry<String, Long> entry : sortedtotal.entrySet()) {
					dayvalues.add(datevalues.get(entry.getKey()+date)==null?0:datevalues.get(entry.getKey()+date));
				}
				data.add(dayvalues);
			}
		}
		returnvalue.put("timelines", x);
		returnvalue.put("yaxis", yaxis);
		returnvalue.put("data", data);
		return returnvalue;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> lucktrend(String eid, String from, String province, String city, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		List<String> x = new ArrayList<String>();
		Map<String, Map<String, Long>> temp = new HashMap<String, Map<String, Long>>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			Path file = datafolder.resolve("hadoop").resolve(eid).resolve(from+"trendata").resolve(date);
			x.add(date);
			if (Files.exists(file)){
				List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
				for (String line:lines){
					String[] parts = line.split("_");
					if (parts.length>7) {
						String p = URLDecoder.decode(parts[0],"UTF-8");
						String c = URLDecoder.decode(parts[1],"UTF-8");
						String gen = parts[6];
						String act = parts[3];
						String pro = parts[2];
						String prize = URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+parts[4]+".prize"+parts[5]+".name"),"UTF-8");
						long times = Long.parseLong(parts[7]);
						String key = prize;
						long value = 0;
						if (("all".equals(province)||p.equals(province))&&
								("all".equals(city)||c.equals(city))&&
								("all".equals(gender)||gen.equals(gender))&&
								("all".equals(activity)||act.equals(activity))&&
								("all".equals(product)||pro.equals(product))){
							value = times;
						}
						if (temp.get(key)==null){
							temp.put(key, new HashMap<String, Long>());
						}
						if (temp.get(key).get(date)==null){
							temp.get(key).put(date, value);
						} else {
							temp.get(key).put(date, temp.get(key).get(date)+value);
						}
					}
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		for (String date:x){
			for (Map.Entry<String, Map<String, Long>> entry:temp.entrySet()){
				if (returnvalue.get(entry.getKey())==null){
					returnvalue.put(entry.getKey(), new ArrayList<Long>());
				}
				if (entry.getValue().get(date)==null){
					((List<Long>)returnvalue.get(entry.getKey())).add(0l);
				} else {
					((List<Long>)returnvalue.get(entry.getKey())).add(entry.getValue().get(date));
				}
			}
		}
		returnvalue.put("xaxis", x);
		returnvalue.put("legend", temp.keySet());
		return returnvalue;
	}
														
	@SuppressWarnings("unchecked")
	public static Map<String, Object> confirmprizetrend(String eid, String from, String province, String city, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		List<String> x = new ArrayList<String>();
		Map<String, Map<String, Long>> temp = new HashMap<String, Map<String, Long>>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			Path file = datafolder.resolve("hadoop").resolve(eid).resolve(from+"confirmprizedata").resolve(date);
			x.add(date);
			if (Files.exists(file)){
				List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
				for (String line:lines){
					String[] parts = line.split("_");
					if (parts.length>7) {
						String p = URLDecoder.decode(parts[0],"UTF-8");
						String c = URLDecoder.decode(parts[1],"UTF-8");
						String gen = parts[6];
						String act = parts[3];
						String pro = parts[2];
						String prize = URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+parts[4]+".prize"+parts[5]+".name"),"UTF-8");
						long times = Long.parseLong(parts[7]);
						String key = prize;
						long value = 0;
						if (("all".equals(province)||p.equals(province))&&
								("all".equals(city)||c.equals(city))&&
								("all".equals(gender)||gen.equals(gender))&&
								("all".equals(activity)||act.equals(activity))&&
								("all".equals(product)||pro.equals(product))){
							value = times;
						}
						if (temp.get(key)==null){
							temp.put(key, new HashMap<String, Long>());
						}
						if (temp.get(key).get(date)==null){
							temp.get(key).put(date, value);
						} else {
							temp.get(key).put(date, temp.get(key).get(date)+value);
						}
					}
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		for (String date:x){
			for (Map.Entry<String, Map<String, Long>> entry:temp.entrySet()){
				if (returnvalue.get(entry.getKey())==null){
					returnvalue.put(entry.getKey(), new ArrayList<Long>());
				}
				if (entry.getValue().get(date)==null){
					((List<Long>)returnvalue.get(entry.getKey())).add(0l);
				} else {
					((List<Long>)returnvalue.get(entry.getKey())).add(entry.getValue().get(date));
				}
			}
		}
		returnvalue.put("xaxis", x);
		returnvalue.put("legend", temp.keySet());
		return returnvalue;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> luckusertrend(String eid, String from, String province, String city, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		Map<String, Object> returnvalue = new HashMap<String, Object>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		List<String> x = new ArrayList<String>();
		Map<String, Map<String, Long>> temp = new HashMap<String, Map<String, Long>>();
		Calendar cal = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			String date = new SimpleDateFormat("yyyy-MM-dd").format(b);
			x.add(date);
			Path folder = datafolder.resolve("hadoop").resolve(eid).resolve(from+"usersdata").resolve(date);
			String[] files = folder.toFile().list();
			if (files!=null){
				for (String file:files){
					String[] parts = URLDecoder.decode(file, "UTF-8").split("_");
					
					String p = URLDecoder.decode(parts[0],"UTF-8");
					String c = URLDecoder.decode(parts[1],"UTF-8");
					String gen = parts[6];
					String act = parts[3];
					String pro = parts[2];
					String prize = URLDecoder.decode(AutoConfig.config(eid, "lsid.pool"+parts[4]+".prize"+parts[5]+".name"),"UTF-8");
					long times = Files.size(folder.resolve(file));
					String key = prize;
					long value = 0;
					if (("all".equals(province)||p.equals(province))&&
							("all".equals(city)||c.equals(city))&&
							("all".equals(gender)||gen.equals(gender))&&
							("all".equals(activity)||act.equals(activity))&&
							("all".equals(product)||pro.equals(product))){
						value = times;
					}
					if (temp.get(key)==null){
						temp.put(key, new HashMap<String, Long>());
					}
					if (temp.get(key).get(date)==null){
						temp.get(key).put(date, value);
					} else {
						temp.get(key).put(date, temp.get(key).get(date)+value);
					}
					
				}
			}
			cal.setTime(b);
			cal.add(Calendar.DATE, 1);
			b = cal.getTime();
		}
		
		for (String date:x){
			for (Map.Entry<String, Map<String, Long>> entry:temp.entrySet()){
				if (returnvalue.get(entry.getKey())==null){
					returnvalue.put(entry.getKey(), new ArrayList<Long>());
				}
				if (entry.getValue().get(date)==null){
					((List<Long>)returnvalue.get(entry.getKey())).add(0l);
				} else {
					((List<Long>)returnvalue.get(entry.getKey())).add(entry.getValue().get(date));
				}
			}
		}
		returnvalue.put("xaxis", x);
		returnvalue.put("legend", temp.keySet());
		return returnvalue;
	}
	 							
	public static List<Map<String, Object>> luckchina(String eid, String from, String pc, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		List<Map<String, Object>> returnvalue = new ArrayList<Map<String, Object>>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		
		Map<String, Object> period = new HashMap<String, Object>();
		period.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		period.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		
		Map<String, Long> temp = new HashMap<String, Long>();
		Calendar c = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			Path file = datafolder.resolve("hadoop").resolve(eid).resolve(from+"trendata").resolve(new SimpleDateFormat("yyyy-MM-dd").format(b));
			if (Files.exists(file)){
				List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
				for (String line:lines){
					String[] parts = line.split("_");
					if (parts.length>7) {
						String province = parts[0];
						String city = parts[1];
						String gen = parts[6];
						String act = parts[3];
						String pro = parts[2];
						long times = Long.parseLong(parts[7]);
						String key = province;
						if ("c".equals(pc)){
							key = city;
						}
						key = URLDecoder.decode(key,"UTF-8");
						long value = 0;
						if (("all".equals(gender)||gen.equals(gender))&&
								("all".equals(activity)||act.equals(activity))&&
								("all".equals(product)||pro.equals(product))){
							value = times;
						}
						if (temp.get(key)==null){
							temp.put(key, value);
						} else {
							temp.put(key, temp.get(key)+value);
						}
					}
				}
			}
			c.setTime(b);
			c.add(Calendar.DATE, 1);
			b = c.getTime();
		}
		if (!temp.isEmpty()){
			Map<String, Long> sortedtemp = sortMapByValue(temp); 
			for (Map.Entry<String, Long> entry : sortedtemp.entrySet()) {
				Map<String, Object> t = new HashMap<String, Object>();
				t.put("name", entry.getKey());
				t.put("value", entry.getValue());
				returnvalue.add(t);
			}
		}
		returnvalue.add(period);
		return returnvalue;
	}
	
	public static List<Map<String, Object>> confirmprizechina(String eid, String from, String pc, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		List<Map<String, Object>> returnvalue = new ArrayList<Map<String, Object>>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		
		Map<String, Object> period = new HashMap<String, Object>();
		period.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		period.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		
		Map<String, Long> temp = new HashMap<String, Long>();
		Calendar c = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			Path file = datafolder.resolve("hadoop").resolve(eid).resolve(from+"confirmprizedata").resolve(new SimpleDateFormat("yyyy-MM-dd").format(b));
			if (Files.exists(file)){
				List<String> lines = Files.readAllLines(file, Charset.forName("UTF-8"));
				for (String line:lines){
					String[] parts = line.split("_");
					if (parts.length>7) {
						String province = parts[0];
						String city = parts[1];
						String gen = parts[6];
						String act = parts[3];
						String pro = parts[2];
						long times = Long.parseLong(parts[7]);
						String key = province;
						if ("c".equals(pc)){
							key = city;
						}
						key = URLDecoder.decode(key,"UTF-8");
						long value = 0;
						if (("all".equals(gender)||gen.equals(gender))&&
								("all".equals(activity)||act.equals(activity))&&
								("all".equals(product)||pro.equals(product))){
							value = times;
						}
						if (temp.get(key)==null){
							temp.put(key, value);
						} else {
							temp.put(key, temp.get(key)+value);
						}
					}
				}
			}
			c.setTime(b);
			c.add(Calendar.DATE, 1);
			b = c.getTime();
		}
		if (!temp.isEmpty()){
			Map<String, Long> sortedtemp = sortMapByValue(temp); 
			for (Map.Entry<String, Long> entry : sortedtemp.entrySet()) {
				Map<String, Object> t = new HashMap<String, Object>();
				t.put("name", entry.getKey());
				t.put("value", entry.getValue());
				returnvalue.add(t);
			}
		}
		returnvalue.add(period);
		return returnvalue;
	}
	
	public static List<Map<String, Object>> luckuserchina(String eid, String from, String pc, String begin, String end, String gender, String activity, String product) throws IOException, ParseException{
		List<Map<String, Object>> returnvalue = new ArrayList<Map<String, Object>>();
		Date b = null;
		try{
			b = new SimpleDateFormat("yyyy-MM-dd").parse(begin);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -31);
			b = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		Date e = null;
		try{
			e = new SimpleDateFormat("yyyy-MM-dd").parse(end);
		}catch(Exception ex){
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -1);
			e = new SimpleDateFormat("yyyy-MM-dd").parse(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
		}
		
		Map<String, Object> period = new HashMap<String, Object>();
		period.put("begin", new SimpleDateFormat("yyyy-MM-dd").format(b));
		period.put("end", new SimpleDateFormat("yyyy-MM-dd").format(e));
		
		Map<String, Long> temp = new HashMap<String, Long>();
		Calendar c = Calendar.getInstance();
		while (b.compareTo(e)<=0){
			Path folder = datafolder.resolve("hadoop").resolve(eid).resolve(from+"usersdata").resolve(new SimpleDateFormat("yyyy-MM-dd").format(b));
			String[] files = folder.toFile().list();
			if (files!=null){
				for (String file:files){
					String[] parts = URLDecoder.decode(file, "UTF-8").split("_");
					String province = parts[0];
					String city = parts[1];
					String gen = parts[6];
					String act = parts[3];
					String pro = parts[2];
					long times = Files.size(folder.resolve(file));
					String key = province;
					if ("c".equals(pc)){
						key = city;
					}
					key = URLDecoder.decode(key,"UTF-8");
					long value = 0;
					if (("all".equals(gender)||gen.equals(gender))&&
							("all".equals(activity)||act.equals(activity))&&
							("all".equals(product)||pro.equals(product))){
						value = times;
					}
					if (temp.get(key)==null){
						temp.put(key, value);
					} else {
						temp.put(key, temp.get(key)+value);
					}
				}
			}
			c.setTime(b);
			c.add(Calendar.DATE, 1);
			b = c.getTime();
		}
		if (!temp.isEmpty()){
			Map<String, Long> sortedtemp = sortMapByValue(temp); 
			for (Map.Entry<String, Long> entry : sortedtemp.entrySet()) {
				Map<String, Object> t = new HashMap<String, Object>();
				t.put("name", entry.getKey());
				t.put("value", entry.getValue());
				returnvalue.add(t);
			}
		}
		returnvalue.add(period);
		return returnvalue;
	}
	
	public static Map<String, Long> sortMapByValue(Map<String, Long> oriMap) {
        if (oriMap == null || oriMap.isEmpty()) {
            return null;
        }
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
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
        return sortedMap;
    }
	
	public static void ordered(String eid, String orderid, String shipinfo) throws Exception{
		String content = Files.readAllLines(datafolder.resolve("todeliver").resolve(eid).resolve(orderid), Charset.forName("UTF-8")).get(0);
		String[] parts = content.split(AutoConfig.SPLIT);
		String enc = AutoConfig.getenc(parts);
		String value = AutoConfig.dataupdatelastatus(content, shipinfo);
		AutoConfig.cacheuserdata(eid, enc, "prize", orderid, "p", value);
		Files.deleteIfExists(datafolder.resolve("todeliver").resolve(eid).resolve(orderid));
	}
	
    public static String sign(String raw){
    	MessageDigest crypt = null;
    	try {
    		crypt = MessageDigest.getInstance("MD5");
    	    crypt.reset();
    	   	crypt.update(raw.getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	    return byteArrayToHexString(crypt.digest());
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println(URLEncoder.encode("介知演示","utf-8").toLowerCase());
		System.out.println(sign("1981"));
	}
    
	private static String[] HexCode = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    private static String byteToHexString(byte b)
    {
        int n = b;
        if (n < 0)
        {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HexCode[d1] + HexCode[d2];
    }

    private static String byteArrayToHexString(byte[] b)
    {
        String result = "";
        for (int i = 0; i < b.length; i++)
        {
            result = result + byteToHexString(b[i]);
        }
        return result;
    }
}
