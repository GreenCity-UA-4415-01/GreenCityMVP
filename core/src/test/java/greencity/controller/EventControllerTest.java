package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDateLocationDto;
import greencity.dto.event.EventDto;
import greencity.dto.event.EventPreviewDto;
import greencity.dto.user.UserVO;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.exception.exceptions.BadRequestException;
import greencity.exception.handler.CustomExceptionHandler;
import greencity.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import greencity.annotations.CurrentUser;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EventControllerTest {
    @InjectMocks
    EventController eventController;
    @Mock
    EventService eventService;

    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // Mock the authenticated user
    UserVO mockUser = UserVO.builder()
        .id(5L)
        .email("test@example.com")
        .name("Test User")
        .build();

    // Custom UserArgumentResolver for testing
    static class TestUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final UserVO userVO;

        public TestUserArgumentResolver(UserVO userVO) {
            this.userVO = userVO;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(CurrentUser.class) != null
                && parameter.getParameterType().equals(UserVO.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {
            return userVO;
        }
    }

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

    @BeforeEach
    public void setup() {
        // Use DefaultErrorAttributes which properly extracts exception information
        DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

        CustomExceptionHandler exceptionHandler = new CustomExceptionHandler(errorAttributes, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new TestUserArgumentResolver(mockUser))
            .setControllerAdvice(exceptionHandler)
            .build();
    }

    private byte[] createValidJpegBytes() {
        byte[] jpegHeader = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] padding = new byte[100]; // Add some padding
        byte[] result = new byte[jpegHeader.length + padding.length];
        System.arraycopy(jpegHeader, 0, result, 0, jpegHeader.length);
        System.arraycopy(padding, 0, result, jpegHeader.length, padding.length);
        return result;
    }

    private byte[] createValidJpegBytes(int size) {
        byte[] jpegHeader = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] result = new byte[size];
        System.arraycopy(jpegHeader, 0, result, 0, Math.min(jpegHeader.length, size));
        return result;
    }

    private byte[] createValidPngBytes() {
        byte[] pngHeader = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] padding = new byte[100]; // Add some padding
        byte[] result = new byte[pngHeader.length + padding.length];
        System.arraycopy(pngHeader, 0, result, 0, pngHeader.length);
        System.arraycopy(padding, 0, result, pngHeader.length, padding.length);
        return result;
    }

    @Test
    public void createEvent_ShouldReturn201Created() throws Exception {
        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile image = new MockMultipartFile(
            "images",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            createValidJpegBytes());

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

    @Test
    public void createEvent_ShouldReturn400BadRequest() throws Exception {
        AddEventDtoRequest invalidDto = AddEventDtoRequest.builder()
                .title("")
                .description("Too short")
                .open(true)
                .datesLocations(List.of(locationDto))
                .build();

        String json = objectMapper.writeValueAsString(invalidDto);

        MockMultipartFile dtoPart = new MockMultipartFile(
                "addEventDtoRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes()
        );

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                createValidJpegBytes()
        );

        when(eventService.createEvent(any(), any(), anyLong()))
                .thenThrow(new BadRequestException("Title must be between 1 and 70 characters"));

        mockMvc.perform(multipart("/events/create")
                        .file(dtoPart)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getVisibleEvents_ShouldReturn200() throws Exception {
        EventDto openEvent = EventDto.builder()
                .id(1L)
                .title("Public Cleanup")
                .description("Open event for everyone")
                .open(true)
                .organizerId(3L)
                .build();

        EventDto friendEvent = EventDto.builder()
                .id(2L)
                .title("Recycling Workshop")
                .description("Learn how to recycle effectively at home.")
                .open(false)
                .organizerId(2L)
                .build();

        List<EventDto> visibleEvents = List.of(openEvent, friendEvent);

        when(eventService.getVisibleEvents(any(UserVO.class))).thenReturn(visibleEvents);

        mockMvc.perform(get("/events/visible")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Public Cleanup"))
                .andExpect(jsonPath("$[0].open").value(true))
                .andExpect(jsonPath("$[1].title").value("Recycling Workshop"))
                .andExpect(jsonPath("$[1].open").value(false));
    }

    @Test
    public void createEventWithWrongImageType() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile image = new MockMultipartFile(
            "images",
            "test.gif",
            MediaType.IMAGE_GIF_VALUE,
            "fake-image-content".getBytes());

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(image)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createEventWithPngImage_ShouldReturn201Created() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile image = new MockMultipartFile(
            "images",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            createValidPngBytes());

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
            .titleImage("event-1-uuid.png")
            .imageUrls(List.of("event-1-uuid.png"))
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
            .andExpect(jsonPath("$.imageUrls[0]").value("event-1-uuid.png"));
    }

    @Test
    public void createEventWithOversizedImage_ShouldReturn400BadRequest() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        // Create a file larger than 10MB
        byte[] largeContent = createValidJpegBytes(11 * 1024 * 1024); // 11MB
        MockMultipartFile oversizedImage = new MockMultipartFile(
            "images",
            "large.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            largeContent);

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(oversizedImage)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createEventWithExactly10MBImage_ShouldReturn201Created() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        // Create a file exactly 10MB
        byte[] exactContent = createValidJpegBytes(10 * 1024 * 1024); // Exactly 10MB
        MockMultipartFile exactSizeImage = new MockMultipartFile(
            "images",
            "exact.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            exactContent);

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
            .file(exactSizeImage)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());
    }

    @Test
    public void createEventWithMoreThan5Images_ShouldReturn400BadRequest() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        // Create 6 images
        MockMultipartFile image1 =
            new MockMultipartFile("images", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image2 =
            new MockMultipartFile("images", "test2.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image3 =
            new MockMultipartFile("images", "test3.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image4 =
            new MockMultipartFile("images", "test4.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image5 =
            new MockMultipartFile("images", "test5.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image6 =
            new MockMultipartFile("images", "test6.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(image1)
            .file(image2)
            .file(image3)
            .file(image4)
            .file(image5)
            .file(image6)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createEventWithExactly5Images_ShouldReturn201Created() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        // Create exactly 5 images (3 JPEG, 2 PNG)
        MockMultipartFile image1 =
            new MockMultipartFile("images", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image2 =
            new MockMultipartFile("images", "test2.png", MediaType.IMAGE_PNG_VALUE, createValidPngBytes());
        MockMultipartFile image3 =
            new MockMultipartFile("images", "test3.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image4 =
            new MockMultipartFile("images", "test4.png", MediaType.IMAGE_PNG_VALUE, createValidPngBytes());
        MockMultipartFile image5 =
            new MockMultipartFile("images", "test5.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());

        EventDto responseDto = EventDto.builder()
            .id(1L)
            .title("Eco Cleanup")
            .description("Let's clean the park together for a better future!")
            .open(true)
            .organizerId(5L)
            .titleImage("event-1-uuid1.jpg")
            .imageUrls(List.of("event-1-uuid1.jpg", "event-1-uuid2.png", "event-1-uuid3.jpg", "event-1-uuid4.png",
                "event-1-uuid5.jpg"))
            .datesLocations(List.of(locationDto))
            .createdAt(OffsetDateTime.now())
            .build();

        when(eventService.createEvent(any(AddEventDtoRequest.class), any(MultipartFile[].class), eq(5L)))
            .thenReturn(responseDto);

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(image1)
            .file(image2)
            .file(image3)
            .file(image4)
            .file(image5)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.imageUrls.length()").value(5))
            .andExpect(jsonPath("$.titleImage").value("event-1-uuid1.jpg"));
    }

    @Test
    public void createEventWithNoImages_ShouldUseDefaultImage() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

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
            .titleImage("default-event-image.jpg")
            .imageUrls(List.of("default-event-image.jpg"))
            .datesLocations(List.of(locationDto))
            .createdAt(OffsetDateTime.now())
            .build();

        when(eventService.createEvent(any(AddEventDtoRequest.class), any(), eq(5L)))
            .thenReturn(responseDto);

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titleImage").value("default-event-image.jpg"))
            .andExpect(jsonPath("$.imageUrls[0]").value("default-event-image.jpg"));
    }

    @Test
    public void createEventWithMultipleImages_FirstShouldBeMain() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        MockMultipartFile image1 =
            new MockMultipartFile("images", "first.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile image2 =
            new MockMultipartFile("images", "second.png", MediaType.IMAGE_PNG_VALUE, createValidPngBytes());
        MockMultipartFile image3 =
            new MockMultipartFile("images", "third.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());

        EventDto responseDto = EventDto.builder()
            .id(1L)
            .title("Eco Cleanup")
            .description("Let's clean the park together for a better future!")
            .open(true)
            .organizerId(5L)
            .titleImage("event-1-first.jpg") // First image becomes main
            .imageUrls(List.of("event-1-first.jpg", "event-1-second.png", "event-1-third.jpg"))
            .datesLocations(List.of(locationDto))
            .createdAt(OffsetDateTime.now())
            .build();

        when(eventService.createEvent(any(AddEventDtoRequest.class), any(MultipartFile[].class), eq(5L)))
            .thenReturn(responseDto);

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(image1)
            .file(image2)
            .file(image3)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.titleImage").value("event-1-first.jpg"))
            .andExpect(jsonPath("$.imageUrls[0]").value("event-1-first.jpg"))
            .andExpect(jsonPath("$.imageUrls.length()").value(3));
    }

    @Test
    public void createEventWithImageWithoutContentType_ShouldReturn400BadRequest() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile imageWithoutContentType = new MockMultipartFile(
            "images",
            "test.jpg",
            null, // No content type
            "fake-content".getBytes());

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(imageWithoutContentType)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void createEventWithMixedValidAndInvalidImages_ShouldReturn400BadRequest() throws Exception {

        String json = objectMapper.writeValueAsString(addEventDtoRequest);

        MockMultipartFile dtoPart = new MockMultipartFile(
            "addEventDtoRequest",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes());

        MockMultipartFile validImage =
            new MockMultipartFile("images", "valid.jpg", MediaType.IMAGE_JPEG_VALUE, createValidJpegBytes());
        MockMultipartFile invalidImage =
            new MockMultipartFile("images", "invalid.bmp", "image/bmp", "invalid-content".getBytes());

        mockMvc.perform(multipart("/events/create")
            .file(dtoPart)
            .file(validImage)
            .file(invalidImage)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void getMyEvents_ShouldReturn200Ok() throws Exception {

        EventPreviewDto eventPreview = EventPreviewDto.builder()
            .id(1L)
            .title("Test Event")
            .description("Test Description")
            .open(true)
            .organizerId(1L)
            .titleImage("test-image.jpg")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .status(EventStatus.UPCOMING)
            .nearestStart(OffsetDateTime.now().plusDays(1))
            .canCancelJoin(true)
            .isFavourite(false)
            .isSubscribed(false)
            .visibility("PUBLIC")
            .latitude(50.45)
            .longitude(30.52)
            .onlineLink(null)
            .build();

        Page<EventPreviewDto> eventPage = new PageImpl<>(List.of(eventPreview), PageRequest.of(0, 10), 1);

        when(eventService.getMyEvents(eq(5L), eq(EventType.BOTH), eq(null), eq(null), any(Pageable.class)))
            .thenReturn(eventPage);

        mockMvc.perform(get("/events/myEvents")
            .param("eventType", "BOTH")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Test Event"))
            .andExpect(jsonPath("$.content[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void getMyEventsWithCoordinates_ShouldReturn200Ok() throws Exception {

        EventPreviewDto eventPreview = EventPreviewDto.builder()
            .id(1L)
            .title("Test Event")
            .description("Test Description")
            .open(true)
            .organizerId(1L)
            .titleImage("test-image.jpg")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .status(EventStatus.UPCOMING)
            .nearestStart(OffsetDateTime.now().plusDays(1))
            .canCancelJoin(true)
            .isFavourite(false)
            .isSubscribed(false)
            .visibility("PUBLIC")
            .latitude(50.45)
            .longitude(30.52)
            .onlineLink(null)
            .build();

        Page<EventPreviewDto> eventPage = new PageImpl<>(List.of(eventPreview), PageRequest.of(0, 10), 1);

        when(eventService.getMyEvents(eq(5L), eq(EventType.PLACE), eq(50.45), eq(30.52), any(Pageable.class)))
            .thenReturn(eventPage);

        mockMvc.perform(get("/events/myEvents")
            .param("eventType", "PLACE")
            .param("userLatitude", "50.45")
            .param("userLongitude", "30.52")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Test Event"))
            .andExpect(jsonPath("$.content[0].status").value("UPCOMING"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void getMyCreatedEvents_ShouldReturn200Ok() throws Exception {

        EventPreviewDto eventPreview = EventPreviewDto.builder()
                .id(1L)
                .title("My Created Event")
                .description("Event I created")
                .open(true)
                .organizerId(5L) // Same as current user
                .titleImage("my-event-image.jpg")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .status(EventStatus.UPCOMING)
                .nearestStart(OffsetDateTime.now().plusDays(1))
                .canCancelJoin(true)
                .canEdit(true) // Should be true for organizer
                .isFavourite(false)
                .isSubscribed(false)
                .visibility("PUBLIC")
                .latitude(50.45)
                .longitude(30.52)
                .onlineLink(null)
                .build();

        Page<EventPreviewDto> eventPage = new PageImpl<>(List.of(eventPreview), PageRequest.of(0, 10), 1);

        when(eventService.getMyCreatedEvents(eq(5L), any(Pageable.class)))
                .thenReturn(eventPage);

        mockMvc.perform(get("/events/myCreatedEvents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("My Created Event"))
                .andExpect(jsonPath("$.content[0].organizerId").value(5))
                .andExpect(jsonPath("$.content[0].canEdit").value(true))
                .andExpect(jsonPath("$.content[0].status").value("UPCOMING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void getMyCreatedEvents_ShouldReturnPassedEvents() throws Exception {

        EventPreviewDto passedEvent = EventPreviewDto.builder()
                .id(2L)
                .title("Past Event")
                .description("Event that already happened")
                .open(true)
                .organizerId(5L)
                .titleImage("past-event-image.jpg")
                .createdAt(OffsetDateTime.now().minusDays(10))
                .updatedAt(OffsetDateTime.now().minusDays(10))
                .status(EventStatus.PASSED)
                .nearestStart(OffsetDateTime.now().minusDays(5))
                .canCancelJoin(false)
                .canEdit(true) // Should still be true for organizer
                .isFavourite(false)
                .isSubscribed(false)
                .visibility("PUBLIC")
                .latitude(50.45)
                .longitude(30.52)
                .onlineLink(null)
                .build();

        Page<EventPreviewDto> eventPage = new PageImpl<>(List.of(passedEvent), PageRequest.of(0, 10), 1);

        when(eventService.getMyCreatedEvents(eq(5L), any(Pageable.class)))
                .thenReturn(eventPage);

        mockMvc.perform(get("/events/myCreatedEvents")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Past Event"))
                .andExpect(jsonPath("$.content[0].status").value("PASSED"))
                .andExpect(jsonPath("$.content[0].canEdit").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
