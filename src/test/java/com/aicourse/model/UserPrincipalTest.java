package com.aicourse.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    public void testUserPrincipalConstructorAndGetters() {
        Users user = new Users();
        user.setId(123456789L);
        user.setUsername("testUser");

        user.setPassword("testPassword");
        user.setCreatedAt(OffsetDateTime.now());
        assertEquals(OffsetDateTime.now().toEpochSecond(), user.getCreatedAt().toEpochSecond(), 1); // Allowing 1 second difference

        UserPrincipal userPrincipal = new UserPrincipal(user);

        assertTrue(userPrincipal.isAccountNonExpired());
        assertTrue(userPrincipal.isAccountNonLocked());
        assertTrue(userPrincipal.isCredentialsNonExpired());
        assertTrue(userPrincipal.isEnabled());
        assertEquals(user.getId(), userPrincipal.getUser().getId());
        assertEquals("testUser", userPrincipal.getUsername());
        assertEquals("testPassword", userPrincipal.getPassword());
    }

}