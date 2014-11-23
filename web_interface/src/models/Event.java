package models;

import helpers.DynamoHelper;
import helpers.HashingHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Event {

	static final String TABLE_NAME = "supnyc_events";
	public static final String[] TYPES = new String[] {
		"party","bar","dining","culture"
	};
	
	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm a");
	
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
	private Long mDislikeCount;
	private Long mGoingCount;
	
	// after calling valid() all the validation errors are stored here
	private ArrayList<String> mValidationErrors;
	
	// indicates whether the user has been created now or has been loaded from Dynamo
	private boolean mNewRecord;
	
	private String mRangeKeyBeginningWithStart;
	
	public Event() {
		mValidationErrors = new ArrayList<String>();
		mNewRecord = true;
		mLikeCount = 0L;
		mDislikeCount = 0L;
		mGoingCount = 0L;
	}

	public void setTitle(String title) { mTitle = title; }
	public String getTitle() { return mTitle; }
	
	public void setDescription(String description) { mDescription = description; }
	public String getDescription() { return mDescription; }
	
	public void setAddress(String address) { mAddress = address; }
	public void setAddressFromForm(String address) { mAddress = (address.isEmpty() ? null : address + ", New York, NY"); }
	public String getAddress() { return mAddress; }
	public String getAddressNoCity() { 
		return (mAddress != null ? mAddress.replaceAll(",\\sNew\\sYork,\\sNY$", "") : "");
	}
	
	public void setLatitude(Double latitude) { mLatitude = latitude; }
	public void setLatitude(String latitude) { mLatitude = (latitude.isEmpty() ? null : Double.valueOf(latitude)); }
	public Double getLatitude() { return mLatitude; }
	
	public void setLongitude(Double longitude) { mLongitude = longitude; }
	public void setLongitude(String longitude) { mLongitude = (longitude.isEmpty() ? null : Double.valueOf(longitude)); }
	public Double getLongitude() { return mLongitude; }
	
	public void setHostUsername(String hostUsername) { mHostUsername = hostUsername; }
	public String getHostUsername() { return mHostUsername; }
	
	public void setType(String type) { mType = type; }
	public String getType() { return mType; }
	
	public void setImageUrl(String imageUrl) { mImageUrl = imageUrl; }
	public String getImageUrl() { return mImageUrl; }
	
	public void setStart(Long start) { mStart = start; }
	public Long getStart() { return mStart; }
	public void setStart(String start) throws ParseException { 
		mStart = (start.isEmpty() ? null : sDateFormat.parse(start).getTime()); 
	}
	public String getFormattedStart() { 
		return (mStart != null ? sDateFormat.format(new Date(mStart)) : "");
	}
	
	public void setEnd(Long end) { mEnd = end; }
	public Long getEnd() { return mEnd; }
	public void setEnd(String end) throws ParseException { 
		mEnd = (end.isEmpty() ? null : sDateFormat.parse(end).getTime()); 
	}
	public String getFormattedEnd() { 
		return (mEnd != null ? sDateFormat.format(new Date(mEnd)) : "");
	}
	
	public void setLikeCount(Long likeCount) { mLikeCount = likeCount; }
	public void setLikeCount(String likeCount) { mLikeCount = (likeCount.isEmpty() ? null : Long.valueOf(likeCount)); }
	public Long getLikeCount() { return mLikeCount; }
	
	public void setDislikeCount(Long dislikeCount) { mDislikeCount = dislikeCount; }
	public void setDislikeCount(String dislikeCount) { mDislikeCount = (dislikeCount.isEmpty() ? null : Long.valueOf(dislikeCount)); }
	public Long getDislikeCount() { return mDislikeCount; }
	
	public void setGoingCount(Long GoingCount) { mGoingCount = GoingCount; }
	public void setGoingCount(String GoingCount) { mGoingCount = (GoingCount.isEmpty() ? null : Long.valueOf(GoingCount)); }
	public Long getGoingCount() { return mGoingCount; }
	
	public void setRangeKeyBeginningWithStart(String rangeKey) { mRangeKeyBeginningWithStart = rangeKey; }
	
	public void markAsNotNewRecord() {
		mNewRecord = false;
	}
	
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
	public String getIndexKeyBeginningWithHost() {
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
		// Start should be before end
		if (mStart != null && mEnd != null && mStart > mEnd) { mValidationErrors.add("End time should after start time."); }
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
		if (mValidationErrors.isEmpty() && mNewRecord) {
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
		if (mDescription != null && !mDescription.isEmpty())
			attrMap.put("description", new AttributeValue().withS(mDescription));
		attrMap.put("address", new AttributeValue().withS(mAddress));
		attrMap.put("host_username", new AttributeValue().withS(mHostUsername));
		attrMap.put("type", new AttributeValue().withS(mType));
		attrMap.put("like_count", new AttributeValue().withN(String.valueOf(mLikeCount)));
		attrMap.put("dislike_count", new AttributeValue().withN(String.valueOf(mDislikeCount)));
		attrMap.put("going_count", new AttributeValue().withN(String.valueOf(mGoingCount)));
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
		
		attrMap.put("range_key_beginning_with_start", new AttributeValue().withS(getRangeKeyBeginningWithStart()));
		attrMap.put("index_key_beginning_with_host", new AttributeValue().withS(getIndexKeyBeginningWithHost()));
		
		return attrMap;
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		try {
			json.put("title", mTitle);
			json.put("description", mDescription);
			json.put("address", mAddress);
			json.put("latitude", mLatitude);
			json.put("longitude", mLongitude);
			json.put("host_username", mHostUsername);
			json.put("type", mType);
			json.put("like_count", mLikeCount);
			json.put("dislike_count", mDislikeCount);
			json.put("going_count", mGoingCount);
			json.put("image_url", mImageUrl);
			json.put("start", mStart);
			json.put("end", mEnd);
			json.put("range_key", mRangeKeyBeginningWithStart);
			return json;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static public Event buildFromForm(Map<String, String[]> attrMap) throws ParseException {
		Event event = new Event();
		if (attrMap.containsKey("title")) { event.setTitle(attrMap.get("title")[0]); }
		if (attrMap.containsKey("description")) { event.setDescription(attrMap.get("description")[0]); }
		if (attrMap.containsKey("address")) { event.setAddressFromForm(attrMap.get("address")[0]); }
		if (attrMap.containsKey("latitude")) { event.setLatitude(attrMap.get("latitude")[0]); }
		if (attrMap.containsKey("longitude")) { event.setLongitude(attrMap.get("longitude")[0]); }
		if (attrMap.containsKey("host_username")) { event.setHostUsername(attrMap.get("host_username")[0]); }
		if (attrMap.containsKey("type")) { event.setType(attrMap.get("type")[0]); }
		if (attrMap.containsKey("image_url")) { event.setHostUsername(attrMap.get("image_url")[0]); }
		if (attrMap.containsKey("start")) { event.setStart(attrMap.get("start")[0]); }
		if (attrMap.containsKey("end")) { event.setEnd(attrMap.get("end")[0]); }
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
		if (attrMap.containsKey("like_count")) { event.setLikeCount(attrMap.get("like_count").getN()); }
		if (attrMap.containsKey("dislike_count")) { event.setDislikeCount(attrMap.get("dislike_count").getN()); }
		if (attrMap.containsKey("going_count")) { event.setGoingCount(attrMap.get("going_count").getN()); }
		if (attrMap.containsKey("start")) { event.setStart(Long.valueOf(attrMap.get("start").getN())); }
		if (attrMap.containsKey("end")) { event.setEnd(Long.valueOf(attrMap.get("end").getN())); }
		if (attrMap.containsKey("range_key_beginning_with_start")) { event.setRangeKeyBeginningWithStart(attrMap.get("range_key_beginning_with_start").getS());}
		event.markAsNotNewRecord();
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
			
			Projection projection = new Projection().withProjectionType(ProjectionType.INCLUDE);
			ArrayList<String> nonKeyAttributes = new ArrayList<String>();
			nonKeyAttributes.add("title");
			nonKeyAttributes.add("description");
			nonKeyAttributes.add("address");
			nonKeyAttributes.add("latitude");
			nonKeyAttributes.add("longitude");
			nonKeyAttributes.add("host_username");
			nonKeyAttributes.add("type");
			nonKeyAttributes.add("image_url");
			nonKeyAttributes.add("start");
			nonKeyAttributes.add("end");
			nonKeyAttributes.add("like_count");
			nonKeyAttributes.add("dislike_count");
			nonKeyAttributes.add("going_count");
			projection.setNonKeyAttributes(nonKeyAttributes);
			
			LocalSecondaryIndex localSecondaryIndex = new LocalSecondaryIndex()
				.withIndexName("by_host_index")
				.withKeySchema(indexKeySchema)
				.withProjection(projection);
			
			ArrayList<LocalSecondaryIndex> localSecondaryIndexes = new ArrayList<LocalSecondaryIndex>();
			localSecondaryIndexes.add(localSecondaryIndex);
			
			// create table
			dynamoHelper.createTable(attributeDefinitions, keySchemaElements, 5L, 5L, TABLE_NAME, localSecondaryIndexes);
		}
	}
	
	static public Event loadSingle(String type, String rangeKey) {
		try {
			DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
			Map<String, AttributeValue> attributes = dynamoHelper.getItemByPrimaryKeyAndRange(
				TABLE_NAME, 
				"type", type, 
				"range_key_beginning_with_start", rangeKey
			);
			
			if (attributes != null) {
				Event event = Event.buildFromDynamo(attributes);
				return event;
			}
			else {
				return null;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static public List<Event> loadAllFromUser(User user, String type) {
		try {
			DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
			List<Map<String, AttributeValue>> attributesList = dynamoHelper.queryByPrimaryKeyAndIndexPrefix(
				TABLE_NAME, 
				"type", type, 
				"index_key_beginning_with_host", user.getUsername(),
				"by_host_index"
			);
			
			List<Event> events = new ArrayList<Event>(attributesList.size());
			for (Map<String, AttributeValue> attributes : attributesList) {
				events.add(Event.buildFromDynamo(attributes));
			}
			return events;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<Event>(1);
		}
	}
	
	static public List<Event> loadAllInRange(String type, Long start, Long end) {
		try {
			DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
			List<Map<String, AttributeValue>> attributesList = dynamoHelper.queryByPrimaryKeyAndRangeKey(
				TABLE_NAME, 
				"type", type, 
				"range_key_beginning_with_start", 
				String.format("%010d", start), 
				String.format("%010d", end)
			);
			
			List<Event> events = new ArrayList<Event>(attributesList.size());
			for (Map<String, AttributeValue> attributes : attributesList) {
				events.add(Event.buildFromDynamo(attributes));
			}
			return events;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<Event>(1);
		}
	}
	
	static public JSONArray toJson(List<Event> events) {
		JSONArray json = new JSONArray();
		for (Event event : events) {
			JSONObject eventJson = event.toJson();
			if (eventJson != null)
				json.put(eventJson);
		}
		return json;
	}
	
	
}
