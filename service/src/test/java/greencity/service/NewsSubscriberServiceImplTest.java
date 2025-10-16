package greencity.service;

import greencity.client.RestClient;
import greencity.dto.econews.AddEcoNewsDtoResponse;
import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;
import greencity.entity.EcoNews;
import greencity.entity.NewsletterSubscription;
import greencity.enums.NewsletterSubscriptionStatus;
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
    void subscribe_ShouldReturnNewToken_WhenEmailExists() {
        String email = "exists@test.com";
        NewsletterSubscription existingSubscription = new NewsletterSubscription();
        existingSubscription.setStatus(NewsletterSubscriptionStatus.UNSUBSCRIBED); // або SUBSCRIBED
        when(subscriptionRepo.findByEmail(email)).thenReturn(Optional.of(existingSubscription));

        NewsSubscriberRequestDto dto = new NewsSubscriberRequestDto(email);
        NewsSubscriberResponseDto result = newsSubscriberService.subscribe(dto);

        assertEquals(email, result.getEmail());
        assertNotNull(result.getUnsubscribeToken());
        verify(subscriptionRepo).save(existingSubscription);
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

    @Test
    void unsubscribe_ShouldReturnInvalidToken_WhenTokenNotFound() {
        String token = "invalid-token";
        when(subscriptionRepo.findByUnsubscribeToken(token)).thenReturn(Optional.empty());

        NewsSubscriberResponseDto result = newsSubscriberService.unsubscribe("test@test.com", token);

        assertNull(result.getEmail());
        assertEquals("invalid token", result.getUnsubscribeToken());
    }

    @Test
    void unsubscribe_ShouldReturnAlreadyUnsubscribed_WhenStatusUnsubscribed() {
        String email = "test@test.com";
        String token = "valid-token";

        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.setEmail(email);
        subscription.setStatus(NewsletterSubscriptionStatus.UNSUBSCRIBED);

        when(subscriptionRepo.findByUnsubscribeToken(token)).thenReturn(Optional.of(subscription));

        NewsSubscriberResponseDto result = newsSubscriberService.unsubscribe(email, token);

        assertEquals(email, result.getEmail());
        assertEquals("already unsubscribed", result.getUnsubscribeToken());
    }

    @Test
    void unsubscribe_ShouldReturnSuccess_WhenValidTokenAndSubscribed() {
        String email = "test@test.com";
        String token = "valid-token";

        NewsletterSubscription subscription = new NewsletterSubscription();
        subscription.setEmail(email);
        subscription.setStatus(NewsletterSubscriptionStatus.SUBSCRIBED);

        when(subscriptionRepo.findByUnsubscribeToken(token)).thenReturn(Optional.of(subscription));

        NewsSubscriberResponseDto result = newsSubscriberService.unsubscribe(email, token);

        assertEquals(email, result.getEmail());
        assertEquals("successfully unsubscribed", result.getUnsubscribeToken());
        assertEquals(NewsletterSubscriptionStatus.UNSUBSCRIBED, subscription.getStatus());
        verify(subscriptionRepo).save(subscription);
    }

}