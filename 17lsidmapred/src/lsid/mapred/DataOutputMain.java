package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import lsid.mapred.util.JobHelper;

public class DataOutputMain{
	public static void main(String[] s) throws IOException{
		try {
			String eid = null;
			if (s.length>0){
				eid = s[0];
			}
			String table = null;
			if (s.length>1){
				table = s[1];
			}
			String col = null;
			if (s.length>2){
				col = s[2];
			}
			if (eid==null||table==null||col==null){
				throw new IOException("please append eid table column to the command");
			}
			
			Configuration config = HBaseConfiguration.create();
			config.set("eid", eid);
			config.set("table", table);
			config.set("col", col);
			config.set("startime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

			Job job = Job.getInstance(config);
			job.setJarByClass(DataOutputMain.class);  
			job.addFileToClassPath(JobHelper.addJarToDistributedCache(ObjectMapper.class, config));
	
			Scan scan = new Scan();		
			TableMapReduceUtil.initTableMapperJob(
				"lsid:17"+table,      
				scan,              
				DataOutputMapper.class,  
				ImmutableBytesWritable.class,       
				ImmutableBytesWritable.class,
				job);
			job.setReducerClass(DataOutputReducer.class);  
			job.setNumReduceTasks(1); 
			job.setOutputFormatClass(NullOutputFormat.class);
			
			boolean b = job.waitForCompletion(true);
			if (!b) {
				throw new IOException("error with job!");
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}