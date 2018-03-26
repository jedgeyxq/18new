package com.lsid.console.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.lsid.autoconfig.client.AutoConfig;

public class DataFiles {
	private static Path datafolder = Paths.get("/data/17new/consoledata/");
	
	public static void scanpositiondata(String eid, String content) throws IOException{
		if (!Files.exists(datafolder.resolve("position").resolve(eid))){
			Files.createDirectories(datafolder.resolve("position").resolve(eid));
		}
		Files.write(datafolder.resolve("position").resolve(eid).resolve(UUID.randomUUID().toString().replaceAll("-", "")), content.getBytes("UTF-8"), StandardOpenOption.CREATE);
	}

	public static void prepare4search(String eid, String content) throws Exception{
		String[] parts = content.split(AutoConfig.SPLIT);
		if (!Files.exists(datafolder.resolve("tosearch").resolve(eid))){
			Files.createDirectories(datafolder.resolve("tosearch").resolve(eid));
		}
		Files.write(datafolder.resolve("tosearch").resolve(eid).resolve(new SimpleDateFormat("yyyy-MM-dd").format(new Date(AutoConfig.getlucktime(parts)))), 
				(content+System.lineSeparator()).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	public static void orderdata(String eid, String orderid, String content) throws IOException{
		if (!Files.exists(datafolder.resolve("todeliver").resolve(eid))){
			Files.createDirectories(datafolder.resolve("todeliver").resolve(eid));
		}
		Files.write(datafolder.resolve("todeliver").resolve(eid).resolve(orderid), content.getBytes("UTF-8"), StandardOpenOption.CREATE);
	}
	
	public static void hadoopdata(String eid, String folder, String file, String content) throws IOException{
		if (!Files.exists(datafolder.resolve("hadoop").resolve(eid).resolve(folder))){
			Files.createDirectories(datafolder.resolve("hadoop").resolve(eid).resolve(folder));
		}
		Files.write(datafolder.resolve("hadoop").resolve(eid).resolve(folder).resolve(file), 
				(content+System.lineSeparator()).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	public static void hadoopidata(String eid, String folder, String file, String i) throws IOException{
		if (!Files.exists(datafolder.resolve("hadoop").resolve(eid).resolve(folder))){
			Files.createDirectories(datafolder.resolve("hadoop").resolve(eid).resolve(folder));
		}
		try{
			Files.write(datafolder.resolve("hadoop").resolve(eid).resolve(folder).resolve(file), 
					new byte[Integer.parseInt(i)], StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}catch(Exception e) {
			Files.write(datafolder.resolve("hadoop").resolve(eid).resolve(folder).resolve(file), 
					new byte[1], StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
	}

	public static String mapredclassparams() throws IOException{
		Path folder = datafolder.resolve("mapredclassparams");
		if (!Files.exists(folder)){
			Files.createDirectories(folder);
		}
		String[] files = folder.toFile().list();
		if ( files!=null && files.length >0 ) {
			String mapredclass = files[0];
			try {
				String params = Files.readAllLines(folder.resolve(files[0]), Charset.forName("UTF-8")).get(0);
				if (params!=null&&!params.isEmpty()) {
					return mapredclass+AutoConfig.SPLIT+params;
				} else {
					return mapredclass;
				}
			}catch(Exception e) {
				return mapredclass;
			} finally {
				Files.deleteIfExists(folder.resolve(files[0]));
			}
		} else {
			return "";
		}
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
