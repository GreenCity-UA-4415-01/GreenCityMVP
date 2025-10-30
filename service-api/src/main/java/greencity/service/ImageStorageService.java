package greencity.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;

public interface ImageStorageService {
    /**
     * Stores multiple images for a given event.
     *
     * @param images  array of image files to store
     * @param eventId ID of the event to associate the images with
     * @return list of stored image file names
     */
    List<String> storeImages(MultipartFile[] images, Long eventId);

    /**
     * Stores a single image for a given event.
     *
     * @param image   image file to store
     * @param eventId ID of the event to associate the image with
     * @return stored image file name
     */
    String storeImage(MultipartFile image, Long eventId);

    boolean deleteImage(String imagePath);

    /**
     * Deletes an image file from storage.
     *
     * @param filename filename to image file
     */
    void deleteImage(String filename);

    /**
     * Returns the root storage location path for event images.
     *
     * @return {@link Path} representing the directory where images are stored
     */
    Path getStorageLocation();
}
