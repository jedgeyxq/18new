package lsid.mapred;

import java.io.IOException;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import com.lsid.util.DefaultCipher;

public class DayCountMapper extends TableMapper<Text, IntWritable>  {
	
	private final IntWritable ONE = new IntWritable(1);

   	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try{
   			if (Bytes.toString(row.get()).endsWith("_"+context.getConfiguration().get("eid"))){
	   			byte[] c = value.getValue(Bytes.toBytes("cf"), Bytes.toBytes(context.getConfiguration().get("col")));
				if (c != null && Bytes.toString(c) != null) {				
					String[] parts = DefaultCipher.dec(Bytes.toString(c)).split("#");
					String suffix = "";
					for (int i=Integer.parseInt(context.getConfiguration().get("start"));
							i<parts.length-Integer.parseInt(context.getConfiguration().get("end"));i++){
						suffix +="_"+parts[i].replaceAll("_", "-");
					}
					Text t = new Text();
		   			t.set(parts[parts.length-Integer.parseInt(context.getConfiguration().get("day"))].split(" ")[0]+suffix);
			   		context.write(t, ONE);
				}
   			}
   		}catch(Exception ex){
   			throw new IOException(ex);
   		}
   		
   	}
}