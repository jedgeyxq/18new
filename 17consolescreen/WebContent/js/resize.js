
    // var phoneW =  parseInt(window.screen.width),phoneScale = phoneW/640,ua = navigator.userAgent;
    // if (/Android (\d+\.\d+)/.test(ua)){
    //     var version = parseFloat(RegExp.$1);
    //     if(version>2.3){
    //         document.write('<meta name="viewport" content="width=640, initial-scale='+phoneScale+', minimum-scale = '+phoneScale+', maximum-scale = '+phoneScale+', target-densitydpi=device-dpi">');
    //     }else{
    //         document.write('<meta name="viewport" content="width=640, target-densitydpi=device-dpi">');
    //     }
    // } else {document.write(
    //     '<meta name="viewport" content="width=640, user-scalable=no, target-densitydpi=device-dpi">');
    // }


    $(function () {
        resizeFun();
        $(window).bind("resize", resizeFun);

    });
    var resizeFun = function() {
        // var metadom1 = document.getElementById('noscale');
        // if(metadom1){
        //     $('head').children('#noscale').remove();
        // }
        var _width = window.screen.width;
        if(_width <= 414){
            // var metaDom = document.createElement("meta");
            // metaDom.name = "viewport";
            // metaDom.content = "user-scalable = no";
            // metaDom.id = "noscale";
            // $('head').prepend(metaDom);
            $('.echarts-left').width('100%');
            $('.trend').height("470px");
            $('.trend-box,.situation-box,.ranking-box').height("515px");
            $('#PCData').css("display", "none");
            $('#PCsidebar').css("display", "none");
            $('#MobileData').css("display", "block");
            $('.data-main, .data-right').css("width", "100%");
            $('.echarts-left p,.data-right .map-box p').css({
                "font-size": "34px",
                "line-height": "95px",
                "height": "95px"
            });
            $('.echarts-left .trend-box, .echarts-left .situation-box, .echarts-left .ranking-box').css("margin-bottom", "50px");
            $('.header .right span, .header .right a').css("font-size", "32px");
            $('.header .center').css({
                "width": "40%",
                "font-size": "42px"
            });
            $('.header .right').css({
                "width": "38%"
            });
            $('.header').css({
                "height": "140px",
                "padding": "25px 55px 0px 55px"
            });
            $('.header .left img').css({
                "width": "100%"
            });
        }else{

            $('#MobileData').css("display", "none");
            $('.echarts-left').width('40%');
            $('.trend').height("270px");
            $('.trend-box,.situation-box,.ranking-box').height("315px");
            $('#PCData').css("display", "block");
            $('#PCsidebar').css("display", "block");
            $('.data-main').css("width", "79%");
            $('.data-right').css("width", "59%");
            $('.echarts-left p').css({
                "font-size": "18px",
                "line-height": "45px",
                "height": "45px"
            });
            $('.data-right .map-box p').css({
                "font-size": "18px",
                "height": "55px",
                "line-height": "55px"
            });
            $('.echarts-left .trend-box, .echarts-left .situation-box, .echarts-left .ranking-box').css("margin-bottom", "20px");
            $('.header .right span, .header .right a').css("font-size", "16px");
            $('.header .center').css({
                "width": "50%",
                "font-size": "22px"
            });
            $('.header .right').css({
                "width": "28%"
            });
            $('.header').css({
                "height": "74px",
                "padding": "0 55px"
            });
            $('.header .left img').css({
                "width": "204px"
            });
            $('.num-box').height($('.today-number').height());
            // var metadom = document.getElementById('noscale');
            // if(metadom){
            //     $('head').children('#noscale').remove();
            // }
        }
    }

// 判断手机横竖屏状态：
// window.addEventListener("onorientationchange" in window ? "orientationchange" : "resize", function() {
//     if (window.orientation === 180 || window.orientation === 0) {
//         alert('竖屏状态！');
//         // window.location.reload();
//         // $('.echarts-left').width('100%');
//         // $('.echarts-left').
//     }
//     if (window.orientation === 90 || window.orientation === -90 ){
//         alert('横屏状态！');
//         window.location.reload();
//     }
// }, false);


