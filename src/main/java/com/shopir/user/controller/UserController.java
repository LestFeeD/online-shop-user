package com.shopir.user.controller;

import com.shopir.user.dto.request.CreateOrEditUserRequestDto;
import com.shopir.user.dto.request.LoginRequestDto;
import com.shopir.user.dto.response.EditUserResponseDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.WebUser;
import com.shopir.user.service.AuthenticationService;
import com.shopir.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "User")
@RestController
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @Operation(
            summary = "Find a user by id in jwt token.",
            description = "The method is used for integration testing and show info for user."
    )
    @GetMapping("/user")
    public ResponseEntity<UserInformationResponseDto> findUserById() {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserInformationResponseDto userInformationResponseDto =  userService.findUser(idUser );
        return ResponseEntity.ok(userInformationResponseDto);
    }

    @Operation(
            summary = "Creating an address for the user.")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/address-user")
    public ResponseEntity<Void> createAddress( @RequestBody CreateOrEditUserRequestDto requestDto) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.createAddress(idUser, requestDto );
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Editing the user's address.")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/address-user")
    public ResponseEntity<Void> editAddress( @RequestBody CreateOrEditUserRequestDto requestDto) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.editAddress( requestDto, idUser);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "User registration.")
    @PostMapping("/user")
    public ResponseEntity<Void> registration(@Valid @RequestBody CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) {
        Long idWebUser = userService.createUser(requestDto, bindingResult);
        URI location = URI.create("/user/" + idWebUser);
        return ResponseEntity.created(location).build();
    }

    @Operation(
            summary = "User authorization.")
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto requestDto) throws BadRequestException {
        String jwt = userService.loginUser(requestDto);

        return ResponseEntity.ok(Map.of("token", jwt).toString());
    }

    @Operation(
            summary = "Editing basic user data.")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/user")
    public ResponseEntity<UserInformationResponseDto> editUser( @Valid @RequestBody CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) {
        Long idUser = authenticationService.getCurrentUserId();

        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserInformationResponseDto updatedUserInfo = userService.findUser(idUser);

        return ResponseEntity.ok(updatedUserInfo);
    }

    @Operation(
            summary = "Editing mail and password.")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/details-user")
    public ResponseEntity<Void> editPasswordOrEmailUser( @Valid @RequestBody CreateOrEditUserRequestDto requestDto, BindingResult bindingResult) {
        Long idUser = authenticationService.getCurrentUserId();

        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.editPasswordOrEmail(idUser, requestDto, bindingResult);
        URI location = URI.create("/login");

        return ResponseEntity.created(location).build();
    }



}
