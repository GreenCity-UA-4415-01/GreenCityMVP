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

    /**
     * Method for subscribing a user to the newsletter. If the user with the given
     * email already exists: - and is already subscribed → returns a message
     * "already subscribed" - and was previously unsubscribed → updates status to
     * SUBSCRIBED and generates a new unsubscribe token If the user does not exist →
     * creates a new subscription with a generated unsubscribe token, saves it to
     * the database, retrieves the latest news, and sends it to the subscriber.
     *
     * @param dto DTO containing the email of the user to subscribe
     * @return NewsSubscriberResponseDto containing the email and either the
     *         unsubscribe token or a status message
     * @throws DataIntegrityViolationException if there is an unexpected database
     *                                         integrity issue
     * @author Andrii Zakordonskyi
     */
    @Override
    public NewsSubscriberResponseDto subscribe(NewsSubscriberRequestDto dto) {
        String email = dto.getEmail();

        try {
            Optional<NewsletterSubscription> newsletterSubscription = subscriptionRepo.findByEmail(email);
            if (newsletterSubscription.isPresent()) {
                NewsletterSubscription subscription = newsletterSubscription.get();
                if (subscription.getStatus() == NewsletterSubscriptionStatus.SUBSCRIBED) {
                    return new NewsSubscriberResponseDto(email, "no token, email already exists");
                }
                String newToken = UUID.randomUUID().toString();
                subscription.setStatus(NewsletterSubscriptionStatus.SUBSCRIBED);
                subscription.setUpdatedDate(LocalDateTime.now());
                subscription.setUnsubscribeToken(newToken);
                subscriptionRepo.save(subscription);

                return new NewsSubscriberResponseDto(email, newToken);
            }
            String token = UUID.randomUUID().toString();
            NewsletterSubscription subscription = new NewsletterSubscription();
            subscription.setEmail(email);
            subscription.setStatus(NewsletterSubscriptionStatus.SUBSCRIBED);
            subscription.setCreatedDate(LocalDateTime.now());
            subscription.setSource(NewsletterSubscriptionSource.LANDING);
            subscription.setUnsubscribeToken(token);
            subscriptionRepo.save(subscription);

            EcoNews latest = ecoNewsRepo.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("No news found"));
            AddEcoNewsDtoResponse newsDto = addEcoNewsDtoResponseMapper.convert(latest);

            NewsSubscriberResponseDto responseDto = new NewsSubscriberResponseDto(email, token);
            restClient.sendNewNewsForSubscriber(List.of(responseDto), newsDto);

            return responseDto;
        } catch (DataIntegrityViolationException e) {
            return new NewsSubscriberResponseDto(email, "Unexpected value");
        }
    }

    /**
     * Method for unsubscribing a user from the newsletter using an unsubscribe
     * token. The method checks if a subscription with the given token exists: - If
     * the token is invalid → returns a message "invalid token" - If the user is
     * already unsubscribed → returns a message "already unsubscribed" - Otherwise →
     * updates the subscription status to UNSUBSCRIBED, sets the updated date, saves
     * the changes to the database, and returns a message "successfully
     * unsubscribed".
     *
     * @param email the email of the user to unsubscribe (used for response
     *              purposes)
     * @param token the unsubscribe token associated with the subscription
     * @return NewsSubscriberResponseDto containing the user's email and a status
     *         message
     * @author Andrii Zakordonskyi
     */
    @Override
    public NewsSubscriberResponseDto unsubscribe(String email, String token) {
        Optional<NewsletterSubscription> subscriptionOpt = subscriptionRepo.findByUnsubscribeToken(token);

        if (subscriptionOpt.isEmpty()) {
            return new NewsSubscriberResponseDto(null, "invalid token");
        }

        NewsletterSubscription subscription = subscriptionOpt.get();

        if (subscription.getStatus() == NewsletterSubscriptionStatus.UNSUBSCRIBED) {
            return new NewsSubscriberResponseDto(subscription.getEmail(), "already unsubscribed");
        }

        subscription.setStatus(NewsletterSubscriptionStatus.UNSUBSCRIBED);
        subscription.setUpdatedDate(LocalDateTime.now());
        subscriptionRepo.save(subscription);

        return new NewsSubscriberResponseDto(subscription.getEmail(), "successfully unsubscribed");
    }
}
