package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface UserRepository extends JpaRepository<User,Long> {

    User findByUsername(String username);

    List<User> findByAvatarIsNull();

}
