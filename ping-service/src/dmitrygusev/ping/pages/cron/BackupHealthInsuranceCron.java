package dmitrygusev.ping.pages.cron;

import java.net.URISyntaxException;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheService;

import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Mailer;

@Meta(AppModule.NO_MARKUP)
public class BackupHealthInsuranceCron {

    public static final String INSURANCE_TICKET = BackupHealthInsuranceCron.class.getName();

    private static final Logger logger = LoggerFactory.getLogger(BackupHealthInsuranceCron.class);
    
    @Inject private MemcacheService memcacheService;
    @Inject private Mailer mailer;
    @Inject private Application application;
    
    public void onActivate() throws URISyntaxException {
        if (memcacheService.contains(INSURANCE_TICKET)) {
            //  Seems healthy
            return;
        }

        logger.error("Backup Health Insurance Failed");
        
        application.runCyclicBackupTask();
        
        mailer.sendSystemMessageToDeveloper(
                "Backup Health Insurance Failed", 
                "Running cyclic backup task from the beginning.");
    }
    
}
