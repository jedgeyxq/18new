var initMove = function (obj) {
    var type = obj.attr('mov');
    var ox,oy,sc;
    switch(type){
        case "fade-up":
            ox=0;
            oy="12%";
            sc=1;
            break;
        case "fade-down":
            ox=0;
            oy="-12%";
            sc=1;
            break;
        case "fade-left":
            ox="-12%";
            oy=0;
            sc=1;
            break;
        case "fade-right":
            ox="12%";
            oy=0;
            sc=1;
            break;
        case "zoom-in":
            ox=0;
            oy=0;
            sc=1;
            break;
        case "zoom-out":
            ox=0;
            oy=0;
            sc=1.2;
            break;
        case "fade":
            ox=0;
            oy=0;
            sc=1;
            break;
    }
    obj.animate({
        opacity:0,
        transform: 'translate('+ox+','+oy+') scale('+sc+','+sc+')'
    },0);
}

var initMove1 = function (obj) {
    var type = obj.attr("mov");
    var ox,oy;
    switch (type){
        case "fade-up":
            ox=0;
            oy="60%";
            break;
        case "fade-down":
            ox=0;
            oy="-60%";
            break;
        case "fade-right":
            ox="50%";
            oy=0;
            break;
    }
    obj.animate({
        opacity: 0,
        transform: 'translate('+ox+','+oy+')'
    },0);
}

var playMove = function (obj) {
    var duration = obj.attr("duration");
    var delay = obj.attr("delay");
    obj.delay(delay).animate({
        opacity:1,
        transform: "translate(0,0) scale(1,1)"
    }, duration);
}
// var playOutMove = function (obj) {
//     var duration = obj.attr("out-duration");
//     var delay = obj.attr("out-delay");
//     obj.delay(delay).animate({
//         opacity:0
//     },duration);
// }
var playPage = function (obj) {
    obj.find('[mov]').each(function () {
        playMove($(this));
    });
    // obj.find('[out]').each(function () {
    //     playOutMove($(this));
    // });
}
var scroll = true;
var kai = true;
window.onload = function () {
    // alert("onload");    //后弹
    playPage($('.banner'));

    setTimeout(function () {
        playPage($('.lscheme'));
    },1500);

    $(window).scroll(function () {
        if ($(document).scrollTop() >= 700 && $(document).scrollTop() <= ($('#product').height() + 100)) {
            if(scroll == true){
                console.log("123");
                playPage($('#WeDid'));
            }
            scroll = false;
        }

        if ($(document).scrollTop() >= ($('.lsfangwei-adv').offset().top -880) && $(document).scrollTop() <= ($('.lsfangwei-adv').height() + $('.lsfangwei-adv').offset().top -880)) {

            if(kai == true){
                console.log("222");
                $('.lsfangwei-adv').find('li').animate({
                    transform: "translateX(0)"
                });
            }
            kai = false;

        }
    });
}
$(document).ready(function () {
    // alert("ready");   //先弹
    $('.banner [mov]').each(function () {
       initMove($(this));
    });

    $('.lscheme [mov],#WeDid [mov]').each(function () {
        initMove1($(this));
    });

})