package dmitrygusev.ping.filters;

import dmitrygusev.ping.services.Utils;

public class EnqueueJobsFilter extends AbstractFilter {
    
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
