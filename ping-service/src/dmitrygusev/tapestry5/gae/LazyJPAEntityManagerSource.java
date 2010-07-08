/**
 * 
 */
package dmitrygusev.tapestry5.gae;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.tapestry5.ioc.services.RegistryShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.jpa.JPAEntityManagerSource;

public class LazyJPAEntityManagerSource implements JPAEntityManagerSource, RegistryShutdownListener 
{
    private static final Logger logger = LoggerFactory.getLogger(LazyJPAEntityManagerSource.class);
    
    private final String persistenceUnit;
    
    private EntityManagerFactory entityManagerFactory;

    public LazyJPAEntityManagerSource(String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    @Override
    public final EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            long startTime = System.currentTimeMillis();
            
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
            
            logger.info("EMF created ({} ms)", System.currentTimeMillis() - startTime);
        }
        return entityManagerFactory;
    }

    @Override
    public EntityManager create() {
        return getEntityManagerFactory().createEntityManager();
    }

    @Override
    public void registryDidShutdown() {
        getEntityManagerFactory().close();
    }
}