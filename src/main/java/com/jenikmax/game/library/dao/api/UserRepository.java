package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User,Long> {

    User findByUsername(String username);

}
