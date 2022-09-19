let baseUrl = ""
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



function getTextBlock(h, txt, lbl) {
    return `
        <div style='display: flex; flex-direction: column; justify-content: center; padding: 0px 20px'>
            <div style='font-size: ${h - 30}px; height: ${h - 20}px; justify-content: center; display: flex'>
                <span>${txt}</span>
            </div>
            <div style="height: 20px; text-align: center; font-size: 0.75em;">${lbl}</div>
        </div>
    `
}

function drawdountChart(ele, h, width) {
    let data = donutData[+ele.getAttribute('xdata')]
    let total = data.reduce((s, p) => s + p.value, 0)
    if (total != 100) data.push({
        value: 100 - total,
        color: '#eee'
    })

    var chart = document.createElement("canvas");
    chart.width = chart.height = h;
    var canvas = chart.getContext("2d");
    ele.appendChild(chart);

    this.x, this.y, this.radius, this.lineWidth, this.strockStyle, this.from, this.to = null;
    this.x = this.y = h / 2;
    this.radius = (h - (width * 2)) / 2;
    this.from = 0;
    this.to = Math.PI * 2;
    this.lineWidth = width;
    this.strockStyle = "#fff";

    canvas.beginPath();
    canvas.lineWidth = this.lineWidth;
    canvas.strokeStyle = this.strockStyle;
    canvas.arc(this.x, this.y, this.radius, this.from, this.to);
    canvas.stroke();
    var df = 0;
    for (var i = 0; i < data.length; i++) {
        canvas.beginPath();
        canvas.strokeStyle = data[i].color;
        canvas.arc(this.x, this.y, this.radius, df, df + (Math.PI * 2) * (data[i].value / 100));
        canvas.stroke();
        df += (Math.PI * 2) * (data[i].value / 100);
    }

}

let donutData = []
function getDonut(h, total, filled, lbl, color) {
    let data = [{ value: 100 * (filled / total), color: color }], key = donutData.length
    donutData.push(data)
    return `
        <div>
            <div xdata="${key}" style="display: flex; justify-content: center">
                <img src onerror="drawdountChart(this.parentElement, ${h - 20}, 10)">
            </div>
            <div style="text-align: center; font-size: 0.75em;">${lbl}</div>
        </div>
    `
}

function getColor(v) {
    var h = (1.0 - v) * 240
    return "hsl(" + h + ", 100%, 50%)";
}

function getStatusBar(lbl, fill, total, filllbl, totallbl, lblstyle) {
    return `
        <div class="status-bar">
            <div class='status-bar-lbl'>
                <small style='${lblstyle}'>${lbl}</small>
            </div>
            <div class="status-bar-box" title="${totallbl}">
                <div style="width: ${(fill / total) * 100}%; background: ${getColor(fill / total)};" class="status-bar-filled" title="${filllbl}"></div>
            </div>
        </div>
    `
}

function diskUsage(n) {
    let t = n.profile.diskTotal, f = t - n.state.diskAvailable;
    return getStatusBar(`Disk (${gb(f)}GB / ${gb(t)}GB)`, f, t, `${((f * 100 / t).toFixed(1))}%`, `total (${gb(t)} GB)`, 'font-weight: bold')
}

function gb(x) { return (x / (1024 * 1024 * 1024)).toFixed(1) }

function memUsage(n) {
    let x = n => (n / (1024 * 1024 * 1024)).toFixed(1), t = n.profile.memorySize, f = n.state.memoryAvailable;
    return getStatusBar(`Mem (${gb(t - f)}GB / ${gb(t)}GB)`, t - f, t, `used (${x(t - f)}GB)`, `total (${x(t)} GB)`, 'font-weight: bold')
}

function getCpuUsage(n) {
    let t = 100, f = n.state.cpuPercent, p = (f * 100 / t).toFixed(1);
    return `
        <div>
            ${getStatusBar(`Cpu (${n.profile.cores} cores, ${p}%)`, f, t, `used (${p}%)`, ``, 'font-weight: bold')}
        </div>
    `
}

function getTaskStats(n) {
    return `
        <div style='margin: 5px 0px; display: flex'>
            <small style="flex-grow: 1">Tasks: <strong>${n.taskStat ? n.taskStat.cnt : 0}</strong></small>
            <small style="flex-grow: 1">Blocks: <strong>${n.state.blocks}</strong></small>
            <small>Last heartbeat: <strong>${(((+new Date()) - n.lastHeartbeat) / 1000).toFixed(0)}s ago</strong></small>
        </div>
    `
}

function getSummary(nodes, jobs) {
    let reducer = x => Object.values(nodes).reduce((s, t) => s + x(t), 0)
    let totMem = reducer(n => n.profile.memorySize), usedMem = totMem - reducer(n => n.state.memoryAvailable)
    let totDisk = reducer(n => n.profile.diskTotal), usedDisk = totDisk - reducer(n => n.state.diskAvailable)
    let totCpu = 100, usedCpu = reducer(n => n.state.cpuPercent) / Object.values(nodes).length, totCores = reducer(n => n.profile.cores)
    let totJobs = Object.values(jobs).length, totBlocks = reducer(n => n.state.blocks)
    let activeJobs = Object.values(jobs).filter(j => ['RUNNING', 'QUEUED', 'ACCEPTED'].includes(j.state)).length

    return `
        <div style="display: flex; margin: 30px 0px 30px 0px">
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, Object.keys(nodes).length, 'Node(s)')}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totMem, usedMem, `Memory (total: ${gb(totMem)}GB)`, getColor(.8), getColor(usedMem / totMem))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totCpu, usedCpu, `CPU used (${totCores} cores)`, getColor(usedCpu / totCpu))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totDisk, usedDisk, `Disk used (${gb(totDisk)}GB)`, getColor(usedDisk / totDisk))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, activeJobs, `Active jobs (total: ${totJobs})`)}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, totBlocks, 'SDFS Blocks')}</div>
        </div>
    `
}
