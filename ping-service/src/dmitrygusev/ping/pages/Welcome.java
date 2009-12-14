package dmitrygusev.ping.pages;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;

import dmitrygusev.ping.services.security.GAEHelper;

public class Welcome {

	@Inject
	private GAEHelper gaeHelper;
	
	@Inject
	private PageRenderLinkSource linkSource;
	
	public String getStartURL() {
		String indexURL = linkSource.createPageRenderLink(Index.class).toString();

		if (gaeHelper.getUserPrincipal() != null) {
			return indexURL;
		}
		
		return gaeHelper.createLoginURL(indexURL);
	}
	
}
