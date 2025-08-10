package com.shopir.user.ITTests.controller;

import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.CreateCartRequestDto;
import com.shopir.user.dto.response.CartResponse;
import com.shopir.user.security.MyUserDetails;
import com.shopir.user.service.KafkaConsumerService;
import com.shopir.user.service.OrderService;
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
import org.mockito.InjectMocks;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
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
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CartControllerIT {

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
    private WebTestClient webTestClient;

    @MockitoBean
    private  KafkaConsumerService kafkaConsumerService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


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
    void createCart() throws Exception {

        ProductKafkaDto productKafkaDto = new ProductKafkaDto();
        productKafkaDto.setIdProduct(1L);
        productKafkaDto.setNameProduct("Наушники JBL");

        when(kafkaConsumerService.getNameProductByIdCart(anyLong())).thenReturn(productKafkaDto);
        CreateCartRequestDto requestDto = CreateCartRequestDto.builder()
                .idProduct(1L)
                .build();

        MyUserDetails userDetails = new MyUserDetails(idUser, "ivan@example.com","$2a$12$Q2htJki53hWX9.VAxAdHluZJP8wGVjnA4g3/ZazAwKzNiSJlzFMdW",  List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String accessToken = jwtUtils.generateToken(authentication);

        String mockBearerToken = "Bearer " + accessToken;

        webTestClient.post()
                .uri("/cart")
                .header(HttpHeaders.AUTHORIZATION, mockBearerToken)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus()
                .isCreated()
        ;

        kafkaTemplate.send("product-request", "1");


        CartResponse findCart = webTestClient.get()
                .uri("/cart/" + 1L)
                .header(HttpHeaders.AUTHORIZATION, mockBearerToken)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CartResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(findCart).isNotNull();
        assertThat(findCart.getIdProduct()).isEqualTo(requestDto.getIdProduct());
    }



}
