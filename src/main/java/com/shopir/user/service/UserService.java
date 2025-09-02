package com.shopir.user.service;

import com.shopir.user.dto.request.CreateOrEditUserRequestDto;
import com.shopir.user.dto.request.LoginRequestDto;
import com.shopir.user.dto.response.EditUserResponseDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.*;
import com.shopir.user.exceptions.BadRequestException;
import com.shopir.user.exceptions.NotFoundException;
import com.shopir.user.exceptions.UnauthorizedException;
import com.shopir.user.factories.UserFactory;
import com.shopir.user.repository.*;
import com.shopir.user.security.MyUserDetails;
import com.shopir.user.utils.JwtUtils;
import com.shopir.user.utils.PasswordChecker;
import com.shopir.user.validation.ValidationErrors;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "User")
@Service
public class UserService implements UserDetailsService  {
    private final WebUserRepository webUserRepository;
    private final RoleUserRepository roleUserRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final UserFactory userFactory;
    private final PasswordChecker passwordEncoder;
    private final ValidationErrors validationErrors;
    private final JwtUtils jwtUtils;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;
     AuthenticationManager authManager;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, CompletableFuture<Long>> pendingRequests = new ConcurrentHashMap<>();

    @Autowired
    public UserService(WebUserRepository webUserRepository, RoleUserRepository roleUserRepository,
                       AddressRepository addressRepository, CityRepository cityRepository, UserFactory userFactory,
                       PasswordChecker passwordEncoder, ValidationErrors validationErrors, JwtUtils jwtUtils, ConfirmationTokenRepository confirmationTokenRepository, EmailService emailService,
                       AuthenticationManager authManager, KafkaTemplate<String, String> kafkaTemplate) {
        this.webUserRepository = webUserRepository;
        this.roleUserRepository = roleUserRepository;
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.validationErrors = validationErrors;
        this.jwtUtils = jwtUtils;
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.emailService = emailService;
        this.authManager = authManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Cacheable(value = "user", key = "#idWebUser")
    public UserInformationResponseDto findUser(Long idWebUser) {
        WebUser webUser = webUserRepository.findById(idWebUser).orElseThrow(() -> new NotFoundException("Not found user by id = " + idWebUser));
        return userFactory.makeUserDto(webUser);
    }

    @Transactional
    public String createUser(CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) throws SQLException {
        if(bindingResult.hasErrors() ) {
            String result = validationErrors.getValidationErrors(bindingResult);
            logger.error("Validation errors occurred while register user: {}", result);
            throw new BadRequestException(result);
        }

        if(webUserRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new BadRequestException("The user with this email already exists.");
        }
        if(requestDto.getPassword().length() < 6) {
            throw new BadRequestException("password must be more than 6 characters.");
        }
        RoleUser role = roleUserRepository.findByNameRole("ROLE_CLIENT").orElseThrow(() -> new NotFoundException("Role not found"));
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        WebUser webUser = WebUser.builder()
                .name(requestDto.getName())
                .surname(requestDto.getSurname())
                .phone(requestDto.getPhone())
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .roleUser(role)
                .activated(0)
                .build();

        webUserRepository.save(webUser);

        WebUser createdWebUser = webUserRepository.findById(webUser.getIdWebUser()).orElseThrow();

        ConfirmationToken confirmationToken = createConfirmationToken(webUser);
        logger.info("ConfirmToken ID: {}", confirmationToken.getIdConfirmationToken());
        sendConfirmationEmail(requestDto, confirmationToken.getUserToken());
        logger.debug("Successful sending of email confirmation");

        return "Confirmation email sent to " + requestDto.getEmail() + ". Please confirm your account.";
    }

    @Transactional
    public String loginUser(LoginRequestDto requestDto)  {
        if (requestDto.getEmail() == null || requestDto.getPassword() == null) {
            throw new BadRequestException("Email and password must not be null");
        }

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));
            WebUser webUser =  webUserRepository.findByEmail(requestDto.getEmail()).orElseThrow();

            if (authentication.isAuthenticated() && webUser.getActivated() == 1) {
                return jwtUtils.generateToken(authentication);
            } else {
                throw new UnauthorizedException("User is not registered");
            }
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Transactional
    public void createAddress(Long idWebUser, CreateOrEditUserRequestDto requestDto) {
        City city = cityRepository.findById(requestDto.getIdCity()).orElseThrow(() -> new NotFoundException("Not found City by Id"));
        WebUser webUser = webUserRepository.findById(idWebUser).orElseThrow();

        if(requestDto.getIdCity() != null ||requestDto.getStreet() != null || requestDto.getHome() != null || requestDto.getFloor() != null) {

        Address address = Address.builder()
                .webUser(webUser)
                .city(city)
                .street(requestDto.getStreet())
                .home(requestDto.getHome())
                .floor(requestDto.getFloor())
                .build();

        if (requestDto.getAdditionalInformation() != null) {
            address.setAdditionalInformation(requestDto.getAdditionalInformation());
        }
        addressRepository.save(address);
        } else {
            throw new BadRequestException("Incorrect parameters were passed for createAddress");
        }

    }


    @Transactional
    @CachePut(value = "user", key = "#idWebUser")
    public UserInformationResponseDto  editUser(CreateOrEditUserRequestDto requestDto, Long idWebUser, BindingResult bindingResult) {
        logger.info("Data for edit user, name = {}, surname = {}, phone = {}", requestDto.getName(), requestDto.getSurname(), requestDto.getPhone());
        if(bindingResult.hasErrors() ) {
            String result = validationErrors.getValidationErrors(bindingResult);
            logger.error("Validation errors occurred while edit user: {}", result);
            throw new BadRequestException(result);
        }
        WebUser webUser = webUserRepository.findById(idWebUser).orElseThrow();

        if(requestDto.getName() != null) {
            webUser.setName(requestDto.getName());
        }

        if(requestDto.getSurname() != null) {
            webUser.setSurname(requestDto.getSurname());
        }

        if(requestDto.getPhone() != null) {
            webUser.setPhone(requestDto.getPhone());
        }

        WebUser updatedUser = webUserRepository.save(webUser);
        logger.info("Data after edit user, name = {}, surname = {}, phone = {}", updatedUser.getName(), updatedUser.getSurname(), updatedUser.getPhone());

        return  userFactory.makeUserDto(updatedUser);
    }

    @Transactional
    public void editPasswordOrEmail(Long idWebUser, CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) throws SQLException {
        logger.info("Data for edit user, email = {}, password = {}", requestDto.getEmail(), requestDto.getPassword());

        if(bindingResult.hasErrors() ) {
            String result = validationErrors.getValidationErrors(bindingResult);
            logger.error("Validation errors occurred while edit password or email: {}", result);
            throw new BadRequestException(result);
        }

        WebUser webUser = webUserRepository.findById(idWebUser).orElseThrow();

        if(requestDto.getEmail() != null) {
            if(webUserRepository.findByEmail(requestDto.getEmail()).isPresent()) {
                throw new BadRequestException("The user with this email already exists.");
            }
            ConfirmationToken confirmationToken = createConfirmationToken(webUser);
            webUser.setTemporaryMail(requestDto.getEmail());
            sendConfirmationUpdateEmail(requestDto, confirmationToken.getUserToken());
            logger.debug("Successful sending of email confirmation in update method");        }

        if(requestDto.getPassword() != null) {
            String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
            webUser.setPassword(encodedPassword);
        }
        webUserRepository.save(webUser);

    }

    @Transactional
    public void editAddress(CreateOrEditUserRequestDto requestDto, Long idWebUser) {

        Address address = addressRepository.findByWebUser_IdWebUser(idWebUser);

        if(requestDto.getIdCity() != null) {
            City city = cityRepository.findById(requestDto.getIdCity()).orElseThrow();
            address.setCity(city);
        }

        if(requestDto.getStreet() != null) {
            address.setStreet(requestDto.getStreet());
        }
        if(requestDto.getHome() != null) {
            address.setHome(requestDto.getHome());
        }
        if(requestDto.getFloor() != null) {
            address.setFloor(requestDto.getFloor());
        }
        if(requestDto.getAdditionalInformation() != null) {
            address.setAdditionalInformation(requestDto.getAdditionalInformation());
        }
        addressRepository.save(address);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        WebUser user = webUserRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(
                String.format("User '%s' not found", email)
        ));

        logger.info("User found: {}", user.getEmail());
        logger.info("Stored password: {}", user.getPassword());
        return MyUserDetails.buildUserDetails(user);
    }

    private ConfirmationToken createConfirmationToken(WebUser user) throws SQLException {
            String token = UUID.randomUUID().toString();

            ConfirmationToken confirmationToken = new ConfirmationToken(
                    token,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMinutes(15),
                    user
            );
            confirmationToken.setWebUser(user);
            user.setConfirmationTokens(new HashSet<>(List.of(confirmationToken)));

            confirmationTokenRepository.save(confirmationToken);

            return confirmationToken;
    }

    private void sendConfirmationEmail(CreateOrEditUserRequestDto dtoRequest, String token) {
            String link = "http://localhost:8090/registration/confirm?token=" + token;
            emailService.send(dtoRequest.getEmail(), buildEmail(dtoRequest.getName(), link));
    }

    private void sendConfirmationUpdateEmail(CreateOrEditUserRequestDto dtoRequest, String token) {

            String link = "http://localhost:8090/registration/confirm?token=" + token;
        emailService.send(dtoRequest.getEmail(), buildEmail(dtoRequest.getName(), link));

    }

    @Scheduled(fixedRate = 900000,  initialDelay = 5000)
    public void removeExpiredUsers() {
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            Set<Long> expiredTokens = confirmationTokenRepository.findAllExpiredToken(timestamp);

            for (Long tokenId : expiredTokens) {

                Long userId = webUserRepository.findByIdToken(tokenId);
                WebUser webUser = webUserRepository.findById(userId).orElseThrow(() -> new NotFoundException("Not found by idUser: " + userId));

                if (userId != null && webUser.getTemporaryMail() != null) {
                    confirmationTokenRepository.deleteById(tokenId);
                    webUserRepository.deleteById(userId);
                }
            }
    }

    public String confirmToken(String token){

                    ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token);
                    logger.info("ConfirmationToken found: ID={}, ExpiresAt={}",
                            confirmationToken.getIdConfirmationToken(), confirmationToken.getExpiresAt());

                    if (confirmationToken.getConfirmedAt() != null) {
                        logger.warn("Token already confirmed at: {}", confirmationToken.getConfirmedAt());
                        throw new IllegalStateException("email already confirmed");
                    }

                    LocalDateTime expiredAt = confirmationToken.getExpiresAt();
                    if (expiredAt.isBefore(LocalDateTime.now())) {
                        logger.warn("Token expired at: {}", expiredAt);
                        throw new IllegalStateException("token expired");
                    }

                    WebUser user = confirmationToken.getWebUser();
                    logger.info("Associated WebUser found: ID={}, CurrentEmail={}, TemporaryEmail={}",
                            user.getIdWebUser(), user.getEmail(), user.getTemporaryMail());

                    user.setActivated(1);

                    if (user.getTemporaryMail() != null) {
                        logger.info("Updating user email from temporary email: {}", user.getTemporaryMail());
                        user.setEmail(user.getTemporaryMail());
                        user.setTemporaryMail(null);
                    }

                    webUserRepository.save(user);

                    confirmationToken.setConfirmedAt(LocalDateTime.now());
                    return "confirmed";
        }



    public String buildEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering. Please click on the below link to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}
