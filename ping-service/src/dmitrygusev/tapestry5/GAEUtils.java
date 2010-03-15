package dmitrygusev.tapestry5;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.tapestry5.services.PageRenderLinkSource;

import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

public class GAEUtils {

	public static TaskOptions buildTaskUrl(PageRenderLinkSource linkSource, Class<?> pageClass) 
		throws URISyntaxException 
	{
		URI uri = new URI(linkSource.createPageRenderLink(pageClass).toAbsoluteURI());

		String path = uri.getPath();
		
		return buildTaskUrl(path);
	}

	public static TaskOptions buildTaskUrl(String path) {
		return url(path.endsWith("/") ? path : path + "/").method(Method.GET);
	}

}
