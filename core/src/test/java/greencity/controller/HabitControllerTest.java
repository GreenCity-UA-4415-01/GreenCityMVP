package greencity.controller;

import greencity.converters.UserArgumentResolver;
import greencity.service.HabitService;
import greencity.service.TagsService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Principal;

import static greencity.ModelUtils.getPrincipal;

@ExtendWith(MockitoExtension.class)
public class HabitControllerTest {
    public static final String BASE = "/habit";
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks
    private HabitController habitController;

    @Mock
    private HabitService habitService;

    @Mock
    private TagsService tagsService;

    @Mock
    private UserService userService;
    @Mock
    private ModelMapper modelMapper;

    private Principal principal = getPrincipal();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(habitController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new UserArgumentResolver(userService, modelMapper))
                .build();
    }

}
