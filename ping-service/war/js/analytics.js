  function initializeMap() {
    var mapCanvas = document.getElementById("map_canvas")
    
    if (mapCanvas == undefined) {
        return;
    }
    
    var myOptions = {
      zoom: 1,
      center: sourceLL,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    var map = new google.maps.Map(mapCanvas, myOptions);

    var sourceMarker = new google.maps.Marker({
      position: sourceLL, 
      map: map, 
      title: sourceTitle
    });

    if (!sourceLL.equals(targetLL)) {
        var targetMarker = new google.maps.Marker({
          position: targetLL, 
          map: map, 
          title: targetTitle
        });
        
        var line = [sourceLL, targetLL];
        
        var path = new google.maps.Polyline({
          path: line,
          strokeColor: "#FF0000",
          strokeOpacity: 0.5,
          strokeWeight: 2
        });
    
        path.setMap(map);
        
        // Fit to markers:
        //     http://blog.shamess.info/2009/09/29/zoom-to-fit-all-markers-on-google-maps-api-v3/
    
        //  Make an array of the LatLng's of the markers you want to show
        var LatLngList = new Array (sourceLL, targetLL);
        //  Create a new viewpoint bound
        var bounds = new google.maps.LatLngBounds ();
        //  Go through each...
        for (var i = 0, LtLgLen = LatLngList.length; i < LtLgLen; i++) {
          //  And increase the bounds to take this point
          bounds.extend (LatLngList[i]);
        }
        //  Fit these bounds to the map
        map.fitBounds (bounds);
    }

  }

jQuery(document).ready(function() {
    jQuery(".c-hb").each(function() {
        var $this = jQuery(this);
        $this.attr("href", "javascript:void(0)");
        $this.click(function(){
            hideOverviewCharts();
            showPrimaryChart();
            removeHighlights();
            plotHistogramChart(jQuery(this), "#chart");
        });
    });
    jQuery(".c-pb").each(function() {
        var $this = jQuery(this);
        $this.attr("href", "javascript:void(0)");
        $this.click(function(){
            hideOverviewCharts();
            showPrimaryChart();
            removeHighlights();
            plotPieChart(jQuery(this), "#chart");
        });
    });
    jQuery(".c-m").click(function() {
        var $this = jQuery(this);
        if (!$this.is(".c-n")) {
            return;    //    not a numeric measure
        }
        hideOverviewCharts();
        showPrimaryChart();
        removeHighlights();
        plotLineChart($this, "#chart");
    });
    initializeMap();
});

function plotHistogramChart($this, chartId, chartTitle) {
    highlightHeaders($this.parent(), chartId, chartTitle, buildChartTitle);
    highlightValuesAndDimensions($this.parent(), false);

    var jsonData = eval('(' + $this.parent().attr("data-json") + ')');
    var d = [];
    var ticks = [];
    
    ticks.push([0, "0"]);
    for (var idx = 0; idx < jsonData.keys.length; idx++) {
        d.push([idx, jsonData.values[idx]]);
        var key = jsonData.keys[idx];
        ticks.push([idx + 1, key.substring(key.indexOf(";") + 1, key.lastIndexOf("."))]);
    }
    
    d.push([idx, jsonData.others]);
    ticks.push([idx + 1, "others"]);
    
    jQuery.plot(jQuery(chartId), [{ 
        data: d, 
        bars: { show: true }
    }], 
    {
        xaxis: {
            ticks: ticks,
            autoscaleMargin: 0.02
        }
    });
}

function plotPieChart($this, chartId, chartTitle) {
    highlightHeaders($this.parent(), chartId, chartTitle, buildChartTitle);
    highlightValuesAndDimensions($this.parent(), false);

    var jsonData = eval('(' + $this.parent().attr("data-json") + ')');

    var intIdx;
    var key;

    var pieLabels = [];
    pieLabels[1]  = 'No data';
    pieLabels[2]  = 'Okay';
    pieLabels[4]  = 'Timeouts';
    pieLabels[8]  = 'HTTP Errors';
    pieLabels[16] = 'Regexp Failures';
    
    var pieColors = [];
    pieColors[1]  = '#999';
    pieColors[2]  = '#4da74d';
    pieColors[4]  = '#edc240';
    pieColors[8]  = '#cb4b4b';
    pieColors[16] = '#afd8f8';

    var actualColors = [];
    
    var data = [];
    for (var idx = 0; idx < jsonData.keys.length; idx++) {
        key = jsonData.keys[idx];
        data[data.length] = { label: pieLabels[key], data: jsonData.values[idx] };
        actualColors[actualColors.length] = pieColors[key];
    }

    jQuery.plot(jQuery(chartId), data,
        {
            colors: actualColors,
            series: {
                pie: {
                    show: true
                }
            },
            legend: {
                show: false
            }
        });
}

function plotLineChart($this, chartId, chartTitle) {
    if (!$this.is(".c-n")) {
        return;    //    not a numeric measure
    }
    
    highlightHeaders($this, chartId, chartTitle, buildLineChartTitle);
    highlightValuesAndDimensions($this, true);
    
    var d = [];
    var ticks = [];
    
    var classes = $this.attr("class").split(' ');
    var columnClass = getColumnClass(classes);
    
    $dimensions = jQuery(".c-sd");
    $values = jQuery(".c-sm").filter("." + columnClass);
    
    for (var i = 0; i < $dimensions.length; i++) {
        ticks.push([i, jQuery($dimensions.get(i)).html()]);
        d.push([i, parseFloat(jQuery($values.get(i)).html().replace(',', '.'))]);
    }

    jQuery.plot(jQuery(chartId), [{ 
        data: d,
        lines: { show: true },
        points: { show: true }
    }], 
    {
        xaxis: {
            ticks: ticks,
            autoscaleMargin: 0.02
        }
    });
}

function removeHighlights() {
    jQuery(".c-sm").removeClass("c-sm");
    jQuery(".c-sh").removeClass("c-sh");
    jQuery(".c-sd").removeClass("c-sd");
}

function getColumnClass(classes) {
    if (classes[1].substr(0,1) == "x") {    //    have parent class
        return classes[5];
    }
    return classes[4];
}

function getDimensionClass(classes) {
    if (classes[1].substr(0,1) == "x") {    //    have parent class
        return classes[6];
    }
    return classes[5];
}

function highlightHeaders($this, chartId, chartTitle, chartTitleBuilder) {
    var classes = $this.attr("class").split(' ');
    var thisClass = classes[0];
    var parts = thisClass.split('-');
    var columnClass = getColumnClass(classes);

    var parentClass = classes[1];
    var offset = 0;
    if (parentClass[0] == "x" && (parts.length - parentClass.split('-').length == 1)) {
        offset = 1;
    }
    
    var $dimensionLabel = jQuery(jQuery("." + thisClass).parent().parent().children().children().get(parts.length-2+offset));
    $dimensionLabel.addClass("c-sh");

    var $aggregateLabel = jQuery(".a" + columnClass);
    var $measureLabel = jQuery("#" + $aggregateLabel.attr("class").split(' ')[1]);

    $aggregateLabel.addClass("c-sh");
    $measureLabel.addClass("c-sh");
    
    if (!chartTitle) {
        chartTitle = chartTitleBuilder($aggregateLabel, $measureLabel, $dimensionLabel, $this);
    }
    setChartTitle(chartId, chartTitle);
}

function buildChartTitle($aggregateLabel, $measureLabel, $dimensionLabel, $valueCell) {
    var aggregateLabel = $aggregateLabel.html();
    
    aggregateLabel = aggregateLabel.substr(0,1).toUpperCase() + aggregateLabel.substring(1);

    var classes = $valueCell.attr("class").split(' ');
    var dimensionClass = getDimensionClass(classes);
    
    var dimensionValue = jQuery("#" + dimensionClass).html();

    return aggregateLabel + " of " + $measureLabel.html() + " for " + $dimensionLabel.html() + " = " + dimensionValue;
}

function buildLineChartTitle($aggregateLabel, $measureLabel, $dimensionLabel, $valueCell) {
    var aggregateLabel = $aggregateLabel.html();
    
    aggregateLabel = aggregateLabel.substr(0,1).toUpperCase() + aggregateLabel.substring(1);
    
    return aggregateLabel + " of " + $measureLabel.html() + " by " + $dimensionLabel.html();
}

function highlightValuesAndDimensions($valueCell, allValues) {
    var classes = $valueCell.attr("class").split(' ');
    var columnClass = getColumnClass(classes);
    var colspan = jQuery($valueCell.parent().children().get(0)).attr("colspan");

    if (allValues == true) {
        jQuery("." + columnClass).each(function() {
            var $this = jQuery(this);
            var _colspan = jQuery($this.parent().children().get(0)).attr("colspan");
            
            if (colspan == _colspan) {
                highlightValueWithParentDimension($this);
            }
        });
    } else {
        highlightValueWithParentDimension($valueCell);
    }
}

function highlightValueWithParentDimension($this) {
    var classes = $this.attr("class").split(' ');
    var dimensionClass = getDimensionClass(classes);
    jQuery("#" + dimensionClass).addClass("c-sd");
    $this.addClass("c-sm");
}

function setChartTitle(chartId, chartTitle) {
    var $chartTitle = jQuery(chartId).parent().children().filter(".chartTitle");
    $chartTitle.text(chartTitle);
}