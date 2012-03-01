package com.anjlab.ping.filters;

import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.gae.QuotaDetails.Quota;
import com.anjlab.ping.services.Utils;
import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.capabilities.CapabilityStatus;

public class EnqueueJobsFilter extends AbstractFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(EnqueueJobsFilter.class);
    
    @Override
    protected boolean disableFilter() {
        if (super.disableFilter()) {
            return true;
        }
        
        if (quotaDetails.isQuotaLimited(Quota.DatastoreWrite)) {
            logger.warn("DatastoreWrite quota is limited.");
            return true;
        }
        
        CapabilitiesService service = CapabilitiesServiceFactory.getCapabilitiesService();
        CapabilityStatus status = service.getStatus(Capability.DATASTORE_WRITE).getStatus();
        
        if (status == CapabilityStatus.DISABLED) {
            logger.warn("Datastore is in read-only mode.");
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void processRequest(EntityTransaction tx) 
            throws Exception
    {
        String cronString = globals.getHTTPServletRequest().getParameter("schedule");
        
        if (Utils.isCronStringSupported(cronString)) {
            application.enqueueJobs(cronString, tx);
        }
    }
    
}
