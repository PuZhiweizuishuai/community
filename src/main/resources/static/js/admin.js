function sendDeleteQuestion(e) {
    var questionId = e.getAttribute("data-id");
    $.ajax({
        type: 'post',
        url: '/api/admin/delete/question',
        data: {
            "questionId": questionId
        },
        success: function (resultdata) {
            if (resultdata.success) {
                window.location.reload(true);
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}
function sendRecoveryQuestion(e) {
    var questionId = e.getAttribute("data-id");
    $.ajax({
        type: 'post',
        url: '/api/admin/recovery/question',
        data: {
            "questionId": questionId
        },
        success: function (resultdata) {
            if (resultdata.success) {
                window.location.reload(true);
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}

function sendUserPower(e, power) {
    var userId = e.getAttribute("data-id");
    $.ajax({
        type: 'post',
        url: '/api/admin/update/user',
        data: {
            "userId": userId,
            "power": power
        },
        success: function (resultdata) {
            if (resultdata.success) {
                window.location.reload(true);
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}

function resetPassword(e) {
    var userId = e.getAttribute("data-id");
    $.ajax({
        type: 'post',
        url: '/api/admin/reset/password',
        data: {
            "userId": userId
        },
        success: function (resultdata) {
            if (resultdata.success) {
                window.location.reload(true);
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}