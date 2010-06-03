package dmitrygusev.ping.pages;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;

import dmitrygusev.ping.services.GAEHelper;

public class Welcome {

	@Inject
	private GAEHelper gaeHelper;
	
	@Inject
	private PageRenderLinkSource linkSource;
	
	private static String indexURL;
	
	public String getStartURL() {
		if (indexURL == null) {
		    indexURL = linkSource.createPageRenderLink(Index.class).toString();
		}

		if (gaeHelper.getUserPrincipal() != null) {
			return indexURL;
		}
		
		return gaeHelper.createLoginURL(indexURL);
	}
	
}
