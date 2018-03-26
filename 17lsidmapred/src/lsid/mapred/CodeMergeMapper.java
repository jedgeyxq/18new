package lsid.mapred;

import java.io.IOException;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;

import com.lsid.util.DefaultCipher;

public class CodeMergeMapper extends TableMapper<ImmutableBytesWritable, ImmutableBytesWritable>  {

	private static final String hashvalue="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try {
   	   		ImmutableBytesWritable encprod = new ImmutableBytesWritable();
   	   		String ex = Bytes.toString(value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("ex")));
   	   		String ip="";
   	   		String time = String.valueOf(System.currentTimeMillis());
   	   		try {
   	   			ip = ex.substring(13, ex.length()-13);
   	   		}catch(Exception e) {
   	   			//do nothing
   	   		}
   	   		try {
	   			time = ex.substring(ex.length()-13);
	   		}catch(Exception e) {
	   			//do nothing
	   		}
	   		
			encprod.set(Bytes.toBytes(DefaultCipher.enc(DefaultCipher.dec(Bytes.toString(value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("c")))).replaceAll(",", "#")+"#"+ip+"#"+time)));
			ImmutableBytesWritable mergerow = new ImmutableBytesWritable();
			String rowvalue = Bytes.toString(row.get()).substring(1);
			String hash = rowvalue;
			mergerow.set(Bytes.toBytes(hashvalue.charAt(Math.abs(hash.hashCode())%hashvalue.length())+rowvalue+"_"+context.getConfiguration().get("eid")));
			context.write(mergerow, encprod);
		} catch (Exception e) {
			throw new IOException(e);
		}
   	}
	
}