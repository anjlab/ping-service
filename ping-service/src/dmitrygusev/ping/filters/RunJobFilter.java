package dmitrygusev.ping.filters;

import static com.google.appengine.api.datastore.KeyFactory.stringToKey;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Job;
import dmitrygusev.ping.pages.task.RunJobTask;

public class RunJobFilter extends AbstractFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RunJobFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
    {
        long startTime = System.currentTimeMillis();
        
        if (emf == null) {
            lazyInit();
        }
        
        String encodedJobKey = request.getParameter(RunJobTask.JOB_KEY_PARAMETER_NAME);

        EntityManager em = emf.createEntityManager();
        
        EntityTransaction tx = null;
        
        try {
            Key key = stringToKey(encodedJobKey);
            
            logger.debug("Running job: {}", key);

            tx = em.getTransaction();
            
            tx.begin();
            
            jobDAO.setEntityManager(em);
            
            Job job = jobDAO.find(key);
        
            if (job != null) {
                globals.storeServletRequestResponse((HttpServletRequest)request, (HttpServletResponse)response);
                
                application.runJob(job);
                
                application.updateJob(job, false);
            }
            
            tx.commit();
        }
        catch (Exception e)
        {
            logger.warn("Error running job", e);
        }
        finally
        {
            if (tx != null && tx.isActive())
            {
                tx.rollback();
            }
            
            em.close();
        }
        
        long endTime = System.currentTimeMillis();
        
        logger.debug("Total time: " + (endTime - startTime));
    }

}
