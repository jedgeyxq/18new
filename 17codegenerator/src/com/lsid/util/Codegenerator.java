package com.lsid.util;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

public class Codegenerator {
	private static void generate(String domain, int length, String eid, long numofcode) throws Exception{
		if (numofcode>5000000){
			throw new Exception("5000000limit");
		}
		if (length>32){
			throw new Exception("32limit");
		}
		
		Files.createDirectories(Paths.get("repo"));
		
		final Path codeg = Paths.get("repo/generating"+eid);
		
		final Path codep = Paths.get("repo/"+eid);
		Files.createDirectories(codep);
		
		Files.write(codeg, new byte[0], StandardOpenOption.CREATE_NEW);
		
		final Path coder = Paths.get("ready/"+eid);
		Files.createDirectories(coder);
		
		final Vector<String> a = new Vector<String>(5000000);
		new Thread(new Runnable(){

			@Override
			public void run() {
				while(Files.exists(codeg)||!a.isEmpty()){
					if (a.isEmpty()){
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						String code = a.remove(0);
						String coderepofile = String.valueOf(Math.abs(code.hashCode()%10000));
						try {
							if (!haveignorecase(codep.resolve(coderepofile),code)){
								Files.write(codep.resolve(coderepofile), (code+System.lineSeparator()).getBytes("UTF-8"), 
										StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				System.out.println(new Date()+" writing repo end");
			}
			
		}).start();
		
		String filename = eid+"-"+length+"-"+numofcode+"-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

		long count = 0;
		System.out.println(new Date()+" " + filename+" starting ("+count+")");
		while (count<numofcode){
			String code = UUID.randomUUID().toString().replaceAll("-", "").substring(0,length);
			
			String coderepofile = String.valueOf(Math.abs(code.hashCode()%100000));
			
			boolean isduplicate = a.contains(code)||haveignorecase(codep.resolve(coderepofile),code);
			
			if (!isduplicate){
				if (count!=0){
					Files.write(coder.resolve(filename), System.lineSeparator().getBytes("UTF-8"), 
							StandardOpenOption.CREATE,StandardOpenOption.APPEND);
				}
				Files.write(coder.resolve(filename), ("http://"+domain+"/"+eid+"/"+code).getBytes("UTF-8"), 
						StandardOpenOption.CREATE,StandardOpenOption.APPEND);
				count++;
				if (count%10000==0){
					System.out.println(count);
				}
				a.add(code);
			}
		}
		Files.deleteIfExists(codeg);
		System.out.println(new Date()+" " + filename+" ready ("+count+")");
	}
	
	private static boolean haveignorecase(Path file, String target) throws Exception{
		if (Files.exists(file)){
			BufferedReader br = Files.newBufferedReader(file, Charset.forName("UTF-8"));
			try{
				String line = br.readLine();
				while(line!=null){
					if (line.toLowerCase().contains(target.toLowerCase())){
						return true;
					}
					line = br.readLine();
				}
				return false;
			}finally{
				br.close();
			}
		}
		return false;
	}
	
	public static void main(String[] s) throws Exception{
		String domain = s[0];
		Integer length = Integer.parseInt(s[1]);
		String eid = s[2];
		Integer amount = Integer.parseInt(s[3]);
		generate(domain, length, eid, amount);
	}
}
