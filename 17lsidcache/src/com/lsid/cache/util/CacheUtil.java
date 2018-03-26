package com.lsid.cache.util;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.lsid.autoconfig.client.AutoConfig;

public class CacheUtil {
	public static final Path cache = Paths.get(AutoConfig.config(null, "lsidvaluecachefolder"));
	public static final String notfoundcachefile = "notfoundfile";
	public static final String notfoundcacheline = "notfoundline";
	
	public static void writecache(String namespace, String tablename, String column, String hash, String row, String value) throws Exception{
		Path cachefile = cachefile(namespace, tablename, column, hash);
		if (!Files.exists(cachefile.getParent())){
			Files.createDirectories(cachefile.getParent());
		}
		Files.write(cachefile, 
				(row+AutoConfig.SPLIT+value+AutoConfig.SPLIT+System.currentTimeMillis()+System.lineSeparator()).getBytes("UTF-8"),
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	public static String readcache(String namespace, String tablename, String column, String hash, String row) throws Exception{
		String returnvalue = "";
		BufferedReader br = null;
		try{
			Path cachefile = cachefile(namespace, tablename, column, hash);
			if (!Files.exists(cachefile)){
				throw new Exception(notfoundcachefile);
			}
			br = Files.newBufferedReader(cachefile, Charset.forName("UTF-8"));
			String line = br.readLine();
			boolean found = false;
			while(line!=null){
				if (line.startsWith(row+AutoConfig.SPLIT)){
					returnvalue = line.substring(line.indexOf(AutoConfig.SPLIT)+1);
					found = true;
				}
				line = br.readLine();
			}
			if (!found){
				throw new Exception(notfoundcacheline);
			}
		} finally {
			if (br!=null){
				br.close();
			}
		}
		if (!AutoConfig.config(namespace, "lsid.expire."+tablename+"."+column).isEmpty()){
			if (System.currentTimeMillis() - Long.parseLong(returnvalue.substring(returnvalue.lastIndexOf(AutoConfig.SPLIT)+1))>Long.parseLong(AutoConfig.config(namespace, "lsid.expire."+tablename+"."+column))){
				return "";
			}
		}
		return returnvalue.substring(0, returnvalue.lastIndexOf(AutoConfig.SPLIT));
	}

	public static List<String> scancache(String namespace, String tablename, String column, String hash, String scankey) throws Exception{
		List<String> returnvalue = new ArrayList<String>();
		BufferedReader br = null;
		try{
			Path cachefile = cachefile(namespace, tablename, column, hash);
			if (Files.exists(cachefile)){
				br = Files.newBufferedReader(cachefile, Charset.forName("UTF-8"));
				String line = br.readLine();
				while(line!=null){
					if (line.startsWith(scankey)){
						String value = line.substring(line.indexOf(AutoConfig.SPLIT)+1);
						if (!AutoConfig.config(namespace, "lsid.expire."+tablename+"."+column).isEmpty()) {
							if (System.currentTimeMillis() - Long.parseLong(value.substring(returnvalue.lastIndexOf(AutoConfig.SPLIT)+1))>Long.parseLong(AutoConfig.config(namespace, "lsid.expire."+tablename+"."+column))){
								value = "";
							}
						}
						if (!value.isEmpty()) {
							returnvalue.add(value);
						}
					}
					line = br.readLine();
				}
			}	
		} finally {
			if (br!=null){
				br.close();
			}
		}
		return returnvalue;
	}

	private static Path cachefile(String namespace, String tablename, String column, String hash){
		return cache.resolve(namespace).resolve(tablename).resolve(column).resolve(String.valueOf(Math.abs(hash.hashCode())%Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.filehash"))));
	}

	public static synchronized void cache(String namespace, String tablename, String column, String hash, String row) throws Exception{
		boolean readhbase = false;
		try{
			readcache(namespace, tablename, column, hash, row);
		}catch(Exception e){
			if (e.getMessage().equals(CacheUtil.notfoundcachefile)||e.getMessage().equals(CacheUtil.notfoundcacheline)){
				readhbase = true;
			}
		}
		if (readhbase){
			String current = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.hbase.read"), 
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.socketimeoutinsec")),
					"eid", namespace, "hash", hash, "table", tablename, "row", row, "col", column);
			writecache(namespace, tablename, column, hash, row, current);
		}
	}
}
