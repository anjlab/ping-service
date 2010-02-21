package dmitrygusev.ping.services.dao;

import java.util.List;

import org.tynamo.jpa.annotations.CommitAfter;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.entities.Ref;
import dmitrygusev.ping.entities.Schedule;

public interface RefDAO {
	@CommitAfter
	public abstract Ref addRef(Account account, Schedule schedule, int accessType);
	@CommitAfter
	public abstract void removeRef(Long id);
	@CommitAfter
	public abstract List<Ref> getRefs(Account account);
	@CommitAfter
	public abstract List<Ref> getRefs(Schedule schedule);
	@CommitAfter
	public abstract Ref find(Account find, Schedule schedule);
	
}
