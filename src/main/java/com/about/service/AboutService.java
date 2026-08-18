package com.about.service;

import com.about.pojo.ChangePasswordRequestPojo;
import com.about.pojo.ProfileResponsePojo;
import com.about.pojo.UpdateProfileRequestPojo;

public interface AboutService {

    ProfileResponsePojo getProfile(Long userId) throws Exception;

    ProfileResponsePojo updateProfile(Long userId, UpdateProfileRequestPojo request) throws Exception;

    void changePassword(Long userId, ChangePasswordRequestPojo request) throws Exception;

}
