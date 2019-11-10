// 获取url上的参数，name为参数名
var GetQueryString = function (name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return decodeURI(r[2]);
    return null;
};

// 更改url
var addLocaleUrl = function (aObjId) {
    var l = GetQueryString('l');
    var page = GetQueryString('page');
    console.log(page);
    //根据id获取超链接,设置href属性
    var aObj = document.getElementById(aObjId);
    if (l === 'en-US') {
        aObj.href = aObj.href + "?l=" + l;
    }
};

var addPageNumberForLanguage = function (aObjId) {
    var l = GetQueryString('l');
    var page = GetQueryString('page');
    console.log(page);
    //根据id获取超链接,设置href属性
    var aObj = document.getElementById(aObjId);
    if (page != null) {
        aObj.href = aObj.href + "&page=" + page;
    }
}


var addLocaleUrlForPage = function (aObjId) {
    var l = GetQueryString('l');
    var aObj = document.getElementById(aObjId);
    if (l === 'en-US') {
        aObj.href = aObj.href + "&l=" + l;
    }
}

var closeSignOut = function () {
    $("#Sign-out-tips").fadeOut();
    delCookie("token");
    location.reload();
};

function getCookie(name) {
    var value = '; ' + document.cookie;
    var parts = value.split('; ' + name + '=');
    if (parts.length === 2) {
        return parts.pop().split(';').shift();
    }
};

function delCookie(name) {
    var exp = new Date();
    exp.setTime(exp.getTime()-1);
    var cval = getCookie(name);
    if (cval != null)
        document.cookie = name + "="+ cval + ";expires=" + exp.toGMTString() + ";path=/";
}

var signOut = function () {
    //debugger;
    window.location.href = "/sign-out";
    delCookie("token");
};



