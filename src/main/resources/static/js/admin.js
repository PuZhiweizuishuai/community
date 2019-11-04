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

function updateTagClass() {
    $.ajax({
        type: 'get',
        url: '/api/admin/updateTagClass',
        success: function (resultdata) {
            if (resultdata.success) {
                alert(resultdata.msg)
            } else {
                alert(resultdata.msg);
            }
        }
    })
}


function showSendStartAd(e) {
    document.getElementById("showSendAd").style.display = "inline";
    document.getElementById("adId").value = e.getAttribute("data-id");
}

function closeSendStartAd(e) {
    document.getElementById("showSendAd").style.display = "none";
}

function SendStartAd() {
    const id = document.getElementById("adId").value;
    const time = document.getElementById("time").value;
    const position = document.getElementById("position").value;
    if (id === -1) {
        document.getElementById("result").innerText = "广告ID错误，请取消后重试！";
        return;
    }
    if (time === null || time < 0) {
        document.getElementById("result").innerText = "时间不能小于0";
        return;
    }
    const createAdvertisement = {
        "id": id,
        "time": time,
        "position": position
    };
    $.ajax({
        type: 'POST',
        url: '/admin/advertisement/setting',
        contentType: 'application/json',
        data: JSON.stringify(createAdvertisement),
        success: function (result) {
            if (result.success) {
                document.getElementById("result").innerText = result.msg;
                window.location.reload();
            } else {
                document.getElementById("result").innerText = result.msg;
                document.getElementById("time").value = result.ad.time;
            }
        }
    });

}

function alterAD(e) {
    const id = e.getAttribute("data-id");
    const url = "/admin/advertisement/" + id;
    window.open(url);
}

function closeAD(e) {
    const id = e.getAttribute("data-id");
    const createAdvertisement = {
        "id": id,
    };
    $.ajax({
        type: 'POST',
        url: '/admin/advertisement/close',
        contentType: 'application/json',
        data: JSON.stringify(createAdvertisement),
        success: function (result) {
            if (result.success) {
                alert(result.msg);
                window.location.reload();
            } else {
                alert(result.msg);
            }
        }
    });
}