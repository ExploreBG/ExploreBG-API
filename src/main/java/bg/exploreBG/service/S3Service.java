package bg.exploreBG.service;

import bg.exploreBG.utils.FileConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;

@Service
public class S3Service {
    private final static String BUCKET = "explore-bg";

    @Value("${aws.region}")
    private String region;
    private final S3Client s3Client;
    private final Logger logger = LoggerFactory.getLogger(S3Service.class);

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadGpxFile(
            String folder,
            String awsId,
            MultipartFile file
    ) {
        StringBuilder key = generateKey(awsId, folder);
        File converted = FileConverterUtil.convertMultipartFileToFile(file);

        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(key.toString())
                            .acl(ObjectCannedACL.PUBLIC_READ)
                            .build();

            this.s3Client.putObject(putObjectRequest, RequestBody.fromFile(converted));
        } catch (S3Exception e) {
            // Handle Amazon S3-specific exceptions
            logger.error("Amazon S3 error: {}", e.getMessage(), e);
            return null;
        } catch (SdkClientException e) {
            // Handle client-side errors
            logger.error("Client-side error: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            // Handle any other exceptions
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return null;
        } finally {
            if (!converted.delete()) {
                logger.warn("Failed to delete the file: {}", converted.getAbsolutePath());
            }
        }
        return "https://" + BUCKET + ".s3." + region + ".amazonaws.com/" + key;
        /*   Programmatically returns the url
         *  GetUrlRequest request = GetUrlRequest.builder().bucket(BUCKET).key(key).build();
         *  String url = s3Client.utilities().getUrl(request).toExternalForm();
         */
    }

    public boolean deleteGpxFile(
            String folder,
            String awsId
    ) {
        StringBuilder key = generateKey(awsId, folder);

        try {
            DeleteObjectRequest deleteObjectRequest =
                    DeleteObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(key.toString())
                            .build();

            this.s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            // Handle Amazon S3-specific exceptions
            logger.error("Amazon S3 error: {}", e.getMessage(), e);
            return false;
        } catch (SdkClientException e) {
            // Handle client-side errors
            logger.error("Client-side error: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            // Handle any other exceptions
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return false;
        }

        return true;
    }

    private StringBuilder generateKey(String awsId, String folder) {
        StringBuilder key = new StringBuilder();
        key.append(folder).append("/").append(awsId);
        return key;
    }
}

