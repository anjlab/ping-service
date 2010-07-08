package dmitrygusev.ping.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.Utils;

public class EnqueueJobsFilter extends AbstractFilter {
    
static final Logger logger = LoggerFactory.getLogger(EnqueueJobsFilter.class);
    
    @Override
    protected void processRequest() 
            throws Exception
    {
        String cronString = globals.getHTTPServletRequest().getParameter("schedule");
        
        if (Utils.isCronStringSupported(cronString)) {
            application.enqueueJobs(cronString);
        }
    }
    
}
