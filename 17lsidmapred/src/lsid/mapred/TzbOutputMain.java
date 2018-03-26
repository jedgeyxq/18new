package lsid.mapred;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import lsid.mapred.util.JobHelper;

public class TzbOutputMain{
	public static void main(String[] s) throws IOException{
		try {
		Configuration config = HBaseConfiguration.create();
		Job job = Job.getInstance(config);
		job.setJarByClass(TzbOutputMain.class);  
		job.addFileToClassPath(JobHelper.addJarToDistributedCache(ObjectMapper.class, config));
		new ObjectMapper().readTree("{\"name\":\"a\"}").get("name").toString();
		Scan scan = new Scan();		
		TableMapReduceUtil.initTableMapperJob(
			"lsid:17prize",      
			scan,              
			TzbOutputMapper.class,  
			ImmutableBytesWritable.class,       
			ImmutableBytesWritable.class,
			job);
		job.setReducerClass(TzbOutputReducer.class);  
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