package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static dmitrygusev.ping.services.Application.DATETIME_FORMAT;

import java.util.Date;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Mailer;

@Meta(AppModule.NO_MARKUP)
public class SimpleTask {

	private static final Logger logger = LoggerFactory.getLogger(SimpleTask.class);
	
	@Inject
	private Request request;
	
	@Inject
	private Cache cache;
	
	@Inject
	private Mailer mailer;
	
	public void onActivate() throws InterruptedException {
		Object idx = request.getParameter("idx");

		logger.warn("Running task {}", idx);
		
		cache.put(getKey(idx), new Date());
		
		StringBuilder builder = new StringBuilder();

		builder.append("idx date time");
		
		boolean allInCache = true;
		for (int i = 0; i < 100; i++) {
			allInCache = allInCache && 
				cache.containsKey(getKey(i));
			
			if (!allInCache) {
				break;
			}
			
			builder.append(i);
			builder.append(" ");
			builder.append(DATETIME_FORMAT.format((Date)cache.get(getKey(i))));
			builder.append("\n");
		}
		
		if (allInCache) {
			Long test = getMemcacheService().increment("test", 1L, 0L);
			
			if (test != null && test > 1) {
				return;
			}
			
			mailer.sendSystemMessageToDeveloper(
					"New experiment results bs=5, r=10/m", 
					builder.toString());
		}
	}

	private String getKey(Object o) {
		return "idx-" + o;
	}
	
}
