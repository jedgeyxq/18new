
var api = "http://106.75.71.181/17console/website/info";
$(document).ready(function () {
    $(window).resize(function () {
        var _width = $(window).width();
        console.log(_width);
        if(_width <= 1000){
            console.log("768");
            $('#Macbook').css('width', '1176px');
            $('.lsfangwei').css('overflow','hidden');
        }else {
            $('#Macbook').css('width', '1369px');
            // $('.lsfangwei').css('overflow','auto');
        }
    });

    $(window).scroll(function (event) {
        event.stopPropagation();
        if($(window).width() > $(window).height() && $(window).width() > 768){
            var iHeight = $('.lsfangwei').offset().top;
            var iRight = $('#phone-hand').css('right');
            if($(document).scrollTop() > iHeight - 600 && $(document).scrollTop() < iHeight + 600 ){
                // console.log(iHeight);
                if( iRight == "-700px"){
                    $('#phone-hand').animate({
                        right: "-200px"
                    }, 1800);
                }
            }else if( $(document).scrollTop() < $('#product').offset().top - 100 ){
                if(iRight == "-200px"){
                    $('#phone-hand').animate({
                        right: "-700px"
                    }, 1000);
                }
            }
        }
    });

    // 导航渐变
    $(window).scroll(function () {
        if($(window).scrollTop() > 0){
            $('#navbar').addClass('bgColor');
            $('#goToTop').show();
        }else{
            $('#navbar').removeClass('bgColor');
            $('#goToTop').hide();
        }
    });

    // 导航跳转
    $('#nav ul li').click(function() {
        $('#nav li').removeClass('on');
        $(this).addClass('on');
        var name = $(this).attr('name');
        var offsetTop = $('#' + name).offset().top - 80;
        // console.log(offsetTop);
        $('html,body').animate({scrollTop: offsetTop}, 'swing');
//            return false;
    });

    // go To Top
    $('#goToTop').click(function () {
        $('html,body').animate({
            scrollTop: 0
        });
    });

    $('#jzphone').click(function () {
        $(this).children('span').slideToggle('slow');
    });
    
    $('#close').click(function () {
        console.log('aaa');
        $('.mask').fadeOut();
    });

    //标记为喜欢
    $('.like').click(function (event) {
        event.stopPropagation();
        var id = $(this).attr('data-id');
        var likes = localStorage.getItem("likes"+id);
        // console.log(likes);

        if(likes=="true"||likes == null){
            $(this).children('i').addClass('active');
            $(this).children('em').addClass('active');
            $(this).unbind('click');
            localStorage.setItem("likes"+id,false);
            var self = $(this);
            $.ajax({
                url: api,
                type: "post",
                datatype: "json",
                data:{type: "dianzan", id:id },
                success: function (res) {
                    var res = eval( '(' +res +')');
                    if(res.result == "success"){
                        var array = new Array();
                        var str = res.dianzan[id];
                        array = str.split(',');
                        self.children('em').html(array[array.length-1]);
                    }
                }
            });
        }else if(likes=="false"){
            console.log("else");
            $(this).unbind('click');
        }
    });
    //新闻详情
    $('.lsinformation .pic').click(function (event) {
        event.stopPropagation();
        var id = $(this).attr('data-id');
        $('.mask').fadeIn();
        ajaxFun(id);
    });

    loadData();
});
function loadData() {
    $.ajax({
        url: api,
        type: "post",
        datatype: "json",
        data: {type: "duzan"},
        success: function (res) {
            res = eval( '(' +res +')');
            if(res.result == "success"){
                $('.like').each( function () {
                    var id=$(this).attr("data-id");
                    var likeorno = localStorage.getItem("likes"+id);
                    if(likeorno == "true" || likeorno == null){
                        $(this).children('i').removeClass('active');
                        $(this).children('em').removeClass('active');
                    }else if(likeorno == "false"){
                        $(this).children('i').addClass('active');
                        $(this).children('em').addClass('active');
                    }
                    for(var i=0; i<res.dianzan.length; i++){
                        if(id==res.dianzan[i].split(',')[0]){
                            $(this).children('em').html(res.dianzan[i].split(',')[1]);
                        }
                    }
                });
            }
        }
    });
}
var previd,nextid;
function ajaxFun(id) {
    $.ajax({
        url: api,
        type: "post",
        datatype: "json",
        data: { type: "news", newsid: id},
        success: function (res) {
            res = eval( '(' +res +')');
            // console.log(res);
            if(res.result == "success"){
                var url = "http://res.leasiondata.cn/lstatic/website/news/";

                $('.mask .title').html(res.title);
                $('.mask .date').children().eq(0).html(res.createdate + " 介知智能营销科技");
                $('.mask .date').children().eq(1).html("浏览次数："+res.cnt);
                $('.mask .jznewpic').children().attr('src', url+res.pic[3]);

                var n=0;
                $('.mask .newspicsm span').each(function () {
                    if(n<res.pic.length-1){
                        // console.log($(this).children().find('img'));
                        $(this).children().attr('src', url+res.pic[n]);
                    }
                    n++;
                });
                var i=0;
                $('.jznewscontent').each(function () {
                    if(i<res.wenben.length){
                        $(this).html(res.wenben[i]);
                    }
                    i++;
                });
                // next prev
                if(res.pretitle == ""){
                    $('#prevnews').html("");
                }else{
                    $('#prevnews').html("上一篇："+res.pretitle);
                }
                if(res.nexttitle == ""){
                    $('#nextnews').html("");
                }else {
                    $('#nextnews').html("下一篇："+res.nexttitle);
                }
                previd = res.preid;
                nextid = res.nextid;

            }
        }
    });
}
$('#prevnews').click(function () {
    ajaxFun(previd);
});
$('#nextnews').click(function () {
    ajaxFun(nextid);
});

// var shareTitle,sharepic;

//share to sina
$('#sharetosina').click(function () {
    var shareTitle = $('.mask .title').html();
    var sharepic = $('.mask .jznewpic').children().attr("src");
    // console.log(newspic);
    sharetosina("介知智能营销科技 | " + shareTitle,"http://res.leasiondata.cn",sharepic);
});

// 分享到新浪微博
function sharetosina(title,url,picurl)
{
    var sharesinastring='http://v.t.sina.com.cn/share/share.php?title='+title+'&url='+url+'&content=utf-8&sourceUrl='+url+'&pic='+picurl;
    window.open(sharesinastring,'newwindow','height=400,width=400,top=100,left=100');
}
