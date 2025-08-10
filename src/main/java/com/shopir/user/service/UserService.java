package com.shopir.user.service;

import com.shopir.user.dto.request.CreateOrEditUserRequestDto;
import com.shopir.user.dto.request.LoginRequestDto;
import com.shopir.user.dto.response.EditUserResponseDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.Address;
import com.shopir.user.entity.City;
import com.shopir.user.entity.RoleUser;
import com.shopir.user.entity.WebUser;
import com.shopir.user.exceptions.BadRequestException;
import com.shopir.user.exceptions.NotFoundException;
import com.shopir.user.exceptions.UnauthorizedException;
import com.shopir.user.factories.UserFactory;
import com.shopir.user.repository.AddressRepository;
import com.shopir.user.repository.CityRepository;
import com.shopir.user.repository.RoleUserRepository;
import com.shopir.user.repository.WebUserRepository;
import com.shopir.user.security.MyUserDetails;
import com.shopir.user.utils.JwtUtils;
import com.shopir.user.utils.PasswordChecker;
import com.shopir.user.validation.ValidationErrors;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
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

import java.util.Map;
import java.util.Optional;
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
     AuthenticationManager authManager;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, CompletableFuture<Long>> pendingRequests = new ConcurrentHashMap<>();

    @Autowired
    public UserService(WebUserRepository webUserRepository, RoleUserRepository roleUserRepository,
                       AddressRepository addressRepository, CityRepository cityRepository, UserFactory userFactory,
                       PasswordChecker passwordEncoder, ValidationErrors validationErrors, JwtUtils jwtUtils,
                       AuthenticationManager authManager, KafkaTemplate<String, String> kafkaTemplate) {
        this.webUserRepository = webUserRepository;
        this.roleUserRepository = roleUserRepository;
        this.addressRepository = addressRepository;
        this.cityRepository = cityRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.validationErrors = validationErrors;
        this.jwtUtils = jwtUtils;
        this.authManager = authManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Cacheable(value = "user", key = "#idWebUser")
    public UserInformationResponseDto findUser(Long idWebUser) {
        WebUser webUser = webUserRepository.findById(idWebUser).orElseThrow(() -> new NotFoundException("Not found user by id = " + idWebUser));
        return userFactory.makeUserDto(webUser);
    }

    @Transactional
    public Long createUser(CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) {
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
                .build();

        webUserRepository.save(webUser);

        WebUser createdWebUser = webUserRepository.findById(webUser.getIdWebUser()).orElseThrow();

        return createdWebUser.getIdWebUser();
    }

    @Transactional
    public String loginUser(LoginRequestDto requestDto)  {
        if (requestDto.getEmail() == null || requestDto.getPassword() == null) {
            throw new BadRequestException("Email and password must not be null");
        }

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

            if (authentication.isAuthenticated()) {
                return jwtUtils.generateToken(authentication);
            }
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return "fail";
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
    public void editPasswordOrEmail(Long idWebUser, CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) {
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
            webUser.setEmail(requestDto.getEmail());
        }

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
}
