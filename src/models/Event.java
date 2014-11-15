package models;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import app.DatabaseHelper;

@SuppressWarnings("rawtypes")
@DynamoDBTable(tableName=DatabaseHelper.tableName)
@XmlRootElement
public class Event implements Comparable
{
	private String id;
	private String location;
	private String keyword;
	private String created;
	private String description;
	
	@DynamoDBHashKey(attributeName="Id")
	@DynamoDBAutoGeneratedKey
	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	
	@DynamoDBAttribute(attributeName="Location")
	public String getLocation(){return location;}
	public void setLocation(String location) {this.location = location;}
	
	@DynamoDBAttribute(attributeName="Keyword")
	public String getKeyword() {return keyword;}
	public void setKeyword(String keyword) {this.keyword = keyword;}
	
	@DynamoDBAttribute(attributeName="DateCreated")
	public String getCreated() {return created;}
	public void setCreated(String created) {this.created = created;}
	
	@DynamoDBAttribute(attributeName="Description")
	public String getDescription() {return description;}
	public void setDescription(String description) {this.created = description;}
	
	
	@DynamoDBIgnore
	public Event withId(String id)
	{
		this.id = id;
		return this;
	}
	
	@DynamoDBIgnore
	public Event withCreated(Date created){
		this.created = created.toString();
		return this;
	}
	
	
	@DynamoDBIgnore
	public Event withLocation(String location){
		this.location = location;
		return this;
	}
	
	@DynamoDBIgnore
	public Event withKeyword(String keyword){
		this.keyword = keyword;
		return this;
	}
	
	@DynamoDBIgnore
	public Event withDescription(String description){
		this.description = description;
		return this;
	}
	
	
	@Override
	@DynamoDBIgnore
	public String toString()
	{
		String str = "{id=" + this.id
			+ ", " + "location=" + this.location
			+ ", " + "keyword=" + this.keyword
			+ ", " + "description=" + this.description
			+ ", " + "created=" + this.created + "}";
		return str;
	}
	@Override
	@DynamoDBIgnore
	public int compareTo(Object o) {
		return this.keyword.compareTo(((Event)o).getKeyword());
	}
}