const vditor = new Vditor('issues-text', {
    toolbar: ['emoji', 'headings', 'bold', 'italic', 'strike', '|', 'line', 'quote', 'list', 'ordered-list', 'check', 'code', 'inline-code', 'undo', 'redo', 'upload', 'link', 'table', 'record', 'both', 'preview', 'fullscreen'],
    typewriterMode: true,
    placeholder: "请输入内容",
    counter: 100000,
    height: 500,
    hint: {
        emojiPath: '/js/vditor/images/emoji',
        emoji: emojis
    },
    tab: '\t',
    upload: {
        accept: 'image/*, .wav, .mp4, .zip, .rar, .7z, .docx, .dox, .ppt, .pptx, .xls, .xlsx, .pdf, .apk',
        url: '/api/file/upload',
        linkToImgUrl: '/api/file/upload',
        filename (name) {
            return name;
        },
    }
});
const text = document.getElementById("issues-text-textarea").value;
if (text != "") {
    vditor.setValue(text);
}