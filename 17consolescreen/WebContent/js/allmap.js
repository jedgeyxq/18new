$(function () {

    $('#closemap').click(function () {
        $('#bigMap-mask').css("display", "none");
        clearInterval(timer);
        clearInterval(timer2);
        timer1 = setInterval(function () {
            console.log("setInterval");
            post("/rt", {}, function (res) {

                updateData(".wxtodayscanusers", res.wxtodayscanusers);
                updateData(".wxtodayscans", res.wxtodayscans);
                updateData(".wxscan", res.wxscan);
                updateData(".wxtotalprizeusers", res.wxtotalprizeusers);
                updateData(".wxtotalprizes", res.wxtotalprizes);
                updateData(".wxtotalprizetimes", res.wxtotalprizetimes);
            });
        }, 10000);
    });
});
var mp = new BMap.Map("allmap");
var timer = null;
var timer2 = null;
function init(point,level) {

    var _width = window.screen.width;
    var _height = window.screen.height;
    $('#bigMap-mask').css("display", "block");
    $('#ca').hide();
    $('#time-query li').eq(1).removeClass('on');
    $('#realTimeData').addClass('on');
    if(_width > _height || _width >= 768) {
        // getscanposition();
        clearInterval(timer1);
        setTimeout(function () {
            post("/rt", {}, function (res) {

                $("#maptodayscansusers").html(res.wxtodayscanusers);
                $("#maptodayscan").html(res.wxtodayscans);
                $("#mapscan").html(res.wxscan);
                $("#maptotalprizetimes").html(res.wxtotalprizeusers);
                $("#mapwxtotalprizeusers").html(res.wxtotalprizes);
                $("#maptotalprizes").html(res.wxtotalprizetimes);

                timer2 = setInterval(function () {
                    console.log("setInterval2");
                    post("/rt", {}, function (res) {

                        $("#maptodayscansusers").html(res.wxtodayscanusers);
                        $("#maptodayscan").html(res.wxtodayscans);
                        $("#mapscan").html(res.wxscan);
                        $("#maptotalprizetimes").html(res.wxtotalprizeusers);
                        $("#mapwxtotalprizeusers").html(res.wxtotalprizes);
                        $("#maptotalprizes").html(res.wxtotalprizetimes);
                    });
                }, 10000);
            });
        },310);

        timer = setInterval(getscanposition, 5000);
        
        // 实时查询 || 日期查询
        $('#time-query li').click(function () {
            // var index = $(this).index();
            $('#time-query li').removeClass('on');
            $(this).addClass('on');
        });
        $('#realTimeData').click(function () {
            $('#ca').hide();
            setTimeout(function () {
                clearInterval(timer);
                timer = setInterval(getscanposition, 5000);
            },410);
        });
        $('#time-query li').eq(1).click(function () {
            clearInterval(timer);
            $('#ca').show();
        });
    }
    mp.centerAndZoom(point, level);
    mp.enableScrollWheelZoom();
// 日历
    $('#ca').calendar({
        width: 320,
        height: 320,
        data: [
            {
                date: '2015/12/24',
                value: 'Christmas Eve'
            },
            {
                date: '2015/12/25',
                value: 'Merry Christmas'
            },
            {
                date: '2016/01/01',
                value: 'Happy New Year'
            }
        ],
        onSelected: function (view, date, data) {
            console.log('view:' + view)
//            alert('date:' + date)
            console.log('data:' + (data || 'None'));
            console.log(date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate());
            // var time = date.getFullYear() + '-' + (date.getMonth()+1) + '-' + date.getDate();
            // post('/sp', {begin: time, }, renderscanposition);
        }
    });
}
function getscanposition() {
    post("/sp", {}, renderscanposition);
}

function renderscanposition(res) {
    console.log(res);
    for (var i = 0; i < res.total; i++) {
        try {
            var data = res["data" + i].split("#");
            var from = data[0];
            var lng = data[1];
            var lat = data[2];
            var province = decodeURIComponent(data[3]);
            var city = decodeURIComponent(data[4]);
            var strict = decodeURIComponent(data[5]);
            var street = decodeURIComponent(data[6]);
            var streetnumber = decodeURIComponent(data[7]);
            var iconurl = data[8];
            var nick = decodeURIComponent(data[9]);
            var enc = data[10];
            var ip = data[11];
            var intime = data[12];
            var playtime = data[13];
            add(from, lng, lat, province, city, strict, street, streetnumber, iconurl, nick, enc, ip, intime, playtime);
        } catch (e) {
            //do nothing
        }
    }

}
function generateinfo(from, lng, lat, iconurl, nickname, enc, scanip, scantime, playtime){
        return "<table id='"+nickname+"-"+enc+"-"+scanip+"-"+playtime+"' style='border-top:1px #aaa solid;'><tr><td rowspan='4' valign='middle' align='center'><div style='width:60px;height:60px;border-radius:30px;margin:10px;overflow:hidden;'><img src='"+iconurl+"' style='width:100%'/></div></td><td>二维码："+enc+"</td></tr>"
            +"<tr><td>来源："+(from=="wx"?"微信":"支付宝")+"</td></tr>"
            +"<tr><td>扫码IP："+scanip+"</td></tr>"
            +"<tr><td>扫码时间："+new Date(parseInt(scantime)).toLocaleString().replace(/年|月/g, "-").replace(/日/g, " ")+"</td></tr>"
            +"<tr><td align='center'>"+nickname+"</td><td>参与时间："+new Date(parseInt(playtime)).toLocaleString().replace(/年|月/g, "-").replace(/日/g, " ")+"</td></tr>"
            +"</table>";
    }
function add(from, lng, lat, province, city, district, street, streetnumber, iconurl, nickname, enc, scanip, scantime, playtime){
    if (lng==undefined||province==undefined||iconurl==undefined||enc==undefined||scanip==undefined||scantime==undefined){
        return;
    }
    if (document.getElementById("icon-"+lng+"-"+lat)){
        if (!document.getElementById(nickname+"-"+enc+"-"+scanip+"-"+playtime)){
            document.getElementById("info-"+lng+"-"+lat).innerHTML += generateinfo(from, lng, lat, iconurl, nickname, enc, scanip, scantime, playtime);
        }
        if (document.getElementById("info-"+lng+"-"+lat).children.length>1){
            document.getElementById("icon-"+lng+"-"+lat).innerHTML="<div style='text-align:center;padding-top:6px;'>"+document.getElementById("info-"+lng+"-"+lat).children.length+"</div>";
        }
    } else {

        var p = new BMap.Point(lng,lat);
        var iconmarker = new IconMarker(p, iconurl);
        mp.addOverlay(iconmarker);
        var marker = new BMap.Marker(p);
        mp.addOverlay(marker);

        var div = document.createElement("div");
        div.id = "info-"+lng+"-"+lat;
        div.innerHTML += generateinfo(from, lng, lat, iconurl, nickname, enc, scanip, scantime, playtime);
        // div.css("z-index","-1");

        document.getElementById('allmap').appendChild(div);
        document.getElementById("info-"+lng+"-"+lat).style.display="none";
        var infowindow = new BMap.InfoWindow("<div style='font-size:8pt;overflow-y:auto;'><div id='addr-"+lng+"-"+lat+"'>地址："+province + ", " + city + ", " + district + ", " + street + ", " + streetnumber+"</div><div id='"+lng+"-"+lat+"'>"
            +"</div></div>");
        marker.addEventListener("click", function(){
            document.getElementById("info-"+lng+"-"+lat).style.display="block";
            this.openInfoWindow(infowindow);
            if (document.getElementById("info-"+lng+"-"+lat).children.length>2){
                document.getElementById( "addr-"+lng+"-"+lat).parentNode.style.height="300px";
            }
            document.getElementById(lng+"-"+lat).innerHTML = document.getElementById("info-"+lng+"-"+lat).innerHTML;
            infowindow.redraw();
            document.getElementById("info-"+lng+"-"+lat).style.display="none";
        });
    }
}
//init
// var mp = new BMap.Map("allmap");
// mp.centerAndZoom("西安市", 6);
// mp.enableScrollWheelZoom();
function myFun(result){
    var cityName = result.name;
    mp.centerAndZoom(cityName, 6);
}
var myCity = new BMap.LocalCity();
myCity.get(myFun);

//icon marker
function IconMarker(point, iconurl){
    this._point = point;
    this._iconurl = iconurl;
}
IconMarker.prototype = new BMap.Overlay();
IconMarker.prototype.initialize = function(map){
    this._map = map;
    var div = this._div = document.createElement("div");
    div.id="icon-"+this._point.lng+"-"+this._point.lat;
    div.style.position = "absolute";
    div.style.zIndex = BMap.Overlay.getZIndex(this._point.lat);
    div.style.width = "28px";
    div.style.height = "28px";
    div.style.overflow = "hidden";
    div.style.borderRadius="14px";
    var img = document.createElement("img");
    img.style.width="28px";
    img.style.height="28px";
    img.src=this._iconurl;
    div.appendChild(img);
    mp.getPanes().labelPane.appendChild(div);
    return div;
}
IconMarker.prototype.draw = function(){
        var map = this._map;
        var pixel = map.pointToOverlayPixel(this._point);
        this._div.style.left = pixel.x - 15 + "px";
        this._div.style.top  = pixel.y - 35 + "px";
    }