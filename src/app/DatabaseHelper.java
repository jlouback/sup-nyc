package app;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

import models.Event;


public class DatabaseHelper
{
	private AmazonDynamoDBClient amazonDynamoDBClient = null;
	public static final String tableName = "Events";
	
	public DatabaseHelper withCredentials(AWSCredentials awsCredentials)
	{
		this.amazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials);
		return this;
	}
	

	public List<Event> getEventsByType(String type)
	{
		System.out.println("Getting all events by type " + type + "...");
		List<Event> scannedTweets = new ArrayList<Event>();
		List<Event> tweets = new ArrayList<Event>();

		try
		{
			DynamoDBMapper mapper = new DynamoDBMapper(this.amazonDynamoDBClient);
			DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

			scanExpression.addFilterCondition("Type", 
					new Condition()
					.withComparisonOperator(ComparisonOperator.EQ)
					.withAttributeValueList(new AttributeValue().withS(type)));

			scannedTweets = mapper.scan(Event.class, scanExpression);
			System.out.println("Retrieved " + scannedTweets.size() + " record(s).");
			tweets.addAll(scannedTweets);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return tweets;
	}
	
	public List<Event> getAllEvents()
	{
		System.out.println("Getting all events ...");
		List<Event> scannedEvents = new ArrayList<Event>();
		List<Event> events = new ArrayList<Event>();

		try
		{			
			DynamoDBMapper mapper = new DynamoDBMapper(this.amazonDynamoDBClient);
			DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
						
			scannedEvents = mapper.scan(Event.class, scanExpression);
			System.out.println("Retrieved " + scannedEvents.size() + " record(s).");
			
			events.addAll(scannedEvents);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return events;
	}
	
	public List<String> getEventLocations(List<Event> events) {
		List<String> locations = new ArrayList<String>();
		for(int i=0;i<events.size();i++) {
			locations.add(events.get(i).getLocation());
		}
		return locations;
	}
	
}