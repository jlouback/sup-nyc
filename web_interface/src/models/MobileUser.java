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

public class MobileUser {
	private static final String TABLE_NAME = "supnyc_mobile_users";
	
	private String mEmail;
	private String mEncryptedPassword;
	private String mPasswordJustSet;
	
	// after calling valid() all the validation errors are stored here
	private ArrayList<String> mValidationErrors;
	
	public MobileUser() {
		mValidationErrors = new ArrayList<String>();
	}
	
	public String getEmail() { return mEmail; }
	public void setEmail(String email) { mEmail = email; }
	
	public void setPassword(String password) {
		mPasswordJustSet = password;
		this.setEncryptedPassword(HashingHelper.hashString(password));
	}
	
	public boolean checkPassword(String password) {
		return HashingHelper.hashString(password).equals(mEncryptedPassword);
	}
	public boolean checkEncryptedPassword(String encryptedPassword) {
		return encryptedPassword.equals(mEncryptedPassword);
	}
	
	public void setEncryptedPassword(String encryptedPassword) { mEncryptedPassword = encryptedPassword; }
	public String getEncryptedPassword() { return mEncryptedPassword; }
	
	public boolean valid() {
		mValidationErrors.clear();
		
		// Email should not be empty
		if (mEmail == null || mEmail.isEmpty()) {
			mValidationErrors.add("Email should not be empty.");
		}
		
		// Password should not be empty
		if (mEncryptedPassword == null) {
			mValidationErrors.add("Must set a password.");
		}
//		else if (mPasswordJustSet == null || mPasswordJustSet.length() < 6) {
//			mValidationErrors.add("Password must be at least 6 characters long.");
//		}
		
		// is valid if there are no errors
		return mValidationErrors.size() == 0;
	}
	
	public ArrayList<String> getValidationErrors() {
		return mValidationErrors;
	}
	
	public String getValidationErrorMessage() {
		StringBuffer errorMessage = new StringBuffer();
		
		errorMessage.append("Mobile user could not be created");
		
		for (String str : getValidationErrors()) {
			errorMessage.append(", " + str);
		}
		errorMessage.append(".");
		
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
				return dynamoHelper.conditionalPutItem(TABLE_NAME, "email", this.getAttributeMap());
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
		attrMap.put("email", new AttributeValue().withS(mEmail));
		attrMap.put("encrypted_password", new AttributeValue().withS(mEncryptedPassword));
		return attrMap;
	}
	
	static public MobileUser buildFromDynamo(Map<String, AttributeValue> attrMap) {
		MobileUser user = new MobileUser();
		user.setEmail(attrMap.get("email").getS());
		user.setEncryptedPassword(attrMap.get("encrypted_password").getS());
		return user;
	}
	
	static public MobileUser loadFromDynamo(String email) {
		try {
			DynamoHelper dynamoHelper = DynamoHelper.getInstance();
			Map<String, AttributeValue> attrMap = dynamoHelper.getItemByPrimaryKey(TABLE_NAME, "email", email);
			
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
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("email").withAttributeType("S"));
			
			// specify that the key is of type hash
			ArrayList<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
			keySchemaElements.add(new KeySchemaElement().withAttributeName("email").withKeyType(KeyType.HASH));
			
			// create table
			dynamoHelper.createTable(attributeDefinitions, keySchemaElements, 1L, 1L, TABLE_NAME);
		}
	}
	
}
