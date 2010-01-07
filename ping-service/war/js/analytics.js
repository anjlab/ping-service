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
			
			//	See Job.java for response codes
//			public static final int PING_RESULT_NOT_AVAILABLE = 1;
//			public static final int PING_RESULT_OK = 2;
//			public static final int PING_RESULT_CONNECTIVITY_PROBLEM = 4;
//			public static final int PING_RESULT_HTTP_ERROR = 8;
//			public static final int PING_RESULT_REGEXP_VALIDATION_FAILED = 16;

			var labels = [];
			labels[1]  = 'No data';
			labels[2]  = 'Okay';
			labels[4]  = 'Connectivity Problems';
			labels[8]  = 'HTTP Errors';
			labels[16] = 'Regexp Validation Failures';
			
			var data = [];
	   		for (var idx in jsonData.keys)
			{
	   			intIdx = parseInt(idx);
	   			key = parseInt(jsonData.keys[intIdx]);
				data[data.length] = { label: labels[key], data: jsonData.values[intIdx] }
			}

			$.plot($("#chart"), data,
				{
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
