package greencity.service;

import greencity.client.RestClient;
import greencity.dto.econews.AddEcoNewsDtoResponse;
import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;
import greencity.entity.EcoNews;
import greencity.entity.NewsletterSubscription;
import greencity.mapping.AddEcoNewsDtoResponseMapper;
import greencity.repository.EcoNewsRepo;
import greencity.repository.NewsletterSubscriptionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NewsSubscriberServiceImplTest {
    @Mock
    private NewsletterSubscriptionRepo subscriptionRepo;

    @Mock
    private EcoNewsRepo ecoNewsRepo;

    @Mock
    private RestClient restClient;

    @Mock
    private AddEcoNewsDtoResponseMapper addEcoNewsDtoResponseMapper;

    @InjectMocks
    private NewsSubscriberServiceImpl newsSubscriberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void subscribe_ShouldReturnSuccess_WhenNewEmail() {
        String email = "new@test.com";
        NewsSubscriberRequestDto dto = new NewsSubscriberRequestDto(email);

        when(subscriptionRepo.findByEmail(email)).thenReturn(Optional.empty());

        EcoNews latestNews = new EcoNews();
        when(ecoNewsRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.of(latestNews));
        when(addEcoNewsDtoResponseMapper.convert(latestNews)).thenReturn(new AddEcoNewsDtoResponse());

        NewsSubscriberResponseDto result = newsSubscriberService.subscribe(dto);

        assertEquals(email, result.getEmail());
        assertNotNull(result.getUnsubscribeToken());
        verify(subscriptionRepo).save(any(NewsletterSubscription.class));
        verify(restClient).sendNewNewsForSubscriber(anyList(), any());
    }

    @Test
    void subscribe_ShouldReturnAlreadyExists_WhenEmailExists() {
        String email = "exists@test.com";
        NewsSubscriberRequestDto dto = new NewsSubscriberRequestDto(email);
        when(subscriptionRepo.findByEmail(email)).thenReturn(Optional.of(new NewsletterSubscription()));

        NewsSubscriberResponseDto result = newsSubscriberService.subscribe(dto);

        assertEquals(email, result.getEmail());
        assertEquals("no token, email already exists", result.getUnsubscribeToken());
        verify(subscriptionRepo, never()).save(any());
        verify(restClient, never()).sendNewNewsForSubscriber(anyList(), any());
    }

    @Test
    void subscribe_ShouldThrowException_WhenNoNewsFound() {
        String email = "nonews@test.com";
        NewsSubscriberRequestDto dto = new NewsSubscriberRequestDto(email);
        when(subscriptionRepo.findByEmail(email)).thenReturn(Optional.empty());
        when(ecoNewsRepo.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> newsSubscriberService.subscribe(dto));
        verify(subscriptionRepo).save(any());
    }

    @Test
    void subscribe_ShouldReturnUnexpected_WhenDataIntegrityError() {
        String email = "error@test.com";
        NewsSubscriberRequestDto dto = new NewsSubscriberRequestDto(email);
        when(subscriptionRepo.findByEmail(email)).thenReturn(Optional.empty());
        when(subscriptionRepo.save(any())).thenThrow(new DataIntegrityViolationException("DB error"));

        NewsSubscriberResponseDto result = newsSubscriberService.subscribe(dto);

        assertEquals(email, result.getEmail());
        assertEquals("Unexpected value", result.getUnsubscribeToken());
    }
}