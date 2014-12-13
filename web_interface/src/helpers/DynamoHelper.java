package helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

/**
 * Singleton class that centralizes access to DynamoDB. 
 * This class should not be used in controllers directly, but rather through models.
 */
public class DynamoHelper {
	private static DynamoHelper sDynamoHelper;
	public static DynamoHelper getInstance() throws IOException {
		if (sDynamoHelper == null) {
			sDynamoHelper = new DynamoHelper(ApplicationHelper.getCredentials());
		}
		return sDynamoHelper;
	}

	private AmazonDynamoDBClient mDynamoDBClient = null;	
	
	public DynamoHelper(AWSCredentials awsCredentials) {
		try {
			// so amazon refreshes the credentials automatically
			this.mDynamoDBClient = new AmazonDynamoDBClient(new InstanceProfileCredentialsProvider());
			// test if the credentials work
			mDynamoDBClient.listTables();
		}
		catch (AmazonClientException e) {
			// probably is not in an EC2 instance, then look for the credentials in the default chain
			// (credentials will expire)
			this.mDynamoDBClient = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain().getCredentials());
		}
		
		// set region to US East
		mDynamoDBClient.setRegion(ApplicationHelper.getAmazonRegion());
	}
	
	/**
	 * Checks if the specified table exists in Amazon AWS.
	 * This method is used at startup to create tables that are missing
	 */
	public boolean checkIfTableExists(String table_name) {
		// checking if the table already exists
		boolean tableExists = true;
		try {
			return mDynamoDBClient.describeTable(table_name).getTable().getTableStatus().equals(TableStatus.ACTIVE.name());
		}
		catch (ResourceNotFoundException e) {
			tableExists = false;
		}
		
		return tableExists;
	}
	
	/**
	 * Creates a new Dynamo Table. 
	 * The caller should check before calling this function if a table with the same name does not
	 * already exist. This method does busy waiting and can take up to 120s to return. To see how 
	 * to create the parameters to this function refer to:
	 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LowLevelJavaWorkingWithTables.html#LowLevelJavaCreate
	 */
	public void createTable(ArrayList<AttributeDefinition> attributes, ArrayList<KeySchemaElement> keySchema, Long readCapacity, Long writeCapacity, String tableName) throws AmazonServiceException{
		// provisions 4 reads and 1 write per sec 
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
			.withReadCapacityUnits(readCapacity)
			.withWriteCapacityUnits(writeCapacity);
		
		CreateTableRequest request = new CreateTableRequest()
			.withTableName(tableName)
			.withAttributeDefinitions(attributes)
			.withKeySchema(keySchema)
			.withProvisionedThroughput(provisionedThroughput);
		mDynamoDBClient.createTable(request);
		
		// wait for table creation
		int triesLeft = 10;
		int secondsBetweenTrials = 12;
		while (triesLeft > 0) {
			try {
				TableDescription tableDescription = mDynamoDBClient.describeTable(tableName).getTable();
				String status = tableDescription.getTableStatus();
				if (status.equals(TableStatus.ACTIVE.toString())) {
					break;
				}
			}
			catch (ResourceNotFoundException e) {}
			triesLeft -= 1;
			try {
				Thread.sleep(secondsBetweenTrials * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (triesLeft == 0) {
			throw new AmazonServiceException("Could not create DynamoDB table (it was created but never appeared as active).");
		}
	}
	
	/**
	 * Creates a new Dynamo Table. 
	 * The caller should check before calling this function if a table with the same name does not
	 * already exist. This method does busy waiting and can take up to 120s to return. To see how 
	 * to create the parameters to this function refer to:
	 * http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LowLevelJavaWorkingWithTables.html#LowLevelJavaCreate
	 */
	public void createTable(ArrayList<AttributeDefinition> attributes, ArrayList<KeySchemaElement> keySchema, Long readCapacity, Long writeCapacity, String tableName, ArrayList<LocalSecondaryIndex> localSecondaryIndexes) throws AmazonServiceException{
		// provisions 4 reads and 1 write per sec 
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
			.withReadCapacityUnits(readCapacity)
			.withWriteCapacityUnits(writeCapacity);
		
		CreateTableRequest request = new CreateTableRequest()
			.withTableName(tableName)
			.withAttributeDefinitions(attributes)
			.withKeySchema(keySchema)
			.withProvisionedThroughput(provisionedThroughput)
			.withLocalSecondaryIndexes(localSecondaryIndexes);
		mDynamoDBClient.createTable(request);
		
		// wait for table creation
		int triesLeft = 10;
		int secondsBetweenTrials = 12;
		while (triesLeft > 0) {
			try {
				TableDescription tableDescription = mDynamoDBClient.describeTable(tableName).getTable();
				String status = tableDescription.getTableStatus();
				if (status.equals(TableStatus.ACTIVE.toString())) {
					break;
				}
			}
			catch (ResourceNotFoundException e) {}
			triesLeft -= 1;
			try {
				Thread.sleep(secondsBetweenTrials * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (triesLeft == 0) {
			throw new AmazonServiceException("Could not create DynamoDB table (it was created but never appeared as active).");
		}
	}
	
	public boolean putItem(String tableName, Map<String, AttributeValue> attributeMap) {
		try {			
			PutItemRequest putItemRequest = new PutItemRequest()
				.withTableName(tableName)
				.withItem(attributeMap);
			mDynamoDBClient.putItem(putItemRequest);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean conditionalPutItem(String tableName, String keyName, Map<String, AttributeValue> attributeMap) {
		try {			
			PutItemRequest putItemRequest = new PutItemRequest()
				.withTableName(tableName)
				.withItem(attributeMap)
				.withConditionExpression("attribute_not_exists(" + keyName + ")");
			
			mDynamoDBClient.putItem(putItemRequest);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Gets an item by its primary key (for tables without range key).
	 * Returns a map of attributes or null if it could not load object
	 */
	public Map<String, AttributeValue> getItemByPrimaryKey(String tableName, String primaryKeyName, String primaryKeyValue) {
		try {			
			HashMap<String, AttributeValue> conditions = new HashMap<String, AttributeValue>();
			conditions.put(primaryKeyName, new AttributeValue().withS(primaryKeyValue));

			GetItemRequest getItemRequest = new GetItemRequest()
			    .withTableName(tableName)
			    .withKey(conditions);
			
			GetItemResult result = mDynamoDBClient.getItem(getItemRequest);
			return result.getItem();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets an item by its primary key and range key (for tables with range key).
	 * Returns a map of attributes or null if it could not load object
	 */
	public Map<String, AttributeValue> getItemByPrimaryKeyAndRange(String tableName, String primaryKeyName, String primaryKeyValue, String rangeKeyName, String rangeKeyValue) {
		try {			
			HashMap<String, AttributeValue> conditions = new HashMap<String, AttributeValue>();
			conditions.put(primaryKeyName, new AttributeValue().withS(primaryKeyValue));
			conditions.put(rangeKeyName, new AttributeValue().withS(rangeKeyValue));

			GetItemRequest getItemRequest = new GetItemRequest()
			    .withTableName(tableName)
			    .withKey(conditions);
			
			GetItemResult result = mDynamoDBClient.getItem(getItemRequest);
			return result.getItem();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Gets all items by its primary key (for tables with range key).
	 * Returns a list of map of attributes
	 */
	public List<Map<String, AttributeValue>> queryByPrimaryKey(String tableName, String primaryKeyName, String primaryKeyValue) {
		Condition primaryKeyCondition = new Condition()
		    .withComparisonOperator(ComparisonOperator.EQ)
		    .withAttributeValueList(new AttributeValue().withS(primaryKeyValue));
	
		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put(primaryKeyName, primaryKeyCondition);
	
		return query(tableName, keyConditions, null);
	}
	
	/**
	 * Gets all items by its primary key and a range greater than some threshold (for tables with range key).
	 * Returns a list of map of attributes
	 */
	public List<Map<String, AttributeValue>> queryByPrimaryKeyAndRangeKey(String tableName, String primaryKeyName, String primaryKeyValue, String rangeKeyName, String rangeKeyStart, String rangeKeyEnd) {
		Condition primaryKeyCondition = new Condition()
		    .withComparisonOperator(ComparisonOperator.EQ)
		    .withAttributeValueList(new AttributeValue().withS(primaryKeyValue));
		
		Condition rangeKeyCondition = new Condition()
			.withComparisonOperator(ComparisonOperator.BETWEEN)
			.withAttributeValueList(
					new AttributeValue().withS(rangeKeyStart),
					new AttributeValue().withS(rangeKeyEnd)
			);
		
		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put(primaryKeyName, primaryKeyCondition);
		keyConditions.put(rangeKeyName, rangeKeyCondition);
	
		return query(tableName, keyConditions, null);
	}
	
	/**
	 * Gets all items by its primary key and a index with some prefix (for tables with range key).
	 * Returns a list of map of attributes
	 */
	public List<Map<String, AttributeValue>> queryByPrimaryKeyAndIndexPrefix(String tableName, String primaryKeyName, String primaryKeyValue, String indexKeyName, String indexKeyPrefix, String indexName) {
		Condition primaryKeyCondition = new Condition()
		    .withComparisonOperator(ComparisonOperator.EQ)
		    .withAttributeValueList(new AttributeValue().withS(primaryKeyValue));
		
		Condition indexKeyPrefixCondition = new Condition()
			.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
			.withAttributeValueList(new AttributeValue().withS(indexKeyPrefix));
		
		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put(primaryKeyName, primaryKeyCondition);
		keyConditions.put(indexKeyName, indexKeyPrefixCondition);
	
		return query(tableName, keyConditions, indexName);
	}
	
	private List<Map<String, AttributeValue>> query(String tableName, Map<String, Condition> keyConditions, String indexName) {
		List<Map<String, AttributeValue>> items = new ArrayList<Map<String, AttributeValue>>(100); 
		
		Map<String, AttributeValue> lastEvaluatedKey = null;
		while(true) {
			QueryRequest queryRequest = new QueryRequest()
				.withTableName(tableName)
				.withKeyConditions(keyConditions);

			if (indexName != null)
				queryRequest = queryRequest.withIndexName(indexName);
			
			if (lastEvaluatedKey != null) {
				for (String key : lastEvaluatedKey.keySet()) {
					queryRequest = queryRequest.addExclusiveStartKeyEntry(key, lastEvaluatedKey.get(key));
				}
			} 
			
			try {
				QueryResult result = mDynamoDBClient.query(queryRequest);
				items.addAll(result.getItems());
				
				if (result.getLastEvaluatedKey() == null) {
					break;
				} else {
					lastEvaluatedKey = result.getLastEvaluatedKey();
				}
			}
			catch (ProvisionedThroughputExceededException e) {
				System.out.println("Provisioned throughput exceeded.. sleeping for a sec");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {	}
			}
			
			return items;
		}
		
		return items;
	}
	
}