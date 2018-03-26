package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;

public class Sort implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"lsidcountercachefolder").isEmpty()){
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				AutoConfig.iamrunning();
				process();
				AutoConfig.iamdone();
			}
    		
    	}).start();
	}
	
	private void process(){
		System.out.println("========"+new Date()+"======== started sorting");
		while(AutoConfig.isrunning){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				//do nothing
			}
			String[] tosorts = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("tosort").toFile().list();
			if (tosorts!=null){
				for (String tosort:tosorts){
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (tosort.split(AutoConfig.SPLIT).length>2){
						final String namespace = tosort.substring(0, tosort.indexOf(AutoConfig.SPLIT));
						final String tablename = tosort.substring(tosort.lastIndexOf(AutoConfig.SPLIT)+1);
						final String column = tosort.substring(tosort.indexOf(AutoConfig.SPLIT)+1,tosort.lastIndexOf(AutoConfig.SPLIT));
						final Map<String, Long> rowcounter = new HashMap<String, Long>();
						Path thefolder = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("incr").resolve(namespace).resolve(tablename).resolve(column);
						try {
							Files.walkFileTree(thefolder, new FileVisitor<Object>(){
	
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
											try{
												long counter = Files.size(Paths.get(file.toString()));
												String hash = Paths.get(file.toString()).getParent().getFileName().toString();
												String row = Paths.get(file.toString()).getFileName().toString();
												
												boolean tosort = false;
												if (row.contains(AutoConfig.SPLIT_HBASE)) {
													try {
														String surfix = row.substring(row.lastIndexOf(AutoConfig.SPLIT_HBASE)+1);
														if (surfix.length()==6) {
															new SimpleDateFormat("yyyyMM").parse(surfix);
														} else if (surfix.length()==8) {
															new SimpleDateFormat("yyyyMMdd").parse(surfix);
														} else {
															tosort = true;
														}
													}catch(Exception e) {
														tosort = true;
													}
												} else if (!row.equals("total")){
													tosort = true;
												}
												if (tosort) {
													Path decrfile = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("decr").resolve(namespace).resolve(tablename).resolve(column).resolve(hash).resolve(row);
													if (Files.exists(decrfile)){
														counter -= Files.size(decrfile);
													}
													rowcounter.put(hash+AutoConfig.SPLIT+row, counter);
													if (rowcounter.size()>100000){
														sorting(rowcounter, namespace, tablename, column);
													}
												}
											}catch(Exception e){
												AutoConfig.log(e,"error processing "+file);
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
							if (!rowcounter.isEmpty()){
								sorting(rowcounter, namespace, tablename, column);
							}
							mergesort(namespace, tablename, column);
							Files.deleteIfExists(Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("tosort").resolve(tosort));
						} catch (Exception e) {
							AutoConfig.log(e, "System exited due to below exception:");
							System.exit(1);
						}
					}
				}
			}
		}
	}
	
	private static void sorting(Map<String, Long> rowcounter, String namespace, String tablename, String column) throws Exception{
		Path sortingfolder = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sorting").resolve(namespace).resolve(tablename).resolve(column);
		if (!Files.exists(sortingfolder)){
			Files.createDirectories(sortingfolder);
		}
		Map<String, Long> resultMap = sortMapByValue(rowcounter);
		Path sortingfile = sortingfolder.resolve(UUID.randomUUID().toString().replaceAll("-", ""));
		if (resultMap!=null&&!resultMap.isEmpty()){
	        for (Map.Entry<String, Long> entry : resultMap.entrySet()) {
	        	Files.write(sortingfile, (entry.getKey()+AutoConfig.SPLIT+entry.getValue()+System.lineSeparator()).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	        }
		}
		rowcounter.clear();
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
	
	private static void mergesort(String namespace, String tablename, String column) throws Exception{
		List<BufferedReader> brs = new ArrayList<BufferedReader>();
		Path sortingfolder = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sorting").resolve(namespace).resolve(tablename).resolve(column);
		String[] filenames = sortingfolder.toFile().list();
		if (filenames!=null) {
			for (String filename:filenames){
				brs.add(Files.newBufferedReader(sortingfolder.resolve(filename), Charset.forName("UTF-8")));
			}
			Path merged = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort").resolve("sortedone").resolve(namespace).resolve(tablename).resolve(column).resolve("merged");
			Files.deleteIfExists(merged);
			if (!Files.exists(merged.getParent())){
				Files.createDirectories(merged.getParent());
			}
			List<String> lines = new ArrayList<String>(brs.size());
			for (int i=0; i<brs.size();i++){
				lines.add(brs.get(i).readLine().trim());
			}
			while(!brs.isEmpty()){
				int maxi = 0;
				for (int i=0; i<lines.size();i++){
					long tocompare = Long.parseLong(lines.get(i).substring(lines.get(i).lastIndexOf(AutoConfig.SPLIT)+1));
					long max = Long.parseLong(lines.get(maxi).substring(lines.get(maxi).lastIndexOf(AutoConfig.SPLIT)+1));
					if (tocompare>max){
						maxi = i;
					}
				}
				Files.write(merged,(lines.get(maxi)+System.lineSeparator()).getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				String line = brs.get(maxi).readLine();
				if (line!=null){
					lines.set(maxi, line);
				} else {
					lines.remove(maxi);
					BufferedReader br = brs.remove(maxi);
					if (br!=null){
						br.close();
						br = null;
						Files.deleteIfExists(sortingfolder.resolve(filenames[maxi]));
					}
				}
			}
			Files.write(merged.getParent().resolve("todistribute"),new byte[0], StandardOpenOption.CREATE);
		}
	}
	
}
