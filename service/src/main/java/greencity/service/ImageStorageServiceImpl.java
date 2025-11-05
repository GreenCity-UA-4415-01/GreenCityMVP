package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.exception.exceptions.DeleteFileException;
import greencity.exception.exceptions.SaveFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageServiceImpl implements ImageStorageService {
    private final AWSCloudStorageService awsCloudStorageService;
    private final Path storage;
    private static final String FOLDER = "event-images/";

    public ImageStorageServiceImpl(
        AWSCloudStorageService awsCloudStorageService,
        @Value("${events.images.dir:./event-images}") String dir) throws IOException {
        this.awsCloudStorageService = awsCloudStorageService;
        this.storage = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.storage);
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    @Override
    public List<String> storeImages(MultipartFile[] images, Long eventId) {
        List<String> urls = new ArrayList<>();
        if (images == null) {
            return urls;
        }
        for (MultipartFile img : images) {
            urls.add(storeImage(img, eventId));
        }
        return urls;
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska & Oleksandr Obydalo.
     */
    @Override
    public String storeImage(MultipartFile image, Long eventId) {
        try {
            String ext = getExtension(image.getOriginalFilename());
            String filename =
                FOLDER + "event-" + eventId + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
            // Wrap the file with the new filename so S3 stores it with the correct path
            MultipartFile fileWithPath = new MultipartFileImpl(
                image.getName(),
                filename,
                image.getContentType(),
                image.getBytes());
            awsCloudStorageService.uploadFile(fileWithPath);
            return filename;
        } catch (IOException e) {
            throw new SaveFileException(ErrorMessage.SAVE_FILE_FAILURE, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Braiko & Andrii Zakordonskyi.
     */
    @Override
    public boolean deleteImage(String filename) {
        if (filename == null || filename.isBlank()) {
            log.debug("Filename (S3 key) is null or empty");
            return false;
        }
        if (!filename.startsWith(FOLDER)) {
            log.warn("Refusing to delete S3 key outside '{}': {}", FOLDER, filename);
            throw new DeleteFileException(ErrorMessage.DELETE_FILE_FAILURE + filename);
        }
        try {
            awsCloudStorageService.deleteFile(filename);
            log.info("Successfully deleted image from S3: {}", filename);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", filename, e);
            throw new DeleteFileException(ErrorMessage.DELETE_FILE_FAILURE + filename, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    private String getExtension(String name) {
        if (name == null) {
            return ".jpg";
        }
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : ".jpg";
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    @Override
    public Path getStorageLocation() {
        return storage;
    }
}
