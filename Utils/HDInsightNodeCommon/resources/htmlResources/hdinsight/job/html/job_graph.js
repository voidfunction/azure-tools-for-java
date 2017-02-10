
function renderJobGraphOnApplicationLevel(jobs) {
    renderJobGraphOnApplicationLevelTest(jobs);
}

function renderJobGraphOnApplicationLevelTest(jobs) {
    g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });

    var counters = jobs.length, i = 0;
    g.setNode(0, {label :"Driver",   class : "type-TOP"});
    for(i =1; i <= counters; ++i) {
        var s = "Job " + i;
        var currentClass = jobs[i - 1]["status"] == "SUCCEEDED" ? "type-Success" : "type-Error";
        g.setNode(i, {label: s,  class : currentClass});
    }

    for(i = 1; i <= counters; ++i) {
        g.setEdge(0, i);
    }

    // Create the renderer
    var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("#jobGraphSvg");

    // remove all graph first
    d3.selectAll("#jobGraphSvg g").remove();

    var inner = svg.append("g");

// Run the renderer. This is what draws the final graph.
    render(d3.select("#jobGraphSvg g"), g);

    var g_width = g.graph().width ;
    var g_height = g.graph().height ;
    var viewBoxValue = "0 0 " + g_width + " " + g_height;
    svg.attr("viewBox", viewBoxValue);
    svg.attr("preserveAspectRatio", "xMidYMid meet");

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


    inner.selectAll("g.node")
        .attr("title", function(v) {
            return setToolTips(jobs, v)
        })
        .each(function(v) { $(this).tipsy({ gravity: "w", opacity: 1, html: true }); });

}

function setToolTips(jobs, v) {
    if(v != "0") {
        var counter = parseInt(v) - 1;
        var job = jobs[counter];
        return "<p class='name'>" + getJobStatusImage(job["status"])
                + "</p><p class='description'>" + job["name"] +
                "</p>" + "<p class='description'>" + getRunningTime(job) + "</p>";
    } else {

    }
}

function getRunningTime(job) {
    var startTime = job["submissionTime"],
        endTime = job["completionTime"];
    var startInSeconds = new Date(formatServerTime(startTime)).getTime(), endInSeconds = new Date(formatServerTime(endTime)).getTime();
    var times = (endInSeconds - startInSeconds)/(1000*60).toFixed(2).toString();
    return "Running Time: " + times + " mins";
}

function getJobStatusImage(status) {
    return status == "SUCCEEDED" ? "<img src='resources/icons/Success.png'>" : "<img src='resources/icons/Error.png'>"
}

function jobStatics(jobs) {
    var successJob = 0;
    jobs.forEach(function (job) {
        if(job["status"] == "SUCCEEDED")++successJob;
    });
    var isFailed = successJob == jobs.length;
    return
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
    // g.setNode(3,  { label: "Stage",        class: "type-DT" });
    // g.setNode(4,  { label: "End",      class: "type-TK" });
    // g.setNode(5,  { label: "Stage",        class: "type-VP" });
    // g.setNode(6,  { label: "Stage",       class: "type-VBZ" });
    // g.setNode(7,  { label: "End",        class: "type-TK" });
    // g.setNode(8,  { label: "Stage",        class: "type-NP" });
    // g.setNode(9,  { label: "Stage",        class: "type-DT" });
    // g.setNode(10, { label: "End",        class: "type-TK" });
    // g.setNode(11, { label: "Stage",        class: "type-NN" });
    // g.setNode(12, { label: "End",   class: "type-TK" });
    // g.setNode(13, { label: ".",         class: "type-." });
    // g.setNode(14, { label: "End",  class: "type-TK" });

    g.nodes().forEach(function(v) {
        var node = g.node(v);
        // Round the corners of the nodes
        node.rx = node.ry = 5;
    });

// Set up edges, no special attributes.
//     g.setEdge(3, 4);
//     g.setEdge(2, 3);
//     g.setEdge(1, 2);
//     g.setEdge(6, 7);
//     g.setEdge(5, 6);
//     g.setEdge(9, 10);
//     g.setEdge(8, 9);
//     g.setEdge(11,12);
//     g.setEdge(8, 11);
//     g.setEdge(5, 8);
//     g.setEdge(1, 5);
//     g.setEdge(13,14);
//     g.setEdge(1, 13);
//     g.setEdge(0, 1)

    g.setEdge(0,1);
    g.setEdge(0,2);

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