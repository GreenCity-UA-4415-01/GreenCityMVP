package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.converters.UserArgumentResolver;
import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDateLocationDto;
import greencity.dto.event.EventDto;
import greencity.dto.user.UserVO;
import greencity.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EventControllerTest {
    @Test
    public void createEvent_ShouldReturn201Created() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        EventService eventService = mock(EventService.class);
        UserArgumentResolver userArgumentResolver = mock(UserArgumentResolver.class);

        EventController eventController = new EventController(eventService);

        // Mock the authenticated user
        UserVO mockUser = UserVO.builder()
                .id(5L)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(userArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(mockUser);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setCustomArgumentResolvers(userArgumentResolver)
                .build();

        EventDateLocationDto locationDto = EventDateLocationDto.builder()
                .startDate(OffsetDateTime.now().plusDays(2))
                .finishDate(OffsetDateTime.now().plusDays(2).plusHours(3))
                .latitude(50.45)
                .longitude(30.52)
                .build();

        AddEventDtoRequest addEventDtoRequest = AddEventDtoRequest.builder()
                .title("Eco Cleanup")
                .description("Let's clean the park together for a better future!")
                .open(true)
                .datesLocations(List.of(locationDto))
                .build();

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes());

        MockMultipartFile dtoPart = new MockMultipartFile(
                "addEventDtoRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes());

        EventDto responseDto = EventDto.builder()
                .id(1L)
                .title("Eco Cleanup")
                .description("Let's clean the park together for a better future!")
                .open(true)
                .organizerId(5L)
                .titleImage("event-1-uuid.jpg")
                .imageUrls(List.of("event-1-uuid.jpg"))
                .datesLocations(List.of(locationDto))
                .createdAt(OffsetDateTime.now())
                .build();

        when(eventService.createEvent(any(AddEventDtoRequest.class), any(MultipartFile[].class), eq(5L)))
                .thenReturn(responseDto);

        mockMvc.perform(multipart("/events/create")
                        .file(dtoPart)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Eco Cleanup"))
                .andExpect(jsonPath("$.organizerId").value(5))
                .andExpect(jsonPath("$.imageUrls[0]").value("event-1-uuid.jpg"));
    }
}
