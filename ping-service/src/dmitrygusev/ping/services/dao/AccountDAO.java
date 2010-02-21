package dmitrygusev.ping.services.dao;

import org.tynamo.jpa.annotations.CommitAfter;

import dmitrygusev.ping.entities.Account;

public interface AccountDAO {
	@CommitAfter
	public abstract Account find(Long id);
	@CommitAfter
	public abstract Account getAccount(String email);
	@CommitAfter
	public abstract void update(Account account);
	@CommitAfter
	public abstract void delete(Long id);

}