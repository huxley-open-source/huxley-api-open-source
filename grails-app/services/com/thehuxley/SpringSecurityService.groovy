package com.thehuxley

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.codec.Hex
import org.springframework.security.crypto.codec.Utf8
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SpringSecurityService {

    static transactional = false

    def passwordEncoder
    def userService

    def getAuthentication() {
        SecurityContextHolder.context?.authentication
    }

    def getPrincipal() {
        authentication?.getPrincipal()
    }

    boolean isLoggedIn() {
        authentication != null
    }

    def getCurrentUser() {
        if (isLoggedIn() && authentication.name != '__grails.anonymous.user__') {
            return userService.findByUsername(authentication.name)
        }

        return null
    }

    def getPasswordEncoder() {
        passwordEncoder = MessageDigest.getInstance("SHA-512");
    }

    String encodePassword(CharSequence rawPassword) {
        try {
            byte[] digest = getPasswordEncoder().digest(Utf8.encode(rawPassword));
            return new String(Hex.encode(digest));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such hashing algorithm", e);
        }

    }

    boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword)
    }


}
