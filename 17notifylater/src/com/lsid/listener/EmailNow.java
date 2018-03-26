package com.lsid.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsid.autoconfig.client.AutoConfig;
import com.lsid.util.DefaultCipher;
import com.sun.mail.util.MailSSLSocketFactory;

public class EmailNow implements ServletContextListener{
	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
    	new Thread(new Runnable(){

			@Override
			public void run() {
				while(!AutoConfig.isrunning||AutoConfig.config(null,"emailaterfolder").isEmpty()){
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
	
	private void process(){
		Path thefolder = Paths.get(AutoConfig.config(null,"emailaterfolder")).resolve("later");
		final Path therrorfolder = Paths.get(AutoConfig.config(null,"emailaterfolder")).resolve("laterror");
		try {
			Files.createDirectories(thefolder);
			System.out.println("========"+new Date()+"======== started watching "+thefolder);
		} catch (IOException e2) {
			AutoConfig.log(e2, "System exited due to below exception:");
			System.exit(1);
			return;
		}
		while(AutoConfig.isrunning){
			String[] namespaces = thefolder.toFile().list();
			if (namespaces==null) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					//do nothing
				}
			} else {
				for (String namespace:namespaces){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						//do nothing
					}
					BufferedReader br = null;
					try {
						Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh","-c", "ls "+thefolder.resolve(namespace).toString()+" -rt | head -n 10000"});
						br = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line = br.readLine();
						while (line!=null){
							Path file = thefolder.resolve(namespace).resolve(line);
							if (AutoConfig.isrunning){
								if (!Files.isDirectory(Paths.get(file.toString()))){
									if (Files.readAllBytes(Paths.get(file.toString())).length==0&&System.currentTimeMillis()-Files.getLastModifiedTime(Paths.get(file.toString())).toMillis()>24*60*60*1000) {
										Files.delete(Paths.get(file.toString()));
									} else {
										String filename = Paths.get(file.toString()).getFileName().toString();
										try{
											long size = Long.valueOf(filename.substring(filename.lastIndexOf(AutoConfig.SPLIT)+1));
											String msgtype=filename.substring(0,filename.indexOf(AutoConfig.SPLIT));
											if (Files.readAllBytes(Paths.get(file.toString())).length==size&&
													!AutoConfig.config(null, "mail.user").isEmpty()&&
													!AutoConfig.config(null, "mail.password").isEmpty()&&
													!AutoConfig.config(namespace, "mail.to."+msgtype).isEmpty()&&
													!AutoConfig.config(namespace, "mail.template."+msgtype).isEmpty()){
												Thread.sleep(10);
												String contentstr = Files.readAllLines(Paths.get(file.toString()), Charset.forName("UTF-8")).get(0);
												String[] content = contentstr.split(AutoConfig.SPLIT);
												if ("prize".equals(msgtype)) {
													String headimgurl = new ObjectMapper().readTree(new String(DefaultCipher.dec(AutoConfig.getencuserinfo(content))))
															.get("headimgurl").asText();
													String nick = new ObjectMapper().readTree(new String(DefaultCipher.dec(AutoConfig.getencuserinfo(content))))
															.get("nickname").asText();
												    String enc = AutoConfig.getenc(content);
												    	long timelong = AutoConfig.getlucktime(content);
												    	String time= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timelong));	
												    String addr = URLDecoder.decode(content[8],"UTF-8")+URLDecoder.decode(content[9],"UTF-8")+URLDecoder.decode(content[10],"UTF-8")+URLDecoder.decode(content[11],"UTF-8")+URLDecoder.decode(content[12],"UTF-8");
												    String sysreq = DefaultCipher.dec(AutoConfig.getsysrequire(content));
												    String userinputs= DefaultCipher.dec(content[38]).replaceAll(":ADDR:", "");
												    BigDecimal bd = new BigDecimal(Double.parseDouble(AutoConfig.config(namespace, "lsid.pool"+AutoConfig.getpoolid(content)+".prize"+AutoConfig.getprizeid(content)+".value"))/100);
												    String value = bd.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
												    
													try {
														email(namespace, msgtype, timelong, new Object[] { headimgurl, nick, enc, time, addr, sysreq, userinputs, value });
														Files.delete(Paths.get(file.toString()));
													}catch(Exception ex) {
														AutoConfig.log(ex,"error processing "+file);
													}
												} else {
													try {
														email(namespace, msgtype, System.currentTimeMillis(), content);
														Files.delete(Paths.get(file.toString()));
													}catch(Exception ex) {
														AutoConfig.log(ex,"error processing "+file);
													}
												}
											}
										}catch(Exception e){
											AutoConfig.log(e,"error processing "+file);
											Files.createDirectories(therrorfolder.resolve(namespace));
											Files.move(Paths.get(file.toString()), therrorfolder.resolve(namespace).resolve(filename), StandardCopyOption.REPLACE_EXISTING);
										}
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
					}finally{
						if (br!=null){
							try {
								br.close();
							} catch (IOException e) {
								AutoConfig.log(e, "error closing command: ls "+thefolder.resolve(namespace).toString()+" -rt | head -n 10000");
							}
						}
						br = null;
					}
				}
			}
		}
	}
	
	private static void email(String namespace, String msgtype, Long timelong, Object[] contentparams) throws Exception{
        Properties prop = new Properties();
        prop.setProperty("mail.transport.protocol", "smtp");
        prop.setProperty("mail.smtp.host", AutoConfig.config(null, "mail.smtp.host"));
        prop.setProperty("mail.smtp.port", AutoConfig.config(null, "mail.smtp.port"));
        prop.setProperty("mail.smtp.auth", "true");
        MailSSLSocketFactory sf = null;
        try {
            sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
        } catch (GeneralSecurityException e1) {
            e1.printStackTrace();
        }
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.ssl.socketFactory", sf);
        
        Session s = Session.getDefaultInstance(prop,new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication pa = null;
				try {
					pa = new PasswordAuthentication(AutoConfig.config(null, "mail.user"), 
							DefaultCipher.dec(AutoConfig.config(null, "mail.password")));
				} catch (Exception e) {
					AutoConfig.log(e,"error processing decrypting password ["+AutoConfig.config(null, "mail.password")+"]");
				}
                return pa;
            }
        });
        MimeMessage msg = new MimeMessage(s);
        msg.setFrom(new InternetAddress(AutoConfig.config(null, "mail.user"),AutoConfig.config(null, "mail.user")));
        msg.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(AutoConfig.config(namespace,"mail.to."+msgtype), false));
	    if (!AutoConfig.config(null, "mail.cc").isEmpty()) {
	    	msg.setRecipients(Message.RecipientType.CC,
				InternetAddress.parse(AutoConfig.config(null,"mail.cc"), false));
	    }
	    if (!AutoConfig.config(null, "mail.bcc").isEmpty()) {
	    	msg.setRecipients(Message.RecipientType.BCC,
					InternetAddress.parse(AutoConfig.config(null,"mail.bcc"), false));
	    }
	    msg.setSubject(URLDecoder.decode(AutoConfig.config(namespace,"mail.to."+msgtype+".subject"),"UTF-8"));
	    
	    StringBuilder sb = new StringBuilder(MessageFormat.format(
				URLDecoder.decode(AutoConfig.config(namespace, "mail.template."+msgtype),"UTF-8"),
				contentparams));
	    msg.setDataHandler(new DataHandler(
	    		new ByteArrayDataSource(sb.toString(), "text/html")));
	    
	    msg.setSentDate(new Date(timelong));
	    Transport.send(msg);
	}
	
	private static void emailqq(String namespace, String msgtype, Long timelong, Object[] contentparams) throws Exception{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.host", AutoConfig.config(null, "mail.smtp.host"));
	    props.put("mail.smtp.port", "587");
	    
	    Session session = Session.getInstance(props, new Authenticator() {
	    	protected PasswordAuthentication getPasswordAuthentication() {  
	            try {
					return new PasswordAuthentication(AutoConfig.config(null, "mail.user"), 
							DefaultCipher.dec(AutoConfig.config(null, "mail.password")));
				} catch (Exception e) {
					AutoConfig.log(e,"error processing decrypting password ["+AutoConfig.config(null, "mail.password")+"]");
				}  
	            return null;
	        }  
	    });
	    
	    Message msg = new MimeMessage(session);
	    msg.setFrom(new InternetAddress(AutoConfig.config(null, "mail.user")));
	    msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(AutoConfig.config(namespace,"mail.to."+msgtype), false));
	    if (!AutoConfig.config(null, "mail.cc").isEmpty()) {
    		msg.setRecipients(Message.RecipientType.CC,
				InternetAddress.parse(AutoConfig.config(null,"mail.cc"), false));
	    }
	    if (!AutoConfig.config(null, "mail.bcc").isEmpty()) {
	    		msg.setRecipients(Message.RecipientType.BCC,
					InternetAddress.parse(AutoConfig.config(null,"mail.bcc"), false));
	    }
	    msg.setSubject(URLDecoder.decode(AutoConfig.config(namespace,"mail.to."+msgtype+".subject"),"UTF-8"));
	    
	    StringBuilder sb = new StringBuilder(MessageFormat.format(
				URLDecoder.decode(AutoConfig.config(namespace, "mail.template."+msgtype),"UTF-8"),
				contentparams));
	    msg.setDataHandler(new DataHandler(
	    		new ByteArrayDataSource(sb.toString(), "text/html")));
	    
	    msg.setSentDate(new Date(timelong));
	    Transport.send(msg);
	}
	
	public static void main(String[] ss) throws Exception{
		System.out.println(DefaultCipher.enc("49f9577b914d1f4870c99b90f9a0a641"));
        Properties prop = new Properties();
        //协议
        prop.setProperty("mail.transport.protocol", "smtp");
        //服务器
        prop.setProperty("mail.smtp.host", "smtp.exmail.qq.com");
        //端口
        prop.setProperty("mail.smtp.port", "465");
        //使用smtp身份验证
        prop.setProperty("mail.smtp.auth", "true");
        //使用SSL，企业邮箱必需！
        //开启安全协议
        MailSSLSocketFactory sf = null;
        try {
            sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
        } catch (GeneralSecurityException e1) {
            e1.printStackTrace();
        }
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.ssl.socketFactory", sf);
        //
        //获取Session对象
        Session s = Session.getDefaultInstance(prop,new Authenticator() {
            //此访求返回用户和密码的对象
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication pa = new PasswordAuthentication("info@jedge.cn", "Admin!23");
                return pa;
            }
        });
        //设置session的调试模式，发布时取消
        s.setDebug(true);
        MimeMessage mimeMessage = new MimeMessage(s);
        try {
            mimeMessage.setFrom(new InternetAddress("info@jedge.cn","info@jedge.cn"));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress("443370165@qq.com"));
            //设置主题
            mimeMessage.setSubject("账户密码重置");
            mimeMessage.setSentDate(new Date());
            //设置内容
            mimeMessage.setText("您使用了密码重置功能");
            mimeMessage.saveChanges();
            //发送
            //Transport.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
	}
	
	public static void main1(String[] args) throws Exception {
		  // 创建Properties 类用于记录邮箱的一些属性
        final Properties props = new Properties();
        // 表示SMTP发送邮件，必须进行身份验证
        props.put("mail.smtp.auth", "true");
        //此处填写SMTP服务器
        props.put("mail.smtp.host", "smtp.qq.com");
        //端口号，QQ邮箱给出了两个端口，但是另一个我一直使用不了，所以就给出这一个587
        props.put("mail.smtp.port", "587");
        // 此处填写你的账号
        props.put("mail.user", "2265616264@qq.com");
        // 此处的密码就是前面说的16位STMP口令
        props.put("mail.password", "vjkqaqokdyipdjid");

        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress form = new InternetAddress(
                props.getProperty("mail.user"));
        message.setFrom(form);

        // 设置收件人的邮箱
        InternetAddress to = new InternetAddress("493158419@qq.com");
        message.setRecipient(RecipientType.TO, to);

        // 设置邮件标题
        message.setSubject("测试邮件");

        // 设置邮件的内容体
        message.setContent("这是一封测试邮件", "text/html;charset=UTF-8");

        // 最后当然就是发送邮件啦
        Transport.send(message);
	}

}
