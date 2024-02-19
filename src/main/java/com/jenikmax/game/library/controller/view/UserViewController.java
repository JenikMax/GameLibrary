package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.model.dto.RegistrationForm;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.exceptions.IllegalPassException;
import com.jenikmax.game.library.model.exceptions.IllegalUsernameException;
import com.jenikmax.game.library.service.data.UserDataService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;

@Controller
public class UserViewController {

    static final Logger logger = LogManager.getLogger(UserViewController.class.getName());

    private final UserDataService userService;


    public UserViewController(UserDataService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Open /login view");
        //request.getHeader()
        Locale locale = request.getLocale();
        if(locale.toString().equals("ru") || locale.toString().equals("en")){
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            localeResolver.setLocale(request, response, locale);
        }
        return "loginView";
    }


    @GetMapping("/register")
    public String registerUserForm(Model model, @ModelAttribute("registrationForm") RegistrationForm registrationForm, @RequestParam(value = "message", required = false) String message) {
        logger.info("Open /register view");
        model.addAttribute("message", message);
        return "registerView";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationForm") RegistrationForm registrationForm, RedirectAttributes redirectAttributes) {
        logger.info("Process /register view");
        try{
            userService.registerUser(registrationForm);
            logger.info("Register new user " + registrationForm.getUsername() + " complete");
            return "redirect:/login";
        }
        catch (IllegalPassException | IllegalUsernameException ex){
            logger.warn("Register new user " + registrationForm.getUsername() + " incomplete. " + ex.getMessage());
            redirectAttributes.addAttribute("message",ex.getMessage());
            return "redirect:/register";
        }
        catch (Exception e){
            logger.error("Register new user " + registrationForm.getUsername() + " incomplete. ",e);
            redirectAttributes.addAttribute("message","System error. Contact your administrator.");
            return "redirect:/register";
        }

    }


    @GetMapping("/profile")
    public String profile(Model model) {
        logger.info("Open /profile view");
        ShortUser user = getCurentUser();
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
        logger.info("Process /profile view");
        try {
            userService.updateUser(userDto);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            model.addAttribute("message", "error");
        }
        ShortUser user = getCurentUser();
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
        logger.info("Process profile/pass view");
        try {
            userService.updateUserPass(userDto);
            model.addAttribute("message", "Updated");
        }
        catch (Exception e){
            logger.error("Error Process profile/pass view. ",e);
            model.addAttribute("message", "System error. Contact your administrator.");
        }
        ShortUser user = getCurentUser();
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
        logger.info("Process /profile/update view");
        try{
            userService.updateUserProfile(id, isActive, isAdmin);
            model.addAttribute("message", "Updated");
        }
        catch (Exception e){
            logger.error("Error Process /profile/update view. ",e);
            model.addAttribute("message", "System error. Contact your administrator.");
        }
        ShortUser user = getCurentUser();
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
        logger.info("Process /profile/pass_reset view");
        try{
            userService.resetUserPass(id);
            model.addAttribute("message", "updated");
        }
        catch (Exception e){
            logger.error("Error Process /profile/pass_reset view. ",e);
            model.addAttribute("message", "error");
        }
        ShortUser user = getCurentUser();
        model.addAttribute("user", user);
        if(user.isAdmin()){
            List<UserDto> users = userService.getAllUsers();
            model.addAttribute("users", users);
            return "profileAdminView";
        }
        return "profileView";
    }

    private ShortUser getCurentUser(){
        return userService.getUserInfoByName(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Locale getSessionLocale(){
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getLocale();
    }



}
