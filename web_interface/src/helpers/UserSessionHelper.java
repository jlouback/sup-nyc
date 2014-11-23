package helpers;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.servlet.http.HttpSession;

import models.User;

public class UserSessionHelper {

	private static final String USER_SESSION_IDENTIFIER_NAME = "user_session_username";
	private static final String USER_SESSION_KEY_NAME = "user_session_key";
	
	/**
	 * Logs this user in.
	 * This method creates a random login key that is persisted in the database and 
	 * kept in the session to keep the user logged in. Returns whether the operation
	 * was successful.
	 */
	public static boolean login(User user, HttpSession session) {
		String sessionKey = generateSessionKey();
		user.setCurrentSessionKey(sessionKey);
		if (user.save()) {
			session.setAttribute(USER_SESSION_IDENTIFIER_NAME, user.getUsername());
			session.setAttribute(USER_SESSION_KEY_NAME, sessionKey);
			return true;
		}
		return false;
	}
	
	public static void logoff(User user, HttpSession session) {
		session.removeAttribute(USER_SESSION_IDENTIFIER_NAME);
		session.removeAttribute(USER_SESSION_KEY_NAME);
		
		if (user != null) {
			user.setCurrentSessionKey(null);
			user.save();
		}
	}
	
	/**
	 * Retrieves the current logged in user.
	 * In case some info does not match, ensure the session is cleared.
	 */
	public static User getLoggedUser(HttpSession session) {
		String username = (String) session.getAttribute(USER_SESSION_IDENTIFIER_NAME);
		if (username != null) {
			String key = (String) session.getAttribute(USER_SESSION_KEY_NAME);
			if (key != null) {
				User user = User.loadFromDynamo(username);
				if (user != null && user.getCurrentSessionKey().equals(key)) {
					return user;
				}
			}
		}
		session.removeAttribute(USER_SESSION_IDENTIFIER_NAME);
		session.removeAttribute(USER_SESSION_KEY_NAME);
		return null;
	}
	
	private static SecureRandom random = new SecureRandom();
	
	private static String generateSessionKey() {
		return new BigInteger(130, random).toString(32);
	}
	
}
