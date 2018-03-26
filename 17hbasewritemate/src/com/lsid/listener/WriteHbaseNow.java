package com.lsid.listener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.hbase.read.HBaseRead;
import com.lsid.hbase.write.HBaseWrite;
import com.lsid.util.DefaultCipher;

public class WriteHbaseNow implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!AutoConfig.isrunning || AutoConfig.config(null, "hbaselaterfolder").isEmpty()) {
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
		Path thefolder = Paths.get(AutoConfig.config(null, "hbaselaterfolder")).resolve("writehbaselater");
		final Path therrorfolder = Paths.get(AutoConfig.config(null, "hbaselaterfolder")).resolve("writehbaselaterror");
		System.out.println("========" + new Date() + "======== started watching " + thefolder);
		while (AutoConfig.isrunning) {
			String[] namespaces = thefolder.toFile().list();
			if (namespaces == null) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// do nothing
				}
			} else {
				for (String namespace : namespaces) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// do nothing
					}
					BufferedReader br = null;
					try {
						Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c",
								"ls " + thefolder.resolve(namespace).toString() + " -rt | head -n 10000" });
						br = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line = br.readLine();
						while (line != null) {
							Path file = thefolder.resolve(namespace).resolve(line);

							if (AutoConfig.isrunning) {
								if (!Files.isDirectory(Paths.get(file.toString()))) {
									String filename = Paths.get(file.toString()).getFileName().toString();
									try {
										long size = Long.valueOf(
												filename.substring(filename.lastIndexOf(AutoConfig.SPLIT) + 1));
										String tablename = filename.substring(0, filename.indexOf(AutoConfig.SPLIT));
										if (Files.readAllBytes(Paths.get(file.toString())).length == size) {
											Thread.sleep(10);
											String contentstr = Files
													.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8"))
													.get(0);
											String[] content = contentstr.split(AutoConfig.SPLIT);
											String action = content[0];
											String hash = content[1];
											String row = content[2];
											String column = content[3];
											if ("put".equals(action)) {
												String value = contentstr
														.substring((action + AutoConfig.SPLIT + hash + AutoConfig.SPLIT
																+ row + AutoConfig.SPLIT + column + AutoConfig.SPLIT)
																		.length());
												try {
													HBaseWrite.getinstance().put(namespace, tablename, hash, row,
															column, value);
													if (tablename.equals("scan") || tablename.equals("prize")) {
														String[] d = value.split(AutoConfig.SPLIT);
														//没授权之前 info  扫码人数
														if (tablename.equals("scan") && value.startsWith("info")) {
															String uuid = null;
															try {
																uuid = row.substring(
																		row.indexOf(AutoConfig.SPLIT_HBASE) + 1,
																		row.lastIndexOf(AutoConfig.SPLIT_HBASE));
																long uuidtimes = AutoConfig.incrementcache(namespace,
																		uuid, "count", uuid,
																		"t" + AutoConfig.SPLIT_HBASE
																				+ AutoConfig.getfrom(d),
																		1);
																if (uuidtimes == 1) {
																	AutoConfig.incrementcachemore(namespace, "preauth",
																			"count", "preauth",
																			"t" + AutoConfig.SPLIT_HBASE
																					+ AutoConfig.getfrom(d),
																			1);
																}
															} catch (Exception e) {
																AutoConfig.log(e,
																		"error incrementing scan data uuid=[" + uuid
																				+ "],row=[" + row + "],value=[" + value
																				+ "]");
															}
														}
														if ("a".equals(namespace)
																&& AutoConfig.getintime(d) > 1503507600000l
																&& tablename.equals("scan")
																&& value.startsWith("play")) {
															try {
																AutoConfig.innerpost(
																		AutoConfig.config(null, "lsid.interface.notify")
																				+ "tdd",
																		Integer.parseInt(AutoConfig.config(null,
																				"lsid.interface.notify.connectimeoutinsec")),
																		Integer.parseInt(AutoConfig.config(null,
																				"lsid.interface.notify.socketimeoutinsec")),
																		"eid", namespace, "msgtype", "aojin", "content",
																		value);
															} catch (Exception e) {
																AutoConfig.log(e,
																		"error notifying aojin auth data,row=[" + row
																				+ "],value=[" + value + "]");
															}
														}

														Long time = 0l;
														try {
															if (tablename.equals("scan")) {
																time = AutoConfig.getintime(d);
															}
															if (tablename.equals("prize")) {
																time = AutoConfig.getfirstplaytime(d);
															}
															if (tablename.equals("prize")) {
																try {
																	//实时数据地图推送
																	AutoConfig.innerpost(
																			AutoConfig.config(null,
																					"lsid.interface.consoledata")
																					+ "spd",
																			Integer.parseInt(AutoConfig.config(null,
																					"lsid.interface.consoledata.connectimeoutinsec")),
																			Integer.parseInt(AutoConfig.config(null,
																					"lsid.interface.consoledata.socketimeoutinsec")),
																			"eid", namespace, "sp", value);
																} catch (Exception e) {
																	AutoConfig.log(e,
																			"error notifying real baidu data,row=["
																					+ row + "],value=[" + value + "]");
																}
																//实物 奖品
																if (AutoConfig.finished(value)) {
																	if (AutoConfig.config(namespace, "lsid.pool"
																			+ AutoConfig.getpoolid(d) + ".prize"
																			+ AutoConfig.getprizeid(d) + ".type")
																			.equals("inkind")) {
																		try {
																			AutoConfig.innerpost(AutoConfig.config(null,
																					"lsid.interface.consoledata")
																					+ "tdd",
																					Integer.parseInt(AutoConfig.config(
																							null,
																							"lsid.interface.consoledata.connectimeoutinsec")),
																					Integer.parseInt(AutoConfig.config(
																							null,
																							"lsid.interface.consoledata.socketimeoutinsec")),
																					"eid", namespace, "oid", row, "tdd",
																					value);
																		} catch (Exception e) {
																			AutoConfig.log(e,
																					"error notifying inkind data,row=["
																							+ row + "],value=[" + value
																							+ "]");
																		}
																	}
																	//领完奖  发货（红包到账）完长度是42，没发货是小于42（抽完奖都是38，领完奖是40，开始发放是42）
																	if (value.split(AutoConfig.SPLIT).length < 42) {
																		try {
																			AutoConfig.incrementcache(namespace,
																					"prize", "count", "prize",
																					"t" + AutoConfig.SPLIT_HBASE
																							+ AutoConfig.getfrom(d),
																					1);
																		} catch (Exception e) {
																			AutoConfig.log(e,
																					"error incrementing confirm prize data,row=["
																							+ row + "],value=[" + value
																							+ "]");
																		}
																		if ("t1".equals(namespace)
																				&& !AutoConfig.config(namespace,
																						"notify.msgtype." + AutoConfig
																								.config(namespace,
																										"lsid.pool"
																												+ AutoConfig
																														.getpoolid(
																																d)
																												+ ".prize"
																												+ AutoConfig
																														.getprizeid(
																																d)
																												+ ".desc"))
																						.isEmpty()) {
																			boolean tonotify = true;
																			//addr#uuid666 都填写了不会通知
																			if (DefaultCipher.dec(d[38]).split(AutoConfig.SPLIT).length>=AutoConfig
																					.config(namespace,
																							"lsid.pool"
																									+ AutoConfig
																											.getpoolid(
																													d)
																									+ ".prize"
																									+ AutoConfig
																											.getprizeid(
																													d)
																									+ ".require").split(AutoConfig.SPLIT).length) {
																				tonotify=false;
																			}
																			if (tonotify) {
																				try {
																					AutoConfig.innerpost(
																							AutoConfig.config(null,
																									"lsid.interface.notify")
																									+ "tdd",
																							Integer.parseInt(
																									AutoConfig.config(null,
																											"lsid.interface.notify.connectimeoutinsec")),
																							Integer.parseInt(
																									AutoConfig.config(null,
																											"lsid.interface.notify.socketimeoutinsec")),
																							"eid", namespace, "msgtype",
																							AutoConfig.config(namespace,
																									"notify.msgtype."
																											+ AutoConfig
																													.config(namespace,
																															"lsid.pool"
																																	+ AutoConfig
																																			.getpoolid(
																																					d)
																																	+ ".prize"
																																	+ AutoConfig
																																			.getprizeid(
																																					d)
																																	+ ".desc")),
																							"content", value);
																				} catch (Exception e) {
																					AutoConfig.log(e,
																							"error notifying tzb data,row=["
																									+ row + "],value=["
																									+ value + "]");
																				}
																			}
																		}
																	}

																	String prizedesc = AutoConfig.config(namespace,
																			"lsid.pool" + AutoConfig.getpoolid(d)
																					+ ".prize"
																					+ AutoConfig.getprizeid(d)
																					+ ".desc");
																	if ("a".equals(namespace)
																			&& AutoConfig.getintime(d) > 1503507600000l
																			&& ("100".equals(prizedesc)
																					|| "101".equals(prizedesc)
																					|| "103".equals(prizedesc)
																					|| "105".equals(prizedesc))) {
																		try {
																			AutoConfig.innerpost(
																					AutoConfig.config(null,
																							"lsid.interface.notify")
																							+ "tdd",
																					Integer.parseInt(AutoConfig.config(
																							null,
																							"lsid.interface.notify.connectimeoutinsec")),
																					Integer.parseInt(AutoConfig.config(
																							null,
																							"lsid.interface.notify.socketimeoutinsec")),
																					"eid", namespace, "msgtype",
																					"aojin", "content", value);
																		} catch (Exception e) {
																			AutoConfig.log(e,
																					"error notifying aojin prize data,row=["
																							+ row + "],value=[" + value
																							+ "]");
																		}
																	}
																}
															}
														} catch (Exception e) {
															AutoConfig.log(e,
																	"error processing data partition table=["
																			+ tablename + "],row=[" + row + "],value=["
																			+ value + "]");
														}
														if (time > 0) {
															tablename += new SimpleDateFormat("yyyyMM")
																	.format(new Date(time));
															HBaseWrite.getinstance().put(namespace, tablename, hash,
																	row, column, value);
														}
													}

													if (tablename.equals("a") || tablename.equals("na")) {
														String[] decprodinfo = DefaultCipher.dec(value)
																.split(AutoConfig.SPLIT);
														String line_batch_prod = decprodinfo[Integer
																.parseInt(AutoConfig.config(namespace,
																		"lsid.code.line" + tablename + ".index"))]
																+ "_-_"
																+ decprodinfo[Integer
																		.parseInt(AutoConfig.config(namespace,
																				"lsid.code.batch"
																						+ tablename + ".index"))]
																+ "_-_"
																+ decprodinfo[Integer.parseInt(AutoConfig.config(
																		namespace,
																		"lsid.code.prod" + tablename + ".index"))];
														String date = "";
														try {
															date = new SimpleDateFormat("yyyy-MM-dd")
																	.format(new SimpleDateFormat(AutoConfig
																			.config(namespace,
																					"lsid.code.valid." + tablename)
																			.split(AutoConfig.SPLIT)[Integer.parseInt(
																					AutoConfig.config(namespace,
																							"lsid.code.time" + tablename
																									+ ".index"))]
																											.substring(
																													5)).parse(
																															decprodinfo[Integer
																																	.parseInt(
																																			AutoConfig
																																					.config(namespace,
																																							"lsid.code.time"
																																									+ tablename
																																									+ ".index"))]));
														} catch (Exception e) {
															date = new SimpleDateFormat("yyyy-MM-dd")
																	.format(new Date());
														}
														Socket s = null;
														BufferedWriter bw = null;
														try {
															s = new Socket(
																	AutoConfig
																			.config(null,
																					"lsid.interface.consoledata.socket")
																			.split(AutoConfig.SPLIT)[0],
																	Integer.parseInt(AutoConfig
																			.config(null,
																					"lsid.interface.consoledata.socket")
																			.split(AutoConfig.SPLIT)[1]));
															bw = new BufferedWriter(
																	new OutputStreamWriter(s.getOutputStream()));
															bw.write("i" + AutoConfig.SPLIT + namespace
																	+ AutoConfig.SPLIT + "repositorydata/" + tablename
																	+ "/" + date + AutoConfig.SPLIT + line_batch_prod
																	+ AutoConfig.SPLIT + 1);
															bw.newLine();
															bw.flush();
															Files.delete(Paths.get(file.toString()));
														} catch (Exception e) {
															AutoConfig.log(e,
																	"error stating a and na code=[" + tablename
																			+ "],row=[" + row + "],value=[" + value
																			+ "]");
														} finally {
															if (bw != null) {
																bw.close();
															}
															if (s != null) {
																s.close();
															}
														}
													} else {
														Files.delete(Paths.get(file.toString()));
													}
												} catch (Exception e) {
													if (HBaseWrite.unavailable.equals(e.getMessage())) {
														// do nothing
													} else {
														throw e;
													}
												}
											} else if ("increment".equals(action)) {
												String amount = "0";
												if (content.length > 4) {
													amount = content[4];
												}
												try {
													HBaseWrite.getinstance().increment(namespace, tablename, hash, row,
															column, Long.valueOf(amount));
													if (tablename.equals("count")) {
														try {
															//扫码人次
															if (column.startsWith("senc" + AutoConfig.SPLIT_HBASE)
																	&& !row.contains(AutoConfig.SPLIT_HBASE)
																	&& !"total".equals(row)) {
																try {
																	AutoConfig.incrementcachemore(namespace, "scan",
																			"count", "scan",
																			"t" + column.substring(column
																					.indexOf(AutoConfig.SPLIT_HBASE)),
																			1);
																} catch (Exception e) {
																	AutoConfig.log(e,
																			"error incrementing scan data,row=[" + row
																					+ "],value=[" + amount + "]");
																}
															}
															//授权人次
															if (column.startsWith("suser" + AutoConfig.SPLIT_HBASE)
																	&& !row.contains(AutoConfig.SPLIT_HBASE)
																	&& !"total".equals(row)) {
																try {
																	AutoConfig.incrementcache(namespace, "auth",
																			"count", "auth",
																			"t" + column.substring(column
																					.indexOf(AutoConfig.SPLIT_HBASE)),
																			1);
																} catch (Exception e) {
																	AutoConfig.log(e,
																			"error incrementing auth times data,row=["
																					+ row + "],value=[" + amount + "]");
																}
																//授权人数
																if (HBaseRead.getinstance().getlong(namespace,
																		tablename, hash, row, column) == 1) {
																	try {
																		AutoConfig.incrementcache(namespace, "user",
																				"count", "user",
																				"t" + column.substring(column.indexOf(
																						AutoConfig.SPLIT_HBASE)),
																				1);
																	} catch (Exception e) {
																		AutoConfig.log(e,
																				"error incrementing auth users data,row=["
																						+ row + "],value=[" + amount
																						+ "]");
																	}
																}
															}
															//领奖码数
															if (column.startsWith("penc" + AutoConfig.SPLIT_HBASE)
																	&& !row.contains(AutoConfig.SPLIT_HBASE)
																	&& !"total".equals(row)
																	&& HBaseRead.getinstance().getlong(namespace,
																			tablename, hash, row, column) == 1) {
																try {
																	AutoConfig.incrementcache(namespace, "enc", "count",
																			"enc",
																			"t" + column.substring(column
																					.indexOf(AutoConfig.SPLIT_HBASE)),
																			1);
																} catch (Exception e) {
																	AutoConfig.log(e,
																			"error incrementing number of enc data,row=["
																					+ row + "],value=[" + amount + "]");
																}
															}
															//抽奖人次 参与人次
															if (column.startsWith("puser" + AutoConfig.SPLIT_HBASE)
																	&& !row.contains(AutoConfig.SPLIT_HBASE)
																	&& !"total".equals(row)) {
																try {
																	AutoConfig.incrementcache(namespace, "luck",
																			"count", "luck",
																			"t" + column.substring(column
																					.indexOf(AutoConfig.SPLIT_HBASE)),
																			1);
																} catch (Exception e) {
																	AutoConfig.log(e,
																			"error incrementing luck times data,row=["
																					+ row + "],value=[" + amount + "]");
																}
																//参与人数
																if (HBaseRead.getinstance().getlong(namespace,
																		tablename, hash, row, column) == 1) {
																	try {
																		AutoConfig.incrementcache(namespace, "luckuser",
																				"count", "luckuser",
																				"t" + column.substring(column.indexOf(
																						AutoConfig.SPLIT_HBASE)),
																				1);
																	} catch (Exception e) {
																		AutoConfig.log(e,
																				"error incrementing lucked users data,row=["
																						+ row + "],value=[" + amount
																						+ "]");
																	}
																}
															}
														} catch (Exception e) {
															AutoConfig.log(e, "error incrementing realtime data,row=["
																	+ row + "],value=[" + amount + "]");
														}
													}
													Files.delete(Paths.get(file.toString()));
												} catch (Exception e) {
													if (HBaseWrite.unavailable.equals(e.getMessage())) {
														// do nothing
													} else {
														throw e;
													}
												}
											} else {
												throw new Exception("unsupported action [" + action + "]");
											}
										}
									} catch (Exception e) {
										AutoConfig.log(e, "error processing " + file);
										Files.createDirectories(therrorfolder.resolve(namespace));
										Files.move(Paths.get(file.toString()),
												therrorfolder.resolve(namespace).resolve(filename),
												StandardCopyOption.REPLACE_EXISTING);
									}
								}
							} else {
								break;
							}
							line = br.readLine();
						}
					} catch (Exception e) {
						AutoConfig.log(e, "System exited due to below exception:");
						System.exit(1);
					} finally {
						if (br != null) {
							try {
								br.close();
							} catch (IOException e) {
								AutoConfig.log(e, "error closing command: ls " + thefolder.resolve(namespace).toString()
										+ " -rt | head -n 10000");
							}
						}
						br = null;
					}
				}
			}
		}
	}

}
