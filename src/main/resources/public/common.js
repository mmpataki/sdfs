let baseUrl = "";//http://inedctst01:5002"
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
    return getStatusBar(`Disk (${gb(f)}GB / ${gb(t)}GB)`, f, t, `${tof((f * 100 / t))}%`, `total (${gb(t)} GB)`, 'font-weight: bold')
}

function gb(x) { return ((x || 0) / (1024 * 1024 * 1024)).toFixed(1) }
function tof(x) { return (x || 0).toFixed(1) }

function memUsage(n) {
    let x = n => tof(n / (1024 * 1024 * 1024)), t = n.profile.memorySize, f = n.state.memoryAvailable;
    return getStatusBar(`Mem (${gb(t - f)}GB / ${gb(t)}GB)`, t - f, t, `used (${x(t - f)}GB)`, `total (${x(t)} GB)`, 'font-weight: bold')
}

function getCpuUsage(n) {
    let t = 100, f = n.state.cpuPercent, p = tof(f * 100 / t);
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
            <small style="flex-grow: 1">Blocks: <strong>${n.state.blocks || 0}</strong></small>
            <small>Last heartbeat: <strong>${tof(((+new Date()) - n.lastHeartbeat) / 1000)}s ago</strong></small>
        </div>
    `
}

function getSummary(nodes, jobs) {
    let reducer = x => Object.values(nodes).filter(n => n.state).reduce((s, t) => s + x(t), 0)
    let totMem = reducer(n => n.profile.memorySize), usedMem = totMem - reducer(n => n.state.memoryAvailable)
    let totDisk = reducer(n => n.profile.diskTotal), usedDisk = totDisk - reducer(n => n.state.diskAvailable)
    let totCpu = 100, usedCpu = reducer(n => n.state.cpuPercent) / Object.values(nodes).length, totCores = reducer(n => n.profile.cores)
    let totJobs = jobs.numJobs, totBlocks = reducer(n => n.state.blocks)
    let activeJobs = jobs.activeJobs
    let totNodes = Object.keys(nodes).length, activeNodes = Object.values(nodes).filter(n => n.active).length

    return `
        <div style="display: flex; margin: 30px 0px 30px 0px">
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, activeNodes, `Active Nodes (total: ${totNodes})`)}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totMem, usedMem, `Memory (total: ${gb(totMem)}GB)`, getColor(.8), getColor(usedMem / totMem))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totCpu, usedCpu, `CPU used (${totCores} cores)`, getColor(usedCpu / totCpu))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getDonut(100, totDisk, usedDisk, `Disk used (${gb(totDisk)}GB)`, getColor(usedDisk / totDisk))}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, activeJobs, `Active jobs (total: ${totJobs})`)}</div>
            <div style="flex-grow: 1; justify-content: center; display: flex">${getTextBlock(100, totBlocks, 'SDFS Blocks')}</div>
        </div>
    `
}

let from = 0, size = 20
function getPages(numJobs) {
    let html = `<div style='width: 80%; margin: auto; margin-top: 20px'>`
    for (i = 0, k = 0; i < numJobs; i += size, k++) {
        if (k % 20 == 0) {
            html += (k != 0 ? '</div>' : '') + "<div style='display: flex; justify-content: center'>"
        }
        html += `<span class='page-item ${i == from ? 'page-item-active' : ''}' onclick='load(from = ${i})'>${i / size}</span>`
    }
    return html + '</div></div>'
}


function getTask(t) {
    return `
        <tr style="margin: 5px">
            <th style="padding-left: 20px">
                ${t.taskLabel}
                ${t.nodeDef ?
            `<small><a style="margin-left: 10px" target="_blank" href="http://${t.nodeDef.addr.hostname}:${t.nodeDef.addr.infoPort}/api/task/logs?taskid=${t.taskId}">logs↗</a></small>` :
            ``
        }
                ${t.nodeDef ?
            `<small><a style="margin-left: 10px" href="./node.html?id=${t.nodeDef.addr.id}">node↗</a></small>` :
            ``
        }
            </th>
            <td></td>
            <td>${getStateLbl(t.state)}</td>
            <td>
                ${t.stateChanges.length ? new Date(t.stateChanges[0].second).toISOString() : ''}
            </td>
            <td>
                ${t.stateChanges.length ? new Date(t.stateChanges[t.stateChanges.length - 1].second).toISOString() : ''}
            </td>
            <td style="min-width: 100px">
                ${getStateChange(t.stateChanges)}
            </td>
        </tr>
    `
}

function setupPopup(back) {
    back.children[0].remove()
    let ele = back.children[0]
    let par = back.parentElement;

    par.addEventListener('click', function (ev) {
        ev.stopPropagation();
        let e = getOffset(par);
        back.style.display = 'block'
        ele.style.left = `${e.left - ele.offsetWidth + par.offsetWidth}px`;
        ele.style.top = `${e.top + 10}px`;
    })
    back.addEventListener('click', function (e) {
        e.stopPropagation();
        back.style.display = 'none'
    })

    document.body.appendChild(back)
}

function getOffset(el) {
    const rect = el.getBoundingClientRect();
    return {
        left: rect.left + window.scrollX,
        top: rect.top + window.scrollY
    };
}

function makePopup(html) {
    return `
        <div style='width: 100%; height: 100%; position: absolute; top: 0px; left: 0px; display: none'>
            <img src onerror="setupPopup(this.parentElement)">
            <div style="display: flex; flex-direction: column; background: white; border: solid 1px gray; padding: 20px; position: absolute">
                ${html}
            </div>
        </div>
    `
}

function getGantt(arr, colmap) {
    let out = [], tot = arr[arr.length - 1].start - arr[0].start
    for (let i = 0; i < arr.length - 1; i++) {
        let tim = (arr[i + 1].start - arr[i].start), tf = (tim / 1000).toFixed(2)
        out.push({ tag: arr[i].lbl, lbl: arr[i].lbl, tim: tf, start: arr[i].base, percent: (tim * 100) / tot })
    }
    console.log(out)
    return `
        <div>
            <div class="status-bar-box" style="display: flex">
                ${out.map(o => {
        return `
                        <div style="width: ${o.percent}%; background: ${colmap[o.tag]};" class="status-bar-filled" title="${o.lbl}"></div>
                    `
    }).join('\n')}
            </div>
            ${makePopup(
        arr.map(o => {
            return `
                        <div style="display: flex; margin: 5px 0px">
                            <div style="width: 50px; height: 6px; background: ${colmap[o.lbl]}; margin-right: 20px" class="status-bar-filled"></div>
                            <small>${o.lbl} (start: ${new Date(o.base).toISOString()})</small>
                        </div>
                    `
        }).join('\n')
    )} 
        </div>
    `
}

function getStateChange(states) {
    if (!states.length) return ''
    states = states.map(s => ({ lbl: s.first, base: s.second, start: s.second - states[0].second }))
    if (states[states.length - 1].lbl.endsWith("ING")) {
        let now = +(new Date())
        states.push({ lbl: 'Now', base: now, start: now - states[states.length - 2].start })
    }
    return getGantt(states, {
        ACCEPTED: 'skyblue',
        QUEUED: 'blue',
        RUNNING: 'orange',
        FAILED: 'red',
        SUCCEEDED: 'green',
        PICKED: 'yellow',
    })
}

function getStateLbl(state) {
    if (!state) return ''
    let cols = {
        succeeded: ['#E8F5E9', '#1B5E20'],
        failed: ['#FFEBEE', '#B71C1C'],
        running: ['#FFF3E0', '#E65100'],
        aborted: ['#FCE4EC', '#880E4F'],
        queued: ['#E3F2FD', '#0D47A1'],
        assigned: ['hotred', 'hotpink'],
        picked: ['purple', 'yellow']
    }
    state = state.toLowerCase()
    console.log(state)
    return `
        <span style="background: ${cols[state][0]}; color: ${cols[state][1]}; padding: 1px 3px; border-radius: 3px">
            ${state}
        </span>
    `
}

function getJob(job) {
    return `
        <tr>
            <th>
                ${job.jobId}
            </th>
            <td style="white-space: pre-wrap; word-break: break-all">${job.jobLabel}</td>
            <td class="nowrap">${getStateLbl(job.state)}</td>
            <td class="nowrap">
                ${new Date(job.stateChanges[0].second).toISOString()}
            </td>
            <td class="nowrap">
                ${new Date(job.stateChanges[job.stateChanges.length - 1].second).toISOString()}
            </td>
            <td style="min-width: 100px">
                ${getStateChange(job.stateChanges)}
            </td>
        </tr>
        <tr><td style="padding-left: 10px"><small><strong>Tasks</strong></small></td></tr>
        ${Object.values(job.taskStates).map(t => getTask(t)).join('\n')}
        <tr><td style="padding: 10px"></td></tr>
    `
}

function getJobs(jobs) {
    return `
        <div style='margin: 0px 0px 0px 0px'>
            <table style="width: 100%; text-align: left">
                <tr style='background: #e0e0e0'>
                    <th>Job Id</th>
                    <th>Label</th>
                    <th>State</th>
                    <th>Accepted</th>
                    <th>Last update</th>
                    <th>Timeline <small>Click</small></th>
                </tr>
                ${Object.values(jobs).sort((b, a) => a.stateChanges[0].second - b.stateChanges[0].second).map(job => getJob(job)).join('\n')}
            </table>
        </div>
    `
}

function applyFilter(page) {
    let elems = document.getElementsByClassName('jobs-filter-item'), sfilter = '',
        searchstr = document.getElementById('job-search-input').value
    for (let i = 0; i < elems.length; i++) {
        if (elems[i].checked)
            sfilter += (sfilter == '' ? '' : ',') + elems[i].id;
    }
    let url = page + (page.includes('?') ? '' : '?')
    if (sfilter != '')
        url += `&states=${sfilter}`
    if (searchstr)
        url += `&q=${searchstr}`
    window.location.href = url
}

function getJobFilters(m, page) {
    let jobStates = ['ACCEPTED', 'QUEUED', 'RUNNING', 'FAILED', 'SUCCEEDED', 'ABORTED']
    return `
        <div style='display: flex'>
            <span style='flex-grow: 1'></span>
            <small style='display: flex; align-items: center'>
                Filters: 
                ${jobStates.map(js => `<label><input oninput='applyFilter("${page}")' type='checkbox' ${(m.states || '').includes(js) ? 'checked="checked"' : ''} id='${js}' class='jobs-filter-item'>${js}</label>`).join('\n')}
                search: <input onchange='applyFilter("${page}")' id='job-search-input' value='${m.q || ''}'>
            </small>
        </div>
    `
}
