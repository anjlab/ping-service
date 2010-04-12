package dmitrygusev.ping.pages.task;

import static com.google.appengine.api.labs.taskqueue.QueueFactory.getQueue;
import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;
import static dmitrygusev.ping.services.GAEHelper.addTaskNonTransactional;

import java.net.URISyntaxException;

import net.sf.jsr107cache.Cache;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;

@Meta(AppModule.NO_MARKUP)
public class GeneratorTask {

	private static final Logger logger = LoggerFactory.getLogger(GeneratorTask.class);
	
	@Inject
	private Application application;
	
	@Inject
	private Cache cache;

	public void onActivate() throws URISyntaxException {
		logger.warn("Begin generating...");

		getMemcacheService().delete("test");
		
		for (int i = 0; i < 100; i++) {
			cache.remove("idx-" + i);
		}

		for (int i = 0; i < 100; i++) {
		    addTaskNonTransactional(
		        getQueue("test-queue"),
				application.buildTaskUrl(SimpleTask.class)
						.param("idx", String.valueOf(i)));
		}

		logger.warn("Done generating");
	}
	
}
