package lsid.mapred;

import java.io.IOException;
import java.net.URLDecoder;
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

public class PrizephoneMapper extends TableMapper<Text, IntWritable>  {
	
	private final IntWritable ONE = new IntWritable(1);
	
	/*
	 column=cf:p, value=
	 luck#
	 wx#
	 qq#
	 JVx%2BnpYUfsmuoNBPFO5PXOLsPZSwz9jkit6ld%2FgLdt0%3D#
	 JVx%2BnpYUfsmuoNBPFO5PXF7jkvFyCJWbxcaylNTexgaGFIMTEL2pAfTb%2BccrFZIuN%2FE0RWQImAQz%0AuLlp1LLS8A%3D%3D#
	 #
	 Mozilla%2F5.0+%28Linux%3B+Android+7.1.1%3B+ONEPLUS+A5000+Build%2FNMF26X%3B+wv%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Version%2F4.0+Chrome%2F53.0.2785.49+Mobile+MQQBrowser%2F6.2+TBS%2F043313+Safari%2F537.36+MicroMessenger%2F6.5.10.1080+NetType%2FWIFI+Language%2Fzh_CN#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501578904973#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 1501578908162#
	 l%2FSGu5OlE7PRpyxJ7ShfJcyOOAUvxN9i3E%2BDNB%2BLCn7Nf49iIkkcBscoJdeOn4nAnp6TT5LO7oHf%0AD3JKUe0BEwgHO6nz0H%2FzhuCtBl0T%2FVM0FKUr4DY57xh2zbuUTGNV9fSpYxmjdfSjTtNg5ZrzI6Is%0A0mjhkXBwJ3FWuElfmDoNasBRn7oQ%2FpDOgTkh3YHfqGxWUQtH57zN%2B5VO8gYNNS2CNonOZMzWN70m%0AxAsGmXA6roQhpAWOdR0DJosKxD4yAyb2wXaSiZuKOusaco%2F%2B9u4%2FNMcZQzc%2BAPxuKDFOJWJlZl06%0AVUBEA8skYwINYQgJfIbsJ4Y0NASHESbi8ndjsuOGr1PLpxoc%2FplRtAJXrdmtWWOyrEiQ73YXoQYO%0AETtrR5y19W2CyiIG%2F%2B8G65dnkg%3D%3D#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 0#
	 null#
	 Mozilla%2F5.0+%28Windows+NT+10.0%3B+WOW64%3B+rv%3A54.0%29+Gecko%2F20100101+Firefox%2F54.0#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501578929874#
	 0#
	 0#
	 aBdgbmrx2Jia5iyYgSddnw%3D%3D#
	 1501578935747
	 
	 finish#
	 wx#
	 qq#
	 JVx%2BnpYUfsmuoNBPFO5PXOLsPZSwz9jkit6ld%2FgLdt0%3D#
	 JVx%2BnpYUfsmuoNBPFO5PXF7jkvFyCJWbxcaylNTexgaGFIMTEL2pAfTb%2BccrFZIuN%2FE0RWQImAQz%0AuLlp1LLS8A%3D%3D#
	 #
	 Mozilla%2F5.0+%28Linux%3B+Android+7.1.1%3B+ONEPLUS+A5000+Build%2FNMF26X%3B+wv%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Version%2F4.0+Chrome%2F53.0.2785.49+Mobile+MQQBrowser%2F6.2+TBS%2F043313+Safari%2F537.36+MicroMessenger%2F6.5.10.1080+NetType%2FWIFI+Language%2Fzh_CN#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501049400656#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 1501049406496#
	 l%2FSGu5OlE7PRpyxJ7ShfJcyOOAUvxN9i3E%2BDNB%2BLCn7Nf49iIkkcBscoJdeOn4nA6pyA7ksrjZWt%0ABZN10nZmiRMSrV4qzj21%2BVYu8GkJbkg7kL4dcqdcwh1kooCQ5Q4r2X0HZaTuxu8foXPX7ft4MrK4%0Athfq0STo6XqIY0mPglD6pIYly2X8gLG6z5XM9PlhnDnxAU0yJuAmMz79254mNh2ficcLwURt3App%0AKc99iYYOZjzhlg2Vt5uUcdUNhieyJG2Q6KDxkpBG%2B0TFfq%2F%2BeepKn2gqGibjTQTxnrVfWz1LWvWF%0AP%2F%2FHcJMtYWoRFpf3cyOy44ApLnbA8wU%2BBNfYubX3NApuX%2BtWkdipvUUP64m9dCsssWVIRvhsmOOJ%0Akw%2BD#
	 m6vN0zv4oA%252FIXUIrwB2ExEX5fN%252B0D74Z%252FL8Ff36oYdQ%253D#
	 0#
	 null#
	 Mozilla%2F5.0+%28Windows+NT+10.0%3B+WOW64%3B+rv%3A54.0%29+Gecko%2F20100101+Firefox%2F54.0#
	 101.81.231.157#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E4%B8%8A%E6%B5%B7%E5%B8%82#
	 %E9%97%B8%E5%8C%97%E5%8C%BA#
	 %E5%A4%A9%E6%BD%BC%E8%B7%AF#
	 619%E5%8F%B7#
	 %E4%B8%AD%E5%9B%BD#
	 121.48789948999993#
	 31.249161578948787#
	 1501049687858#
	 0#
	 0#
	 aBdgbmrx2Jia5iyYgSddnw%3D%3D#
	 1501475236765#
	 K6qiBo6RgxWyZ0AIYNQCbw%3D%3D#
	 1501476768039
	 */
	
	//http://www.fynas.com/ua/search?b=&d=iphone
	
   	public void map(ImmutableBytesWritable row, Result value, Context context) throws IOException, InterruptedException {
   		try{
   			byte[] c = value.getValue(Bytes.toBytes("cf"), Bytes.toBytes("p"));
	   		if (c!=null&&Bytes.toString(c)!=null) {
	   			String[] parts = Bytes.toString(c).split("#");
	   			String eid = parts[2];
	   			if (eid.equals("t2")) {
	   				String province = parts[8];
	   				String city = parts[9];
	   				String gen = "unknown";
	   				try {
		   				int g = new ObjectMapper().readTree(new String(DefaultCipher.dec(parts[19]))).get("sex").asInt();
		   				if (g==1) {
		   					gen = "boy";
		   				} else if (g==2){
		   					gen = "girl";
		   				}
	   				}catch(Exception e) {
	   					//do nohting
	   				}
	   				String model = "unknown";
	   				String useragent = URLDecoder.decode(parts[6],"UTF-8");
	   				if (useragent.toUpperCase().contains(" ONEPLUS ")) {
	   					model = "oneplus";
	   				} else if (useragent.toUpperCase().contains(" SM919 ")||useragent.toUpperCase().contains(" OD105 ") ||useragent.toUpperCase().contains(" SM801 ")||useragent.toUpperCase().contains(" YQ601 ")||useragent.toUpperCase().contains(" SM701 ")) {
	   					model = "smartisan";
	   				} else if(useragent.toUpperCase().contains(" OPPO ")) {
	   					model = "OPPO";
	   				} else if(useragent.toUpperCase().contains(" VIVO ")) {
	   					model = "vivo";
	   				} else if (useragent.toUpperCase().contains(" PRO 6 ")||useragent.toUpperCase().contains(" PRO 5 ") ||useragent.toUpperCase().contains(" MX5 ")||useragent.toUpperCase().contains(" MX6 ")||useragent.toUpperCase().contains(" SM701 ")) {
	   					model = "meizu";
	   				} else if (useragent.toUpperCase().contains(" SM-")) {
	   					model = "samsung";
	   				} else if (useragent.toUpperCase().contains(" MI ")) {
	   					model = "xiaomi";
	   				} else if (useragent.toUpperCase().contains(" REDMI ")) {
	   					model = "redmi";
	   				} else if (useragent.toUpperCase().contains("HUAWEI")) {
	   					model = "huawei";
	   				} else if (useragent.toUpperCase().contains("HONOR")) {
	   					model = "honor";
	   				} else if (useragent.toUpperCase().contains("IPHONE")) {
	   					model = useragent.replaceAll("_", "-");
	   				}
	   				Text t = new Text();
		   			t.set(Bytes.toBytes(model+"_"+province+"_"+city+"_"+gen));
			   		context.write(t, ONE);
	   			}
	   		}
   			
   		}catch(Exception ex){
   			throw new IOException(ex);
   		}
   		
   		
   	}
}