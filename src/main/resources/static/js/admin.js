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

function settingIndexTopQuestion() {
    var questionId1 = $("#question-Id-1").val();
    var questionId2 = $("#question-Id-2").val();
    var qusetionId3 = $("#question-Id-3").val();

    if(questionId1 != '' && questionId1 < 0) {
        document.getElementById("question-id-warning").style.display = "inline";
        document.getElementById("question-id-warning").innerText = "输入框1的问题ID不能为负数"
    }

    if(questionId2 != '' && questionId2 < 0) {
        document.getElementById("question-id-warning").style.display = "inline";
        document.getElementById("question-id-warning").innerText = "输入框2的问题ID不能为负数"
    }

    if(qusetionId3 != '' && qusetionId3 < 0) {
        document.getElementById("question-id-warning").style.display = "inline";
        document.getElementById("question-id-warning").innerText = "输入框3的问题ID不能为负数"
    }

    $.ajax({
        type: 'post',
        url: '/api/admin/settingTopQuestion',
        data: {
            'id1' : questionId1,
            'id2' : questionId2,
            'id3' : qusetionId3
        },
        success: function (resultdata) {
            if (resultdata.success) {
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })


}

function updateHotTag() {
    $.ajax({
        type: 'get',
        url: '/api/admin/updateHotTag',
        success: function (resultdata) {
            if (resultdata.success) {
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}

function updateHotQuestion() {
    $.ajax({
        type: 'get',
        url: '/api/admin/updateHotQuestion',
        success: function (resultdata) {
            if (resultdata.success) {
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}

function updateHotUser() {
    $.ajax({
        type: 'get',
        url: '/api/admin/updateHotUser',
        success: function (resultdata) {
            if (resultdata.success) {
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}