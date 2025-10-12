package greencity.service;

import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;

public interface NewsSubscriberService {
    NewsSubscriberResponseDto subscribe(NewsSubscriberRequestDto newsSubscriberRequestDto);
}
