package com.shopir.user.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordChecker implements PasswordEncoder {

    private final BCrypt.Hasher hasher = BCrypt.withDefaults();
    private final BCrypt.Verifyer verifier = BCrypt.verifyer();



    @Override
    public String encode(CharSequence rawPassword) {
        return hasher.hashToString(12, rawPassword.toString().toCharArray());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        BCrypt.Result result = verifier.verify(rawPassword.toString().toCharArray(), encodedPassword);
        return result.verified;
    }
}
