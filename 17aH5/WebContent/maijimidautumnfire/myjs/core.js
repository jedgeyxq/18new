var dataurl = "http://ha0y.cn/share";

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
			　　		popshow("errordata");
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
		    		        'onMenuShareAppMessage',
		    		        'addCard'
		    		       ];
		wx.config(data.wxsign);
		wx.ready(function () {
			sessionStorage.setItem("wxjssdk","ready");
			afterinitwxjssdk();
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
	$("body").append("<div id='cover' class='pop' style='position:absolute;z-index:10005;background:#000;opacity:0.7;top:0px;left:0px;right:0px;bottom:0px;'></div>");
	
	$("body").append("<img id='errorconn' class='pop msg' src='images/errorconn.png'/>");
	$("body").append("<img id='errordata' class='pop msg' src='images/errordata.png'/>");
	
	init();
});
