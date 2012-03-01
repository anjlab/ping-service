package com.anjlab.ping.pages;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;

import com.anjlab.ping.services.GAEHelper;

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

    public Long[] getPingServiceJobContext() {
        //  XXX Performance optimization: don't touch database on the welcome page.
        
//        List<Job> jobs = jobDAO.findByPingURL(Application.PING_SERVICE_PING_URL);
//        if (jobs.size() != 1) {
//            return new Long[0];
//        }
//        return Utils.createJobContext(jobs.get(0));
        
        return new Long[] { 2865005L };
    }
}
