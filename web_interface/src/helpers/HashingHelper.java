package helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashingHelper {

	public static String hashString(String string) {
		StringBuffer hexString = new StringBuffer();
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(string.getBytes());
			byte[] digest = messageDigest.digest();
			
			for (int i=0; i<digest.length; i++) {
				if ((0xff & digest[i]) < 0x10) {
					hexString.append("0" + Integer.toHexString((0xFF & digest[i])));
				} else {
					hexString.append(Integer.toHexString(0xFF & digest[i]));
				}
			}
			return hexString.toString();
		} 
		catch (NoSuchAlgorithmException e) {
			// if the algorithm is not available does not encrypt
			return string;
		}
	}
}
