package com.lsid.luck.util;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.lsid.autoconfig.client.AutoConfig;

public class Luck {
	private static final Path pooling = Paths.get("pooling");
	
	public static Map<String, String> getprogress(String eid) throws Exception{
		Map<String, String> returnvalue = new HashMap<String, String>();
		String[] files = pooling.resolve(eid).toFile().list();
		if (files!=null) {
			for (String file:files) {
				if (file.startsWith("repository")||file.startsWith("ratio")||file.startsWith("remaining")) {
					returnvalue.put(file, Files.readAllLines(pooling.resolve(eid).resolve(file), Charset.forName("UTF-8")).get(0));
				}
			}
		}
		return returnvalue;
	}
	
	public synchronized static String draw(String eid, String poolid) throws Exception{
		Path poolingfolder = pooling.resolve(eid);
		if (!Files.exists(poolingfolder)){
			Files.createDirectories(poolingfolder);
		}
		int buffer = 1;
		try{
			buffer = Integer.parseInt(AutoConfig.config(eid, "lsid.pool"+poolid+".buffer"));
		}catch(Exception ex){
			//do nothing
		}
		
		String returnvalue = "-1";
		if (!AutoConfig.config(eid, "lsid.pool"+poolid).isEmpty()&&!AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").isEmpty()){
			String[] repositoryconfig = AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").split(AutoConfig.SPLIT);
			String[] poolconfig = AutoConfig.config(eid, "lsid.pool"+poolid).split(AutoConfig.SPLIT);
			if (repositoryconfig.length!=poolconfig.length) {
				throw new Exception("wrong"+eid+poolid);
			}
			if (!Files.exists(poolingfolder.resolve("repository"+poolid))) {
				Files.write(poolingfolder.resolve("repository"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
			}
			
			if (!Files.exists(poolingfolder.resolve("ratio"+poolid))) {
				Files.write(poolingfolder.resolve("ratio"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
			}
			
			if (!Files.exists(poolingfolder.resolve("pooling"+poolid))){
				Files.write(poolingfolder.resolve("pooling"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid).getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
			}
			
			if (!Files.exists(poolingfolder.resolve("remaining"+poolid))){
				Files.write(poolingfolder.resolve("remaining"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").getBytes("UTF-8"), StandardOpenOption.CREATE_NEW);
			}
			
			String oldrepositorystr = Files.readAllLines(poolingfolder.resolve("repository"+poolid), Charset.forName("UTF-8")).get(0);
			if (!AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").equals(oldrepositorystr)) {
				String[] remainingstr = Files.readAllLines(poolingfolder.resolve("remaining"+poolid), Charset.forName("UTF-8")).get(0).split(AutoConfig.SPLIT);
				long[] remaining = new long[remainingstr.length];
				for (int i = 0; i <remainingstr.length; i++){
					remaining[i] = Long.parseLong(remainingstr[i]);
				}
				
				String[] newrepository = new String[repositoryconfig.length];
				String[] oldrepository = oldrepositorystr.split(AutoConfig.SPLIT);
				for (int i = 0; i < repositoryconfig.length; i++) {
					if (i>=oldrepository.length) {
						newrepository[i] = repositoryconfig[i];
					} else {
						newrepository[i] = String.valueOf(remaining[i]+Long.parseLong(repositoryconfig[i])-Long.parseLong(oldrepository[i]));
					}
				}
				
				String newremainingstr = String.valueOf(newrepository[0]);
				for (int i = 1; i < newrepository.length; i++){
					newremainingstr += AutoConfig.SPLIT+String.valueOf(newrepository[i]);
				}
				Files.write(poolingfolder.resolve("remaining"+poolid), newremainingstr.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				Files.write(poolingfolder.resolve("repository"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid+".remaining").getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			
			String oldratiostr = Files.readAllLines(poolingfolder.resolve("ratio"+poolid), Charset.forName("UTF-8")).get(0);
			if (!AutoConfig.config(eid, "lsid.pool"+poolid).equals(oldratiostr)) {
				String[] poolingstr = Files.readAllLines(poolingfolder.resolve("pooling"+poolid), Charset.forName("UTF-8")).get(0).split(AutoConfig.SPLIT);
				double[] pooling = new double[poolingstr.length];
				for (int i = 0; i <poolingstr.length; i++){
					pooling[i] = Double.parseDouble(poolingstr[i]);
				}
				
				String[] newpooling = new String[poolconfig.length];
				String[] oldratio = oldratiostr.split(AutoConfig.SPLIT);
				for (int i = 0; i < poolconfig.length; i++) {
					if (i>=oldratio.length) {
						newpooling[i] = poolconfig[i];
					} else {
						if (poolconfig[i].equals(oldratio[i])) {
							newpooling[i] = String.valueOf(pooling[i]);
						} else {
							newpooling[i] = poolconfig[i];
						}
					}
				}
				
				String newpoolingstr = String.valueOf(newpooling[0]);
				for (int i = 1; i < newpooling.length; i++){
					newpoolingstr += AutoConfig.SPLIT+String.valueOf(newpooling[i]);
				}
				Files.write(poolingfolder.resolve("pooling"+poolid), newpoolingstr.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				Files.write(poolingfolder.resolve("ratio"+poolid), AutoConfig.config(eid, "lsid.pool"+poolid).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			
			double[] pool = new double[poolconfig.length];
			for (int i = 0; i <poolconfig.length; i++){
				pool[i] = Double.parseDouble(poolconfig[i]);
			}
			
			String[] poolingstr = Files.readAllLines(poolingfolder.resolve("pooling"+poolid), Charset.forName("UTF-8")).get(0).split(AutoConfig.SPLIT);
			double[] pooling = new double[poolingstr.length];
			for (int i = 0; i <poolingstr.length; i++){
				pooling[i] = Double.parseDouble(poolingstr[i]);
			}
				
			String[] remainingstr = Files.readAllLines(poolingfolder.resolve("remaining"+poolid), Charset.forName("UTF-8")).get(0).split(AutoConfig.SPLIT);
			long[] remaining = new long[remainingstr.length];
			for (int i = 0; i <remainingstr.length; i++){
				remaining[i] = Long.parseLong(remainingstr[i]);
			}
			returnvalue = String.valueOf(draw(pool, pooling, remaining));
			for (int i = 1; i < buffer; i++){
				returnvalue += AutoConfig.SPLIT+String.valueOf(draw(pool, pooling, remaining));
			}
			
			String newpoolingstr = String.valueOf(pooling[0]);
			for (int i = 1; i < pooling.length; i++){
				newpoolingstr += AutoConfig.SPLIT+String.valueOf(pooling[i]);
			}
			Files.write(poolingfolder.resolve("pooling"+poolid), newpoolingstr.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			
			String newremainingstr = String.valueOf(remaining[0]);
			for (int i = 1; i < remaining.length; i++){
				newremainingstr += AutoConfig.SPLIT+String.valueOf(remaining[i]);
			}
			Files.write(poolingfolder.resolve("remaining"+poolid), newremainingstr.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} else {
			throw new Exception("missing"+eid+poolid);
		}
		return returnvalue;
	}
	
	private static int draw(double[] pool, double[] pooling, long[] remaining){
		boolean got = false;
		int returnvalue = -1;
		for (int i=0;i<pooling.length;i++){
			if (pooling[i]>=1){
				pooling[i]-=1;
				if (remaining[i]>=1){
					remaining[i]-=1;
					returnvalue = i;
				}
				got = true;
				break;
			}
		}
		if (!got){
			boolean ok = true;
			for (int i=0;i<pool.length;i++) {
				if (pool[i]>0&&remaining[i]>0) {
					ok=false;
					break;
				}
			}
			while(!ok) {
				for (int i=0;i<pooling.length;i++){
					if (remaining[i]>0) {
						pooling[i]+=pool[i];
					}
					if (pooling[i]>=1) {
						ok=true;
					}
				}	
			}
			for (int i=0;i<pooling.length;i++){
				if (pooling[i]>=1){
					pooling[i]-=1;
					if (remaining[i]>=1){
						remaining[i]-=1;
						returnvalue = i;
					}
					break;
				}
			}
		}
		return returnvalue;
	}
	
}
