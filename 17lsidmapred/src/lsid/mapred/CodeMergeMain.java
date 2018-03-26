package lsid.mapred;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;

import com.fasterxml.jackson.databind.ObjectMapper;

import lsid.mapred.util.JobHelper;

//file:lsid.mapred.CodeMergeMain
//content:t:a#a:a#q:a#y:a#h:a#c:a#r:a#p:a#t1:a#lsid:17a
public class CodeMergeMain{
	public static void main(String[] s) throws IOException{
		if (s==null||s.length==0) {
			throw new IOException("missing code source table and target table as params");
		}
		try {
			for (int i=0;i<s.length-1;i++) {
				Configuration config = HBaseConfiguration.create();
				config.set("eid", s[i].substring(0,s[i].indexOf(":")));
				Job job = Job.getInstance(config);
				
				job.setJarByClass(CodeMergeMain.class);
				job.addFileToClassPath(JobHelper.addJarToDistributedCache(ObjectMapper.class, config));

				//11
				//22
				Scan scan = new Scan();		
				TableMapReduceUtil.initTableMapperJob(
					s[i],      
					scan,              
					CodeMergeMapper.class,  
					ImmutableBytesWritable.class,       
					ImmutableBytesWritable.class,
					job);
				
				TableMapReduceUtil.initTableReducerJob(s[s.length-1], CodeMergeReducer.class, job);
				
				boolean b = job.waitForCompletion(true);
				if (!b) {
					throw new IOException("error with job!");
				}
			}
		}catch(Exception ex) {
			throw new IOException(ex);
		}
	}
}