
var optionPop1 = {
    backgroundColor: '#1f2840',
    // title: {
    //     padding: 10,
    //     text: '     7日扫码趋势',
    //     textStyle: {
    //         color: '#ffffff',
    //         fontWeight: 'normal'
    //     }
    // },
    tooltip : {
        trigger: 'axis'
        // axisPointer: {
        //     type: 'cross',
        //     label: {
        //         backgroundColor: '#7e8392'
        //     }
        // }
    },
    // legend: {
    //     data:['邮件营销','联盟广告','视频广告','直接访问','搜索引擎']
    // },
    // toolbox: {
    //     feature: {
    //         saveAsImage: {}
    //     }
    // },
    grid: {
        left: '4%',
        right: '6%',
        bottom: '8%',
        containLabel: true
    },
    xAxis : [
        {
            type : 'category',
            boundaryGap : false,
            data:[],
            // data : ['周一','周二','周三','周四','周五','周六','周日'],
            axisLabel: {
                show: true,
                interval:0,
                rotate: 0,
                textStyle: {
                    color: '#7e8392'
                }
            },
            splitLine:{
                lineStyle: {
                    width: 1,
                    type: 'dashed',  //虚线
                    color: 'rgba(255,255,255,.2)'
                }
            }
        }
    ],
    yAxis : [
        {
            type : 'value',
            axisLabel: {
                formatter: '{value}',
                textStyle: {
                    color: '#7e8392'
                }
            },
            splitLine:{
                lineStyle: {
                    width: 1,
                    type: 'dashed',  //虚线
                    color: 'rgba(255,255,255,.2)'
                }
            }
        }
    ],
    series : [
        {
            name: '扫码量',
            type: 'line',
            smooth: true,
            stack: '总量',
            label: {
                normal: {
                    show: true,
                    position: 'top'
                }
            },
            areaStyle: {
                normal: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {
                            offset: 0,
                            color: 'rgba(97, 41, 164, 0.95)'
                        },{
                            offset: 1,
                            color: 'rgba(82, 81, 212, 0.95)'
                        }
                    ])
                }
            },
            data:[],
            //data:[820, 932, 901, 934, 1290, 1330, 1320],
            itemStyle : {
                normal : {
                    color:'#683ac6',   //折点颜色
                    lineStyle:{
                        color:'#c182ff'
                    }
                }
            }
        }
    ]
};
var optionPop2 = {
    backgroundColor: '#1f2840',
    // title: {
    //     top: 10,
    //     text: '     7日中奖情况',
    //     textStyle: {
    //         color: '#ffffff',
    //         fontWeight: 'normal'
    //     }
    // },
    tooltip: {
        trigger: 'axis'
    },
    legend: {
        top: 5,
        left: 25,
        right: 25,
        data: [],
        // data:['1元','2元','99元','滴滴','OfO'],
        textStyle: {
            color: '#7e8392'
        }
    },
    grid: {
        left: '4%',
        right: '6%',
        bottom: '6%',
        containLabel: true
    },
    // toolbox: {
    //     feature: {
    //         saveAsImage: {}
    //     }
    // },
    xAxis: {
        type: 'category',
        boundaryGap: false,
        data:[],
        // data: ['周一','周二','周三','周四','周五','周六','周日'],
        axisLabel: {
            show: true,
            interval:0,
            rotate: 0,
            textStyle: {
                color: '#7e8392'
            }
        },
        splitLine:{
            lineStyle: {
                width: 1,
                type: 'dashed',  //虚线
                color: 'rgba(255,255,255,.2)'
            }
        }
    },
    yAxis: {
        type: 'value',
        axisLabel: {
            formatter: '{value}',
            textStyle: {
                color: '#7e8392'
            }
        },
        axisLine: {
            lineStyle: {
                type: 'solid',
                color: 'rgba(255,255,255,.2)',//左边线的颜色
                width:'1'//坐标线的宽度
            }
        },
        splitLine:{
            lineStyle: {
                width: 1,
                type: 'dashed',  //虚线
                color: 'rgba(255,255,255,.2)'
            }
        }
    },
    series: [
        // {
        //     name: [],
        //     smooth: false,
        //     type: "line",
        //     stack: "总量",
        //     data: []
        // }
    ]
};
var optionPop3 = {
    backgroundColor: '#1f2840',
    // title: {
    //     top: 10,
    //     text: '     7日活动参与区域排名',
    //     textStyle: {
    //         color: '#ffffff',
    //         fontWeight: 'normal'
    //     }
    // },
    tooltip: {},
    legend: {},
    grid: {
        left: '4%',
        right: '6%',
        bottom: '6%',
        containLabel: true
    },
    xAxis: {
        type: 'category',
        data:[],
        // data: ["厦门","福州","莆田","温州","惠州","武汉","成都","北海","上海","无锡"],
        axisLabel: {
            show: true,
            textStyle: {
                color: '#7e8392'
            }
        },
        splitLine:{
            show: false
        }
    },
    yAxis: {
        type: 'value',
        data: [],
        // data: ["周一","周二","周三","周四","周五","周六","周日"],
        axisLabel: {
            formatter: '{value}',
            textStyle: {
                color: '#7e8392'
            }
        },
        axisLine: {
            lineStyle: {
                type: 'solid',
                color: 'rgba(255,255,255,.2)',//左边线的颜色
                width:'1'//坐标线的宽度
            }
        },
        splitLine:{
            lineStyle: {
                width: 1,
                type: 'dashed',  //虚线
                color: 'rgba(255,255,255,.2)'
            }
        }
    },
    series: [
        {
            type: 'bar',
            barMaxWidth: 25,
            data: [],
            // data: [5, 20, 36, 10, 10, 20, 14, 30, 40, 17],
            itemStyle : {
                normal : {
                    //每个柱子的颜色即为colorList数组里的每一项，如果柱子数目多于colorList的长度，则柱子颜色循环使用该数组
                    color: function (params){
                        var colorList = ['#434bfb', '#7c4afb', '#cb4b93', '#0669ff', '#bf41f4', '#434bfb', '#7c4afb', '#cb4b93', '#0669ff', '#7c4afb'];
                        return colorList[params.dataIndex];
                    }
                }
            }
        }
    ]
};
$(function (){
    // myChartpopup1.resize();
    // myChartpopup2.resize();
    // myChartpopup3.resize();
    var data = {"begin": "", "from": "wx", "pc": "p", "province": "all", "city": "all", "end": "", "gender": "all", "activity": "all", "product": "all"};

    var conditional = 0;
    var iDay = 7;

    $('#women2').click(function () {
        var myDate = new Date();   //now
        $('#man2').removeClass('active');
        $('#genderAll2').removeClass('active');
        $(this).addClass('active');
        data.gender = 2;
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun2();
        },310);
    });
    $('#man2').click(function () {
        var myDate = new Date();   //now
        $('#women2').removeClass('active');
        $('#genderAll2').removeClass('active');
        $(this).addClass('active');
        data.gender = 1;
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun2();
        },310);
    });
    $('#women3').click(function () {
        var myDate = new Date();   //now
        $('#man3').removeClass('active');
        $('#genderAll3').removeClass('active');
        $(this).addClass('active');
        data.gender = 2;
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun3();
        },310);
    });
    $('#man3').click(function () {
        var myDate = new Date();   //now
        $('#women3').removeClass('active');
        $('#genderAll3').removeClass('active');
        $(this).addClass('active');
        data.gender = 1;
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun3();
        },310);
    });
    $('#genderAll2').click(function () {
        $(this).addClass('active');
        $('#man2').removeClass('active');
        $('#women2').removeClass('active');
        var myDate = new Date();   //now
        $(this).addClass('active');
        data.gender = "all";
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun2();
        },310);
    });
    $('#genderAll3').click(function () {
        $(this).addClass('active');
        $('#man3').removeClass('active');
        $('#women3').removeClass('active');
        var myDate = new Date();   //now
        $(this).addClass('active');
        data.gender = "all";
        myDate.setDate(myDate.getDate() - iDay);
        time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
        data.begin = time;
        setTimeout(function () {
            postFun3();
        },310);
    });
   $('#echart-mask li').click(function () {
       $('#echart-mask li').removeClass('active');
       $(this).addClass('active');
       var index = $(this).index();
       var myDate = new Date();   //now
       // console.log(myDate.getDate());  当天
       var time;

       if(index == 0){
           iDay = 15;
           myDate.setDate(myDate.getDate() - iDay);
           time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
           data.begin = time;
           optionPop1.xAxis[0].axisLabel.interval = 1;
           optionPop1.xAxis[0].axisLabel.rotate = 0;
           optionPop2.xAxis.axisLabel.interval = 1;
           optionPop2.xAxis.axisLabel.rotate = 0;
       }else if(index == 1){
           iDay = 30;
           myDate.setDate(myDate.getDate() - iDay);
           time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();    //获取当前日期
           data.begin = time;
           optionPop1.xAxis[0].axisLabel.interval = 1;
           optionPop1.xAxis[0].axisLabel.rotate = 40;
           optionPop2.xAxis.axisLabel.interval = 1;
           optionPop2.xAxis.axisLabel.rotate = 40;
       }else if(index == 2){
           iDay = 90;
           myDate.setDate(myDate.getDate() - iDay);
           time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();
           data.begin = time;
           optionPop1.xAxis[0].axisLabel.interval = 5;
           optionPop1.xAxis[0].axisLabel.rotate = 40;
           optionPop2.xAxis.axisLabel.interval = 5;
           optionPop2.xAxis.axisLabel.rotate = 40;
       }else{
           iDay = 365;
           myDate.setDate(myDate.getDate() - iDay);
           time = myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();
           data.begin = time;
           optionPop1.xAxis[0].axisLabel.interval = 10;
           optionPop1.xAxis[0].axisLabel.rotate = 40;
           optionPop2.xAxis.axisLabel.interval = 10;
           optionPop2.xAxis.axisLabel.rotate = 40;
       }
       if($(this).parent().attr("id") == "queryTrend"){
           setTimeout(function () {
               postFun1();
           },310);
       }else if($(this).parent().attr("id") == "queryLuck"){
           setTimeout(function () {
               postFun2();
           },310);
       }else {
           setTimeout(function () {
               postFun3();
           },310);
       }

   });
   function postFun1(){
       post("/sc", data, function(res){
           var array = new Array();
           for(var i = 0; i < res.data.length; i++){
               var x = 0;
               for(var j = 0; j < res.data[i].length; j++){
                   x += res.data[i][j];
               }
               // console.log(x);
               array.push(x);
           }
           optionPop1.xAxis[0].data = res.timelines;
           optionPop1.series[0].data = array;
           myChartpopup1.setOption(optionPop1);
       });
   }
   function postFun2(){
        post("/tr", data, function(res){
            var array = new Array();
            var len =res.data.legend.length;
            for(var i=0;i<len;i++){
                var name =res.data.legend[i];
                // console.log(name);
                if(name!=""){
                    array.push(name);
                }
            }
            optionPop2.xAxis.data=[];
            optionPop2.xAxis.data = res.data.xaxis;
            optionPop2.legend.data = array;
            optionPop2.series=[];
            console.log("----");
            console.log(optionPop2.series.length);
            for (var i = 0; i < array.length; i++) {
                optionPop2.series.push({
                    "name": array[i],
                    "smooth": false,
                    "type": "line",
                    "stack": "总量",
                    "data": res.data[array[i]]
                });
            }
            myChartpopup2.setOption(optionPop2,true);
        });
    }
    function postFun3() {
        post("/c", data, function(res){
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
            optionPop3.series[0].data =[];
            optionPop3.xAxis.data = array2;
            optionPop3.series[0].data = arrValues;
            // 使用刚指定的配置项和数据显示图表。
            myChartpopup3.setOption(optionPop3);
            // myChartpopup3.setOption(option3);
        });
    }
    // function man2PostFun() {
    //     post("/tr", data,)
    // }
});