package greencity.service;

import greencity.client.RestClient;
import greencity.dto.econews.AddEcoNewsDtoResponse;
import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;
import greencity.entity.EcoNews;
import greencity.entity.NewsletterSubscription;
import greencity.enums.NewsletterSubscriptionSource;
import greencity.enums.NewsletterSubscriptionStatus;
import greencity.mapping.AddEcoNewsDtoResponseMapper;
import greencity.repository.EcoNewsRepo;
import greencity.repository.NewsletterSubscriptionRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class NewsSubscriberServiceImpl implements NewsSubscriberService {
    private final NewsletterSubscriptionRepo subscriptionRepo;
    private final RestClient restClient;
    private final EcoNewsRepo ecoNewsRepo;
    private final AddEcoNewsDtoResponseMapper addEcoNewsDtoResponseMapper;

    @Override
    public NewsSubscriberResponseDto subscribe(NewsSubscriberRequestDto dto) {
        String email = dto.getEmail();
        String token = UUID.randomUUID().toString();

        try {
            Optional<NewsletterSubscription> newsletterSubscription = subscriptionRepo.findByEmail(email);
            if (newsletterSubscription.isPresent()) {
                return new NewsSubscriberResponseDto(email, "no token, email already exists");
            }
            NewsletterSubscription subscription = new NewsletterSubscription();
            subscription.setEmail(email);
            subscription.setStatus(NewsletterSubscriptionStatus.SUBSCRIBED);
            subscription.setCreatedDate(LocalDateTime.now());
            subscription.setSource(NewsletterSubscriptionSource.LANDING);

            subscriptionRepo.save(subscription);
            NewsSubscriberResponseDto responseDto = new NewsSubscriberResponseDto(dto.getEmail(), token);
            List<NewsSubscriberResponseDto> subscriber = List.of(responseDto);

            EcoNews latest = ecoNewsRepo.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("No news found"));

            AddEcoNewsDtoResponse newsDto = addEcoNewsDtoResponseMapper.convert(latest);
            restClient.sendNewNewsForSubscriber(subscriber, newsDto);

            return new NewsSubscriberResponseDto(email, token);
        } catch (DataIntegrityViolationException e) {
            return new NewsSubscriberResponseDto(email, "Unexpected value");
        }
    }
}
