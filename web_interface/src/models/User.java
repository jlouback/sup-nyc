package models;

import helpers.DynamoHelper;
import helpers.HashingHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public class User {
	private static final String TABLE_NAME = "supnyc_users";
	
	private String mUsername;
	private String mEmail;
	private String mEncryptedPassword;
	private boolean mActivated;
	private String mCurrentSessionKey;
	
	// attributes that store the decrypted password only if the user is being created now, otherwise are null
	private String mPasswordJustSet; 
	private String mPasswordJustSetConfirmation;
	
	// after calling valid() all the validation errors are stored here
	private ArrayList<String> mValidationErrors;
	
	// indicates whether the user has been created now or has been loaded from Dynamo
	private boolean mNewRecord;
	
	public User() {
		mValidationErrors = new ArrayList<String>();
		mNewRecord = true;
		mActivated = false;
		mCurrentSessionKey = null;
	}
	
	public String getUsername() {return mUsername;}
	public void setUsername(String username) {mUsername =username;}
	
	public String getEmail() {return mEmail;}
	public void setEmail(String email) {mEmail = email;}
	
	public void setPassword(String password) {
		mPasswordJustSet = password;
		this.setEncryptedPassword(HashingHelper.hashString(password));
	}
	public String getPassword() { return mPasswordJustSet; }
	public void setPasswordConfirmation(String passwordConfirmation) { mPasswordJustSetConfirmation = passwordConfirmation;	}
	public String getPasswordConfirmation() { return mPasswordJustSetConfirmation; }
	
	public boolean checkPassword(String password) {
		return HashingHelper.hashString(password).equals(mEncryptedPassword);
	}
	
	public void setEncryptedPassword(String encryptedPassword) { mEncryptedPassword = encryptedPassword; }
	public String getEncryptedPassword() { return mEncryptedPassword; }
	
	public void markAsNotNewRecord() {
		mNewRecord = false;
	}
	
	public void setCurrentSessionKey(String key) { mCurrentSessionKey = key; }
	public String getCurrentSessionKey() { return mCurrentSessionKey; }
	
	public void setActivated(boolean activated) { mActivated = activated; }
	public boolean getActivated() { return mActivated; }
	
	private static final String EMAIL_REGEXP = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final String COLUMBIA_EMAIL_REGEXP = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@columbia.edu";
	
	/**
	 * Verifies if the user is valid (i.e. ready to be saved).
	 * If there are errors, they are put in mValidationErrors and can be seen by 
	 * calling getValidationErrors() and getValidationErrorMessages().
	 */
	public boolean valid() {
		mValidationErrors.clear();
		
		// Username should not be empty
		if (mUsername == null || mUsername.isEmpty()) {
			mValidationErrors.add("Username should not be empty.");
		}
		// Username should not have more than 10 characters
		else if (mUsername.length() > 10) {
			mValidationErrors.add("Username should have 10 characters or less.");
		}
		// Username should not have special characters
		else if (mUsername.matches(".*[^a-zA-Z0-9].*")) {
			mValidationErrors.add("Username should not have special characters or punctuation.");
		}
		else if (mNewRecord){
			try {
				DynamoHelper dynamoHelper = DynamoHelper.getInstance();
				if (dynamoHelper.getItemByPrimaryKey(TABLE_NAME, "username", mUsername) != null) {
					mValidationErrors.add("This username has already been taken.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Email should be from @columbia.edu
		if (mEmail == null || mEmail.isEmpty()) {
			mValidationErrors.add("Email should not be empty.");
		}
		else if (!mEmail.matches(EMAIL_REGEXP)) {
			mValidationErrors.add("Email is not valid.");
		}
		else if (!mEmail.matches(COLUMBIA_EMAIL_REGEXP)) {
			mValidationErrors.add("Email must be from Columbia.");
		}
		
		// Password should not be empty
		if (mEncryptedPassword == null) {
			mValidationErrors.add("Must set a password.");
		}
		else if (mNewRecord) {
			// Password confirmation must match
			if (mPasswordJustSet == null || !mPasswordJustSet.equals(mPasswordJustSetConfirmation)) {
				mValidationErrors.add("Password confirmation did not match password.");
			}
			// Password should be at least 6 chars long
			else if (mPasswordJustSet == null || mPasswordJustSet.length() < 6) {
				mValidationErrors.add("Password must be at least 6 characters long.");
			}
		}
		
		// is valid if there are no errors
		return mValidationErrors.size() == 0;
	}
	
	public ArrayList<String> getValidationErrors() {
		return mValidationErrors;
	}
	
	public String getValidationErrorMessage() {
		StringBuffer errorMessage = new StringBuffer();
		
		if (mNewRecord) {
			errorMessage.append("Account could not be created:");
		} else {
			errorMessage.append("Account could not be updated:");
		}
		
		errorMessage.append("<ul>");
		for (String str : getValidationErrors()) {
			errorMessage.append("<li>" + str + "</li>");
		}
		errorMessage.append("</ul>");
		
		return errorMessage.toString();
	}
	
	/**
	 * Saves the user in DynamoDB, but only does that if the user is valid.
	 * Returns true if has successfully saved, otherwise false
	 */
	public boolean save() {
		try {
			if (this.valid()) {
				DynamoHelper dynamoHelper = DynamoHelper.getInstance();
				return dynamoHelper.putItem(TABLE_NAME, this.getAttributeMap());
			}
			else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public Map<String, AttributeValue> getAttributeMap() {
		Map<String, AttributeValue> attrMap = new HashMap<String, AttributeValue>();
		attrMap.put("username", new AttributeValue().withS(mUsername));
		attrMap.put("email", new AttributeValue().withS(mEmail));
		attrMap.put("encrypted_password", new AttributeValue().withS(mEncryptedPassword));
		attrMap.put("activated", new AttributeValue().withBOOL(mActivated));
		if (mCurrentSessionKey != null)
			attrMap.put("current_session_key", new AttributeValue().withS(mCurrentSessionKey));
		return attrMap;
	}
	
	static public User buildFromForm(Map<String, String[]> attrMap) {
		User user = new User();
		if (attrMap.containsKey("username")) { user.setUsername(attrMap.get("username")[0]); }
		if (attrMap.containsKey("email")) { user.setEmail(attrMap.get("email")[0]); }
		if (attrMap.containsKey("password")) { user.setPassword(attrMap.get("password")[0]); }
		if (attrMap.containsKey("password_confirmation")) { user.setPasswordConfirmation(attrMap.get("password_confirmation")[0]); }
		return user;
	} 
	
	static public User buildFromDynamo(Map<String, AttributeValue> attrMap) {
		User user = new User();
		user.setUsername(attrMap.get("username").getS());
		user.setEmail(attrMap.get("email").getS());
		user.setEncryptedPassword(attrMap.get("encrypted_password").getS());
		user.setActivated(attrMap.get("activated").getBOOL());
		if (attrMap.containsKey("current_session_key"))
			user.setCurrentSessionKey(attrMap.get("current_session_key").getS());
		user.markAsNotNewRecord();
		return user;
	}
	
	static public User loadFromDynamo(String username) {
		try {
			DynamoHelper dynamoHelper = DynamoHelper.getInstance();
			Map<String, AttributeValue> attrMap = dynamoHelper.getItemByPrimaryKey(TABLE_NAME, "username", username);
			
			if (attrMap != null) {
				return buildFromDynamo(attrMap);
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Checks if the users table exists in Dynamo and if not create the table.
	 * @throws IOException 
	 */
	static public void ensureTableExists() throws IOException {
		DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
		if (!dynamoHelper.checkIfTableExists(TABLE_NAME)) {
			// create a hash key for the email (string)
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("username").withAttributeType("S"));
			
			// specify that the key is of type hash
			ArrayList<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
			keySchemaElements.add(new KeySchemaElement().withAttributeName("username").withKeyType(KeyType.HASH));
			
			// create table
			dynamoHelper.createTable(attributeDefinitions, keySchemaElements, 2L, 2L, TABLE_NAME);
		}
	}
	
}
