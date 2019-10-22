function comment() {
    var questionId = $("#questionId").val();
    var content = vditor.getValue();
    if(content == '\n' || content == " ") {
        alert("评论不能为空！");
    }
    var captcha = $("#captcha").val();
    sendComments(questionId, questionId, content, 1, captcha);
}


function twoLevelComment(e) {
    var parentId = e.getAttribute("data-id");
    var questionId = $("#questionId").val();
    var content = $("#second-content-" + parentId).val();
    var captcha = $("#second-captcha-" + parentId).val();
    sendComments(questionId, parentId, content, 2, captcha);
}


function sendComments(questionId, parentId, content, type, captcha) {
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
            if(response.code == 200) {
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
    if(type === 2) {
        const commentBody = $("#commentBody-" + id);
        const img = document.getElementById("get_now_online_user_src");
        const c = $("<div/>",{
            "class": "media mt-3 mr-2 ml-2"
        }).append($("<img/>",{
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
            "href": "/user/" + document.getElementById("now-online-user-userId").value,
            html: document.getElementById("now-online-user-name").value
        })).append($("<spam/>", {
            html: "&nbsp;&nbsp;刚刚"
        }))).append($("<div/>",{
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
            html: response.data.content
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
        }))).append($("<a/>", {
            "href": "#"
        }).append($("<img/>", {
            "src": "/image/icon/comment.svg",
            "height": 20
        })).append($("<span/>", {
            "class": "badge badge-light",
            html: 0
        }))).append($("<hr/>", {

        })));
        commentBody.prepend(c);
        const text = "second-content-" + response.data.parentId;
        const captcha = "second-captcha-" + response.data.parentId;
        document.getElementById(text).value = "";
        document.getElementById(captcha).value = "";

    } else {
        window.location.reload();
    }
}

/**
 * 显示二级评论
 * */
function showSecondComment(e) {
    var id = e.getAttribute("data-id");
    var comment = $("#comment-"+id);
    var collapse = e.getAttribute("data-collapse");
    var commentBody = $("#commentBody-" + id);
    if(collapse) {
        // 折叠
        comment.collapse('hide');
        e.removeAttribute("data-collapse");
        e.classList.remove("active");
        //commentBody.empty();

    } else {
        $.getJSON("/api/twoLevelComment/" + id, function (data) {
            if(commentBody.children().length != 1) {
                comment.collapse('show');
                e.setAttribute("data-collapse", "show");
                e.classList.add("active");
            } else {
                $.each(data.data.reverse(), function (index, userComment) {
                    var c = $("<div/>",{
                        "class": "media mt-3 mr-2 ml-2"
                    }).append($("<img/>",{
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
                        "href": "/user/" + userComment.user.userId,
                        html: userComment.user.userName
                    })).append($("<spam/>", {
                        html: "&nbsp;&nbsp;" + userComment.createTime
                    }))).append($("<div/>",{
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
                        html: userComment.content
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
                    }))).append($("<a/>", {
                        "href": "#"
                    }).append($("<img/>", {
                        "src": "/image/icon/comment.svg",
                        "height": 20
                    })).append($("<span/>", {
                        "class": "badge badge-light",
                        html: userComment.commentCount
                    }))).append($("<hr/>", {

                    })));
                    commentBody.prepend(c);
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
    if(notifier==null) {
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
        if(response.success) {
            if(response.msg == "取消点赞成功！") {
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
    if(notifier==null) {
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
        if(response.success) {
            if(response.msg == "取消点赞成功！") {
                document.getElementById(likeCountID).innerText = (parseInt(likeCount) - 1);
                if(type == 1) {
                    e.className = "btn btn-outline-primary btn-sm mb-0";
                }
                alert(response.msg);
            } else {
                document.getElementById(likeCountID).innerText = (parseInt(likeCount) + 1);
                if(type == 1) {
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
    for(var i = 0; i <ca.length; i++) {
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