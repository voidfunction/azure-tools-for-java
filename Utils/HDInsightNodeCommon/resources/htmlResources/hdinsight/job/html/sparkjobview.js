var localhost = "http://localhost:39128/clusters/";

$(function () {
    $('#jobGraphDiv').hide();
    // hide the error messagae tab first
    // $('#myTab li:eq(0)').hide();
    // show the job output tab
    $('#myTab li:eq(4) a').tab('show');
    $('#jobGraphLink').on('shown.bs.tab', function() {
        $('#jobGraphDiv').show();
        renderJobGraph();
    });
    $('#jobGraphLink').on('hidden.bs.tab', function() {
        $('#jobGraphDiv').hide();
    });

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

        //get Application Id
        appId = $(this).find('td:eq(1)').text();
        // get last attempt
        attemptId = $(this).find('td:eq(4)').text();
        applicationName = $(this).find('td:eq(2)').text();
        $("#jobName").text("Job Name: " + applicationName);

        if (appId == null) {
            return;
        }
        // save current Application ID to LocalStorage
        localStorage.setItem("selectedAppID", appId);
        setBasicInformation();
        setAMcontainer();
        setDiagnosticsLog();
        setLivyLog();
        setDebugInfo("end livy log");
        setJobDetail();
        setStoredRDD();
        setStageDetails();
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

function getJobHistory() {
    getMessageAsync(localhost + projectId + "/applications/", function (s) {
        writeToTable(s);
        refreshGetSelectedApplication();
        $("#JobHistoryTbody tr:eq(0)").click();
    });
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
}

function refreshGetSelectedApplication() {
    var selectedAppid = localStorage.getItem("selectedAppID");
    if (selectedAppid == null) {
        return;
    }

    var tableRow = $("#myTable tbody tr").filter(function () {
        return $(this).children('td:eq(1)').text() == selectedAppid;
    }).closest("tr");

    // if (tableRow.size() != 0) {
    //     tableRow.click();
    // }
}


function getFirstAttempt(attempts) {
    return findElement(attempts, function (a) {
        return typeof a.attemptId == 'undefined' || a.attemptId == 1;
    });
}
function getLastAttempt(attempts) {
    return findElement(attempts, function (a) {
        return typeof a.attemptId == 'undefined' || a.attemptId == attemptId;
    });
}

function setBasicInformation() {
    getMessageAsync(localhost + projectId + "/applications/" + appId, function (s) {
        var application = JSON.parse(s);
        $("#startTime").text(getFirstAttempt(application.attempts).startTime);
        $("#endTime").text(getLastAttempt(application.attempts).endTime);

        if (appId.substr(0, 5) != "local" && attemptId != 0) {
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
    if(app.attempts.length == 1 && typeof app.attempts[0].attemptId == 'undefined') {
        lists.push(0);
    } else {
        lists.push(app.attempts.length);
    }
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
    if (appId.substr(0, 5) == "local") {
        $("#containerNumber").text("Local Task");
    } else {
        getMessageAsync(localhost + projectId + "/cluster/apps/" + appId + "/appattempts?restType=yarn", function (str) {
            var object = JSON.parse(str);
            containerId = object.appAttempts.appAttempt[0].containerId;
            nodeId = object.appAttempts.appAttempt[0].nodeId;
            $("#containerNumber").text(containerId);
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
    if (attemptId == 0 || typeof containerId == 'undefined') {
        return;
    }
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
        var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/spark/stderr?restType=yarnhistory";
        getResultFromSparkHistory(url, function (s) {
            $("#sparkDriverLog").text(s);
        });
    });
}

function getJobResult() {
    if (attemptId == 0 || typeof containerId == 'undefined') {
        return;
    }

    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
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
    var selectedApp = findElement(applicationList, function (d) {
       return d.id == appId;
    });
    if(typeof selectedApp == 'undefined') {
        return;
    }
    setDebugInfo("selectApp " + appId);
    if(selectedApp.attempts[0].sparkUser == 'hive') {
        return;
    }
    var url = localhost + projectId + "/applications/" + appId + "/" +　attemptId ;
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

///applications/[app-id]/storage/rdd
function setStoredRDD() {
    if(attemptId == 0) {
        renderStoredRDD('');
        return;
    }
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/storage/rdd",function(s) {
        var rdds = JSON.parse(s);
        renderStoredRDD(rdds);
    });
}

function setStageDetails() {
    if(attemptId == 0) {
        $("#stage_detail_info_message").text("No Stage Info");
        return;
    }
    $("#stage_detail_info_message").text('');
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/stages", function (s) {
        var stages = JSON.parse(s);
        renderStagesDetails(stages);
        stages.forEach(function(stage) {
            id = stage.stageId;
            setTaskDetails(id);
        });
    });
}

function setTaskDetails(stageId) {
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/" + "stages/" + stageId + "/0/taskList",function(s){
        var tasks = JSON.parse(s);
        renderTaskSummary(tasks);
    });
}

function setDebugInfo(s) {
    $("#debuginfo").text(s);
}