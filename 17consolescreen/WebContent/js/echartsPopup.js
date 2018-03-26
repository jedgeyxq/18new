var myChartpopup1 = echarts.init(document.getElementById('trend1'));
var myChartpopup2 = echarts.init(document.getElementById('situation1'));
var myChartpopup3 = echarts.init(document.getElementById('ranking1'));

var iPop = 0;
$(function () {
    var _width = 0;
    var _height = 0;
    // if(_width > _height || _width >= 768){
        $('#trend').click(function () {
            _width = window.screen.width;
            _height = window.screen.height;
            // myChartpopup1.resize();
            if(_width > _height || _width >= 768){
                $('#echart-mask li').removeClass('active');
                $('.echart-popup').css("display", "none");
                $('#echart-mask').css("display", "block");
                $('#echart-popup1').css("display", "block");
                $('#echart-popup1 em').removeClass('active');
            }
            setTimeout(function () {
                OptionFun1();
            }, 310);
        });
        for(var i=1; i<4; i++){
            $('#close'+ i).click(function () {
                $('#echart-mask').css("display", "none");
            });
        }

        $('#situation').click(function () {
            _width = window.screen.width;
            _height = window.screen.height;
            // myChartpopup2.resize();
            if(_width > _height || _width >= 768) {
                $('#echart-mask li').removeClass('active');
                $('.echart-popup').css("display", "none");
                $('#echart-mask').css("display", "block");
                $('#echart-popup2').css("display", "block");
                $('#echart-popup2 em').removeClass('active');
            }
            setTimeout(function () {
                OptionFun2();
            },310);
        });
        $('#ranking').click(function () {
            _width = window.screen.width;
            _height = window.screen.height;
            // myChartpopup3.resize();
            if(_width > _height || _width >= 768) {
                $('#echart-mask li').removeClass('active');
                $('.echart-popup').css("display", "none");
                $('#echart-mask').css("display", "block");
                $('#echart-popup3').css("display", "block");
                $('#echart-popup3 em').removeClass('active');
            }
            setTimeout(function () {
                OptionFun3();
            },310);
        });
    // }
});
function OptionFun1() {
//     //设置日期，当前日期的前七天
    var myDate = new Date(); //获取今天日期
    myDate.setDate(myDate.getDate() - 7);
    var  time1=myDate.getFullYear()+"-"+(myDate.getMonth()+1)+"-"+myDate.getDate();//time1表示当前时间
    // console.log(time1);
    post("/sc", {"begin": time1, "from": "wx", "pc":"p", "province": "all", "city": "all","end":"","gender":"all","activity":"all","product":"all"}, function(res) {
        //  console.log(res);
        // 填入数据
        var len = res.data.length;
        var arrayObj = new Array();
        for (var i = 0; i < len; i++) {
            var x = 0;
            for (var j = 0; j < res.data[i].length; j++) {
                //  console.log(res.data[i][j]);
                x += res.data[i][j];
            }
            //   console.log(x);
            arrayObj.push(x);
        }
        //  console.log(arrayObj);
        option1.xAxis[0].axisLabel.interval = 0;
        option1.xAxis[0].data = res.timelines;
        option1.series[0].data = arrayObj;

        // 使用刚指定的配置项和数据显示图表。
            myChartpopup1.setOption(option1);
    });

}
function OptionFun2() {
    //设置日期，当前日期的前七天
    var myDate = new Date(); //获取今天日期
    myDate.setDate(myDate.getDate() - 7);
    var time1 = myDate.getFullYear() + "-" + (myDate.getMonth() + 1) + "-" + myDate.getDate();//time1表示当前时间
    // console.log(time1);
    post("/tr", {
        "begin": time1,
        "from": "wx",
        "pc": "p",
        "province": "all",
        "city": "all",
        "end": "",
        "gender": "all",
        "activity": "all",
        "product": "all"
    }, function (res) {
        // console.log(res);
        var arrayObj = new Array();
        var len =res.data.legend.length;
        for(var i=0;i<len;i++){
            var name =res.data.legend[i];
            // console.log(name);
            if(name!=""){
                arrayObj.push(name);
            }
        }
        option2.xAxis.data = [];
        option2.xAxis.data = res.data.xaxis;
        option2.legend.data = arrayObj;
        option2.series=[];
        for (var i = 0; i < arrayObj.length; i++) {
            option2.series.push({
                "name": arrayObj[i],
                "smooth": false,
                "type": "line",
                "stack": "总量",
                "data": res.data[arrayObj[i]]
            });
        }

        // 使用刚指定的配置项和数据显示图表。
        myChartpopup2.setOption(option2,true);
    });

}
function OptionFun3() {
    var myDate = new Date(); //获取今天日期
    myDate.setDate(myDate.getDate() - 7);
    var  time1=myDate.getFullYear()+"-"+(myDate.getMonth()+1)+"-"+myDate.getDate();//time1表示当前时间
    // console.log(time1);
    post("/c", {"begin": time1, "from": "wx", "pc":"p", "province": "all", "city": "all","end":"","gender":"all","activity":"all","product":"all"}, function(res){
        // console.log(res);
        var len=res.data.length;
        var arrayObj = new Array();
        for(var i=0;i<len;i++){
            var x=0;
            for(var j=0;j<res.data[i].length;j++){
                //  console.log(res.data[i][j]);
                x+=res.data[i][j];
            }
            //   console.log(x);
            arrayObj.push(x);
        }
        var array2 =  new Array();
        var arrValues = new Array();
        var len=res.data.length;
        if(len>10){
            len=10;
        }
        for(var n = 0; n<len; n++){
            var m = '';
            var x = '';
            m += res.data[n].name;
            x += res.data[n].value;
            array2.push(m);
            arrValues.push(x);
        }
        option3.xAxis.data = array2;
        option3.series[0].data = arrValues;
        // 使用刚指定的配置项和数据显示图表。
        myChartpopup3.setOption(option3);
    });
}