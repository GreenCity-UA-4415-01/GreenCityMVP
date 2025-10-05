package greencity.controller;

import greencity.converters.UserArgumentResolver;
import greencity.dto.PageableDto;
import greencity.dto.search.SearchNewsDto;
import greencity.dto.search.SearchResponseDto;
import greencity.dto.user.EcoNewsAuthorDto;
import greencity.dto.user.UserVO;
import greencity.enums.Role;
import greencity.enums.UserStatus;
import greencity.service.LanguageService;
import greencity.service.SearchService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {
    private MockMvc mockMvc;
    @InjectMocks
    SearchController searchController;

    @Mock
    SearchService searchService;

    @Mock
    UserService userService;

    @Mock
    ModelMapper modelMapper;

    @Mock
    Validator mockValidator;

    UserVO user;
    SearchNewsDto news1;
    SearchNewsDto news2;
    SearchResponseDto searchResponse;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(searchController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new UserArgumentResolver(userService, modelMapper))
            .setValidator(mockValidator)
            .build();

        user = UserVO.builder()
            .id(1L)
            .name("Іван Петренко")
            .email("test@example.com")
            .role(Role.ROLE_USER)
            .userCredo("Жити екологічно!")
            .userStatus(UserStatus.ACTIVATED)
            .rating(4.5)
            .dateOfRegistration(LocalDateTime.now().minusMonths(2))
            .lastActivityTime(LocalDateTime.now())
            .firstName("Іван")
            .city("Київ")
            .build();

        news1 = SearchNewsDto.builder()
            .id(1L)
            .title("Новина про екологію")
            .author(new EcoNewsAuthorDto(1L, "Іван"))
            .creationDate(ZonedDateTime.now().minusDays(3))
            .tags(List.of("екологія", "довкілля"))
            .build();

        news2 = SearchNewsDto.builder()
            .id(2L)
            .title("Сортування сміття")
            .author(new EcoNewsAuthorDto(2L, "Олена"))
            .creationDate(ZonedDateTime.now().minusDays(5))
            .tags(List.of("переробка", "поради"))
            .build();

        searchResponse = SearchResponseDto.builder()
            .ecoNews(List.of(news1, news2))
            .countOfResults(2L)
            .build();
    }

    @Test
    void testGlobalSearchWithValidQuery() throws Exception {
        String language = "ua";
        String searchQuery = "eco";

        when(searchService.search(searchQuery, language))
            .thenReturn(searchResponse);

        mockMvc.perform(get("/search?searchQuery=" + searchQuery)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.countOfResults").value(searchResponse.getCountOfResults()))
            .andExpect(jsonPath("$.ecoNews.length()").value(searchResponse.getEcoNews().size()))

            .andExpect(jsonPath("$.ecoNews[0].id").value(news1.getId()))
            .andExpect(jsonPath("$.ecoNews[0].title").value(news1.getTitle()))
            .andExpect(jsonPath("$.ecoNews[0].author.id").value(news1.getAuthor().getId()))
            .andExpect(jsonPath("$.ecoNews[0].author.name").value(news1.getAuthor().getName()))
            .andExpect(jsonPath("$.ecoNews[0].tags[0]").value(news1.getTags().get(0)))
            .andExpect(jsonPath("$.ecoNews[0].tags[1]").value(news1.getTags().get(1)))

            .andExpect(jsonPath("$.ecoNews[1].id").value(news2.getId()))
            .andExpect(jsonPath("$.ecoNews[1].title").value(news2.getTitle()))
            .andExpect(jsonPath("$.ecoNews[1].author.id").value(news2.getAuthor().getId()))
            .andExpect(jsonPath("$.ecoNews[1].author.name").value(news2.getAuthor().getName()))
            .andExpect(jsonPath("$.ecoNews[1].tags[0]").value(news2.getTags().get(0)))
            .andExpect(jsonPath("$.ecoNews[1].tags[1]").value(news2.getTags().get(1)));
    }

    @Test
    void testGlobalSearchWithInValidQuery() throws Exception {
        String language = "ua";
        String searchQuery = "invalid-query";

        mockMvc.perform(get("/search?" + searchQuery)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGlobalSearchWithAbsentQuery() throws Exception {
        String language = "ua";

        mockMvc.perform(get("/search")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testEcoNewsSearch() throws Exception {
        String language = "ua";
        String searchQuery = "eco";
        Pageable pageable = PageRequest.of(0, 5);
        PageableDto<SearchNewsDto> pageableDto = new PageableDto<>(List.of(news1, news2), 0, 1, 2);

        when(searchService.searchAllNews(pageable, searchQuery, language))
            .thenReturn(pageableDto);

        mockMvc.perform(get("/search/econews?searchQuery=eco&page=0&size=5")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(pageableDto.getTotalElements()))
            .andExpect(jsonPath("$.currentPage").value(pageableDto.getCurrentPage()))
            .andExpect(jsonPath("$.totalPages").value(pageableDto.getTotalPages()))

            .andExpect(jsonPath("$.page[0].id").value(news1.getId()))
            .andExpect(jsonPath("$.page[0].title").value(news1.getTitle()))
            .andExpect(jsonPath("$.page[0].author.id").value(news1.getAuthor().getId()))
            .andExpect(jsonPath("$.page[0].author.name").value(news1.getAuthor().getName()))
            .andExpect(jsonPath("$.page[0].tags[0]").value(news1.getTags().get(0)))
            .andExpect(jsonPath("$.page[0].tags[1]").value(news1.getTags().get(1)))

            .andExpect(jsonPath("$.page[1].id").value(news2.getId()))
            .andExpect(jsonPath("$.page[1].title").value(news2.getTitle()))
            .andExpect(jsonPath("$.page[1].author.id").value(news2.getAuthor().getId()))
            .andExpect(jsonPath("$.page[1].author.name").value(news2.getAuthor().getName()))
            .andExpect(jsonPath("$.page[1].tags[0]").value(news2.getTags().get(0)))
            .andExpect(jsonPath("$.page[1].tags[1]").value(news2.getTags().get(1)));
    }

}
