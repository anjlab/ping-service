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

    private final String appPackage;
    
    public LazyJPATransactionManager(JPAEntityManagerSource source, String appPackage) {
        this.source = source;
        this.appPackage = appPackage;
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
                    
                    StringBuilder trace = ProfilingDelegate.buildStackTrace(appPackage);
                    
                    logger.info("Transaction created ({} ms) for context {}", System.currentTimeMillis() - startTime, trace);
                }
            }

            @Override
            public void setFlushMode(FlushModeType flushMode) {
                getEM().setFlushMode(flushMode);
            }
            
            @Override
            public void remove(Object entity) {
                assureTxBegin();
                getEM().remove(entity);
            }
            
            @Override
            public void refresh(Object entity) {
                assureTxBegin();
                getEM().refresh(entity);
            }
            
            @Override
            public void persist(Object entity) {
                assureTxBegin();
                getEM().persist(entity);
            }

            @Override
            public <T> T merge(T entity) {
                assureTxBegin();
                return getEM().merge(entity);
            }
            
            @Override
            public void lock(Object entity, LockModeType lockMode) {
                assureTxBegin();
                getEM().lock(entity, lockMode);
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
            public <T> T getReference(Class<T> entityClass, Object primaryKey) {
                assureTxBegin();
                return getEM().getReference(entityClass, primaryKey);
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
            public <T> T find(Class<T> entityClass, Object primaryKey) {
                assureTxBegin();
                return getEM().find(entityClass, primaryKey);
            }
            
            @Override
            public Query createQuery(String qlString) {
                assureTxBegin();
                return getEM().createQuery(qlString);
            }
            
            @Override
            public Query createNativeQuery(String sqlString, String resultSetMapping) {
                assureTxBegin();
                return getEM().createNativeQuery(sqlString, resultSetMapping);
            }
            
            @SuppressWarnings("unchecked")
            @Override
            public Query createNativeQuery(String sqlString, Class resultClass) {
                assureTxBegin();
                return getEM().createNativeQuery(sqlString, resultClass);
            }
            
            @Override
            public Query createNativeQuery(String sqlString) {
                assureTxBegin();
                return getEM().createNativeQuery(sqlString);
            }
            
            @Override
            public Query createNamedQuery(String name) {
                assureTxBegin();
                return getEM().createNamedQuery(name);
            }
            
            @Override
            public boolean contains(Object entity) {
                return getEM().contains(entity);
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