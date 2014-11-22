package models;

import helpers.DynamoHelper;
import helpers.HashingHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public class Event {

	static final String TABLE_NAME = "supnyc_events";
	public static final String[] TYPES = new String[] {
		"party","bar","dining","culture"
	};
	
	private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("MMM dd HH:mm");
	
	private String mTitle;
	private String mDescription;
	private String mAddress;
	private Double mLatitude;
	private Double mLongitude;
	private String mHostUsername;
	private Long mStart;
	private Long mEnd;
	private String mType;
	private String mImageUrl;
	private Long mLikeCount;
	private Long mIntendToGoCount;
	
	// after calling valid() all the validation errors are stored here
	private ArrayList<String> mValidationErrors;
	
	// indicates whether the user has been created now or has been loaded from Dynamo
	private boolean mNewRecord;
	
	public Event() {
		mValidationErrors = new ArrayList<String>();
		mNewRecord = true;
		mLikeCount = 0L;
		mIntendToGoCount = 0L;
	}

	public void setTitle(String title) { mTitle = title; }
	public String getTitle() { return mTitle; }
	
	public void setDescription(String description) { mDescription = description; }
	public String getDescription() { return mDescription; }
	
	public void setAddress(String address) { mAddress = address; }
	public String getAddress() { return mAddress; }
	
	public void setLatitude(Double latitude) { mLatitude = latitude; }
	public void setLatitude(String latitude) { mLatitude = Double.valueOf(latitude); }
	public Double getLatitude() { return mLatitude; }
	
	public void setLongitude(Double longitude) { mLongitude = longitude; }
	public void setLongitude(String longitude) { mLongitude = Double.valueOf(longitude); }
	public Double getLongitude() { return mLongitude; }
	
	public void setHostUsername(String hostUsername) { mHostUsername = hostUsername; }
	public String getHostUsername() { return mHostUsername; }
	
	public void setType(String type) { mType = type; }
	public String getType() { return mType; }
	
	public void setImageUrl(String imageUrl) { mImageUrl = imageUrl; }
	public String getImageUrl() { return mImageUrl; }
	
	public void setStart(Long start) { mStart = start; }
	public Long getStart() { return mStart; }
	public void setStart(String start) throws ParseException { mStart = sSimpleDateFormat.parse(start).getTime(); }
	public String getFormattedStart() { 
		return (mStart != null ? sSimpleDateFormat.format(new Date(mStart)) : "");
	}
	
	public void setEnd(Long end) { mEnd = end; }
	public Long getEnd() { return mEnd; }
	public void setEnd(String end) throws ParseException { mEnd = sSimpleDateFormat.parse(end).getTime(); }
	public String getFormattedEnd() { 
		return (mEnd != null ? sSimpleDateFormat.format(new Date(mEnd)) : "");
	}
	
	public void setLikeCount(Long likeCount) { mLikeCount = likeCount; }
	public void setLikeCount(String likeCount) { mLikeCount = Long.valueOf(likeCount); }
	public Long getLikeCount() { return mLikeCount; }
	
	public void setIntendToGoCount(Long intendToGoCount) { mIntendToGoCount = intendToGoCount; }
	public void setIntendToGoCount(String intendToGoCount) { mIntendToGoCount = Long.valueOf(intendToGoCount); }
	public Long getIntendToGoCount() { return mIntendToGoCount; }
	
	/**
	 * Generate range key that is composed by:
	 *  - 10 digits for start (in seconds since 1970)
	 *  - 10 digits for host username (padded with spaces to 10 digits)
	 *  - rest is a MD5 hash of title
	 */
	public String getRangeKeyBeginningWithStart() {
		if (mStart == null || mStart == 0L || mHostUsername == null || mHostUsername.isEmpty() || mTitle == null || mTitle.isEmpty()) {
			return null;
		}
		
		String startString = String.format("%010d", mStart);
		String usernameString = StringUtils.rightPad(mHostUsername, 10, ' ');
		return startString + usernameString + HashingHelper.hashString(mTitle);
	}
	
	/**
	 * Generate range key that is composed by:
	 *  - 10 digits for host username (padded with spaces to 10 digits)
	 *  - 10 digits for start (in seconds since 1970)
	 *  - rest is a MD5 hash of title
	 */
	public String getRangeKeyBeginningWithHost() {
		if (mStart == null || mStart == 0L || mHostUsername == null || mHostUsername.isEmpty() || mTitle == null || mTitle.isEmpty()) {
			return null;
		}
		
		String startString = String.format("%010d", mStart);
		String usernameString = StringUtils.rightPad(mHostUsername, 10, ' ');
		return usernameString + startString + HashingHelper.hashString(mTitle);
	}
	
	/**
	 * Verifies if the event is valid (i.e. ready to be saved).
	 * If there are errors, they are put in mValidationErrors and can be seen by 
	 * calling getValidationErrors() and getValidationErrorMessages().
	 */
	public boolean valid() {
		mValidationErrors.clear();
		
		// Title should not be empty
		if (mTitle == null || mTitle.isEmpty()) { mValidationErrors.add("Title should not be empty."); }
		// Address should not be empty
		if (mAddress == null || mAddress.isEmpty()) { mValidationErrors.add("Address should not be empty."); }
		// HostEmail should not be empty
		if (mHostUsername == null || mHostUsername.isEmpty()) { mValidationErrors.add("Host email should not be empty."); }
		// Start should not be empty
		if (mStart == null || mStart == 0L) { mValidationErrors.add("Start time should be set."); }
		// Type should be valid
		if (mType == null) {
			mValidationErrors.add("Event type should not be empty.");
		} else {
			boolean typeExists = false;
			for (int i=0; i<TYPES.length; i++) {
				if (TYPES[i].equals(mType)) {
					typeExists = true;
					break;
				}
			}
			if (!typeExists) {
				mValidationErrors.add("Type is not valid (" + mType + ")");
			}
		}
		
		// only if everything else is valid we are going to check if event is unique
		if (mValidationErrors.isEmpty()) {
			try {
				DynamoHelper dynamoHelper = DynamoHelper.getInstance();
				if (dynamoHelper.getItemByPrimaryKeyAndRange(TABLE_NAME, "type", mType, "range_key_beginning_with_start", getRangeKeyBeginningWithStart()) != null) {
					mValidationErrors.add("There is an event with that name at that same time already.");
				}
			} catch (IOException e) {
				e.printStackTrace();
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
			errorMessage.append("Event could not be created:");
		} else {
			errorMessage.append("Event could not be updated:");
		}
		
		errorMessage.append("<ul>");
		for (String str : getValidationErrors()) {
			errorMessage.append("<li>" + str + "</li>");
		}
		errorMessage.append("</ul>");
		
		return errorMessage.toString();
	}
	
	/**
	 * Saves the event in DynamoDB, but only does that if the event is valid.
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
		attrMap.put("title", new AttributeValue().withS(mTitle));
		attrMap.put("description", new AttributeValue().withS(mDescription));
		attrMap.put("address", new AttributeValue().withS(mAddress));
		attrMap.put("host_username", new AttributeValue().withS(mHostUsername));
		attrMap.put("type", new AttributeValue().withS(mType));
		attrMap.put("like_count", new AttributeValue().withN(String.valueOf(mLikeCount)));
		attrMap.put("intend_to_go_count", new AttributeValue().withN(String.valueOf(mIntendToGoCount)));
		if (mLatitude != null)
			attrMap.put("latitude", new AttributeValue().withN(String.valueOf(mLatitude)));
		if (mLongitude != null)
			attrMap.put("longitude", new AttributeValue().withN(String.valueOf(mLongitude)));
		if (mImageUrl != null)
			attrMap.put("image_url", new AttributeValue().withS(mImageUrl));
		if (mStart != null)
			attrMap.put("start", new AttributeValue().withN(String.valueOf(mStart)));
		if (mEnd != null)
			attrMap.put("end", new AttributeValue().withN(String.valueOf(mEnd)));
		
		return attrMap;
	}
	
	static public Event buildFromForm(Map<String, String[]> attrMap) throws ParseException {
		Event event = new Event();
		if (attrMap.containsKey("title")) { event.setTitle(attrMap.get("title")[0]); }
		if (attrMap.containsKey("description")) { event.setDescription(attrMap.get("description")[0]); }
		if (attrMap.containsKey("address")) { event.setAddress(attrMap.get("address")[0]); }
		if (attrMap.containsKey("latitude")) { event.setLatitude(attrMap.get("latitude")[0]); }
		if (attrMap.containsKey("longitude")) { event.setLongitude(attrMap.get("longitude")[0]); }
		if (attrMap.containsKey("host_username")) { event.setHostUsername(attrMap.get("host_username")[0]); }
		if (attrMap.containsKey("type")) { event.setAddress(attrMap.get("type")[0]); }
		if (attrMap.containsKey("image_url")) { event.setHostUsername(attrMap.get("image_url")[0]); }
		if (attrMap.containsKey("start")) { event.setStart(attrMap.get("start")[0]); }
		if (attrMap.containsKey("end")) { event.setStart(attrMap.get("end")[0]); }
		return event;
	} 
	
	static public Event buildFromDynamo(Map<String, AttributeValue> attrMap) {
		Event event = new Event();
		if (attrMap.containsKey("title")) { event.setTitle(attrMap.get("title").getS()); }
		if (attrMap.containsKey("description")) { event.setDescription(attrMap.get("description").getS()); }
		if (attrMap.containsKey("address")) { event.setAddress(attrMap.get("address").getS()); }
		if (attrMap.containsKey("latitude")) { event.setLatitude(attrMap.get("latitude").getN()); }
		if (attrMap.containsKey("longitude")) { event.setLongitude(attrMap.get("longitude").getN()); }
		if (attrMap.containsKey("host_username")) { event.setHostUsername(attrMap.get("host_username").getS()); }
		if (attrMap.containsKey("type")) { event.setType(attrMap.get("type").getS()); }
		if (attrMap.containsKey("image_url")) { event.setImageUrl(attrMap.get("image_url").getS()); }
		if (attrMap.containsKey("like_count")) { event.setImageUrl(attrMap.get("like_count").getN()); }
		if (attrMap.containsKey("intend_to_go_count")) { event.setImageUrl(attrMap.get("intend_to_go_count").getN()); }
		try {
			if (attrMap.containsKey("start")) { event.setStart(attrMap.get("start").getN()); }
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			if (attrMap.containsKey("end")) { event.setEnd(attrMap.get("end").getN()); }
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return event;
	}
	
	/**
	 * Checks if the events table exists in Dynamo and if not create the table.
	 * @throws IOException 
	 */
	static public void ensureTableExists() throws IOException {
		DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
		if (!dynamoHelper.checkIfTableExists(TABLE_NAME)) {
			// create a hash key for the type (string) and a range key
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("type").withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("range_key_beginning_with_start").withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("index_key_beginning_with_host").withAttributeType("S"));
			
			// specify that the key is of type hash
			ArrayList<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
			keySchemaElements.add(new KeySchemaElement().withAttributeName("type").withKeyType(KeyType.HASH));
			keySchemaElements.add(new KeySchemaElement().withAttributeName("range_key_beginning_with_start").withKeyType(KeyType.RANGE));
			
			// create local secondary index
			ArrayList<KeySchemaElement> indexKeySchema = new ArrayList<KeySchemaElement>();
			indexKeySchema.add(new KeySchemaElement().withAttributeName("type").withKeyType(KeyType.HASH));
			indexKeySchema.add(new KeySchemaElement().withAttributeName("index_key_beginning_with_host").withKeyType(KeyType.RANGE));
			
			// create table
			dynamoHelper.createTable(attributeDefinitions, keySchemaElements, 5L, 5L, TABLE_NAME);
		}
	}
	
}
