package dmitrygusev.ping.services.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.services.dao.AccountDAO;

@SuppressWarnings("unchecked")
public class AccountDAOImpl implements AccountDAO {

	@Inject
    private EntityManager em;
	
	@Override
	public Account getAccount(String email) {
		if (email != null) {
			email = email.toLowerCase();
		}
		
		Account result = findByEmail(email);
		
		if (result == null) {
			result = createAccount(email);
		}
		
		return result;
	}

	private Account findByEmail(String email) {
		Query q = em.createQuery("SELECT a FROM Account a WHERE a.email = :email").
			setParameter("email", email).setMaxResults(1);
		
		List<Account> result = q.getResultList();
		
		return result.isEmpty() ? null : result.get(0);
	}
	
	private Account createAccount(String email) {
		Account result;
		result = new Account();
		result.setEmail(email);
		
		em.persist(result);
		return result;
	}

	@Override
	public Account find(Long id) {
		return em.find(Account.class, id);
	}

	@Override
	public void update(Account account) {
		em.merge(account);
	}
	
	@Override
	public void delete(Long id) {
		Account account = find(id);
		em.remove(account);
	}
}
