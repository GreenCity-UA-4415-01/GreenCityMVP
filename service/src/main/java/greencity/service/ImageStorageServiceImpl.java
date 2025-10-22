package greencity.service;

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
public class ImageStorageServiceImpl implements ImageStorageService {
    private final AWSCloudStorageService awsCloudStorageService;
    private final Path storage;
    private final String folder = "event-images/";

    public ImageStorageServiceImpl(
            AWSCloudStorageService awsCloudStorageService,
            @Value("${events.images.dir:./event-images}") String dir) throws IOException {
        this.awsCloudStorageService = awsCloudStorageService;
        this.storage = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.storage);
    }

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

    @Override
    public String storeImage(MultipartFile image, Long eventId) {
        try {
            String ext = getExtension(image.getOriginalFilename());
            String filename = folder + "event-" + eventId + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
            // Wrap the file with the new filename so S3 stores it with the correct path
            MultipartFile fileWithPath = new MultipartFileImpl(
                    image.getName(),
                    filename,
                    image.getContentType(),
                    image.getBytes()
            );
            awsCloudStorageService.uploadFile(fileWithPath);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }

    private String getExtension(String name) {
        if (name == null) {
            return ".jpg";
        }
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i) : ".jpg";
    }

    @Override
    public Path getStorageLocation() {
        return storage;
    }
}
