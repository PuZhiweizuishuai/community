var chheckEmail = function (email) {
    var regx = /^([a-zA-Z0-9]+[-_.]?)*[a-zA-Z0-9]+@([a-zA-Z0-9]+[-_.]?)*[a-zA-Z0-9]+\.[a-zA-Z]{2,6}$/;
    var emailRegx = new RegExp(regx);
    if(emailRegx.test(email)) {
        return true;
    } else {
        return false;
    }
};

var checkPassword = function (password) {
    if(password.length >= 6 && password.length <= 30) {
        return true;
    } else {
        return false;
    }
};

