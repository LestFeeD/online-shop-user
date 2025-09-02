package com.shopir.user.ITTests.controller;

import com.shopir.user.dto.request.CreateOrEditUserRequestDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.ConfirmationToken;
import com.shopir.user.repository.ConfirmationTokenRepository;
import com.shopir.user.security.MyUserDetails;
import com.shopir.user.service.KafkaConsumerService;
import com.shopir.user.service.UserService;
import com.shopir.user.utils.JwtUtils;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@EmbeddedKafka(partitions = 1, controlledShutdown = true)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerIT {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;


    private static Long idUser;

    @BeforeAll
    static void setUp() throws Exception {
        Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new Liquibase("db/changelog/main-changelog.xml",
                new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id_web_user FROM web_user WHERE name = 'Иван' ORDER BY name DESC LIMIT 1"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    idUser = rs.getLong(1);
                    System.out.println("Inserted idUser = " + idUser);
                }
            }
        }
    }

    @Test
    void addNewUser() throws Exception {

        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .name("test")
                .surname("test")
                .phone("+71111125031")
                .email("test@gmail.com")
                .password("test1234")
                .build();

        webTestClient.post()
                .uri("/registration")
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus()
                .isOk();

        MyUserDetails userDetails = new MyUserDetails(idUser, "test@gmail.com","test1234",  List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String accessToken = jwtUtils.generateToken(authentication);

        String mockBearerToken = "Bearer " + accessToken;

        ConfirmationToken token = confirmationTokenRepository.findAll()
                .stream()
                .filter(t -> t.getWebUser().getEmail().equals(requestDto.getEmail()))
                .findFirst()
                .orElseThrow();

        String confirmResponse = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/registration/confirm")
                        .queryParam("token", token.getUserToken())
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(confirmResponse).contains("confirmed");


        UserInformationResponseDto finUser = webTestClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, mockBearerToken)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInformationResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(finUser).isNotNull();
        assertThat(finUser.getName()).isEqualTo(requestDto.getName());
    }

    @Test
    void editUser() throws Exception {

        MyUserDetails userDetails = new MyUserDetails(idUser, "ivan@example.com","$2a$12$Q2htJki53hWX9.VAxAdHluZJP8wGVjnA4g3/ZazAwKzNiSJlzFMdW",  List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String accessToken = jwtUtils.generateToken(authentication);

        String mockBearerToken = "Bearer " + accessToken;

        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .name("NewName")
                .build();

        webTestClient.patch()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, mockBearerToken)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus()
                .isOk();

        UserInformationResponseDto finUserAfterPatch = webTestClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, mockBearerToken)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserInformationResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(finUserAfterPatch).isNotNull();
        assertThat(finUserAfterPatch.getName()).isEqualTo(requestDto.getName());

    }

}
