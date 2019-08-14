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

// 发从登陆请求，测试用
var sendSignInPost = function () {
    var email = document.forms["user"]["email"].value;
    var password = document.forms["user"]["password"].value;
    var remember = document.forms["user"]["remember"].checkbox;

    $.ajax({
        type: 'get',
        url: '/api/checkEmail',
        data: {
            "email": email,
            "password": password,
            "remember": remember
        },
        success: function (resultdata) {
            console.log(resultdata);
        }

    });
}