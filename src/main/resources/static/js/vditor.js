const text = document.querySelector("#issues-text-textarea").value;
const vditor = new Vditor('issues-text', {
    // toolbar: ['emoji', 'headings', 'bold', 'italic', 'strike', '|', 'line', 'quote', 'list', 'ordered-list', 'check', 'code', 'inline-code', 'undo', 'redo', 'upload', 'link', 'table', 'record', 'both', 'preview', 'fullscreen'],
    // typewriterMode: true,
    placeholder: "请自觉遵守互联网相关的政策法规，严禁发布色情、暴力、反动的言论。",
    counter: 100000,
    height: 700,
    hint: {
        emojiPath: '/js/vditor/dist/images/emoji',
        emoji: emojis
    },
    tab: '\t',
    upload: {
        accept: 'image/*, .wav, .mp4, .zip, .rar, .7z, .docx, .dox, .ppt, .pptx, .xls, .xlsx, .pdf, .apk, .mp3, .txt',
        url: '/api/file/upload',
        // linkToImgUrl: '/api/file/upload',
        filename (name) {
            return name.replace(/[^(a-zA-Z0-9\u4e00-\u9fa5\.)]/g, '')
                .replace(/[\?\\/:|<>\*\[\]\(\)\$%\{\}@~]/g, '')
                .replace('/\\s/g', '');
        },
    },
    after() {
        if (text != "") {
            vditor.setValue(text);
        }
    }
});


