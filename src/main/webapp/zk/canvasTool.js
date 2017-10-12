/**
* Utility functions that help testing
* canvas - HTMLCanvasElement
* fileName - string
*/
function saveCanvasAsPng(canvas, fileName){
    saveBase64AsFile(canvas.toDataURL("image/png"), fileName);
}

function saveBase64AsFile(base64, fileName) {
    var link = document.createElement("a");
    link.setAttribute("href", base64);
    link.setAttribute("download", fileName);
    link.click();
}