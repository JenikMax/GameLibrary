package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;

@Service
public class UserDataService implements UserService {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";
    private final static String BASE_64_PNG_PREFIX = "data:image/png;base64,";
    private final static String BASE_64_EMPTY = "data:image/jpeg;base64";
    private final static String BASE_64_PNG_EMPTY = "data:image/png;base64";
    private final static String DEFAULT_PASS = "qwerty1234";


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(User user) {
        user.setCreateTs(new Timestamp(new Date().getTime()));
        user.setAvatar(getDefaultAvatar());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserDto updateUser(UserDto user) {
        Optional<User> findResult = userRepository.findById(user.getId());
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        if(user.getAvatar().equals(BASE_64_EMPTY) || user.getAvatar().equals(BASE_64_PNG_EMPTY)) {
            currentUser.setAvatar(getDefaultAvatar());
        }
        else{
            currentUser.setAvatar(Base64.getDecoder().decode(user.getAvatar().replaceAll(BASE_64_PREFIX,"").replaceAll(BASE_64_PNG_PREFIX,"")));
        }
        userRepository.save(currentUser);
        return getUserInfoByName(currentUser.getUsername());
    }

    @Transactional
    public UserDto updateUserPass(UserDto user) {
        if(user.getPass() == null && user.getPass().isEmpty()) throw new IllegalArgumentException("IncorrectPass");
        Optional<User> findResult = userRepository.findById(user.getId());
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        currentUser.setPassword(passwordEncoder.encode(user.getPass()));
        userRepository.save(currentUser);
        return getUserInfoByName(currentUser.getUsername());
    }

    @Transactional
    public UserDto resetUserPass(Long userId){
        Optional<User> findResult = userRepository.findById(userId);
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        currentUser.setPassword(passwordEncoder.encode(DEFAULT_PASS));
        userRepository.save(currentUser);
        return getUserInfoByName(currentUser.getUsername());
    }

    @Transactional
    public UserDto changeUserActivity(Long userId, boolean isActive){
        Optional<User> findResult = userRepository.findById(userId);
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        currentUser.setActive(isActive);
        userRepository.save(currentUser);
        return getUserInfoByName(currentUser.getUsername());
    }

    @Transactional
    public UserDto changeUserPrivilegy(Long userId, boolean isAdmin){
        Optional<User> findResult = userRepository.findById(userId);
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        currentUser.setAdmin(isAdmin);
        userRepository.save(currentUser);
        return getUserInfoByName(currentUser.getUsername());
    }

    @Transactional
    public void updateUserProfile(Long id, Boolean isActive, Boolean isAdmin){
        Optional<User> findResult = userRepository.findById(id);
        if(!findResult.isPresent()) throw new IllegalArgumentException("UserNotFound");
        User currentUser = findResult.get();
        if(isActive != null) currentUser.setActive(isActive);
        if(isAdmin != null) currentUser.setAdmin(isAdmin);
        userRepository.save(currentUser);
    }


    @Override
    public UserDto getUserInfoByName(String userName) {
        User user = userRepository.findByUsername(userName);
        UserDto userDto = new UserDto();
        if (user == null) return userDto;
        userDto.setId(user.getId());
        userDto.setName(user.getUsername());
        userDto.setAdmin(user.isAdmin());
        userDto.setAvatar(user.getAvatar() != null ? BASE_64_PREFIX + Base64.getEncoder().encodeToString(user.getAvatar()) : null);
        return userDto;
    }


    public List<UserDto> getAllUsers(){
        List<User> users = userRepository.findAll();
        List<UserDto> result = new ArrayList<>();
        for(User user : users){
            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setName(user.getUsername());
            userDto.setActive(user.isActive());
            userDto.setAdmin(user.isAdmin());
            userDto.setAvatar(user.getAvatar() != null ? BASE_64_PREFIX + Base64.getEncoder().encodeToString(user.getAvatar()) : null);
            result.add(userDto);
        }
        return result;
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
