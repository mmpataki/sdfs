let baseUrl = window.location.port == 5003 ? "http://localhost:8080" : ""
let rejectCodeList = [400, 401, 500, 403];

function _ajax(method, url, data, hdrs, cancelToken) {
    return new Promise((resolve, reject) => {
        var xhttp = new XMLHttpRequest();
        if (cancelToken) {
            cancelToken.cancel = function () {
                xhttp.abort();
                reject(new Error("Cancelled"));
            };
        }
        xhttp.onreadystatechange = function () {
            if (this.readyState == 4 && this.status == 200) {
                let json;
                try { json = JSON.parse(this.responseText); } catch (e) { }
                resolve({ response: this.responseText, json })
            }
            if (this.readyState == 4 && rejectCodeList.includes(this.status)) {
                reject({ message: JSON.parse(this.responseText).message, code: this.status });
            }
        };
        xhttp.onerror = function () {
            reject({ message: JSON.parse(this.responseText).message, code: this.status });
        }
        xhttp.open(method, url, true);
        hdrs && Object.keys(hdrs).forEach(key => xhttp.setRequestHeader(key, hdrs[key]))
        xhttp.send(data);
    });
}

function ajax(method, url, data, hdrs, cancelToken) {
    return _ajax(method, `${baseUrl}${url}`, data, hdrs, cancelToken)
}

function get(url, token) {
    return ajax("GET", url, undefined, {}, token);
}