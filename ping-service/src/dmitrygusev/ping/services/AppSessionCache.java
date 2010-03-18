package dmitrygusev.ping.services;

import java.io.Serializable;

import dmitrygusev.ping.entities.Account;
import dmitrygusev.ping.services.dao.AccountDAO;

public class AppSessionCache implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Account account;
	
	public Account getUserAccount(GAEHelper helper, AccountDAO accountDAO) {
		String principalName = helper.getUserPrincipal().getName();
		if (account == null ||
				//	Other user logged in within current session
				!account.getEmail().equals(principalName)) {
			account = Utils.isNullOrEmpty(principalName) ? null : accountDAO.getAccount(principalName);
		}
		return account;
	}

}
