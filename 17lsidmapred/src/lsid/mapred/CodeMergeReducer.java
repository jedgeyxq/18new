package lsid.mapred;

import java.io.IOException;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;

public class CodeMergeReducer  extends TableReducer<ImmutableBytesWritable, ImmutableBytesWritable, ImmutableBytesWritable>  {
	
	@Override
	public void setup(Context context) throws IOException {
  	}
	
	public void reduce(ImmutableBytesWritable row, Iterable<ImmutableBytesWritable> values, Context context) throws IOException, InterruptedException {
		try{
			for (ImmutableBytesWritable value:values){
				Put p = new Put(row.get());
				p.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("c"),value.get());
				context.write(null, p);
			}
		}catch(Exception e){
			throw new IOException(e);
		}
	}
	
	@Override
	public void cleanup(Context context) throws IOException {
	}

}