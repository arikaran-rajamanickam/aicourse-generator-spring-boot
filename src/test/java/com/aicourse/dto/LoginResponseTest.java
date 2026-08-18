package com.aicourse.dto;

import com.aicourse.enums.UserRole;
import com.aicourse.model.Users;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    public void testLoginResponse_Getters() {
        LoginResponse testLogin = new LoginResponse(
                "testToken",
                new UserResponse(
                        1234567890L,
                        "testUser",
                        UserRole.ADMIN)
        );

        //verifying the ggetters
        assertEquals("testToken", testLogin.getToken());
        assertEquals(1234567890L, testLogin.getUser().getId());
        assertEquals("testUser", testLogin.getUser().getUsername());
        assertEquals(UserRole.ADMIN, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_NullUser() {
        LoginResponse testLogin = new LoginResponse("testToken", null);

        assertEquals("testToken", testLogin.getToken());
        assertNull(testLogin.getUser());
    }

    @Test
    public void testLoginResponse_EmptyToken() {
        LoginResponse testLogin = new LoginResponse("", new UserResponse(123L, "user", UserRole.USER));

        assertEquals("", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_NullToken() {
        LoginResponse testLogin = new LoginResponse(null, new UserResponse(123L, "user", UserRole.USER));

        assertNull(testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullFields() {
        UserResponse nullUser = new UserResponse(null, null, null);
        LoginResponse testLogin = new LoginResponse("token", nullUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertNull(testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithEmptyUsername() {
        UserResponse emptyUsernameUser = new UserResponse(123L, "", UserRole.USER);
        LoginResponse testLogin = new LoginResponse("token", emptyUsernameUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertEquals("", testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullRole() {
        UserResponse nullRoleUser = new UserResponse(123L, "user", null);
        LoginResponse testLogin = new LoginResponse("token", nullRoleUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullId() {
        UserResponse nullIdUser = new UserResponse(null, "user", UserRole.USER);
        LoginResponse testLogin = new LoginResponse("token", nullIdUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullUsername() {
        UserResponse nullUsernameUser = new UserResponse(123L, null, UserRole.USER);
        LoginResponse testLogin = new LoginResponse("token", nullUsernameUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertNull(testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullIdAndUsername() {
        UserResponse nullIdAndUsernameUser = new UserResponse(null, null, UserRole.USER);
        LoginResponse testLogin = new LoginResponse("token", nullIdAndUsernameUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertNull(testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithNullIdAndRole() {
        UserResponse nullIdAndRoleUser = new UserResponse(null, "user", null);
        LoginResponse testLogin = new LoginResponse("token", nullIdAndRoleUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }


    @Test
    public void testLoginResponse_UserWithNullUsernameAndRole() {
        UserResponse nullUsernameAndRoleUser = new UserResponse(123L, null, null);
        LoginResponse testLogin = new LoginResponse("token", nullUsernameAndRoleUser);

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertNull(testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithAllNullFields() {
        UserResponse allNullUser = new UserResponse(null, null, null);
        LoginResponse testLogin = new LoginResponse(null, allNullUser);

        assertNull(testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertNull(testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithAllEmptyFields() {
        UserResponse allEmptyUser = new UserResponse(null, "", null);
        LoginResponse testLogin = new LoginResponse("", allEmptyUser);

        assertEquals("", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertNull(testLogin.getUser().getId());
        assertEquals("", testLogin.getUser().getUsername());
        assertNull(testLogin.getUser().getRole());
    }

    @Test
    public void testLoginResponse_UserWithResponseConstructor(){
        Users users = new Users();
        users.setId(123L);
        users.setUsername("user");
        users.setRoles(UserRole.USER);
        LoginResponse testLogin = new LoginResponse("token", new UserResponse(users));

        assertEquals("token", testLogin.getToken());
        assertNotNull(testLogin.getUser());
        assertEquals(123L, testLogin.getUser().getId());
        assertEquals("user", testLogin.getUser().getUsername());
        assertEquals(UserRole.USER, testLogin.getUser().getRole());    }
}