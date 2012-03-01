package com.anjlab.ping.services.dao;

import java.util.List;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.entities.Ref;


public interface RefDAO {
    @CommitAfter
    public abstract Ref addRef(Account account, String scheduleName, int accessTypeFull);
    @CommitAfter
    public abstract void removeRef(Long id);
    @CommitAfter
    public abstract List<Ref> getRefs(Account account);
    @CommitAfter
    public abstract Ref find(Account account, String scheduleName);
    @CommitAfter
    public abstract Ref find(Long id);
    @CommitAfter
    public abstract List<Ref> getAll();
    @CommitAfter
    public abstract void update(Ref ref);
    @CommitAfter
    public abstract List<Ref> getRefs(String scheduleName);
}
