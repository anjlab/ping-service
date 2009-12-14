package dmitrygusev.ping.services.dao;

import dmitrygusev.ping.entities.Account;

public interface AccountDAO {

	public abstract Account find(Long id);

	public abstract Account getAccount(String email);

	public abstract void update(Account account);

	public abstract void delete(Long id);

}