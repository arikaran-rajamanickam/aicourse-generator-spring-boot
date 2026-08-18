package com.auth.service;

import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class UserDetailService implements UserDetailsService {

    private static final Logger LOGGER = Logger.getLogger(UserDetailService.class.getName());

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.log(Level.FINE, "Loading user by username: {0}", new Object[]{username});
        Users user = userRepo.findByUsername(username);
        if (user == null) {
            LOGGER.log(Level.WARNING, "User not found with username: {0}", new Object[]{username});
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        LOGGER.log(Level.FINE, "User loaded successfully: {0}", new Object[]{username});
        return new UserPrincipal(user);
    }
}

