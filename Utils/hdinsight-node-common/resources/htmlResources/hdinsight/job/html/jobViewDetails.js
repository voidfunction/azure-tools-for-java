function renderJobSummary(times, svg_g_id) {
    var graph = new dagreD3.graphlib.Graph()
        .setGraph({
            nodesep: 20,
            ranksep: 20,
            rankdir: "LR",
            marginx: 10,
            marginy: 10})
        .setDefaultEdgeLabel(function() { return {}; });
    var i = 0;
    var nodes = ["Preparing", "Queued", "Running", "Finalizing"];
    nodes.forEach(function (node) {
        graph.setNode(node,{ style: "fill: #afa"});
    });
    graph.nodes().forEach(function(v) {
        var node = graph.node(v);
        node.rx = node.ry = 5;
    });
    i = 0;
    for(i=0;i<3;++i) {
        graph.setEdge(nodes[i],nodes[i+1],{label:times[i]});
    }

    var zoom = d3.behavior.zoom().on("zoom", function() {
        inner.attr("transform", "translate(" + d3.event.translate + ")" +
            "scale(" + d3.event.scale + ")");
    });

    var render = new dagreD3.render();
    var svg = d3.select("#job-timeline");
    var inner = d3.select(svg_g_id);

    /*
     render(svg,graph);
     var graph_width = graph.graph().width;
     var graph_height = graph.graph().height;

     var width = $("#job-timeline-div").width();
     var height = $("#job-timeline-div").height();
     var viewbox = "0 0 " + graph_width + " " + graph_height;
     */
    svg.attr("viewBox","0 0 405 64");
    render(svg,graph);
    renderStoredRDD(["a","b"]);
}

var jobDetailsColumn= ["Job Id", "Name", "Submission Time", "Job Status", "Task(s)", "Failed Task(s)", "Failed Stage(s)"];

var jobSummaryColumn = ['Jobs Number', 'Tasks Number', 'Completed Tasks', 'Failed Tasks', 'Skipped Tasks', 'Stages Number', 'Completed Stages', 'Failed Stages', 'Skipped Stages' ];
function renderJobDetails(myData) {
    var counter = 0;
    d3.select("#job-details-by-job").html("");
    d3.select("#job-details-info-table").html("");
    d3.select("#job-details-by-job").selectAll("li")
        .data(myData)
        .enter()
        .append("li")
        .attr("role","presentation")
        .append("a")
        .attr("role","menuitem")
        .attr("tabindex", -1)
        .text(function(d) {
            return "Job ID " + d.jobId;
        }).on('click',function(job,i) {
            d3.select("#summaryTitle").html("Job details");
            d3.select("#basicInformationTitle").html("Basic Job Information");
            d3.select("#job-details-info-table").html("");
            d3.select("#job-details-info-table")
                    .selectAll("tr")
                    .data(jobDetailsColumn)
                    .enter()
                    .append("tr")
                    .html(function(data, i) {
                        return "<td>" + data + "</td> <td>" + getJobDetailsValue(job, i)  + "</td>";
                    });
            filterStageTaskTableWithStageIds(job.stageIds);
        });
        var details = getJobSummaryValue(myData);
        d3.select("#job-details-info-table")
            .selectAll('tr')
            .data(jobSummaryColumn)
            .enter()
            .append('tr')
            .attr('align','center')
            .html(function(d, i) {
                return "<td align='center'>" + d + "</td> <td align='center'>" + details[i]+ "</td>";
            });
};

function getJobDetailsValue(job, i) {
    switch (i) {
        case 0:
            return job.jobId;
        case 1:
            return job.name;
        case 2:
            return job.submissionTime;
        case 3:
            return job.status;
        case 4:
            return job.numTasks;
        case 5:
            return job.numFailedTasks;
        case 6:
            return job.numFailedStages;
        default:
            return "Unknown";
    }
}

function getJobSummaryValue(jobs) {
    var details = [0, 0, 0, 0, 0, 0, 0, 0, 0];
    details[0] = jobs.length;
    jobs.forEach(function (j) {
        details[1] += j.numTasks;
        details[2] += j.numCompletedTasks;
        details[3] += j.numFailedTasks;
        details[4] += j.numSkippedTasks;

        details[5] += j.stageIds.length;
        details[6] += j.numCompletedStages;
        details[7] += j.numFailedStages;
        details[8] += j.numSkippedStages;
    });
    return details;
}

var storedRDDSummaryColumn = ["RDD Number", "diskUsed", "memoryUsed"];
var storedRDDDetailsColumn = ["name", "numPartitions", "numCachedPartitions","memoryUsed","diskUsed"];
function renderStoredRDD(myData) {
    var counter = 0;
    if(typeof myData == 'string' || myData.length == 0) {
        d3.select("#stored_rdd_details_div_message").text("No stored RDD");
        d3.select("#stored_rdd_info")
            .selectAll("tr").remove();
    } else {
        d3.select("#stored_rdd_details_div_message").text("");
        d3.select("#stored_rdd_info")
            .selectAll("tr").remove();
        d3.select("#stored_rdd_info")
            .selectAll("tr")
            .data(storedRDDSummaryColumn)
            .enter()
            .append("tr")
            .html(function(d, i) {
                if(i == 0) {
                    return "<td>"+ d + "</td><td>" + myData.length + "</td>";
                } else {
                    var sum = 0;
                    var v =  myData.forEach(function(data){
                        sum += data[d];
                    });
                    return "<td>"+ d + "</td><td>" + sum + "</td>";
                }
            });
        d3.select("#stored_rdd_details").selectAll("li")
            .data(myData)
            .enter()
            .append("li")
            .attr("role","presentation")
            .append("a")
            .attr("role","menuitem")
            .attr("tabindex", -1)
            .text(function(d) {
                ++counter;
                return "RDD " + d.id;
            }).on("click", function(d,i) {
                d3.select("#stored_rdd_info")
                    .selectAll("tr")
                    .remove();
            d3.select("#stored_rdd_info")
                .selectAll("tr")
                .data(storedRDDDetailsColumn)
                .enter()
                .append("tr")
                .html(function(inner) {
                    return "<td>"+ inner + "</td><td>" + d[inner] + "</td>";
                });
        });
    }
}

var stagesDetailsColumn = ["status","stageId","executorRunTime","inputBytes","outputBytes","shuffleReadBytes","shuffleWriteBytes"];
var summaryStagesColumn = ["executorRunTime","inputBytes","outputBytes","shuffleReadBytes","shuffleWriteBytes"];
function renderStageSummary(myData) {
    d3.select('#stageSummaryTbody')
        .selectAll('tr')
        .data(myData)
        .enter()
        .append('tr')
        .attr('align', 'center')
        .attr('class','ui-widget-content')
        .html(function(d) {
            return generateStageSummaryLine(d);
        });

}

function generateStageSummaryLine(task) {
    var html = '';
    stagesDetailsColumn.forEach(function(d) {
        html += '<td>'+ task[d] + '</td>';
    });
    return html;
}
function renderStagesDetails(myData) {
    d3.select("#stages_detail").selectAll("li")
        .data(myData)
        .enter()
        .append("li")
        .attr("role","presentation")
        .append("a")
        .attr("role","menuitem")
        .attr("tabindex", -1)
        .text(function(d) {
            return "Stage " + d.stageId;
        }).on("click", function(stage,i) {
            d3.selectAll("#stage_detail_info tr").remove();
            d3.select("#stage_detail_info")
                .selectAll("tr")
                .data(stagesDetailsColumn)
                .enter()
                .append("tr")
                .html(function(d) {
                    return "<td>" + d + "</td> <td>" + stage[d] + "</td>";
                });
    });
    d3.select("#stage_detail_info")
        .selectAll("tr")
        .remove();
    d3.select("#stage_detail_info")
        .selectAll("tr")
        .data(summaryStagesColumn)
        .enter()
        .append("tr")
        .html(function(d) {
            return "<td>" + d + "</td> <td>" + getAllStageInfo(myData, d) + "</td>";
        });
}
function getAllStageInfo(stages, item) {
    var total = 0;
    stages.forEach(function (i) {
        total += i[item];
    })
    return total;
}

function findStageItemById(myData, stageId) {
    myData.forEach(function (d) {
        if(d.stageId == stageId){
            return d;
        }
    });
    return '';
}

function renderJobGraph(myData) {
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });
    // Here we"re setting nodeclass, which is used by our custom drawNodes function
// below.
    g.setNode(0,  { label: "Driver",       class: "type-TOP" });
    g.setNode(1,  { label: "Job 1",         class: "type-S" });
    g.setNode(2,  { label: "Job 2",        class: "type-NP" });
    g.setNode(3,  { label: "Stage",        class: "type-DT" });
    g.setNode(4,  { label: "End",      class: "type-TK" });
    g.setNode(5,  { label: "Stage",        class: "type-VP" });
    g.setNode(6,  { label: "Stage",       class: "type-VBZ" });
    g.setNode(7,  { label: "End",        class: "type-TK" });
    g.setNode(8,  { label: "Stage",        class: "type-NP" });
    g.setNode(9,  { label: "Stage",        class: "type-DT" });
    g.setNode(10, { label: "End",        class: "type-TK" });
    g.setNode(11, { label: "Stage",        class: "type-NN" });
    g.setNode(12, { label: "End",   class: "type-TK" });
    g.setNode(13, { label: ".",         class: "type-." });
    g.setNode(14, { label: "End",  class: "type-TK" });

    g.nodes().forEach(function(v) {
        var node = g.node(v);
        // Round the corners of the nodes
        node.rx = node.ry = 5;
    });

// Set up edges, no special attributes.
    g.setEdge(3, 4);
    g.setEdge(2, 3);
    g.setEdge(1, 2);
    g.setEdge(6, 7);
    g.setEdge(5, 6);
    g.setEdge(9, 10);
    g.setEdge(8, 9);
    g.setEdge(11,12);
    g.setEdge(8, 11);
    g.setEdge(5, 8);
    g.setEdge(1, 5);
    g.setEdge(13,14);
    g.setEdge(1, 13);
    g.setEdge(0, 1)

// Create the renderer
    var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("#jobGraphSvg"),
        inner = svg.append("g");

// Run the renderer. This is what draws the final graph.
    render(d3.select("#jobGraphSvg g"), g);
    var g_width = g.graph().width + 50;
    var g_height = g.graph().height + 50;
    svg.attr("viewBox","0 0 " + g_width + " " + g_height);
    render(d3.select("#jobGraphSvg g"), g);
// Center the graph
    var width = $("#jobGraphSvg").width();
    var height = $("#jobGraphSvg").height();

    var zoom = d3.behavior.zoom().on("zoom", function() {
        inner.attr("transform", "translate(" + d3.event.translate + ")" +
            "scale(" + d3.event.scale + ")");
    });
    svg.call(zoom);

    // Simple function to style the tooltip for the given node.
    var styleTooltip = function(name, description) {
        return "<p class='name'>" + name + "</p><p class='description'>" + description + "</p>";
    };

    inner.selectAll("g.node")
        .attr("title", function(v) { return styleTooltip("abc", "def") })
        .each(function(v) { $(this).tipsy({ gravity: "w", opacity: 1, html: true }); });

// Center the graph
//         var initialScale = 0.75;
//         zoom
//             .translate([(svg.attr("width") - g.graph().width * initialScale) / 2, 20])
//             .scale(initialScale)
//             .event(svg);
//         svg.attr('height', g.graph().height * initialScale + 40);
    var graph = d3.select("#myTab li a[href='#jobGraphDiv']");
    graph.on("click",function(d) {

    });
}


function renderTaskSummary(myData) {
    d3.select('#taskSummaryTbody')
        .selectAll('tr')
        .data(myData)
        .enter()
        .append('tr')
        .attr('align', 'center')
        .attr('class','ui-widget-content')
        .html(function(d) {
            return generateTaskSummaryLine(d);
    });

}
function taskSummaryObjToList(myTaskSummary) {
    var lists = [];
    lists.push(myTaskSummary.taskId);
    lists.push(myTaskSummary.index);
    lists.push(myTaskSummary.attempt);
    lists.push(myTaskSummary.launchTime);
    lists.push(myTaskSummary.executorId);
    lists.push(myTaskSummary.host);
    lists.push(myTaskSummary.taskLocality);
    list.push(myTaskSummary.speculative);
    return lists;
}

var taskSummaryColumn = ['taskId','index','attempt','launchTime','executorId','host','taskLocality','speculative'];
function generateTaskSummaryLine(task) {
    return taskSummaryColumn.reduce(function(sumSoFar, d) {
                return sumSoFar + '<td>'+ task[d] + '</td>'
            }, '');
}

var executorSummaryColumn = ["id","hostPort", "rddBlocks", "memoryUsed","diskUsed","totalDuration", "totalInputBytes", "totalShuffleRead", "totalShuffleWrite","maxMemory"]
function renderExecutorsOnPage(myData) {
    d3.select("#executorDetailsBody")
        .selectAll('tr')
        .data(myData)
        .enter()
        .append('tr')
        .attr('align', 'center')
        .attr('class','ui-widget-content')
        .html(function(d){
            return generateExecutorDetailsLine(d);
        });
}

function generateExecutorDetailsLine(data) {
    var html = '';
    executorSummaryColumn.forEach(function(d) {
        html += '<td>' + data[d] + '</td>';
    });
    return html;
}