jQuery.fn.extend({
            autoHeight: function(){
                return this.each(function(){
                    var $this = jQuery(this);
                    if( !$this.attr('_initAdjustHeight') ){
                        $this.attr('_initAdjustHeight', $this.outerHeight());
                    }
                    _adjustH(this).on('input', function(){
                        _adjustH(this);
                    });
                });
                
                function _adjustH(elem){
                    var $obj = jQuery(elem);
                    return $obj.css({height: $obj.attr('_initAdjustHeight'), 'overflow-y': 'hidden'})
                            .height( elem.scrollHeight );
                }
            }
        });

function findElement(arrs, func) {
    for(var i = 0; i < arrs.length; ++i) {
        if(func(arrs[i]))return arrs[i];
    }
}

var asyncMessageCounter = 0;

function getRestHeaders(type) {
    return {'http-type' : type, 'cluster-name' : spark.clusterName }
}

function getMessageAsync(url, type, callback) {
    var headers = getRestHeaders(type);
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.timeout = 60 * 1000;
    xmlHttp.ontimeout = function () {
        if (--asyncMessageCounter === 0) {
            $('body').css("cursor", "default");
        }
    };
    ++asyncMessageCounter;
    $('body').css("cursor", "progress");

    xmlHttp.onreadystatechange = function () {
        if (xmlHttp.readyState === 4) {
            if (--asyncMessageCounter === 0) {
                $('body').css("cursor", "default");
            }
            if (xmlHttp.status === 200 || xmlHttp.status === 201) {
                var s = xmlHttp.responseText;
                if (s === '') {
                    return;
                }
                callback(s);
            }
        }
    };

    xmlHttp.open('GET', spark.localhost + url, true);
    Object.keys(headers).forEach(function(k) {
        xmlHttp.setRequestHeader(k, headers[k]);
    });

    xmlHttp.send(null);
}

function reloadTableStyle() {
    $("table tbody tr:nth-child(odd)").addClass("odd-row");
    /* For cell text alignment */
    $("table tbody td:first-child, table th:first-child").addClass("first");
    /* For removing the last border */
    $("table tbody td:last-child, table th:last-child").addClass("last");
}

function formatServerTime(gmtTime) {
    var strictIsoParse = d3.utcParse("%Y-%m-%dT%H:%M:%S.%LGMT");
    var formatTime = d3.timeFormat("%b %d, %Y %H:%M:%S");
    var ptime = strictIsoParse(gmtTime);
    return formatTime(ptime);
}