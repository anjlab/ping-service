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
});
