package greencity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.IOException;
import java.util.List;

@Service
public class AWSCloudStorageService {
    private final S3Client s3Client;
    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    public AWSCloudStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a single file to the S3 bucket. The file's original filename is used
     * as the S3 object key.
     *
     * @param file The MultipartFile to upload.
     * @throws IOException If there is an error reading the file's input stream.
     * @author Oleksandr Obydalo
     */
    public void uploadFile(MultipartFile file) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(file.getOriginalFilename())
            .contentType(file.getContentType())
            .build();
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    /**
     * Uploads a list of files to the S3 bucket.
     *
     * @param files The list of MultipartFiles to upload.
     * @throws IOException If there is an error reading any file's input stream.
     * @author Oleksandr Obydalo
     */
    public void uploadFiles(List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            uploadFile(file);
        }
    }

    /**
     * Deletes a file from the configured S3 bucket using the provided key
     * (filename). This method is called by ImageStorageServiceImpl.deleteImage.
     *
     * @param key The object key (path/filename, e.g., "event-images/...") of the
     *            file to delete.
     */
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }
}
