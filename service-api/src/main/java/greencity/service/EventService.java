package greencity.service;

import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.user.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {
    /**
     * Method that creates event.
     *
     * @return created event {@link EventDto}
     */
    EventDto createEvent(AddEventDtoRequest request, MultipartFile[] images, Long organizerId);

    /**
     * Method for deleting the {@link EventDto} instance by its id.
     *
     * @param id   - {@link EventDto} instance id which will be deleted.
     * @param user current {@link UserVO} that wants to delete.
     */
    void deleteEvent(Long id, UserVO user);

    /**
     * Method for getting the {@link EventDto} instance by its id.
     *
     * @param id {@link EventDto} instance id.
     * @return {@link EventDto} instance.
     */
    EventDto findById(Long id);
}
