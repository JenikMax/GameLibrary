package com.jenikmax.game.library.config;


import com.jenikmax.game.library.config.security.CustomAccessDeniedHandler;
import com.jenikmax.game.library.config.security.CustomLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //http.csrf().disable();
        http
                .authorizeRequests()
                    .antMatchers("/resources/**").permitAll()
                    .antMatchers("/css/**").permitAll()
                    .antMatchers("/img/**").permitAll()
                    .antMatchers("/js/**").permitAll()
                    .antMatchers("/login").permitAll()
                    .antMatchers("/register").permitAll()
                    .antMatchers("/changeLocale").permitAll()
                    .antMatchers(HttpMethod.GET,"/profile").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/profile").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/profile/pass").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/profile/update").hasRole("ADMIN")
                    .antMatchers(HttpMethod.POST,"/profile/pass_reset").hasRole("ADMIN")
                    .antMatchers(HttpMethod.GET,"/library").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.GET,"/library/game/{id}").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.GET,"/library/game/{id}/edit").hasRole("ADMIN")
                    .antMatchers(HttpMethod.POST,"/library/game/{id}/edit").hasRole("ADMIN")
                    .antMatchers(HttpMethod.GET,"/library/game/{id}/download").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/library/game/{id}/grab").hasRole("ADMIN")
                    .antMatchers(HttpMethod.POST,"/filter").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/sort").hasAnyRole("ADMIN","USER")
                    .antMatchers(HttpMethod.POST,"/scan").hasRole("ADMIN")

                    .antMatchers("/admin/**").hasRole("ADMIN")

                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .defaultSuccessUrl("/library").permitAll()
                    //.successHandler(loginSuccessHandler())
                    .and()
                .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                    .and()
                .exceptionHandling()
                    .accessDeniedHandler(new CustomAccessDeniedHandler());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return new CustomLoginSuccessHandler();
    }

}
