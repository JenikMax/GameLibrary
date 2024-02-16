package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;

@Service
public class UserDataService implements UserService {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(User user) {
        // Проверяйте уникальность имени пользователя и другие правила валидации
        user.setCreateTs(new Timestamp(new Date().getTime()));
        user.setAvatar(getDefaultAvatar());
        // Хэшируете пароль перед сохранением в базе данных
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Сохраняете пользователя в базе данных
        userRepository.save(user);
    }

    @Override
    public ShortUser getUserInfoByName(String userName) {
        User user = userRepository.findByUsername(userName);
        ShortUser shortUser = new ShortUser();
        if (user == null) return shortUser;

        shortUser.setName(user.getUsername());
        shortUser.setAdmin(user.isAdmin());
        shortUser.setAvatar(user.getAvatar() != null ? BASE_64_PREFIX + Base64.getEncoder().encodeToString(user.getAvatar()) : null);
        return shortUser;
    }

    private byte[] getDefaultAvatar(){
        ClassPathResource resource = new ClassPathResource("static/img/user.png");
        try {
            return Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            return null;
        }
    }

}
