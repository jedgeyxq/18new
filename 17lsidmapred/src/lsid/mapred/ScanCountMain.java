package lsid.mapred;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;

import com.fasterxml.jackson.databind.ObjectMapper;

import lsid.mapred.util.JobHelper;

public class ScanCountMain{
	public static void main(String[] s) throws Exception{
		Configuration config = HBaseConfiguration.create();
		Job job = Job.getInstance(config);
		job.setJarByClass(ScanCountMain.class);  
		job.addFileToClassPath(JobHelper.addJarToDistributedCache(ObjectMapper.class, config));

		List<Scan>scans=new ArrayList<Scan>();
		Scan scan = new Scan();			
		scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes("lsid:17a"));
		scans.add(scan);
		
		Scan scann = new Scan();			
		scann.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes("lsid:17scan"));
		scans.add(scann);
		
		TableMapReduceUtil.initTableMapperJob(     
				scans,              
			CountMapper.class,  
			Text.class,       
			IntWritable.class,
			job);
		job.setReducerClass(CountReducer.class);  
		job.setNumReduceTasks(1); 
		job.setOutputFormatClass(NullOutputFormat.class);
		
		boolean b = job.waitForCompletion(true);
		if (!b) {
			throw new IOException("error with job!");
		}
		
	}
}