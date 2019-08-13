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
    if(l === 'en-US') {
        //根据id获取超链接,设置href属性
        var aObj = document.getElementById(aObjId);
        aObj.href = aObj.href +  "?l=" + l;
    }
};