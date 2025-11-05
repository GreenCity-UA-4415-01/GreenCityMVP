package greencity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSCloudStorageServiceTest {

    private static final String MOCK_BUCKET_NAME = "test-green-city-bucket";
    private static final String MOCK_FILE_KEY = "test-key-123.jpg";
    private static final String MOCK_FILE_NAME_1 = "original-file-1.png";
    private static final String MOCK_FILE_NAME_2 = "original-file-2.jpg";
    private static final String MOCK_CONTENT_TYPE = "image/png";
    private static final byte[] MOCK_FILE_CONTENT = "Mock file data".getBytes();

    @Mock
    private S3Client s3Client;

    @Captor
    private ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;

    @Captor
    private ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestCaptor;

    @InjectMocks
    private AWSCloudStorageService awsCloudStorageService;

    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to inject the private 'bucketName' field.
        // This is the accepted Spring testing pattern that replaces
        // Field.setAccessible(true).
        ReflectionTestUtils.setField(awsCloudStorageService, "bucketName", MOCK_BUCKET_NAME);
    }
    // --- uploadFile tests ---

    @Test
    void uploadFile_shouldCallPutObjectWithCorrectRequest() throws IOException {
        MultipartFile file = new MockMultipartFile("file", MOCK_FILE_NAME_1, MOCK_CONTENT_TYPE, MOCK_FILE_CONTENT);

        awsCloudStorageService.uploadFile(file);

        verify(s3Client, times(1)).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = putObjectRequestCaptor.getValue();
        assertEquals(MOCK_BUCKET_NAME, request.bucket());
        assertEquals(MOCK_FILE_NAME_1, request.key());
        assertEquals(MOCK_CONTENT_TYPE, request.contentType());
    }

    @Test
    void uploadFile_shouldThrowIOException_onInputStreamError() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);

        lenient().when(mockFile.getOriginalFilename()).thenReturn(MOCK_FILE_NAME_1);
        lenient().when(mockFile.getContentType()).thenReturn(MOCK_CONTENT_TYPE);
        lenient().when(mockFile.getSize()).thenReturn((long) MOCK_FILE_CONTENT.length);

        when(mockFile.getInputStream()).thenThrow(new IOException("Simulated I/O failure"));

        assertThrows(IOException.class, () -> awsCloudStorageService.uploadFile(mockFile));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // --- uploadFiles tests ---

    @Test
    void uploadFiles_shouldCallPutObjectTwice_forTwoFiles() throws IOException {
        MultipartFile file1 = new MockMultipartFile("file1", MOCK_FILE_NAME_1, MOCK_CONTENT_TYPE, "data1".getBytes());
        MultipartFile file2 = new MockMultipartFile("file2", MOCK_FILE_NAME_2, "image/jpeg", "data2".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);

        awsCloudStorageService.uploadFiles(files);

        verify(s3Client, times(2)).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> requests = putObjectRequestCaptor.getAllValues();

        assertEquals(MOCK_FILE_NAME_1, requests.get(0).key());
        assertEquals(MOCK_BUCKET_NAME, requests.get(0).bucket());

        assertEquals(MOCK_FILE_NAME_2, requests.get(1).key());
        assertEquals(MOCK_BUCKET_NAME, requests.get(1).bucket());
    }

    @Test
    void uploadFiles_shouldHandleEmptyListGracefully() throws IOException {
        List<MultipartFile> files = Collections.emptyList();

        awsCloudStorageService.uploadFiles(files);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // --- deleteFile tests ---

    @Test
    void deleteFile_shouldCallDeleteObjectWithCorrectRequest() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        awsCloudStorageService.deleteFile(MOCK_FILE_KEY);

        verify(s3Client, times(1)).deleteObject(deleteObjectRequestCaptor.capture());

        DeleteObjectRequest request = deleteObjectRequestCaptor.getValue();
        assertEquals(MOCK_BUCKET_NAME, request.bucket());
        assertEquals(MOCK_FILE_KEY, request.key());
    }

    @Test
    void deleteFile_shouldPropagateS3Exception() {
        S3Exception expectedException = (S3Exception) S3Exception.builder()
            .message("Simulated S3 Access Denied")
            .statusCode(403)
            .build();

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(expectedException);

        S3Exception actualException =
            assertThrows(S3Exception.class, () -> awsCloudStorageService.deleteFile(MOCK_FILE_KEY));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        assertEquals(expectedException.statusCode(), actualException.statusCode());
        assertEquals(expectedException.getMessage(), actualException.getMessage());
    }
}