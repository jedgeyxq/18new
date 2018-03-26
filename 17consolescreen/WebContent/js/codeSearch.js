$(function () {
    var options = {
        url: "",
        clearForm: true,
        dataType: "html",
        success: function (result) {
            try {
                var res = eval("(" + result + ")");
                if (res.reason == "expire") {
                    alert("登录过期，请重新登录");
                    sessionStorage.removeItem("t0ken");
                    $(window.location).attr('href', "index.html");
                } else {
                    if (res.result == "success") {
                        sessionStorage.setItem("t0ken", aesdecrypt(res.t0ken));
                        renderesult(res);
                    } else {
                        sessionStorage.setItem("t0ken", aesdecrypt(res.t0ken));
                        if(res.reason.indexOf("toofrequent") != -1){
                            alert("系统正在处理您的上一次操作，请稍候继续。");
                        }else{
                            alert("系统错误：" + res.reason);
                        }
                    }
                }
            } catch (e) {
               console.log(e);
                alert("系统繁忙，请稍后再试");
            }
        }
    };
    $("#theForm").ajaxForm(options);

    $('#erCode-search').click(function () {
        var codeInput = $('#erCode-input').val();

        if(codeInput!=""){
            post('/rd', {"enc": aesencrypt(codeInput)}, function (res) {
               console.log(res);
                renderesult(res);
            });
        } else{
            var file =$("#file-input").val();
            console.log(file);
            if(file) {
                console.log("file exist!");
                options.url = interfacehost + "/rd?t0ken=" + sessionStorage.getItem("t0ken");
                $("#theForm").ajaxForm(options);
                $("#thesubmit").click();

            }
        }
    });

    $("#close").click(function () {
        $("#mask").css("display","none");
    });
})
function renderesult(res){
    $("#uldiv").children().remove();
    if(res.coderepositorydata != undefined && res.coderepositorydata.active == "true"){
        $('#no').removeClass('active');
        $('#yes').addClass('active');
    }else{
        $('#yes').removeClass('active');
        $('#no').addClass('active');
    }
    if (res.codeprizedata!=undefined&&res.codeprizedata.length>0) {
        for (var i = 0; i < res.codeprizedata.length; i++) {
            var stat;
            var status=decodeURIComponent(res.codeprizedata[i].status);
            if(status.indexOf("success")==0){
                stat="已到账";
            }else if(status.indexOf("fail")==0){
                if(status.indexOf("NOTENOUGH")>0){
                    stat="余额不足";
                }else if(status.indexOf("SENDNUM_LIMIT")>0){
                    stat="到达当日上限";
                }else if(status.indexOf("V2_ACCOUNT_SIMPLE_BAN")>0){
                    stat="用户未实名";
                }else if(status.indexOf("FREQ_LIMIT")>0){
                    stat="到账中";
                }else{
                    stat="联系客服";
                }
            }else{
                stat=status;
            }
            var sysreq;
            if(res.codeprizedata[i].sysreq != "" && res.codeprizedata[i].prizeid.indexOf("666") != -1){
                sysreq = '<em>' + res.codeprizedata[i].sysreq + ' </em>';
            }else{
                sysreq = '';
            }

            $("#mask").css("display", "block");
            $("#uldiv").append("<ul><li><span><img src='" + res.codeprizedata[i].headimgurl + "'></span></li><li>"+decodeURIComponent(res.codeprizedata[i].nick)+"</li><li style='font-family: Gotham-MediumRegular;'>"+new Date(parseInt(res.codeprizedata[i].time)).toLocaleString().replace(/年|月/g, "-").replace(/日/g, " ").replace(/上午/g," ").replace(/下午/g," ")+"</li><li style='font-family: Gotham-MediumRegular;'>"+res.codeprizedata[i].ip+"</li><li>"+decodeURIComponent(res.codeprizedata[i].addr)+"</li><li>"+res.codeprizedata[i].prizeid+"<br>"+ sysreq+"</li><li>"+stat+"</li></ul>");
            $('li em').parent().css({'padding-top': '10px', 'line-height': 'normal'});
        }
    }
}
