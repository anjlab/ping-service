package dmitrygusev.ping.pages.cron;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.services.AppModule;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Mailer;
import dmitrygusev.ping.services.dao.JobDAO;

@Meta(AppModule.NO_MARKUP)
public class BackupHealthInsuranceCron {

    private static final Logger logger = LoggerFactory.getLogger(BackupHealthInsuranceCron.class);
    
    @Inject private JobDAO jobDAO;
    @Inject private Mailer mailer;
    @Inject private Application application;
    
    public void onActivate() throws URISyntaxException {
        Job job = jobDAO.findRecent();
        Date now = new Date();
        
        //  This code will run at least every X hours a day (see 'backup' queue in queue.xml),
        long x = Math.round(24d / 5d);
        //  but lets put insurance ticket that will live a bit longer
        int insuranceIntervalMillis = (int) TimeUnit.MILLISECONDS.convert(x + 1, TimeUnit.HOURS);
        
        long millisecondsFromLastBackup = now.getTime() - job.getLastBackupTimestamp().getTime();
        
        if (millisecondsFromLastBackup < insuranceIntervalMillis) {
            //  Seems healthy
            return;
        }

        logger.error("Backup Health Insurance found that cyclic backups was inactive. Restarting now.");
        
        application.runCyclicBackupTask();
        
        mailer.sendSystemMessageToDeveloper(
                "Backup Health Insurance Failed", 
                "Running cyclic backup task from the beginning.");
    }
    
}
