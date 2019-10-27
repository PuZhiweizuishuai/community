function followTopic(e) {
    const followTopicId = e.getAttribute("followId-data");
    const followTopicTitle = e.getAttribute("followTitle-data");
    const token = getCookieToken("token");
    const followTopicDTO = {
        topicId: followTopicId,
        topicTitle: followTopicTitle,
        token: token
    };
    const count = document.getElementById("tag_class_number").innerText;
    fetch("/api/followTopic", {
        method: 'POST', // or 'PUT'
        body: JSON.stringify(followTopicDTO), // data can be `string` or {object}!
        headers: new Headers({
            'Content-Type': 'application/json'
        })
    }).then(res => res.json())
        .catch(error => console.error('Error:', error))
        .then(function (response) {
            if (response.success) {
                if (response.msg === "取消关注成功！") {
                    document.getElementById("tag_class_number").innerText = (parseInt(count) - 1);
                    document.getElementById("tag_class_button").innerText = "关注";
                } else {
                    document.getElementById("tag_class_number").innerText = (parseInt(count) + 1);
                    document.getElementById("tag_class_button").innerText = "取消关注";
                }
            } else {
                alert(response.msg);
            }
        });
}

function followQuestion(e) {
    const followQuestionId = e.getAttribute("questionId-data");
    const token = getCookieToken("token");
    const followQuestionDTO = {
        followQuestionId: followQuestionId,
        token: token
    };
    const count = document.getElementById("follow_question_number").innerText;
    fetch("/api/followQuestion", {
        method: 'POST', // or 'PUT'
        body: JSON.stringify(followQuestionDTO), // data can be `string` or {object}!
        headers: new Headers({
            'Content-Type': 'application/json'
        })
    }).then(res => res.json())
        .catch(error => console.error('Error:', error))
        .then(function (response) {
            if (response.success) {
                if (response.msg === "取消关注成功！") {
                    document.getElementById("follow_question_number").innerText = (parseInt(count) - 1);
                    document.getElementById("follow_question_text").innerText = "关注";
                    e.className = "btn btn-outline-secondary btn-sm mb-0";
                } else {
                    document.getElementById("follow_question_number").innerText = (parseInt(count) + 1);
                    document.getElementById("follow_question_text").innerText = "取消关注";
                    e.className = "btn btn-secondary btn-sm mb-0";
                }
            } else {
                alert(response.msg);
            }
        });
}

function followUser(e) {
    const followUserId = e.getAttribute("id-data");
    const token = getCookieToken("token");
    const followUserDTO = {
        followUserId: followUserId,
        token: token
    };
    fetch("/api/followUser", {
        method: 'POST', // or 'PUT'
        body: JSON.stringify(followUserDTO), // data can be `string` or {object}!
        headers: new Headers({
            'Content-Type': 'application/json'
        })
    }).then(res => res.json())
        .catch(error => console.error('Error:', error))
        .then(function (response) {
            if (response.success) {
                if (response.msg === "取消关注成功！") {
                    document.getElementById("follow_user_button").innerText = "关注";
                } else {
                    document.getElementById("follow_user_button").innerText = "取消关注";
                }
            } else {
                alert(response.msg);
            }
        });
}


function getCookieToken(cname) {
    var name = cname + "=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}