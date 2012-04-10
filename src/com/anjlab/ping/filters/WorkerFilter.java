package com.anjlab.ping.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anjlab.ping.services.Mailer;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public class WorkerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(WorkerFilter.class);
    
    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1,
            FilterChain arg2) throws IOException, ServletException {
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Ref");
        List<Entity> refs =  datastore.prepare(query).asList(FetchOptions.Builder.withLimit(20000));
        logger.warn("Found {} refs", refs.size());
        
        query = new Query("Account");
        List<Entity> accounts = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(20000)); 
        
        query = new Query("Job");
        List<Entity> jobs = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(20000));
        
        Map<String, Integer> counters = new HashMap<String, Integer>();
        
        boolean sendEmails = false;
        
        for (Entity ref : refs) {
            Key accountKey = (Key) ref.getProperty("accountKey");
            boolean foundAccount = false;
            for (Entity account : accounts) {
                if (new Long(accountKey.getId()).equals(account.getKey().getId())) {
                    foundAccount = true;
                    break;
                }
            }
            Object schedulName = ref.getProperty("scheduleName");
            
            boolean foundJob = false;
            for (Entity job : jobs) {
                if (job.getProperty("scheduleName").equals(schedulName)) {
                    foundJob = true;
                    break;
                }
            }
            
            if (!foundJob) {
                logger.warn("Found 0 jobs for accountKey={} for scheduleName={}", accountKey.getId(), schedulName);
                datastore.delete(ref.getKey());
            }
            else
            if (!foundAccount) {
                Integer count = counters.get(schedulName);
                if (count == null) {
                    count = 0;
                }
                count++;
                counters.put((String)schedulName, count);
                logger.warn("Found 0 accounts for accountKey={} for scheduleName={}", accountKey.getId(), schedulName);
                if (sendEmails) {
                    datastore.delete(ref.getKey());
                }
            }
        }
        
        logger.warn("Counters -- {}", counters);
        
        if (sendEmails) {
            Mailer mailer = new Mailer();
            for (String email : counters.keySet()) {
                mailer.sendMail2("text/html", Mailer.PING_SERVICE_NOTIFY_GMAIL_COM, email, 
                        "Ping Service Notice", 
                        "<p>Dear Ping Service user,</p>" +
                        "<p>We notify you that during recent Ping Service upgrade some data associated with your schedule" +
                        " was lost -- this is information about your schedule sharings with other users.</p>" +
                        "<p>We can't fix this automatically so you will need to share your schedule once again with those users.</p>" +
                        "<p>Sorry for the inconvenience.</p>");
            }
        }
        
//        deleteAhSessions();
    }

    @SuppressWarnings("unused")
    private void deleteAhSessions() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("_ah_SESSION");
        query.setKeysOnly();
        List<Entity> results =  datastore.prepare(query).asList(FetchOptions.Builder.withLimit(20000));
        logger.warn("Found {} entities", results.size());
        List<Key> keys = new ArrayList<Key>();
        for (Entity entity : results) {
             keys.add(entity.getKey());
        }
        datastore.delete(keys);
        logger.warn("Deleted");
    }
    
    @Override
    public void init(FilterConfig arg0) throws ServletException { }
    
    @Override
    public void destroy() { }
    
}
