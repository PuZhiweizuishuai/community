function comment() {
    var questionId = $("#questionId").val();
    var content = vditor.getValue();
    if (content == '\n' || content == " ") {
        alert("评论不能为空！");
    }
    var captcha = $("#captcha").val();
    sendComments(questionId, questionId, questionId, content, 1, captcha);
}


function twoLevelComment(e) {
    const father = e.getAttribute("data-id");
    const questionId = $("#questionId").val();
    const content = $("#second-content-" + father).val();
    const captcha = $("#second-captcha-" + father).val();
    const textId = "second-content-" + father;
    const text = document.getElementById(textId);
    if (text.getAttribute("type") === "3") {
        sendAtComment(questionId, father, text.getAttribute("parentid"), content, 2, captcha, text.getAttribute("user-src"), text.getAttribute("name"));
    } else {
        const parentId = e.getAttribute("data-id");
        sendComments(questionId, parentId, parentId, content, 2, captcha);
    }
}

/**
 * at “@” 用户回复
 * */
function atComment(e) {
    const fatherComment = e.getAttribute("father");
    const textId = "second-content-" + fatherComment;
    const text = document.getElementById(textId);
    text.placeholder = "回复 @" + e.getAttribute("user-name") + " :";
    text.setAttribute("type", "3");
    text.setAttribute("parentId", e.getAttribute("data-id"));
    text.setAttribute("name", e.getAttribute("user-name"));
    text.setAttribute("user-src", "/user/" + e.getAttribute("user-src"));
}


/**
 * 发送 @ 消息
 * */
function sendAtComment(questionId, parentCommentId, parentId, content, type, captcha, targetUrl, targetName) {
    if (content == null || content === "") {
        alert("评论内容不能为空！");
        return;
    }
    if (captcha == null || captcha === "") {
        alert("验证码不能为空！");
        return;
    }
    $.ajax({
        type: "POST",
        url: "/api/comment",
        contentType: 'application/json',
        data: JSON.stringify({
            "questionId": questionId,
            "parentCommentId": parentCommentId,
            "parentId": parentId,
            "content": content,
            "type": type,
            "captcha": captcha
        }),
        success: function (response) {
            if (response.code === 2003) {
                const isAccepted = confirm("你还没有登陆，请先登陆后再操作！");
                if (isAccepted) {
                    window.open("/sign-in");
                    window.localStorage.setItem("closable", "true");
                }
                return;
            }
            if (response.code === 200) {
                drawCommentForTwo(response, parentId, parentCommentId, targetUrl, targetName);
            } else {
                alert(response.message);
            }
        },
        dataType: "json"
    });
}


function sendComments(questionId, parentCommentId, parentId, content, type, captcha) {
    if (content == null || content === "") {
        alert("评论内容不能为空！");
        return;
    }
    if (captcha == null || captcha === "") {
        alert("验证码不能为空！");
        return;
    }
    $.ajax({
        type: "POST",
        url: "/api/comment",
        contentType: 'application/json',
        data: JSON.stringify({
            "questionId": questionId,
            "parentCommentId": parentCommentId,
            "parentId": parentId,
            "content": content,
            "type": type,
            "captcha": captcha
        }),
        success: function (response) {
            if (response.code == 2003) {
                var isAccepted = confirm("你还没有登陆，请先登陆后再操作！");
                if (isAccepted) {
                    window.open("/sign-in");
                    window.localStorage.setItem("closable", "true");
                }
                return;
            }
            if (response.code == 200) {
                drawComment(response, type, parentId);
                vditor.clearCache();
            } else {
                alert(response.message);
            }
        },
        dataType: "json"
    });
}

function drawComment(response, type, id) {
    if (type === 2) {
        const commentBody = $("#commentBody-" + id);
        const img = document.getElementById("get_now_online_user_src");
        const c = $("<div/>", {
            "class": "media mt-3 mr-2 ml-2"
        }).append($("<img/>", {
            "class": "align-self-start mr-3 rounded-circle",
            "src": img.getAttribute("src"),
            "width": 35,
            "height": 35
        })).append($("<div/>", {
            "class": "media-body"
        }).append($("<div/>", {
            "class": "row mt-0"
        }).append($("<div/>", {
            "class": "col"
        }).append($("<a/>", {
            "id": "user-" + response.data.commentId,
            "user-id": document.getElementById("now-online-user-id").value,
            "href": "/user/" + document.getElementById("now-online-user-userId").value,
            html: document.getElementById("now-online-user-name").value
        })).append($("<spam/>", {
            html: "&nbsp;&nbsp;刚刚"
        }))).append($("<div/>", {
            "class": "col text-right"
        }).append($("<a/>", {
            "href": "#",
            html: ""
        })).append($("<span/>", {
            html: "&nbsp;"
        })).append($("<a/>", {
            "href": "#",
            html: ""
        })))).append($("<p/>", {
            "class": "mt-2",
            text: response.data.content
        })).append($("<button/>", {
            "style": "background-color: transparent;border: 0px;",
            "data-id": response.data.commentId,
            "onclick": "clickLikeComment(this, 2)"
        }).append($("<img/>", {
            "src": "/image/icon/clicklike.svg",
            "height": 20
        })).append($("<span/>", {
            "id": "comment-likeCount-" + response.data.commentId,
            "class": "badge badge-light",
            html: 0
        }))).append($("<button/>", {
            "style": "background-color: transparent;border: 0px;",
            "data-id": response.data.commentId,
            "user-id": document.getElementById("now-online-user-id").value,
            "user-src": document.getElementById("now-online-user-userId").value,
            "user-name": document.getElementById("now-online-user-name").value,
            "father": id,
            "onclick": "atComment(this)"
        }).append($("<img/>", {
            "src": "/image/icon/comment.svg",
            "height": 20
        })).append($("<span/>", {
            "class": "badge badge-light",
            html: 0
        }))).append($("<hr/>", {})));
        commentBody.prepend(c);
        const text = "second-content-" + response.data.parentId;
        const captcha = "second-captcha-" + response.data.parentId;
        document.getElementById(text).value = "";
        document.getElementById(captcha).value = "";

    } else {
        window.location.reload();
    }
}

function drawCommentForTwo(response, parentId, id, targetUrl, targetName) {
    const user = document.getElementById("user-" + parentId);
    const commentBody = $("#commentBody-" + id);
    const img = document.getElementById("get_now_online_user_src");
    const c = $("<div/>", {
        "class": "media mt-3 mr-2 ml-2"
    }).append($("<img/>", {
        "class": "align-self-start mr-3 rounded-circle",
        "src": img.getAttribute("src"),
        "width": 35,
        "height": 35
    })).append($("<div/>", {
        "class": "media-body"
    }).append($("<div/>", {
        "class": "row mt-0"
    }).append($("<div/>", {
        "class": "col"
    }).append($("<a/>", {
        "id": "user-" + response.data.commentId,
        "user-id": document.getElementById("now-online-user-id").value,
        "href": "/user/" + document.getElementById("now-online-user-userId").value,
        html: document.getElementById("now-online-user-name").value
    })).append($("<spam/>", {
        html: "&nbsp;&nbsp;刚刚"
    }))).append($("<div/>", {
        "class": "col text-right"
    }).append($("<a/>", {
        "href": "#",
        html: ""
    })).append($("<span/>", {
        html: "&nbsp;"
    })).append($("<a/>", {
        "href": "#",
        html: ""
    })))).append($("<p/>", {
        "class": "mt-2",
        // 注意 XSS
        html: "回复 <a target='_blank' href='" + targetUrl + "'>@" + targetName + "</a> :"
    }).append($("<span/>", {
        text: response.data.content
    }))).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": response.data.commentId,
        "onclick": "clickLikeComment(this, 2)"
    }).append($("<img/>", {
        "src": "/image/icon/clicklike.svg",
        "height": 20
    })).append($("<span/>", {
        "id": "comment-likeCount-" + response.data.commentId,
        "class": "badge badge-light",
        html: 0
    }))).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": response.data.commentId,
        "user-id": user.getAttribute("user-id"),
        "user-src": user.src,
        "user-name": user.innerText,
        "father": id,
        "onclick": "atComment(this)"
    }).append($("<img/>", {
        "src": "/image/icon/comment.svg",
        "height": 20
    })).append($("<span/>", {
        "class": "badge badge-light",
        html: 0
    }))).append($("<hr/>", {})));
    commentBody.prepend(c);
    const text = "second-content-" + response.data.parentCommentId;
    const captcha = "second-captcha-" + response.data.parentCommentId;
    document.getElementById(text).value = "";
    document.getElementById(captcha).value = "";
}


/**
 * 显示二级评论
 * */
function showSecondComment(e) {
    var id = e.getAttribute("data-id");
    var comment = $("#comment-" + id);
    var collapse = e.getAttribute("data-collapse");
    var commentBody = $("#commentBody-" + id);
    if (collapse) {
        const textAreaId = "second-content-" + id;
        const textArea = document.getElementById(textAreaId);
        if (textArea.getAttribute("type") === "3") {
            textArea.removeAttribute("type");
            textArea.removeAttribute("parentid");
            textArea.removeAttribute("user-src");
            textArea.removeAttribute("name");
            textArea.placeholder = "请自觉遵守互联网相关的政策法规，严禁发布色情、暴力、反动的言论。";
            return;
        }
        // 折叠
        comment.collapse('hide');
        e.removeAttribute("data-collapse");
        e.classList.remove("active");
        //commentBody.empty();
    } else {
        $.getJSON("/api/twoLevelComment/" + id, function (data) {
            if (commentBody.children().length != 1) {
                comment.collapse('show');
                e.setAttribute("data-collapse", "show");
                e.classList.add("active");
            } else {
                $.each(data.data.reverse(), function (index, userComment) {
                    if (userComment.parentCommentId != userComment.parentId) {
                        commentBody.prepend(drawTwoComment(index, userComment));
                    } else {
                        commentBody.prepend(draw(index, userComment));
                    }
                });
            }
            comment.collapse('show');
            e.setAttribute("data-collapse", "show");
            e.classList.add("active");
        });

        comment.collapse('show');
        e.setAttribute("data-collapse", "show");
        e.classList.add("active");

    }
}


function clickLikeQuestion(e) {
    var notifier = $("#now-online-user-id").val();
    if (notifier == null) {
        alert("请先登陆！");
        return;
    }
    var questionId = e.getAttribute("questionid-data");
    var receiver = e.getAttribute("receiver-data");
    var ClickLikeDTO = {
        notifier: notifier,
        receiver: receiver,
        questionId: questionId,
        commentId: -1,
        token: getCookie('token')
    };

    var likeCount = document.getElementById("like-button-number").innerText;
    fetch("/api/clickLike", {
        method: 'POST', // or 'PUT'
        body: JSON.stringify(ClickLikeDTO), // data can be `string` or {object}!
        headers: new Headers({
            'Content-Type': 'application/json'
        })
    }).then(res => res.json())
        .catch(error => console.error('Error:', error))
        .then(function (response) {
            if (response.success) {
                if (response.msg == "取消点赞成功！") {
                    document.getElementById("like-button-number").innerText = (parseInt(likeCount) - 1);
                    document.getElementById("like-button").className = "btn btn-outline-primary btn-sm mb-0";
                } else {
                    document.getElementById("like-button-number").innerText = (parseInt(likeCount) + 1);
                    document.getElementById("like-button").className = "btn btn-primary btn-sm mb-0";
                }

            } else {
                alert(response.msg);
            }
        });
}

function clickLikeComment(e, type) {
    var notifier = $("#now-online-user-id").val();
    if (notifier == null) {
        alert("请先登陆！");
        return;
    }
    var questionId = document.getElementById("like-button").getAttribute("questionid-data");
    // var receiver = document.getElementById("like-button").getAttribute("receiver-data");
    var ClickLikeDTO = {
        notifier: notifier,
        // receiver: receiver,
        questionId: questionId,
        commentId: e.getAttribute("data-id"),
        token: getCookie('token')
    };
    var likeCountID = "comment-likeCount-" + e.getAttribute("data-id");
    var likeCount = document.getElementById(likeCountID).innerText;
    fetch("/api/clickLike", {
        method: 'POST', // or 'PUT'
        body: JSON.stringify(ClickLikeDTO), // data can be `string` or {object}!
        headers: new Headers({
            'Content-Type': 'application/json'
        })
    }).then(res => res.json())
        .catch(error => console.error('Error:', error))
        .then(function (response) {
            if (response.success) {
                if (response.msg == "取消点赞成功！") {
                    document.getElementById(likeCountID).innerText = (parseInt(likeCount) - 1);
                    if (type == 1) {
                        e.className = "btn btn-outline-primary btn-sm mb-0";
                    }
                    alert(response.msg);
                } else {
                    document.getElementById(likeCountID).innerText = (parseInt(likeCount) + 1);
                    if (type == 1) {
                        e.className = "btn btn-primary btn-sm mb-0";
                    }
                    alert(response.msg);
                }
            } else {
                alert(response.msg);
            }
        });
}


function getCookie(cname) {
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


function draw(index, userComment) {
    var c = $("<div/>", {
        "class": "media mt-3 mr-2 ml-2"
    }).append($("<img/>", {
        "class": "align-self-start mr-3 rounded-circle",
        "src": userComment.user.headUrl,
        "width": 35,
        "height": 35
    })).append($("<div/>", {
        "class": "media-body"
    }).append($("<div/>", {
        "class": "row mt-0"
    }).append($("<div/>", {
        "class": "col"
    }).append($("<a/>", {
        "id": "user-" + userComment.commentId,
        "user-id": userComment.user.id,
        "href": "/user/" + userComment.user.userId,
        html: userComment.user.userName
    })).append($("<spam/>", {
        html: "&nbsp;&nbsp;" + userComment.createTime
    }))).append($("<div/>", {
        "class": "col text-right"
    }).append($("<a/>", {
        "href": "#",
        html: ""
    })).append($("<span/>", {
        html: "&nbsp;"
    })).append($("<a/>", {
        "href": "#",
        html: ""
    })))).append($("<p/>", {
        "class": "mt-2",
        text: userComment.content
    })).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": userComment.commentId,
        "onclick": "clickLikeComment(this, 2)"
    }).append($("<img/>", {
        "src": "/image/icon/clicklike.svg",
        "height": 20
    })).append($("<span/>", {
        "id": "comment-likeCount-" + userComment.commentId,
        "class": "badge badge-light",
        html: userComment.likeCount
    }))).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": userComment.commentId,
        "user-id": userComment.user.id,
        "user-src": userComment.user.userId,
        "user-name": userComment.user.userName,
        "father": userComment.parentId,
        "onclick": "atComment(this)"
    }).append($("<img/>", {
        "src": "/image/icon/comment.svg",
        "height": 20
    })).append($("<span/>", {
        "class": "badge badge-light",
        html: userComment.commentCount
    }))).append($("<hr/>", {})));
    return c;
}

function drawTwoComment(index, userComment) {
    var c = $("<div/>", {
        "class": "media mt-3 mr-2 ml-2"
    }).append($("<img/>", {
        "class": "align-self-start mr-3 rounded-circle",
        "src": userComment.user.headUrl,
        "width": 35,
        "height": 35
    })).append($("<div/>", {
        "class": "media-body"
    }).append($("<div/>", {
        "class": "row mt-0"
    }).append($("<div/>", {
        "class": "col"
    }).append($("<a/>", {
        "id": "user-" + userComment.commentId,
        "user-id": userComment.user.id,
        "href": "/user/" + userComment.user.userId,
        html: userComment.user.userName
    })).append($("<spam/>", {
        html: "&nbsp;&nbsp;" + userComment.createTime
    }))).append($("<div/>", {
        "class": "col text-right"
    }).append($("<a/>", {
        "href": "#",
        html: ""
    })).append($("<span/>", {
        html: "&nbsp;"
    })).append($("<a/>", {
        "href": "#",
        html: ""
    })))).append($("<p/>", {
        "class": "mt-2",
        // 注意 XSS
        html: "回复 <a target='_blank' href='" + userComment.targetUrl + "'>@" + userComment.targetName + "</a> :"
    }).append($("<span/>", {
        text: userComment.content
    }))).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": userComment.commentId,
        "onclick": "clickLikeComment(this, 2)"
    }).append($("<img/>", {
        "src": "/image/icon/clicklike.svg",
        "height": 20
    })).append($("<span/>", {
        "id": "comment-likeCount-" + userComment.commentId,
        "class": "badge badge-light",
        html: userComment.likeCount
    }))).append($("<button/>", {
        "style": "background-color: transparent;border: 0px;",
        "data-id": userComment.commentId,
        "user-id": userComment.user.id,
        "user-src": userComment.user.userId,
        "user-name": userComment.user.userName,
        "father": userComment.parentCommentId,
        "onclick": "atComment(this)"
    }).append($("<img/>", {
        "src": "/image/icon/comment.svg",
        "height": 20
    })).append($("<span/>", {
        "class": "badge badge-light",
        html: userComment.commentCount
    }))).append($("<hr/>", {})));
    return c;
}