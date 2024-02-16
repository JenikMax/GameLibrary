package com.jenikmax.game.library.service.data.api;

import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.UserDataService;

public interface UserService {

    void registerUser(User user);

    ShortUser getUserInfoByName(String userName);
}
