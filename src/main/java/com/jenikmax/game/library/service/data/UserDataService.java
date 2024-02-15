package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDataService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        // Проверяйте уникальность имени пользователя и другие правила валидации

        // Хэшируете пароль перед сохранением в базе данных
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Сохраняете пользователя в базе данных
        userRepository.save(user);
    }

}
