package com.shopir.user.service;

import java.util.concurrent.CompletableFuture;

public interface EmailSender {
    CompletableFuture<Void> send(String to, String email);

}
