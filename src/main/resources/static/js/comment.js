function sendComment() {
    var questionId = $("#questionId").val();
    var content = $("#content").val();
    var captcha = $("#captcha").val();
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
        url: "/comment",
        contentType: 'application/json',
        data: JSON.stringify({
            "questionId": questionId,
            "parentId": questionId,
            "content": content,
            "type": 1
        }),
        success: function (response) {
            console.log(response);
            if (response.code == 2003) {
                var isAccepted = confirm("你还没有登陆，请先登陆后再操作！");
                if (isAccepted) {
                    window.open("/sign-in");
                    window.localStorage.setItem("closable", "true");
                }
                return;
            }
        },
        dataType: "json"
    });
}