package greencity.controller;

import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;
import greencity.service.NewsSubscriberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/newsSubscriber")
public class NewsSubscriberController {
    private final NewsSubscriberService newsSubscriberService;

    @PostMapping
    public ResponseEntity<NewsSubscriberResponseDto> subscribe(
        @RequestBody @Valid NewsSubscriberRequestDto newsSubscriberRequestDto) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(newsSubscriberService.subscribe(newsSubscriberRequestDto));
    }
}
