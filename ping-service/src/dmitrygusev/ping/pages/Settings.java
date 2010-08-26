package dmitrygusev.ping.pages;

import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.AfterRender;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.services.Application;
import dmitrygusev.ping.services.Utils;
import dmitrygusev.ping.services.dao.AccountDAO;

public class Settings {

    @Property
    @Persist(PersistenceConstants.FLASH)
    @SuppressWarnings("unused")
    private String message;

    @Property
    @Persist(PersistenceConstants.FLASH)
    @SuppressWarnings("unused")
    private String messageColor;

    @Property
    @SuppressWarnings("unused")
    private final String timeZoneModel = Utils.getTimeZoneModel();
    
    @AfterRender
    public void cleanup() {
        userAccount = null;
    }
    
    @Inject
    private Application application;
    
    private Account userAccount;
    
    public Account getUserAccount() {
        if (userAccount == null) {
            userAccount = application.getUserAccount();
        }
        return userAccount;
    }
    
    @Inject
    private AccountDAO accountDAO;
    
    public void onSuccess() {
        try {
            accountDAO.update(getUserAccount());

            this.message = "Saved";
            this.messageColor = "green";
        } catch (Exception e) {
            this.message = e.getMessage();
            this.messageColor = "red";
        }
    }
    
}
