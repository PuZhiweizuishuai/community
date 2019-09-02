function addTag(value) {
    // var str = $("#issues-tag").val();
    // //$("#issues-tag").tagsinput('abc',value);
    // if(str) {
    //     $("#issues-tag").val(value);
    // } else {
    //     $("#issues-tag").val(str + ',' + value);
    // }


    var str = $("#issues-tag").val();
    var list = str.split(",");
    console.log(list.length);
    console.log(str);
    if(list.length > 5) {
        document.getElementById("tagHelp").innerText = "最多只能输入6个标签！";
        //$("#tagHelp").val("最多只能输入6个标签！");
        return;
    }
    $("#issues-tag").tagsinput('add',value);
}


function getTag() {
    console.log($("#issues-tag").val());
}



function showMayTag() {
    $("#many-tag-show").slideDown();
}


function closeMauTag() {
    $("#many-tag-show").slideUp();
}


