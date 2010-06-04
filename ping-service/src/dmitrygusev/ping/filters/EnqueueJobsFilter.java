package dmitrygusev.ping.filters;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dmitrygusev.ping.services.Utils;

public class EnqueueJobsFilter extends AbstractFilter {
    
private static final Logger logger = LoggerFactory.getLogger(RunJobFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
    {
        long startTime = System.currentTimeMillis();
        
        if (emf == null) {
            lazyInit();
        }
        
        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = null;
        
        try {
            
            tx = em.getTransaction();
            
            tx.begin();
            
            jobDAO.setEntityManager(em);
            
            String cronString = request.getParameter("schedule");
            
            if (Utils.isCronStringSupported(cronString)) {
                globals.storeServletRequestResponse((HttpServletRequest)request, (HttpServletResponse)response);
                
                application.enqueueJobs(cronString);
            }
            
            tx.commit();
        }
        catch (Exception e)
        {
            logger.warn("Error enqueueing job", e);
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
