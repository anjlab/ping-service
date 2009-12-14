package dmitrygusev.ping.services;

public class NotAuthorizedException extends RuntimeException {

	public NotAuthorizedException(String message) {
		super(message);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
