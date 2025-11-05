package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.exception.exceptions.DeleteFileException;
import greencity.exception.exceptions.SaveFileException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceImplTest {

    @Mock
    private AWSCloudStorageService awsCloudStorageService;

    // Use InjectMocks only for manual constructor injection
    private ImageStorageServiceImpl imageStorageService;

    private Path tempDir;
    private final Long eventId = 1L;
    private final String imageContent = "test image data";

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("event-images-test");
        String tempDirPath = tempDir.toAbsolutePath().toString();

        imageStorageService = new ImageStorageServiceImpl(awsCloudStorageService, tempDirPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDir);
    }

    @Test
    void getStorageLocation_shouldReturnCorrectPath() {
        assertTrue(imageStorageService.getStorageLocation().toString().startsWith(tempDir.toString()));
    }

    // --- storeImage tests ---

    @Test
    void storeImage_shouldUploadAndReturnFilename_withExtension() throws IOException {
        MultipartFile image = new MockMultipartFile(
            "file", "test.png", "image/png", imageContent.getBytes());

        doNothing().when(awsCloudStorageService).uploadFile(any(MultipartFile.class));

        String resultFilename = imageStorageService.storeImage(image, eventId);

        verify(awsCloudStorageService, times(1)).uploadFile(any(MultipartFile.class));

        assertTrue(resultFilename.startsWith("event-images/event-" + eventId));
        assertTrue(resultFilename.endsWith(".png"));
    }

    @Test
    void storeImage_shouldUseDefaultExtension_ifNonePresent() throws IOException {
        MultipartFile image = new MockMultipartFile(
            "file", "testfile", "image/jpeg", imageContent.getBytes());

        doNothing().when(awsCloudStorageService).uploadFile(any(MultipartFile.class));

        String resultFilename = imageStorageService.storeImage(image, eventId);

        verify(awsCloudStorageService, times(1)).uploadFile(any(MultipartFile.class));
        assertTrue(resultFilename.startsWith("event-images/event-" + eventId));
        assertTrue(resultFilename.endsWith(".jpg"));
    }

    @Test
    void storeImage_shouldThrowSaveFileException_onIOException() throws IOException {
        MultipartFile mockImage = mock(MultipartFile.class);
        when(mockImage.getOriginalFilename()).thenReturn("f.jpg");
        when(mockImage.getBytes()).thenThrow(new IOException("Simulated IO Error"));

        SaveFileException exception =
            assertThrows(SaveFileException.class, () -> imageStorageService.storeImage(mockImage, eventId));

        assertEquals(ErrorMessage.SAVE_FILE_FAILURE, exception.getMessage());
        verify(awsCloudStorageService, never()).uploadFile(any());
    }

    // --- storeImages tests ---

    @Test
    void storeImages_shouldUploadAllFilesAndReturnUrls() throws IOException {
        MockMultipartFile image1 = new MockMultipartFile("f1", "img1.png", "image/png", "data1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("f2", "img2.jpg", "image/jpeg", "data2".getBytes());
        MultipartFile[] images = {image1, image2};

        doNothing().when(awsCloudStorageService).uploadFile(any(MultipartFile.class));

        List<String> urls = imageStorageService.storeImages(images, eventId);

        assertEquals(2, urls.size());
        verify(awsCloudStorageService, times(2)).uploadFile(any(MultipartFile.class));
        assertTrue(urls.stream().allMatch(url -> url.startsWith("event-images/event-" + eventId)));
        assertTrue(urls.get(0).endsWith(".png"));
        assertTrue(urls.get(1).endsWith(".jpg"));
    }

    @Test
    void storeImages_shouldReturnEmptyList_whenImagesAreNull() throws IOException {
        List<String> urls = imageStorageService.storeImages(null, eventId);

        assertTrue(urls.isEmpty());
        verify(awsCloudStorageService, never()).uploadFile(any());
    }

    @Test
    void storeImages_shouldReturnEmptyList_whenImagesAreEmpty() throws IOException {
        List<String> urls = imageStorageService.storeImages(new MultipartFile[] {}, eventId);

        assertTrue(urls.isEmpty());
        verify(awsCloudStorageService, never()).uploadFile(any());
    }

    // --- deleteImage tests ---

    @Test
    void deleteImage_shouldCallAwsServiceAndReturnTrue_onSuccess() {
        String filename = "event-images/my-image-key.jpg";
        doNothing().when(awsCloudStorageService).deleteFile(filename);

        boolean result = imageStorageService.deleteImage(filename);

        assertTrue(result);
        verify(awsCloudStorageService, times(1)).deleteFile(filename);
    }

    @Test
    void deleteImage_shouldReturnFalse_whenFilenameIsNull() {
        boolean result = imageStorageService.deleteImage(null);

        assertFalse(result);
        verify(awsCloudStorageService, never()).deleteFile(any());
    }

    @Test
    void deleteImage_shouldReturnFalse_whenFilenameIsBlank() {
        boolean result = imageStorageService.deleteImage("   ");

        assertFalse(result);
        verify(awsCloudStorageService, never()).deleteFile(any());
    }

    @Test
    void deleteImage_shouldThrowDeleteFileException_onAwsError() {
        String filename = "event-images/failing-key.jpg";
        doThrow(new RuntimeException("Simulated S3 Delete Error")).when(awsCloudStorageService).deleteFile(filename);

        DeleteFileException exception =
            assertThrows(DeleteFileException.class, () -> imageStorageService.deleteImage(filename));

        assertTrue(exception.getMessage().contains(ErrorMessage.DELETE_FILE_FAILURE + filename));
        verify(awsCloudStorageService, times(1)).deleteFile(filename);
    }

    @Test
    void deleteImage_shouldThrowDeleteFileException_whenFilenameLacksPrefix() {
        String filename = "some-other-folder/image.png";

        DeleteFileException exception =
            assertThrows(DeleteFileException.class, () -> imageStorageService.deleteImage(filename));

        assertTrue(exception.getMessage().contains(ErrorMessage.DELETE_FILE_FAILURE + filename));
        verify(awsCloudStorageService, never()).deleteFile(any());
    }

    @Test
    void deleteImage_shouldThrowDeleteFileException_whenFilenameHasPartialPrefix() {
        String filename = "event-images_wrong/image.png";

        DeleteFileException exception =
            assertThrows(DeleteFileException.class, () -> imageStorageService.deleteImage(filename));

        assertTrue(exception.getMessage().contains(ErrorMessage.DELETE_FILE_FAILURE + filename));
        verify(awsCloudStorageService, never()).deleteFile(any());
    }
}