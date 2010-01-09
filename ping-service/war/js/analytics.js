$(document).ready(function() {
	$(".c-hb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			var jsonData = eval('(' + $(this).parent().attr("data-json") + ')');
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
	   		
	   		$.plot($("#chart"), [{ 
	   			data: d, 
	   			bars: { show: true }
	   		}], 
	   		{
	   			xaxis: {
	   				ticks: ticks,
	   				autoscaleMargin: 0.02
	   			}
	   		});
		});
	});
	$(".c-pb").each(function() {
		var $this = $(this);
		$this.attr("href", "javascript:void(0)");
		$this.click(function(){
			var jsonData = eval('(' + $(this).parent().attr("data-json") + ')');

			var intIdx;
			var key;

			var labels = [];
			labels[1]  = 'No data';
			labels[2]  = 'Okay';
			labels[4]  = 'Timeouts';
			labels[8]  = 'HTTP Errors';
			labels[16] = 'Regexp Failures';
			
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
				data[data.length] = { label: labels[key], data: jsonData.values[intIdx] };
				actualColors[actualColors.length] = pieColors[key];
			}

			$.plot($("#chart"), data,
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
		});
	});
	$(".c-m").click(function() {
		$(".c-sm").removeClass("c-sm");
		$(".c-sh").removeClass("c-sh");
		$(".c-sd").removeClass("c-sd");

		var $this = $(this);
		
		if (!$this.is(".c-n")) {
			return;	//	not a numeric measure
		}

		var classes = $this.attr("class").split(' ');
		var thisClass = classes[0];

		var parts = thisClass.split('-');
		var part = parts[parts.length - 1];

		var columnClass = classes[classes.length - 3];

		var parentClass = classes[1];
		var offset = 0;
		if (parentClass[0] == "x") {
			if (parts.length - parentClass.split('-').length == 1) {
				offset = 1;
			}
		}
		
		var $dimension = $($("." + thisClass).parent().parent().children().children().get(parts.length-2+offset));
		
		$dimension.addClass("c-sh");
		
		$("." + columnClass).each(function() {
			var $this = $(this);
			var classes = $this.attr("class").split(' ');
			var thisClass = classes[0];
			var dimensionClass = classes[classes.length-2];
			
			var _parts = thisClass.split('-');
			var _part = _parts[_parts.length - 1];
			
			if (part == _part && parts.length == _parts.length) {
				$this.addClass("c-sm");
				$("#" + dimensionClass).addClass("c-sd");
			}
		});
		
   		var d = [];
   		var ticks = [];
   		
   		$dimensions = $(".c-sd");
   		$values = $(".c-sm");
   		
   		for (var i = 0; i < $dimensions.length; i++) {
   			ticks.push([i, $($dimensions.get(i)).html()]);
   			d.push([i, parseFloat($($values.get(i)).html())]);
   		}

   		$.plot($("#chart"), [{ 
   			data: d
   		}], 
   		{
   			xaxis: {
   				ticks: ticks,
   				autoscaleMargin: 0.02
   			}
   		});

	});
});
