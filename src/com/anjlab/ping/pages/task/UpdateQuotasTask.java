package com.anjlab.ping.pages.task;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.anjlab.ping.entities.Account;
import com.anjlab.ping.services.AppModule;
import com.anjlab.ping.services.dao.AccountDAO;

@Meta(AppModule.NO_MARKUP)
public class UpdateQuotasTask {

    @Inject
    private AccountDAO accountDAO;
    
    public void onActivate() throws URISyntaxException {
        List<Account> accounts = accountDAO.getAll();
        for (Account account : accounts) {
            if (!account.hasAnyQuotaLimitsApplied()) {
                account.setDefaultQuotas();
                accountDAO.update(account);
            }
        }
    }
}
