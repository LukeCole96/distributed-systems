document.getElementById('path').addEventListener('change', function () {
    const selectedPath = this.value;
    if (selectedPath.includes('{param}')) {
        document.getElementById('paramInput').style.display = 'block';
    } else {
        document.getElementById('paramInput').style.display = 'none';
    }
});

document.getElementById('method').addEventListener('change', function () {
    const method = this.value;
    if (method === 'GET') {
        document.getElementById('bodyContainer').style.display = 'none';
    } else {
        document.getElementById('bodyContainer').style.display = 'block';
    }
});

document.getElementById('host').addEventListener('change', function () {
    const selectedHost = this.value;
    let headers = {
        "Content-Type": "application/json"
    };

    if (selectedHost === 'http://localhost:90') {
        headers["Authorization"] = "Basic Y3JzOmNyc3Bhc3M=";
    } else {
        headers["Authorization"] = "Basic Y21zOmNtc3Bhc3M=";
    }

    document.getElementById('headers').value = JSON.stringify(headers, null, 2);
});

let requestStatusList = [];

function saveState() {
    localStorage.setItem('host', document.getElementById('host').value);
    localStorage.setItem('path', document.getElementById('path').value);
    localStorage.setItem('method', document.getElementById('method').value);
    localStorage.setItem('headers', document.getElementById('headers').value);
    localStorage.setItem('body', document.getElementById('body').value);
}

document.getElementById('apiForm').addEventListener('submit', async function (event) {
    event.preventDefault();
    const host = document.getElementById('host').value;
    let path = document.getElementById('path').value;
    const method = document.getElementById('method').value;
    const bodyInput = document.getElementById('body').value;

    if (path.includes('{param}')) {
        const paramValue = document.getElementById('pathParam').value;
        if (!paramValue) {
            alert('Please enter a value for {param}');
            return;
        }
        path = path.replace('{param}', encodeURIComponent(paramValue));
    }

    const url = `${host}${path}`;
    const body = (method === 'POST' && bodyInput) ? JSON.parse(bodyInput) : null;
    const headers = JSON.parse(document.getElementById('headers').value);
    const startTime = performance.now();

    try {
        const response = await fetch(url, {
            method: method,
            headers: headers,
            body: method === 'POST' ? JSON.stringify(body) : null 
        });

        const duration = (performance.now() - startTime).toFixed(2);
        let responseBody;

        const responseHeaders = [...response.headers.entries()]
            .map(([key, value]) => `${key}: ${value}`)
            .join('\n');

        const headersOutput = `Response Headers:\n${responseHeaders}\n\n`;

        const contentType = response.headers.get('Content-Type');
        if (contentType && contentType.includes('application/json')) {
            responseBody = await response.json();
            responseBody = JSON.stringify(responseBody, null, 2);
        } else {
            responseBody = await response.text();
        }

        const statusMessage = `Request to ${url} took ${duration}ms - Status: ${response.status} ${response.statusText}`;
        addStatusToList(statusMessage);

        document.getElementById('responseOutput').textContent = headersOutput + responseBody;

    } catch (error) {
        const duration = (performance.now() - startTime).toFixed(2);
        addStatusToList(`Error after ${duration}ms: ${error.message}`);
        document.getElementById('responseOutput').textContent = 'Error: ' + error.message;
    }
});

function addStatusToList(status) {
    requestStatusList.unshift(status);
    if (requestStatusList.length > 10) {
        requestStatusList.pop();
    }

    const statusListElement = document.getElementById('statusList');
    statusListElement.innerHTML = '';

    requestStatusList.forEach(statusMessage => {
        const listItem = document.createElement('li');
        listItem.textContent = statusMessage;
        statusListElement.appendChild(listItem);
    });

    localStorage.setItem('statusList', JSON.stringify(requestStatusList));
}

let allLogs = [];

async function fetchDowntimeLogs(showAll = false) {
    const url = 'http://localhost:90/get-downtime-logs';
    const headers = {
        "Authorization": "Basic Y3JzOmNyc3Bhc3M="
    };

    try {
        const response = await fetch(url, { headers: headers });
        if (!response.ok) throw new Error('Failed to fetch downtime logs');

        const downtimeLogs = await response.json();
        allLogs = downtimeLogs;
        renderDowntimeLogs(showAll ? allLogs : allLogs.slice(-30));
    } catch (error) {
        console.error('Error fetching downtime logs:', error);
    }
}

function renderDowntimeLogs(logs) {
    const tableBody = document.querySelector('#downtimeTable tbody');
    tableBody.innerHTML = '';

    logs.forEach(log => {
        if (log.id && log.downtimeTimestamp) {
            const row = `<tr><td>${log.id}</td><td>${log.downtimeTimestamp}</td></tr>`;
            tableBody.insertAdjacentHTML('beforeend', row);
        }
    });
}

document.getElementById('showAllButton').addEventListener('click', function () {
    fetchDowntimeLogs(true);
    document.getElementById('showAllButton').style.display = 'none';
    document.getElementById('showRecentButton').style.display = 'inline';
});

document.getElementById('showRecentButton').addEventListener('click', function () {
    fetchDowntimeLogs(false);
    document.getElementById('showRecentButton').style.display = 'none';
    document.getElementById('showAllButton').style.display = 'inline';
});

setInterval(fetchDowntimeLogs, 30000);

fetchDowntimeLogs();
