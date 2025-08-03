package com.shopir.user.exceptions;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public record ResponseException(String message, HttpStatus httpStatus, ZonedDateTime timestamp) {
}
