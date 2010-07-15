/**
 * 
 */
package dmitrygusev.tapestry5.gae;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tynamo.jpa.JPAEntityManagerSource;
import org.tynamo.jpa.JPATransactionManager;

public class LazyJPATransactionManager implements JPATransactionManager
{
    private static final Logger logger = LoggerFactory.getLogger(LazyJPATransactionManager.class);
    
    private final JPAEntityManagerSource source;
    
    private EntityManager instance;

    public LazyJPATransactionManager(JPAEntityManagerSource source) {
        this.source = source;
    }

    private EntityManager getEM() {
        if (instance == null) {
            instance = source.create();
        }
        return instance;
    }

    @Override
    public EntityManager getEntityManager() {
        return new EntityManager() {
            
            private void assureTxBegin() {
                EntityTransaction tx = getEM().getTransaction();
                if (!tx.isActive()) {
                    long startTime = System.currentTimeMillis();

                    tx.begin();
                    
                    StringBuilder trace = ProfilingDelegate.buildStackTrace();
                    
                    logger.info("Transaction created ({} ms) for context {}", System.currentTimeMillis() - startTime, trace);
                }
            }

            @Override
            public void setFlushMode(FlushModeType arg0) {
                getEM().setFlushMode(arg0);
            }
            
            @Override
            public void remove(Object arg0) {
                assureTxBegin();
                getEM().remove(arg0);
            }
            
            @Override
            public void refresh(Object arg0) {
                assureTxBegin();
                getEM().refresh(arg0);
            }
            
            @Override
            public void persist(Object arg0) {
                assureTxBegin();
                getEM().persist(arg0);
            }

            @Override
            public <T> T merge(T arg0) {
                assureTxBegin();
                return getEM().merge(arg0);
            }
            
            @Override
            public void lock(Object arg0, LockModeType arg1) {
                assureTxBegin();
                getEM().lock(arg0, arg1);
            }
            
            @Override
            public void joinTransaction() {
                assureTxBegin();
                getEM().joinTransaction();
            }
            
            @Override
            public boolean isOpen() {
                return getEM().isOpen();
            }
            
            @Override
            public EntityTransaction getTransaction() {
                return getEM().getTransaction();
            }
            
            @Override
            public <T> T getReference(Class<T> arg0, Object arg1) {
                assureTxBegin();
                return getEM().getReference(arg0, arg1);
            }
            
            @Override
            public FlushModeType getFlushMode() {
                return getEM().getFlushMode();
            }
            
            @Override
            public Object getDelegate() {
                return getEM().getDelegate();
            }
            
            @Override
            public void flush() {
                assureTxBegin();
                getEM().flush();
            }
            
            @Override
            public <T> T find(Class<T> arg0, Object arg1) {
                assureTxBegin();
                return getEM().find(arg0, arg1);
            }
            
            @Override
            public Query createQuery(String arg0) {
                assureTxBegin();
                return getEM().createQuery(arg0);
            }
            
            @Override
            public Query createNativeQuery(String arg0, String arg1) {
                assureTxBegin();
                return getEM().createNativeQuery(arg0, arg1);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public Query createNativeQuery(String arg0, Class arg1) {
                assureTxBegin();
                return getEM().createNativeQuery(arg0, arg1);
            }
            
            @Override
            public Query createNativeQuery(String arg0) {
                assureTxBegin();
                return getEM().createNativeQuery(arg0);
            }
            
            @Override
            public Query createNamedQuery(String arg0) {
                assureTxBegin();
                return getEM().createNamedQuery(arg0);
            }
            
            @Override
            public boolean contains(Object arg0) {
                return getEM().contains(arg0);
            }
            
            @Override
            public void close() {
                getEM().close();
            }
            
            @Override
            public void clear() {
                getEM().clear();
            }
        };
    }

    @Override
    public void commit() {
        if (instance == null) {
            //  No transaction
            return;
        }
        
        EntityTransaction tx = getEM().getTransaction();
        if (tx.isActive()) {
            long startTime = System.currentTimeMillis();
            
            tx.commit();
            
            logger.info("Transaction committed ({} ms)", System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public void abort() {
        if (instance == null) {
            //  No transaction
            return;
        }
        
        EntityTransaction tx = getEM().getTransaction();
        if (tx.isActive()) {
            long startTime = System.currentTimeMillis();
            
            tx.rollback();

            logger.info("Transaction rolled back ({} ms)", System.currentTimeMillis() - startTime);
       }
    }
}