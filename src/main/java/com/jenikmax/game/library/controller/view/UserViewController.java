package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.dto.RegistrationForm;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.UserDataService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class UserViewController {

    private final UserRepository userRepository;
    private final UserDataService userService;


    public UserViewController(UserRepository userRepository, UserDataService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "loginView";
    }


    @GetMapping("/register")
    public String registerUserForm(@ModelAttribute("registrationForm") RegistrationForm registrationForm) {
        return "registerView";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationForm") RegistrationForm registrationForm) {
        User user = new User();
        user.setUsername(registrationForm.getUsername());
        user.setPassword(registrationForm.getPassword());
        userService.registerUser(user);

        return "redirect:/login";
    }


    @GetMapping("/profile")
    public String profile(Model model) {
        ShortUser user = userService.getUserInfoByName(getUserName());
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }



    @PostMapping("/profile")
    public String profileUpdate(Model model, UserDto userDto) {
        try {
            userService.updateUser(userDto);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            model.addAttribute("message", "error");
        }
        ShortUser user = userService.getUserInfoByName(getUserName());
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }

    @PostMapping("/profile/pass")
    public String profilePassUpdate(Model model, UserDto userDto) {
        try {
            userService.updateUserPass(userDto);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            model.addAttribute("message", "error");
        }
        ShortUser user = userService.getUserInfoByName(getUserName());
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }

    @PostMapping("/profile/update")
    public String profileAdminUpdate(Model model, @RequestParam(value = "id") Long id,
                                     @RequestParam(value = "isAdmin", required = false) boolean isAdmin,
                                     @RequestParam(value = "isActive", required = false) boolean isActive) {

        try{
            userService.updateUserProfile(id, isActive, isAdmin);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            model.addAttribute("message", "error");
        }
        ShortUser user = userService.getUserInfoByName(getUserName());
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }


    @PostMapping("/profile/pass_reset")
    public String profileAdminReset(Model model, @RequestParam(value = "id") Long id) {

        try{
            userService.resetUserPass(id);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            model.addAttribute("message", "error");
        }
        ShortUser user = userService.getUserInfoByName(getUserName());
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }

    private String getUserName(){
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
