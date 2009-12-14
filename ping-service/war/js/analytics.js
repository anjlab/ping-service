$(document).ready(function() {
	$(".c-cb").each(function() {
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
});