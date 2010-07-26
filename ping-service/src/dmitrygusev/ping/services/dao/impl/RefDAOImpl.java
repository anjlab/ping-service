package dmitrygusev.ping.services.dao.impl;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import com.google.appengine.api.datastore.Key;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;
import dmitrygusev.ping.services.dao.RefDAO;

@SuppressWarnings("unchecked")
public class RefDAOImpl implements RefDAO {

    @Inject
    private EntityManager em;

    @Override
    public Ref addRef(Account account, Schedule schedule, int accessType) {
        Ref ref = find(account, schedule);
        
        if (ref != null) {
            return ref;
        }
        
        ref = new Ref();
        ref.setAccountKey(createKey(Account.class.getSimpleName(), account.getId()));
        ref.setScheduleKey(createKey(Schedule.class.getSimpleName(), schedule.getId()));
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
    public List<Ref> getRefs(Account account) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.accountKey = :key");
        q.setParameter("key", createKey(Account.class.getSimpleName(), account.getId()));
        return q.getResultList();
    }

    @Override
    public List<Ref> getRefs(Schedule schedule) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.scheduleKey = :key");
        q.setParameter("key", createKey(Schedule.class.getSimpleName(), schedule.getId()));
        return q.getResultList();
    }

    @Override
    public Ref find(Account account, Schedule schedule) {
        Query q = em.createQuery("SELECT r FROM Ref r WHERE r.scheduleKey = :key AND r.accountKey = :key2");
        q.setParameter("key", createKey(Schedule.class.getSimpleName(), schedule.getId()));
        q.setParameter("key2", createKey(Account.class.getSimpleName(), account.getId()));
        
        List<Ref> refs = q.getResultList();
        
        return refs.isEmpty() ? null : refs.get(0);
    }
    
    public Ref find(Long id) {
        Key key = createKey(Ref.class.getSimpleName(), id);
        return em.find(Ref.class, key);
    }

}
