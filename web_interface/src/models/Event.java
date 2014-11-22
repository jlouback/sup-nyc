package models;

import helpers.HashingHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class Event {

	static final String TABLE_NAME = "supnyc_events";
	public static final String[] TYPES = new String[] {
		"party","bar","dining","culture"
	};
	
	private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("MMM dd HH:mm");
	
	private String mTitle;
	private String mDescription;
	private String mAddress;
	private String mHostUsername;
	private Long mStart;
	private Long mEnd;
	private String mType;
	private String mImageUrl;
	
	// after calling valid() all the validation errors are stored here
	private ArrayList<String> mValidationErrors;
	
	public Event() {
		mValidationErrors = new ArrayList<String>();
	}

	public void setTitle(String title) { mTitle = title; }
	public String getTitle() { return mTitle; }
	
	public void setDescription(String description) { mDescription = description; }
	public String getDescription() { return mDescription; }
	
	public void setAddress(String address) { mAddress = address; }
	public String getAddress() { return mAddress; }
	
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
	
	/**
	 * Generate range key that is composed by:
	 *  - 10 digits for start (in seconds since 1970)
	 *  - 10 digits for host username (padded with spaces to 10 digits)
	 *  - rest is a MD5 hash of title
	 */
	public String getStartRangeKey() {
		if (mStart == null || mStart == 0L || mHostUsername == null || mHostUsername.isEmpty() || mTitle == null || mTitle.isEmpty()) {
			return null;
		}
		
		String startString = String.format("%010d", mStart);
		String usernameString = StringUtils.leftPad(mHostUsername, 10, ' ');
		return startString + usernameString + HashingHelper.hashString(mTitle);
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
		
		// is valid if there are no errors
		return mValidationErrors.size() == 0;
	}
	
	
	static public Event buildFromForm(Map<String, String[]> attrMap) throws ParseException {
		Event event = new Event();
		if (attrMap.containsKey("title")) { event.setTitle(attrMap.get("title")[0]); }
		if (attrMap.containsKey("description")) { event.setDescription(attrMap.get("description")[0]); }
		if (attrMap.containsKey("address")) { event.setAddress(attrMap.get("address")[0]); }
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
		if (attrMap.containsKey("host_username")) { event.setHostUsername(attrMap.get("host_username").getS()); }
		if (attrMap.containsKey("type")) { event.setType(attrMap.get("type").getS()); }
		if (attrMap.containsKey("image_url")) { event.setImageUrl(attrMap.get("image_url").getS()); }
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
	
}
