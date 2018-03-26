package lsid.mapred;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.util.DefaultCipher;

public class tzbprovinceTimeCountMapper extends TableMapper<Text, IntWritable> {

	private final IntWritable ONE = new IntWritable(1);

	public void map(ImmutableBytesWritable row, Result value, Context context)
			throws IOException, InterruptedException {
		try {
			byte[] c = value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("p"));
			if (c != null && Bytes.toString(c) != null) {
				String[] parts = Bytes.toString(c).split("#");
				if ("t1".equals(parts[2])) {
					long lucktime = 0l;
					if (parts.length > 33) {
						try {
							lucktime = Long.parseLong(parts[16]);
						} catch (Exception e) {
							// do nothing
						}
					}
					if (lucktime <= 1514735999000l) {
						int pool = Integer.valueOf(parts[34].toString());
						if ((pool % 2 != 0 && pool != 9 && pool != 7)
								|| pool == 4 || pool == 2) {
							Text t = new Text();
							String province = parts[25];
							String city = parts[26];
							String prize = parts[34] + parts[35];
//							if (parts.length > 40) {
//								t.set(city + "_" + prize + "_success");
//							} else 
								if (parts.length > 38) {
									t.set(city + "_" + prize);								
							}
							context.write(t, ONE);
						}
					}
				}
			}

//			if (prizinfo.isEmpty()) {
//				t.set(city + "_" + prize + "_1");
//			} else {
//				t.set(city + "_" + prize + "_0");
//			}
			
			
			// try {
			// byte[] c = value.getValue(Bytes.toBytes("cf"),
			// Bytes.toBytes("p"));
			// if (c != null && Bytes.toString(c) != null) {
			// String[] parts = Bytes.toString(c).split("#");
			// if ("q".equals(parts[2])) {
			// long lucktime = 0l;
			// if (parts.length > 33) {
			// try {
			// lucktime = Long.parseLong(parts[16]);
			// } catch (Exception e) {
			// // do nothing
			// }
			// }
			// if (lucktime <= 1514735999000l) {
			// Date date =new Date(lucktime);
			// int hour=date.getHours();
			// String province = parts[25];
			// Text t = new Text();
			// t.set(hour+"");
			// context.write(t, ONE);
			// }
			// }
			// }
			// long lucktime = 0l;
			// if (parts.length>33) {
			// try {
			// lucktime = Long.parseLong(parts[16]);
			// }catch(Exception e) {
			// //do nothing
			// }
			// }
			// SimpleDateFormat sdf = new SimpleDateFormat("HH");
			// String curhour =sdf.format(lucktime);
			//
			// String gen = "0";
			// try {
			// gen=String.valueOf(new ObjectMapper().readTree(new
			// String(DefaultCipher.dec(parts[19]))).get("sex").asInt());
			// }catch(Exception e) {
			// //do nohting
			// }
			//
			// Text t = new Text();
			// t.set(curhour+"_"+gen);
			// context.write(t, ONE);

		} catch (Exception ex) {
			throw new IOException(ex);
		}

	}
}