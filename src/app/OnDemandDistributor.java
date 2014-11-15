package app;
import java.util.Date;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.model.Aliases;
import com.amazonaws.services.cloudfront.model.CacheBehaviors;
import com.amazonaws.services.cloudfront.model.CookiePreference;
import com.amazonaws.services.cloudfront.model.CreateDistributionRequest;
import com.amazonaws.services.cloudfront.model.CreateDistributionResult;
import com.amazonaws.services.cloudfront.model.CreateStreamingDistributionRequest;
import com.amazonaws.services.cloudfront.model.CreateStreamingDistributionResult;
import com.amazonaws.services.cloudfront.model.DefaultCacheBehavior;
import com.amazonaws.services.cloudfront.model.DistributionConfig;
import com.amazonaws.services.cloudfront.model.DistributionSummary;
import com.amazonaws.services.cloudfront.model.ForwardedValues;
import com.amazonaws.services.cloudfront.model.ListDistributionsRequest;
import com.amazonaws.services.cloudfront.model.ListDistributionsResult;
import com.amazonaws.services.cloudfront.model.LoggingConfig;
import com.amazonaws.services.cloudfront.model.Origin;
import com.amazonaws.services.cloudfront.model.Origins;
import com.amazonaws.services.cloudfront.model.PriceClass;
import com.amazonaws.services.cloudfront.model.S3Origin;
import com.amazonaws.services.cloudfront.model.S3OriginConfig;
import com.amazonaws.services.cloudfront.model.StreamingDistributionConfig;
import com.amazonaws.services.cloudfront.model.StreamingLoggingConfig;
import com.amazonaws.services.cloudfront.model.TrustedSigners;

public class OnDemandDistributor
{
	private AmazonCloudFrontClient amazonCloudFrontClient;
	private String bucketName = "";
    private String originDomainName = "";
    private String originId = "";
	
	public OnDemandDistributor(){}
	
	public OnDemandDistributor withCredentialsProvider(AWSCredentialsProvider provider)
	{
		this.amazonCloudFrontClient = new AmazonCloudFrontClient(provider); 
		return this;
	}
	
	public OnDemandDistributor withAWSCredentials(AWSCredentials awsCredentials)
	{
		this.amazonCloudFrontClient = new AmazonCloudFrontClient(awsCredentials);
		return this;
	}
	
	public OnDemandDistributor withBucketName(String bucketName)
	{
		this.bucketName = bucketName;
		this.originDomainName = this.bucketName + ".s3.amazonaws.com";
		this.originId = "S3-" + this.bucketName;
		
		return this;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getOriginDomainName() {
		return originDomainName;
	}

	public void setOriginDomainName(String originDomainName) {
		this.originDomainName = originDomainName;
	}

	public String getOriginId() {
		return originId;
	}

	public void setOriginId(String originId) {
		this.originId = originId;
	}
	
	public String createWebDistribution()
	{
		String webDistributionDomainName = "";
		System.out.println("Trying to create web distribution ...");
		
		try
		{
	       	Origins origins = new Origins()
	       						.withItems(new Origin().withDomainName(this.originDomainName).withId(this.originId).withS3OriginConfig(new S3OriginConfig().withOriginAccessIdentity("")))
	       						.withQuantity(Integer.valueOf(1));
			DefaultCacheBehavior defaultCacheBehavior = new DefaultCacheBehavior()
								.withViewerProtocolPolicy("allow-all")
								.withMinTTL(0L)
								.withForwardedValues(new ForwardedValues().withQueryString(false).withCookies(new CookiePreference().withForward("none")))
								.withTargetOriginId(this.originId)
								.withTrustedSigners(new TrustedSigners().withEnabled(false).withQuantity(0));
	       	DistributionConfig distributionConfig = new DistributionConfig()
	       						.withAliases(new Aliases().withQuantity(0))
	       						.withOrigins(origins)
	       						.withLogging(new LoggingConfig().withEnabled(false).withIncludeCookies(false).withBucket("").withPrefix(""))
	       						.withComment("")
	       						.withCallerReference((new Date()).toString())
	       						.withDefaultRootObject("")
	       						.withPriceClass(PriceClass.PriceClass_100)
	       						.withEnabled(true)
	       						.withDefaultCacheBehavior(defaultCacheBehavior)
	       						.withCacheBehaviors(new CacheBehaviors().withQuantity(0));
	       	
	       	CreateDistributionRequest createDistributionRequest = new CreateDistributionRequest()
	       						.withDistributionConfig(distributionConfig);
	       	
	       	CreateDistributionResult createDistributionResult = this.amazonCloudFrontClient.createDistribution(createDistributionRequest);
	       	webDistributionDomainName = createDistributionResult.getDistribution().getDomainName();
	       	
	       	System.out.println("Created web distribution : " + webDistributionDomainName);
	       	
       }
       catch (AmazonServiceException ase)
       {
               System.out.println("Caught Exception: " + ase.getMessage());
               System.out.println("Reponse Status Code: " + ase.getStatusCode());
               System.out.println("Error Code: " + ase.getErrorCode());
               System.out.println("Request ID: " + ase.getRequestId());
       }
		return webDistributionDomainName;
	}
	
	public String createRtmpDistribution()
	{
		String rtmpDistributionDomainName = "";
		System.out.println("Trying to create RTMP distribution ...");
		
		try
        {
     	   
	 		StreamingDistributionConfig streamingDistributionConfig = new StreamingDistributionConfig()
	 							.withEnabled(true)
	 							.withComment("")
	 							.withPriceClass(PriceClass.PriceClass_100)
	 							.withAliases(new Aliases().withQuantity(0))
	 							.withLogging(new StreamingLoggingConfig().withBucket("").withEnabled(false).withPrefix(""))
	 							.withTrustedSigners(new TrustedSigners().withEnabled(false).withQuantity(0))
	 							.withS3Origin(new S3Origin().withDomainName(this.originDomainName).withOriginAccessIdentity(""))
	 							.withCallerReference((new Date()).toString());
	 		
	 		CreateStreamingDistributionRequest createStreamingDistributionRequest = new CreateStreamingDistributionRequest()
	 							.withStreamingDistributionConfig(streamingDistributionConfig);
	 				
        	CreateStreamingDistributionResult createStreamingDistributionResult = this.amazonCloudFrontClient.createStreamingDistribution(createStreamingDistributionRequest);
        	rtmpDistributionDomainName = createStreamingDistributionResult.getStreamingDistribution().getDomainName();
        	
        	System.out.println("Created RTMP distribution : " + rtmpDistributionDomainName);
        }
        catch (AmazonServiceException ase)
        {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }
		return rtmpDistributionDomainName;
	}
	
	public String getWebDistributionName()
	{
		System.out.println("Trying to get web distribution domain name ...");
		ListDistributionsRequest listDistributionsRequest = new ListDistributionsRequest().withMaxItems("3");
		ListDistributionsResult listDistributionsResult = this.amazonCloudFrontClient.listDistributions(listDistributionsRequest);
		
		List<DistributionSummary> distros = listDistributionsResult.getDistributionList().getItems();
		String distibutionName = "";
		for (DistributionSummary distributionSummary : distros) {
			if(distributionSummary.getComment().toLowerCase().contains("yes")) {
				distibutionName = distributionSummary.getDomainName();
			}
			System.out.println("Retrieved : " + distibutionName);
		}
		
		
		return distibutionName;
	}

}