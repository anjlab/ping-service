package dmitrygusev.ping.services.dao;

import java.util.List;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;

public interface RefDAO {

	public abstract Ref addRef(Account account, Schedule schedule, int accessType);
	
	public abstract void removeRef(Long id);

	public abstract List<Ref> getRefs(Account account);

	public abstract List<Ref> getRefs(Schedule schedule);

	public abstract Ref find(Account find, Schedule schedule);
	
}
