var dataurl = "http://ha0y.cn/text";

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
			yescallback(data);
		},
	　　	complete : function(XMLHttpRequest,status){
	　　		if (status!="success"){
	　　			ajaxpost.abort();
		　　		alert("网络异常["+status+"]");
	　　		}
	　　	}
	});
}

$(document).ready(function(){
	init();
});
