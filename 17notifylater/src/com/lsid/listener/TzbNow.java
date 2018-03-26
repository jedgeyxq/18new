package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;

public class TzbNow implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!AutoConfig.isrunning || AutoConfig.config(null, "outerlaterfolder").isEmpty()) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				AutoConfig.iamrunning();
				process();
				AutoConfig.iamdone();
			}

		}).start();
	}

	private void process() {
		final Path thefolder = Paths.get(AutoConfig.config(null, "outerlaterfolder")).resolve("later");
		try {
			Files.createDirectories(thefolder);
			System.out.println("========" + new Date() + "======== once a day started watching " + thefolder);
		} catch (IOException e2) {
			AutoConfig.log(e2, "System exited due to below exception:");
			System.exit(1);
			return;
		}
		try {
			ScheduledExecutorService executor1 = Executors.newScheduledThreadPool(2);
			long oneDay = 24 * 60 * 60 * 1000;
			long initDelay = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
					.parse(new SimpleDateFormat("yyyyMMdd ").format(new Date()) + "01:00:00").getTime()
					- System.currentTimeMillis();
			initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;

			executor1.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {

					if (AutoConfig.isrunning) {
						String[] namespaces = thefolder.toFile().list();
						if (namespaces == null) {
							try {
								Thread.sleep(30000);
							} catch (InterruptedException e) {
								// do nothing
							}
						} else {
							for (String namespace : namespaces) {
								if (!"t1".equals(namespace)) {
									continue;
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e1) {
									// do nothing
								}
								BufferedReader br = null;
								try {
									Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "ls "
											+ thefolder.resolve(namespace).toString() + " -rt | head -n 100000" });
									br = new BufferedReader(new InputStreamReader(p.getInputStream()));
									String line = br.readLine();
									List<TzbDataChild> datalist = new ArrayList<TzbDataChild>(10000);
									List<Path> todelete = new ArrayList<Path>(100000);
									while (line != null) {
										Path file = thefolder.resolve(namespace).resolve(line);
										if (AutoConfig.isrunning) {
											if (!Files.isDirectory(Paths.get(file.toString()))) {
												if (Files.readAllBytes(Paths.get(file.toString())).length == 0
														&& System.currentTimeMillis()
																- Files.getLastModifiedTime(Paths.get(file.toString()))
																		.toMillis() > 24 * 60 * 60 * 1000) {
													Files.delete(Paths.get(file.toString()));
												} else {
													String filename = Paths.get(file.toString()).getFileName()
															.toString();
													long size = Long.valueOf(filename
															.substring(filename.lastIndexOf(AutoConfig.SPLIT) + 1));
													if (Files.readAllBytes(Paths.get(file.toString())).length == size) {
														Thread.sleep(10);
														String contentstr = Files
																.readAllLines(Paths.get(file.toString()),
																		Charset.forName("UTF-8"))
																.get(0);
														String[] content = contentstr.split(AutoConfig.SPLIT);
														TzbDataChild tdc = new TzbDataChild();
														tdc.setactivity_datetime(
																String.valueOf(AutoConfig.getlucktime(content)));
														tdc.setopen_id(
																DefaultCipher.dec(AutoConfig.getplayid(content)));
														tdc.setnick_name(new ObjectMapper()
																.readTree(DefaultCipher
																		.dec(AutoConfig.getencuserinfo(content)))
																.get("nickname").asText());
														String value = AutoConfig.config(namespace,
																"lsid.pool" + AutoConfig.getpoolid(content) + ".prize"
																		+ AutoConfig.getprizeid(content) + ".value");
														tdc.setaction_amount(new StringBuilder(value)
																.insert(value.length() - 2, ".").toString());
														datalist.add(tdc);
														todelete.add(Paths.get(file.toString()));

														if (datalist.size() == 10000) {
															TzbData data = new TzbData();
															data.setdata_list(datalist);
															transfer(data);
															datalist.clear();
															datalist = new ArrayList<TzbDataChild>(10000);
															for (Path todeletefile : todelete) {
																Files.deleteIfExists(todeletefile);
															}
															todelete.clear();
															todelete = new ArrayList<Path>(100000);
														}

													}
												}
											}
										} else {
											break;
										}
										line = br.readLine();
									}
									if (!datalist.isEmpty()) {
										TzbData data = new TzbData();
										data.setdata_list(datalist);
										transfer(data);
										datalist.clear();
										datalist = new ArrayList<TzbDataChild>(10000);
										for (Path todeletefile : todelete) {
											Files.deleteIfExists(todeletefile);
										}
										todelete.clear();
										todelete = new ArrayList<Path>(100000);
									}
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									if (br != null) {
										try {
											br.close();
										} catch (IOException e) {
											AutoConfig.log(e, "error closing command: ls "
													+ thefolder.resolve(namespace).toString() + " -rt | head -n 10000");
										}
									}
									br = null;
								}
							}
						}
					}
				}
			}, initDelay, oneDay, TimeUnit.MILLISECONDS);
			while (AutoConfig.isrunning) {
				Thread.sleep(30000);
			}
			executor1.shutdown();
		} catch (Exception ex) {
			AutoConfig.log(ex, "System existed due to below exception:");
			System.exit(1);
		}
	}

	public static void main(String[] s) {
		System.out.println(new StringBuilder("666").insert("666".length() - 2, ".").toString());
	}

	public static void main1(String[] s) throws Exception {

		List<TzbDataChild> datalist = new ArrayList<TzbDataChild>(10000);

		for (int i = 0; i < 2; i++) {
			TzbDataChild tdc = new TzbDataChild();
			tdc.setactivity_datetime(String.valueOf(new Date().getTime()));
			tdc.setopen_id(DefaultCipher.dec("rhFgmXGoneicsI1v6NgLlttUD%2BHGIcxSYGV7RxMYHi4%3D"));
			tdc.setnick_name("特种兵" + i);
			tdc.setaction_amount("6.66");
			datalist.add(tdc);
		}

		TzbData data = new TzbData();

		data.setdata_list(datalist);

		CloseableHttpClient httpclient = HttpClients.custom().build();

		String tosend = new ObjectMapper().writeValueAsString(data);

		System.out.println(tosend);

		HttpPost httpPost = new HttpPost("http://wx.susa-hk.com/addons/vote/pushData.php");
		httpPost.addHeader("Content-type", "application/json;charset=UTF-8");
		httpPost.setHeader("Accept", "application/json");
		StringEntity entity = new StringEntity(tosend, "UTF-8");
		entity.setContentEncoding("UTF-8");
		entity.setContentType("application/json");
		httpPost.setEntity(entity);
		CloseableHttpResponse response = httpclient.execute(httpPost);

		System.out.println("[" + EntityUtils.toString(response.getEntity(), "UTF-8") + "]");
	}

	private static void transfer(TzbData data) throws Exception {
		CloseableHttpClient httpclient = HttpClients.custom().build();
		String tosend = new ObjectMapper().writeValueAsString(data);
		HttpPost httpPost = new HttpPost("http://wx.susa-hk.com/addons/vote/pushData.php");
		httpPost.addHeader("Content-type", "application/json;charset=UTF-8");
		httpPost.setHeader("Accept", "application/json");
		StringEntity entity = new StringEntity(tosend, "UTF-8");
		entity.setContentEncoding("UTF-8");
		entity.setContentType("application/json");
		httpPost.setEntity(entity);
		CloseableHttpResponse response = httpclient.execute(httpPost);
		JsonNode jn = new ObjectMapper().readTree(EntityUtils.toString(response.getEntity(), "UTF-8"));
		if (jn.get("error") != null) {
			throw new Exception(jn.toString());
		}
	}

}
