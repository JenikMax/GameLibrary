package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.controller.user.RegistrationForm;
import com.jenikmax.game.library.service.data.UserDataService;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserViewController {

    @Autowired
    private UserRepository userRepository;
    @Autowired

    private UserDataService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/user")
    public String user(Model model) {
        // Получаем текущего пользователя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Получаем пользователя из базы данных
        User user = userRepository.findByUsername(username);

        model.addAttribute("user", user);

        return "user";
    }

    @GetMapping("/register")
    public String registerUserv(@ModelAttribute("registrationForm") RegistrationForm registrationForm) {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationForm") RegistrationForm registrationForm) {
        // Проверяете введенные данные, выполняете валидацию и прочие проверки

        // Создаете нового пользователя на основе данных формы регистрации
        User user = new User();
        user.setUsername(registrationForm.getUsername());
        user.setPassword(registrationForm.getPassword());
        // Установите остальные свойства пользователя на основе данных из формы

        // Регистрируете нового пользователя через сервис UserService
        userService.registerUser(user);

        return "redirect:/login";
    }


}
