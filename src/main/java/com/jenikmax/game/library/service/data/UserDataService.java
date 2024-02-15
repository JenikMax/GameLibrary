package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDataService implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(User user) {
        // Проверяйте уникальность имени пользователя и другие правила валидации

        // Хэшируете пароль перед сохранением в базе данных
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Сохраняете пользователя в базе данных
        userRepository.save(user);
    }


}
