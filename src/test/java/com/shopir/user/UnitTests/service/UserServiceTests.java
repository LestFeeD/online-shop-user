package com.shopir.user.UnitTests.service;

import com.shopir.user.dto.request.CreateOrEditUserRequestDto;
import com.shopir.user.dto.request.LoginRequestDto;
import com.shopir.user.entity.*;
import com.shopir.user.factories.UserFactory;
import com.shopir.user.repository.AddressRepository;
import com.shopir.user.repository.CityRepository;
import com.shopir.user.repository.RoleUserRepository;
import com.shopir.user.repository.WebUserRepository;
import com.shopir.user.service.UserService;
import com.shopir.user.utils.JwtUtils;
import com.shopir.user.utils.PasswordChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @InjectMocks
    private UserService userService;
    @Mock
    private  WebUserRepository webUserRepository;
    @Mock
    private  RoleUserRepository roleUserRepository;
    @Mock
    private  AddressRepository addressRepository;
    @Mock
    private  CityRepository cityRepository;
    @Mock
    private  UserFactory userFactory;
    @Mock
    private  PasswordChecker passwordEncoder;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtUtils jwtUtils;
    @Test
    void createUser_validDataForCreateNewUser() throws Exception {
        RoleUser roleUser = new RoleUser();
        roleUser.setNameRole("CLIENT");
        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .name("test")
                .surname("test")
                .phone("testPhone")
                .email("test@gmail.com")
                .password("41414222")
                .build();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(roleUserRepository.findByNameRole("ROLE_CLIENT")).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("41414222")).thenReturn("encodedPassword");
        when(webUserRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());

        WebUser savedUser = WebUser.builder()
                .idWebUser(1L)
                .name("test")
                .surname("test")
                .phone("+71234567890")
                .email("test@gmail.com")
                .password("encodedPassword")
                .roleUser(roleUser)
                .build();

        when(webUserRepository.save(any(WebUser.class))).thenAnswer(invocation -> {
            WebUser user = invocation.getArgument(0);
            user.setIdWebUser(1L);
            return user;
        });
        when(webUserRepository.findById(1L)).thenReturn(Optional.of(savedUser));

         userService.createUser(requestDto, bindingResult);

        verify(roleUserRepository).findByNameRole("ROLE_CLIENT");
    }

    @Test
    void loginUser_transitionToAccountWithValidData_returnUserEntity()  {
        LoginRequestDto requestDto = LoginRequestDto.builder()
                .email("test@gmail.com")
                .password("testPassw")
                .build();
        WebUser webUser = new WebUser();
        webUser.setPassword("testPassw");
        webUser.setEmail("test@gmail.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateToken(authentication)).thenReturn("mock-jwt-token");

        String token  = userService.loginUser(requestDto);

        assertEquals("mock-jwt-token", token);
        verify(authManager).authenticate(any());
        verify(jwtUtils).generateToken(authentication);
    }

    @Test
    void createAddress_validDataForCreateAddress() {
        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .idCity(1L)
                .street("testStreet")
                .home("testHome")
                .floor(5)
                .build();

        WebUser webUser = Mockito.mock();
        City city = new City();
        city.setIdCity(1L);

        when(cityRepository.findById(1L)).thenReturn(Optional.ofNullable(city));
        when(webUserRepository.findById(1L)).thenReturn(Optional.ofNullable(webUser));

        when(addressRepository.save(any(Address.class))).thenReturn(any(Address.class));

        userService.createAddress(1L, requestDto);

        verify(addressRepository, times(1)).save(any(Address.class));

    }
    @Test
    void editUser_validDataForEditUser() {
        City city = new City();
        city.setIdCity(2L);
        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .name("testEditName")
                .surname("testEditSurname")
                .email("testEditEmail@gmail.com")
                .build();

        WebUser webUser = WebUser.builder()
                .name("testEditName")
                .surname("testEditSurname")
                .email("testEmail@gmail.com")
                .build();

        WebUser savedUser = WebUser.builder()
                .name("testEditName")
                .surname("testEditSurname")
                .email("testEditEmail@gmail.com")
                .build();

        when(webUserRepository.findById(1L)).thenReturn(Optional.ofNullable(webUser));

        when(bindingResult.hasErrors()).thenReturn(false);
        when(webUserRepository.save(webUser)).thenReturn(savedUser);

        userService.editUser(requestDto, 1L, bindingResult);

        verify(webUserRepository, times(1)).save(webUser);
    }


    @Test
    void editAddress_validDataForEditAddress() {
        City city = new City();
        city.setIdCity(2L);
        CreateOrEditUserRequestDto requestDto = CreateOrEditUserRequestDto.builder()
                .idCity(1L)
                .street("testEditStreet")
                .home("testEditHome")
                .floor(6)
                .build();

        Address address = Address.builder()
                .city(city)
                .street("testStreet")
                .home("testHome")
                .floor(5)
                .build();

        when(addressRepository.findByWebUser_IdWebUser(1L)).thenReturn(address);

        when(cityRepository.findById(1L)).thenReturn(Optional.ofNullable(city));
        when(addressRepository.save(address)).thenReturn(any(Address.class));

        userService.editAddress(requestDto, 1L);

        verify(addressRepository, times(1)).save(address);
    }
    }

