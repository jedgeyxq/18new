var dataurl = "http://ha0y.cn";

function post(ticket, datajson, yescallback){
	if (ticket==null){
		datajson.ticket=sessionStorage.getItem("ticket");
	} else {
		datajson.ticket=ticket;
	}
	$(".popmsg").hide();
	var ajaxpost = $.ajax({
		url:dataurl,  
	　　	timeout : 20000, 
	　　	type : 'post',  
	　　	data :datajson, 
	　　	dataType:'json',
	　　	success:function(data){
			if (data.result=="success"){
				if (data.nickname){
					sessionStorage.setItem("nick",decodeURIComponent(data.nickname));
				}
				if (data.headimgurl){
					sessionStorage.setItem("head",data.headimgurl);
				}
				if (yescallback){
					sessionStorage.setItem("data-"+yescallback.name,JSON.stringify(data));
					yescallback(data);
				}
			} else {
				if (data.reason.indexOf("invalidticket")!=-1){
			　　		popshow("errorexpire");
				} else if (data.reason.indexOf("daylimit")!=-1){
			　　		popshow("errordaylimit");
				} else if (data.reason.indexOf("monthlimit")!=-1){
			　　		popshow("errormonthlimit");
				} else if (data.reason.indexOf("denied")!=-1){
					popshow("errordenied");
				} else {
					popshow("errordata");
				}
			}
	　　	},
	　　	complete : function(XMLHttpRequest,status){
	　　		if (status!="success"){
	　　			ajaxpost.abort();
		　　		popshow("errorconn");
	　　		}
	　　	}
	});
}

function share(url,title,img){
	if (sessionStorage.getItem("wxjssdk")!=null){
		var wxjson = {
				title:title,
				link:url,
				imgUrl:img,
				trigger: function (res) {
				},
				success: function (res) {
				},
				cancel: function (res) {
				},
				fail: function (res) {
				}
		};
		wx.onMenuShareAppMessage(wxjson);
		wx.onMenuShareTimeline(wxjson);
	}
}

function initwxjssdk(data){
	if (data.wxsign){
		data.wxsign.debug=false;
		data.wxsign.jsApiList=[
		                    'hideMenuItems',
		       		        'onMenuShareTimeline',
		    		        'onMenuShareAppMessage'
		    		       ];
		wx.config(data.wxsign);
		wx.ready(function () {
			sessionStorage.setItem("wxjssdk","ready");
			init(JSON.parse(sessionStorage.getItem("data-initwxjssdk")));
			wx.hideMenuItems({
		        menuList: [
		                   "menuItem:share:qq",
		                   "menuItem:share:QZone",
		                   "menuItem:share:weiboApp",
		                   "menuItem:share:facebook",
		                   "menuItem:share:QZone",
		                   "menuItem:jsDebug",
		                   "menuItem:editTag",
		                   "menuItem:delete",
		                   "menuItem:originPage",
		                   "menuItem:openWithQQBrowser",
		                   "menuItem:openWithSafari",
		                   "menuItem:share:email",
		                   "menuItem:share:brand"
		                  ]
		    });
		});	
	}
}

function imagesloaded(callback){
	var isloading = 0;
	$("img").each(function(){
		if (this.height==0||this.width==0){
			isloading = 1;
			return false;
		}
	});
	if (isloading==0){
		callback();
	} else {
		setTimeout(function(){
			imagesloaded(callback);
		},100);
	}
}

function popshow(id){
	pophide();
	if ($("#"+id).length>0){
		$("#cover").fadeIn(1000);
		$("#"+id).fadeIn(1000);
	}
}

function pophide(){
	$(".pop").hide();
}

$(document).ready(function(){
	if (window.location.href.split("?").length!=2||window.location.href.split("?")[1].length<32){
		alert("非法访问，请重新扫码！");
	} else {
		sessionStorage.setItem("ticket",window.location.href.split("?")[1].substring(0,32));
		
		$("body").append("<img id='logo' class='pop' style='position:absolute;z-index:10001;height:130px;top:50%;left:50%;transform: translateX(-50%) translateY(-200px);' src='images/logo.png'/>");
		$("body").append("<img id='loadingbg' style='position:absolute;top:0px;left:0px;width:100%;height:100%;' src='images/loadingbg.jpg'/>");
		$("body").append("<div id='cover' class='pop' style='position:absolute;z-index:10000;background:#000;opacity:0.8;top:0px;left:0px;right:0px;bottom:0px;'></div>");
		$("body").append("<img id='title' class='pop' style='position:absolute;z-index:10001;height:130px;top:50%;left:50%;transform: translateX(-50%) translateY(-50%);' src='images/title.png'/>");
		$("body").append("<div class='pop' style='position:absolute;width:90px;z-index:10001;height:90px;background-size:100% 100%;background-repeat:no-repeat;animation: loadingmascot 1s linear 0s infinite alternate;top:50%;left:50%;transform: translateX(-50%) translateY(80px);'></div>");
		$("body").append("<div class='pop' style='position:absolute;height:25px;z-index:10001;background-size:25px;background-repeat:repeat-x;background-image:url(images/loadingdot.png);animation: loadingdot 5s linear 0s forwards;top:50%;left:10%;transform: translateY(180px);'></div>");

		$("body").append("<img id='errorconn' class='pop msg' src='images/errorconn.png'/>");
		$("body").append("<img id='errordata' class='pop msg' src='images/errordata.png'/>");
		$("body").append("<img id='errordaylimit' class='pop msg' src='images/errordaylimit.png'/>");
		$("body").append("<img id='errordenied' class='pop msg' src='images/errordenied.png'/>");
		$("body").append("<img id='errorexpire' class='pop msg' src='images/errorexpire.png'/>");
		$("body").append("<img id='errormonthlimit' class='pop msg' src='images/errormonthlimit.png'/>");
		setTimeout(function(){
			imagesloaded(function(){
				//post(null, {url4wxjssdk:window.location.href}, initwxjssdk);
				//post(null, {}, init);
				var data = {"result":"success","7tasksresult":"none","activedate":"2018-03-13 18:18:46","nickname":"Richard","prodid":"testp","lng":"121.48789948999993","tasks":{"task0":1,"task1":1,"task2":1,"task3":1,"task4":1,"task6":1,"task5":1},"pools":[{"id":"0","ticket":"d1faacdd940347e88e302907ddc4f216","require":"","type":"task"}],"eid":"t2","city":"%E6%9C%9D%E9%98%B3","playid":"YnHz8F7D92gmcDluJ%2Bofn8tXn9%2BXkKX8hEVD%2B6cOE9o%3D","countenc":0,"prizes":[],"credit":0,"headimgurl":"http://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83eqoyRqfH5icpBXe6cxyHV8nGicNrkqFV45Qa2E9vxYqZzx5leYxUjZnlzNAz1Gy6L5D9o8xeE6EZvdQ/132","lat":"31.249161578948787","7tasks":"213239bea1814323824f620abc7ca720"};
				data.pools=[];
				data.tasks={"task0":0,"task1":0,"task2":0,"task3":0,"task4":0,"task6":0,"task5":0};
				sessionStorage.setItem("data-init",JSON.stringify(data));
				init(data);
			});
			
		},3000);
		
	}
});