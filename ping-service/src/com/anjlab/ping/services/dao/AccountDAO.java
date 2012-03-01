package com.anjlab.ping.services.dao;

import java.util.List;

import org.apache.tapestry5.jpa.annotations.CommitAfter;

import com.anjlab.ping.entities.Account;


public interface AccountDAO {
    @CommitAfter
    public abstract Account find(Long id);
    @CommitAfter
    public abstract Account getAccount(String email);
    @CommitAfter
    public abstract void update(Account account);
    @CommitAfter
    public abstract void delete(Long id);
    @CommitAfter
    public abstract List<Account> getAll();

}