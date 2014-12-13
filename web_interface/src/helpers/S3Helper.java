package helpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;

public class S3Helper
{
	public static final String IMAGES_BUCKET = "supnyc-images";
	
	private static S3Helper sS3Helper;
	public static S3Helper getInstance() throws IOException {
		if (sS3Helper == null) {
			sS3Helper = new S3Helper(ApplicationHelper.getCredentials());
		}
		return sS3Helper;
	}

	public static final String ASSIGNMENT3_BUCKET_NAME = "jhm-assignment3";
	private AmazonS3Client mS3Client = null;	
	
	public S3Helper(AWSCredentials awsCredentials) {
		try {
			// so amazon refreshes the credentials automatically
			this.mS3Client = new AmazonS3Client(new InstanceProfileCredentialsProvider());
			// test if the credentials work
			mS3Client.listBuckets();
		}
		catch (AmazonClientException e) {
			// probably is not in an EC2 instance, then look for the credentials in the default chain
			// (credentials will expire)
			this.mS3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain().getCredentials());
		}

		// set region to US East
		mS3Client.setRegion(ApplicationHelper.getAmazonRegion());
	}
	
	public void createBucketIfNeeded(String bucketName) {
		 try {
			 if(!mS3Client.doesBucketExist(bucketName)) {
				 // create bucket
				 mS3Client.createBucket(bucketName, Region.US_Standard);
	         }
         } 
		 catch (Exception e) {
			 e.printStackTrace();
         }
	}
	
	public InputStream readObject(String bucketName, String key) {
		GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
		S3Object object = mS3Client.getObject(getObjectRequest);
		return object.getObjectContent();
	}
	
	public void uploadObject(String bucketName, String key, File file) {
		mS3Client.putObject(new PutObjectRequest(bucketName, key, file));
	}
	
	public void uploadImage(String bucketName, String key, InputStream inputstream, String contentType) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType(contentType);
		
		PutObjectRequest request = new PutObjectRequest(bucketName, key, inputstream, metadata)
			.withCannedAcl(CannedAccessControlList.PublicRead);
			
		PutObjectResult result = mS3Client.putObject(request);
	}
	
}