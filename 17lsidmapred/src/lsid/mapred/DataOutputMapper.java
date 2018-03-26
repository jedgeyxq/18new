package lsid.mapred;

import java.io.IOException;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;

public class DataOutputMapper extends TableMapper<ImmutableBytesWritable, ImmutableBytesWritable> {

	public void map(ImmutableBytesWritable row, Result value, Context context)
			throws IOException, InterruptedException {
		try {
  			if (Bytes.toString(row.get()).endsWith("_"+context.getConfiguration().get("eid"))){
				byte[] c = value.getValue(Bytes.toBytes("cf"), Bytes.toBytes(context.getConfiguration().get("col")));
				if (c != null && Bytes.toString(c) != null) {
					ImmutableBytesWritable val = new ImmutableBytesWritable();
					val.set(c);
					context.write(row, val);
				}
			}
		} catch (Exception ex) {
			throw new IOException(ex);
		}

	}
}