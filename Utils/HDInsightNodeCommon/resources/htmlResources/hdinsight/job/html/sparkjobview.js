var localhost = "http://localhost:39128/clusters/";

$(function () {
    $('#jobGraphDiv').hide();
    $('#myTab li:eq(0)').hide();
    $('#myTab li:eq(4) a').tab('show');
    $('#jobGraphLink').on('shown.bs.tab', function() {
        $('#jobGraphDiv').show();
        renderJobGraph();
    });
    $('#jobGraphLink').on('hidden.bs.tab', function() {
        $('#jobGraphDiv').hide();
    });

    // renderJobSummary(["1","2","3","4"],"#job-timeline g");
    //
    //
    // renderTaskSummary(testData);
    getProjectId();
    $("#leftDiv").scrollTop($("#leftDiv")[0].scrollHeight);
    $("#JobHistoryTbody").on('click', 'tr', function () {
        $("#errorMessage").text("");
        $("#jobOutput").text("");
        $("#livyJobLog").text("");
        $("#sparkDriverLog").text("");
        var rows = $("#JobHistoryTbody tr");
        rows.removeClass('selected-hight');
        $(this).addClass('selected-hight');

        appId = $(this).find('td:eq(1)').text();

        localStorage.setItem("selectedAppID", appId);

        if (appId == null) {
            return;
        }

        setBasicInformation();
        setAMcontainer();
        setDiagnosticsLog();
        setLivyLog();
        setJobTimeLine();
        setJobDetail();

        //renderJobDetails();
    });

    $("#openSparkUIButton").click(function () {
        var id = typeof appId == 'undefined' ? "" : appId.toString();
        if (id != "") {
            var application = $.grep(applicationList, function (e) {
                return e.id == id;
            });
            if (application != null && application.length == 1) {
                var currentAttemptId = application[0].attempts[0].attemptId;
                if (currentAttemptId != null) {
                    id = id + "/" + currentAttemptId;
                }
            }
        }
        JobUtils.openSparkUIHistory(id);
    });

    $("#openYarnUIButton").click(function () {
        JobUtils.openYarnUIHistory(typeof appId == 'undefined' ? "" : appId.toString());
    });

    $("#refreshButtion").click(function () {
        location.reload();
        refreshGetSelectedApplication();
    });

    getJobHistory();
});

function reloadTableStyle() {
    $("table tbody tr:nth-child(odd)").addClass("odd-row");
    /* For cell text alignment */
    $("table tbody td:first-child, table th:first-child").addClass("first");
    /* For removing the last border */
    $("table tbody td:last-child, table th:last-child").addClass("last");
}

function getProjectId() {
    queriresMap = {};
    var urlinfo = window.location.href;
    var len = urlinfo.length;
    var offset = urlinfo.indexOf("?");

    var additionalInfo = urlinfo.substr(offset + 1, len);
    var infos = additionalInfo.split("&");
    for (var i = 0; i < infos.length; ++i) {
        strs = infos[i].split("=");
        queriresMap[strs[0]] = strs[1];
    }
    projectId = queriresMap["projectid"];

    var webType = queriresMap["engintype"];
    console.log('Project id:' + projectId);
}

var asyncMessageCounter = 0;

function getMessageAsync(url, callback) {
    ++asyncMessageCounter;
    console.log("http request for " + url);
    $('body').css("cursor", "progress");
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.timeout = 60 * 1000;
    xmlHttp.ontimeout = function () {
        if (--asyncMessageCounter == 0) {
            $('body').css("cursor", "default");
        }
    };

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState == 4) {
            if (xmlHttp.status == 200 || xmlHttp.status == 201) {
                var s = xmlHttp.responseText;
                if (s == "") {
                    return;
                }
                callback(s);
                if (--asyncMessageCounter == 0) {
                    $('body').css("cursor", "default");
                }
            }
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function refreshGetSelectedApplication() {
    var selectedAppid = localStorage.getItem("selectedAppID");
    if (selectedAppid == null) {
        return;
    }

    var tableRow = $("#myTable tbody tr").filter(function () {
        return $(this).children("td:eq(2)").text() == selectedAppid;
    }).closest("tr");

    if (tableRow.size() != 0) {
        tableRow.click();
    }
}

function getJobHistory() {
    console.log('get all spark history from cluster');
    getMessageAsync(localhost + projectId + "/applications/", function (s) {
        writeToTable(s);
        refreshGetSelectedApplication();
    });
}

function setBasicInformation() {
    console.log("set Basic Information");
    getMessageAsync(localhost + projectId + "/applications/" + appId, function (s) {
        var application = JSON.parse(s);
        jobBasicInformation = application;
        attemptId = jobBasicInformation.attempts[0].attemptId;
        $("#startTime").text(application.attempts[0].startTime);
        $("#endTime").text(application.attempts[0].endTime);

        if (appId.substr(0, 5) != "local") {
            getJobResult();
            getSparkDriverLog();
        }
    });
}

function setMessageForLable(str) {
    var ss = document.getElementById("demo");
    ss.innerHTML = str;
}

function writeToTable(message) {
    applicationList = JSON.parse(message);
    $('#myTable tbody').html("");
    d3.select("#myTable tbody")
        .selectAll('tr')
        .data(applicationList)
        .enter()
        .append('tr')
        .attr("align","center")
        .attr("class","ui-widget-content")
        .selectAll('td')
        .data(function(d) {
            return appInformationList(d);
        })
        .enter()
        .append('td')
        .attr('class',"ui-widget-content")
        .attr('id',function(d,i) {
            return i;
        })
        .html(function(d, i) {
            return d;
        });
}

function appInformationList(app) {
    var lists = [];
    var status = app.attempts[app.attempts.length - 1].completed;
    lists.push(getTheJobStatusImgLabel(status));
    lists.push(app.id);
    lists.push(app.name);
    lists.push(app.attempts[0].startTime);
    lists.push(app.attempts.length);
    lists.push(app.attempts[0].sparkUser);
    return lists;
}

function getTheJobStatusImgLabel(str) {
    if (str == true) {
        return "<img src=\"resources/icons/Success.png\">";
    } else {
        return "<img src=\"resources/icons/Error.png\">";
    }
}

function setAMcontainer() {
    console.log("set AM Container for Application:" + appId);

    if (appId.substr(0, 5) == "local") {
        $("#containerNumber").text("Local Task");
    } else {
        getMessageAsync(localhost + projectId + "/cluster/apps/" + appId + "/appattempts?restType=yarn", function (str) {
            var object = JSON.parse(str);
            containerId = object.appAttempts.appAttempt[0].containerId;
            nodeId = object.appAttempts.appAttempt[0].nodeId;
            console.log("current job Container ID:" + containerId);
            $("#containerNumber").text(containerId);

            if (appId.substr(0, 5) != "local") {
                getJobResult();
                getSparkDriverLog();
            }
        });
    }
}

function setDiagnosticsLog() {
    if (appId.substr(0, 5) == "local") {
        $("#errorMessage").text("No Yarn Error Message");
    } else {
        getMessageAsync(localhost + projectId + "/cluster/apps/" + appId + "?restType=yarn", function (s) {
            var object = JSON.parse(s);
            var message = object.app.diagnostics;
            if (message == 'undefined' || message == "") {
                message = "No Error Message";
            }
            $("#errorMessage").text(message);
        });
    }
}

// stdout : https://spark-linux.azurehdinsight.net/yarnui/jobhistory
// /logs/10.0.0.10/port/30050/container_e01_1462807780116_0004_01_000001/container_e01_1462807780116_0004_01_000001/spark/stdout/?start=0

function getSparkDriverLog() {
    if (typeof attemptId == 'undefined' || typeof containerId == 'undefined') {
        return;
    }
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
        console.log(appId + " job driver inner ip address : " + ipAddress);
        var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/spark/stderr?restType=yarnhistory";
        getResultFromSparkHistory(url, function (s) {
            $("#sparkDriverLog").text(s);
        });
    });
}

function getJobResult() {
    if (typeof attemptId == 'undefined' || typeof containerId == 'undefined') {
        return;
    }

    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
        console.log(appId + " job driver inner ip address : " + ipAddress);
        var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/spark/stdout?restType=yarnhistory";
        getResultFromSparkHistory(url, function (s) {
            if (s == "") {
                s = "No out put";
            }
            $("#jobOutput").text(s);
        });
    });
}

function getResultFromSparkHistory(url, callback) {
    getMessageAsync(url, function (s) {
        callback(s);
    });
}

function getDriverPortFromExecutor(executorsObject) {
    for (i = 0; i < executorsObject.length; ++i) {
        if (executorsObject[i].id == "driver") {
            return executorsObject[i].hostPort;
        }
    }
}

function setLivyLog() {
    getMessageAsync(localhost + projectId + "/?restType=livy&applicationId=" + appId, function (s) {
        $("#livyJobLog").text(s);
    });
}
function setJobDetail() {
    var selectedApp = applicationList.find(function(d) {
        return d.id == appId;
    });
    var maxAttempt = selectedApp.attempts.length;
    if(selectedApp.attempts[maxAttempt - 1].sparkUser == 'hive') {
        return;
    }
    var url = localhost + projectId + "/applications/" + appId + "/" +　maxAttempt ;
    getMessageAsync(url + "/jobs", function (s) {
        var jobs = JSON.parse(s);

        renderJobDetails(jobs);
    });
}
function stagesInfo(jobs, url) {
        getMessageAsync(url + "/stages", function (s) {
            var data = new Object();
            var stages = JSON.parse(s);
            data.jobs = jobs;
            data.stages = stages;
            data.stageDetails = [];
            jobs.stageIds.forEach(function(stageNumber) {
                getMessageAsync(url + "/stages" + "/" + stageNumber, function(s) {
                    var detail = JSON.parse(s);
                    data.stageDetails.push(detail);
                });
            });
        });
}

function setJobTimeLine() {
    var url = localhost + projectId + "/cluster/apps/" + appId + "?restType=yarn";
    getMessageAsync(url, function(s) {
        var t = s;
    });
}

function setJobGraph() {
    getMessageAsync(localhost + projectId + "/applications/" + appId + "jobs", function (s) {
        var application = JSON.parse(s);
        jobBasicInformation = application;
        attemptId = jobBasicInformation.attempts[0].attemptId;
        $("#startTime").text(application.attempts[0].startTime);
        $("#endTime").text(application.attempts[0].endTime);

        if (appId.substr(0, 5) != "local") {
            getJobResult();
            getSparkDriverLog();
        }
    });
}