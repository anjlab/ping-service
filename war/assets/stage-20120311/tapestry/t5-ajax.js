T5.define("ajax",function(){var k=T5._;var g=T5.$;var h=T5.spi;var d,f,c;function m(){}function a(n){c.open();c.write(n);c.close()}function l(){var n=document.viewport.getDimensions();f.width=n.width-100;f.height=n.height-(100+20)}function b(p){if(!d){var o=["<div class='t-exception-container'>","<iframe class='t-exception-frame' width='100%'></iframe>","<div class='t-exception-controls'>","<span class='t-exception-close'>Close</span>","</div>","</div>"].join("");d=T5.dom.find(T5.dom.appendMarkup(document.body,o),"div.t-exception-container");f=T5.dom.find(d,"iframe");c=(f.contentWindow||f.contentDocument);if(c.document){c=c.document}var n=T5.dom.find(d,".t-exception-close");T5.dom.observe(n,"click",function(q){q.stop();a("");T5.dom.hide(d)});l();Event.observe(window,"resize",k.debounce(l,20))}a(p);d.show()}function i(n){}function j(n){}function e(o,n){throw"not yet implemented"}return{defaultFailure:i,defaultException:j,defaultSuccess:m,showExceptionDialog:b,request:e}});