package com.jenikmax.game.library.service.data.api;

import com.jenikmax.game.library.model.dto.RegistrationForm;
import com.jenikmax.game.library.model.dto.UserDto;

import java.util.List;

public interface UserService {

    void registerUser(RegistrationForm user);

    UserDto getUserInfoByName(String userName);

    UserDto updateUser(UserDto user);

    UserDto updateUserPass(UserDto user);

    String resetUserPass(Long userId);

    UserDto changeUserActivity(Long userId, boolean isActive, String currentUsername);

    UserDto changeUserPrivilegy(Long userId, boolean isAdmin);

    void updateUserProfile(Long id, Boolean isActive, Boolean isAdmin);

    List<UserDto> getAllUsers();
}
