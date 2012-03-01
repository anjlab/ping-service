package com.anjlab.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.entities.Ref;
import com.anjlab.ping.services.dao.RefDAO;
import com.google.appengine.api.datastore.Key;


@SuppressWarnings("unchecked")
public class RefDAOImpl implements RefDAO {

    @Inject
    private EntityManager em;

    @Override
    public Ref addRef(Account account, String scheduleName, int accessType) {
        Ref ref = find(account, scheduleName);
        
        if (ref != null) {
            return ref;
        }
        
        ref = new Ref();
        ref.setAccountKey(createKey(Account.class.getSimpleName(), account.getId()));
        ref.setScheduleName(scheduleName);
        ref.setAccessType(accessType);
        em.persist(ref);
        
        return ref;
    }

    @Override
    public void removeRef(Long id) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.id = :id").setParameter("id", id);
        
        List<Ref> refs = q.getResultList();
        
        if (! refs.isEmpty()) {
            em.remove(refs.get(0));
        }
    }

    @Override
    public List<Ref> getRefs(String scheduleName) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.scheduleName = :scheduleName");
        q.setParameter("scheduleName", scheduleName);
        return q.getResultList();
    }

    @Override
    public List<Ref> getRefs(Account account) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.accountKey = :key");
        q.setParameter("key", createKey(Account.class.getSimpleName(), account.getId()));
        return q.getResultList();
    }

    @Override
    public Ref find(Account account, String scheduleName) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.scheduleName = :scheduleName AND r.accountKey = :accountKey");
        q.setParameter("scheduleName", scheduleName);
        q.setParameter("accountKey", createKey(Account.class.getSimpleName(), account.getId()));
        
        List<Ref> refs = q.getResultList();
        
        return refs.isEmpty() ? null : refs.get(0);
    }
    
    public Ref find(Long id) {
        Key key = createKey(Ref.class.getSimpleName(), id);
        return em.find(Ref.class, key);
    }
    
    @Override
    public List<Ref> getAll() {
        return em.createQuery("SELECT FROM Ref").getResultList();
    }
    
    @Override
    public void update(Ref ref) {
        em.merge(ref);
    }
}
