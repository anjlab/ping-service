$(document).ready(function() {
	$(".c-hb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			hideOverviewCharts();
			showPrimaryChart();
			removeHighlights();
			plotHistogramChart($(this), "#chart");
		});
	});
	$(".c-pb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			hideOverviewCharts();
			showPrimaryChart();
			removeHighlights();
			plotPieChart($(this), "#chart");
		});
	});
	$(".c-m").click(function() {
		var $this = $(this);
		if (!$this.is(".c-n")) {
			return;	//	not a numeric measure
		}
		hideOverviewCharts();
		showPrimaryChart();
		removeHighlights();
		plotLineChart($this, "#chart");
	});
});

function plotHistogramChart($this, chartId, chartTitle) {
	highlightHeaders($this.parent(), chartId, chartTitle, buildChartTitle);
	highlightValuesAndDimensions($this.parent(), false);

	var jsonData = eval('(' + $this.parent().attr("data-json") + ')');
	var d = [];
	var ticks = [];
	
	var intIdx;
	
	ticks.push([0, "0"]);
	for (var idx in jsonData.keys) {
		intIdx = parseInt(idx);
		d.push([intIdx, jsonData.values[intIdx]]);
		var key = jsonData.keys[intIdx];
		ticks.push([intIdx + 1, key.substring(key.indexOf(";") + 1, key.lastIndexOf("."))]);
	}
	
	intIdx++;
	
	d.push([intIdx, jsonData.others]);
	ticks.push([intIdx + 1, "others"]);
	
	$.plot($(chartId), [{ 
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
		for (var idx in jsonData.keys)
	{
		intIdx = parseInt(idx);
		key = parseInt(jsonData.keys[intIdx]);
		data[data.length] = { label: pieLabels[key], data: jsonData.values[intIdx] };
		actualColors[actualColors.length] = pieColors[key];
	}

	$.plot($(chartId), data,
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
		return;	//	not a numeric measure
	}
	
	highlightHeaders($this, chartId, chartTitle, buildLineChartTitle);
	highlightValuesAndDimensions($this, true);
	
	var d = [];
	var ticks = [];
	
	var classes = $this.attr("class").split(' ');
	var columnClass = getColumnClass(classes);
	
	$dimensions = $(".c-sd");
	$values = $(".c-sm").filter("." + columnClass);
	
	for (var i = 0; i < $dimensions.length; i++) {
		ticks.push([i, $($dimensions.get(i)).html()]);
		d.push([i, parseFloat($($values.get(i)).html().replace(',', '.'))]);
	}

	$.plot($(chartId), [{ 
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
	$(".c-sm").removeClass("c-sm");
	$(".c-sh").removeClass("c-sh");
	$(".c-sd").removeClass("c-sd");
}

function getColumnClass(classes) {
	if (classes[1][0] == "x") {	//	have parent class
		return classes[5];
	}
	return classes[4];
}

function getDimensionClass(classes) {
	if (classes[1][0] == "x") {	//	have parent class
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
	
	var $dimensionLabel = $($("." + thisClass).parent().parent().children().children().get(parts.length-2+offset));
	$dimensionLabel.addClass("c-sh");

	var $aggregateLabel = $(".a" + columnClass);
	var $measureLabel = $("#" + $aggregateLabel.attr("class").split(' ')[1]);

	$aggregateLabel.addClass("c-sh");
	$measureLabel.addClass("c-sh");
	
	if (!chartTitle) {
		chartTitle = chartTitleBuilder($aggregateLabel, $measureLabel, $dimensionLabel, $this);
	}
	setChartTitle(chartId, chartTitle);
}

function buildChartTitle($aggregateLabel, $measureLabel, $dimensionLabel, $valueCell) {
	var aggregateLabel = $aggregateLabel.html();
	
	aggregateLabel = aggregateLabel[0].toUpperCase() + aggregateLabel.substring(1);

	var classes = $valueCell.attr("class").split(' ');
	var dimensionClass = getDimensionClass(classes);
	
	var dimensionValue = $("#" + dimensionClass).html();

	return aggregateLabel + " of " + $measureLabel.html() + " for " + $dimensionLabel.html() + " = " + dimensionValue;
}

function buildLineChartTitle($aggregateLabel, $measureLabel, $dimensionLabel, $valueCell) {
	var aggregateLabel = $aggregateLabel.html();
	
	aggregateLabel = aggregateLabel[0].toUpperCase() + aggregateLabel.substring(1);
	
	return aggregateLabel + " of " + $measureLabel.html() + " by " + $dimensionLabel.html();
}

function highlightValuesAndDimensions($valueCell, allValues) {
	var classes = $valueCell.attr("class").split(' ');
	var columnClass = getColumnClass(classes);
	var colspan = $($valueCell.parent().children().get(0)).attr("colspan");

	if (allValues == true) {
		$("." + columnClass).each(function() {
			var $this = $(this);
			var _colspan = $($this.parent().children().get(0)).attr("colspan");
			
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
	$("#" + dimensionClass).addClass("c-sd");
	$this.addClass("c-sm");
}

function setChartTitle(chartId, chartTitle) {
	var $chartTitle = $(chartId).parent().children().filter(".chartTitle");
	$chartTitle.text(chartTitle);
}