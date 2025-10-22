package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.exception.exceptions.DeleteFileException;
import greencity.exception.exceptions.StoragePathException;
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
    private final Path storage;

    public ImageStorageServiceImpl(@Value("${events.images.dir:./event-images}") String dir) throws IOException {
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
     * @author Kateryna Holtvianska.
     */
    @Override
    public String storeImage(MultipartFile image, Long eventId) {
        try {
            String ext = getExtension(image.getOriginalFilename());
            String filename = "event-" + eventId + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
            Path target = storage.resolve(filename);
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Braiko.
     */
    @Override
    public void deleteImage(String filename) {
        if (filename == null || filename.isEmpty()) {
            log.debug("Filename is null or empty");
            return;
        }

        try {
            Path filePath = this.storage.resolve(filename).normalize();

            if (!filePath.startsWith(this.storage)) {
                throw new StoragePathException(ErrorMessage.INVALID_STORAGE_PATH);
            }

            boolean deleted = Files.deleteIfExists(filePath);

            if (!deleted) {
                log.debug("Warning: File not found or already deleted: {}", filename);
            }
        } catch (IOException e) {
            throw new DeleteFileException(ErrorMessage.DELETE_FILE_FAILURE + filename);
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
