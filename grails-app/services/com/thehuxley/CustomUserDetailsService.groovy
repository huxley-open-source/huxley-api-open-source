package com.thehuxley

import grails.plugin.springsecurity.userdetails.GrailsUser
import grails.plugin.springsecurity.userdetails.GrailsUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException

class CustomUserDetailsService implements GrailsUserDetailsService {

    UserDetails loadUserByUsername(String username, boolean loadRoles) throws UsernameNotFoundException {
        return loadUserByUsername(username)
    }

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String realUsername = username;
        boolean adminLogin = false;
        if (username.endsWith("@admin")) {
            realUsername = username.substring(0, username.indexOf("@admin"));
            adminLogin = true;
        }

        User.withTransaction {

            User user = getUser(realUsername)

            if (!user) {
                throw new UsernameNotFoundException('User not found', username)
            }

            User admin = null;
            if (adminLogin) {
                admin = getUser("admin");
            }

            new GrailsUser(user.username, adminLogin ? admin.password : user.password, user.enabled, !user.accountExpired,
                    !user.passwordExpired, !user.accountLocked, user.roles, user.id)
        }
    }

    private User getUser(String username) {
        User.createCriteria().get {
            or {
                ilike('username', username)
                ilike('email', username)
            }
        }
    }

}
