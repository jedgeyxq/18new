var interfacehost = "http://106.75.71.181/17console";
function post(context, postdata, callback){
	if (sessionStorage.getItem("t0ken"+context)!=null){
		postdata.t0ken=sessionStorage.getItem("t0ken"+context);
	} else {
		if (sessionStorage.getItem("t0ken")!=null){
			postdata.t0ken=sessionStorage.getItem("t0ken");
		}
	}
	if (context=="/l0gin"){
		var crypt = new JSEncrypt();
		crypt.setKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCJfwNgpo3aT+o6MW3UNqXvZ/a1sPOOGJ/DuubU"+
				"fjJ3QCf+b3kpfPVQV1qmdGuRYkLfJ0wOMbriiAIvzr8njDTBUhK7SYZ2iZxicBPMu85t9oDwIhIU"+
				"Wv4X09KiOaz05/2ZY7fX5j8BsmzEEh/Cka1Z5DfHYpOj+9I+NVMzk5EIxQIDAQAB");
		sessionStorage.setItem("k3y",Math.random()*100000000000000000);
		postdata.passw0rd = encodeURIComponent(crypt.encrypt(sessionStorage.getItem("k3y")+"#"+postdata.passw0rd));
	}
	$.post(interfacehost+context,
		postdata,
		function(result){
			try{
				var res = eval("("+result+")");
				if (res.reason=="expire"){
					alert("登录过期，请重新登录");
					sessionStorage.removeItem("t0ken");
					$(window.location).attr('href', "index.html");
				} else if (res.reason&&res.reason.indexOf("toofrequent")!=-1){
					alert("系统正在处理您的上一次操作，请稍候再继续！");
				} else {
					if (res.result=="success"||(res.reason&&res.reason.indexOf("toofrequent")==-1)){
						if (res.result=="success"&&context=="/l0gin"){
							sessionStorage.setItem("eid", res.eid);
							sessionStorage.setItem("t0ken/sp", aesdecrypt(res.t0ken0));
							sessionStorage.setItem("t0ken/rt", aesdecrypt(res.t0ken1));
							sessionStorage.setItem("t0ken", aesdecrypt(res.t0ken2));
						} else {
							if (res.t0ken){
								if (context=="/sp"||context=="/rt"){
									sessionStorage.setItem("t0ken"+context, aesdecrypt(res.t0ken));
								} else {
									sessionStorage.setItem("t0ken", aesdecrypt(res.t0ken));
								}
							}
						}
					}
					if (res.result=="success"){
						callback(res);
					} else {
						if (context=="/l0gin"){
							callback(res);
						} else {
							alert("系统错误："+res.reason);
						}
					}
				}	
			}catch(e){
				alert("系统繁忙，请稍后再试");
			}
		}
	);	
}

function aesencrypt(raw){
	var iterationCount = 1000;
	var keySize = 128;
	var encryptionKey  = sessionStorage.getItem("k3y");
	var iv = "dc0da04af8fee58593442bf834b30739"
	var salt = "dc0da04af8fee58593442bf834b30739"
	var aesUtil = new AesUtil(keySize, iterationCount);
	return aesUtil.encrypt(salt, iv, encryptionKey, raw);
}

function aesdecrypt(enc){
	var iterationCount = 1000;
	var keySize = 128;
	var encryptionKey  = sessionStorage.getItem("k3y");
	var iv = "dc0da04af8fee58593442bf834b30739"
	var salt = "dc0da04af8fee58593442bf834b30739"
	var aesUtil = new AesUtil(keySize, iterationCount);
	return aesUtil.decrypt(salt, iv, encryptionKey, enc);
}
