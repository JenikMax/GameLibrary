package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Репозиторий для работы с пользователями системы.
 */
public interface UserRepository extends JpaRepository<User,Long> {

    /**
     * Находит пользователя по имени пользователя.
     * @param username имя пользователя
     * @return пользователь или null
     */
    User findByUsername(String username);

    /**
     * Находит всех пользователей, у которых не установлен аватар.
     * @return список пользователей без аватара
     */
    List<User> findByAvatarIsNull();

    /**
     * Подсчитывает количество пользователей с указанным флагом администратора.
     * @param isAdmin флаг администратора
     * @return количество пользователей
     */
    long countByAdmin(boolean isAdmin);

}
