package com.shopir.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopir.user.dto.UserKafkaDto;
import com.shopir.user.entity.WebUser;
import com.shopir.user.repository.WebUserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private final WebUserRepository webUserRepository;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, WebUserRepository webUserRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.webUserRepository = webUserRepository;
    }

    @KafkaListener(topics = "user-request", groupId = "product-group")
    public void handleProductByIdRequest(String email) throws JsonProcessingException {
        logger.debug("Received user email={}", email);
        WebUser webUser = webUserRepository.findByEmail(email).orElseThrow();
        UserKafkaDto dto = UserKafkaDto.builder()
                .idUser(webUser.getIdWebUser())
                .email(webUser.getEmail())
                .password(webUser.getPassword())
                .role(webUser.getRoleUser().getNameRole())
                .build();

        String json = objectMapper.writeValueAsString(dto);

        kafkaTemplate.send("user-response", json);

    }
}
