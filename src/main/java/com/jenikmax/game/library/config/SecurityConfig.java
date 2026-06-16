package com.jenikmax.game.library.config;


import com.jenikmax.game.library.config.jwt.JwtAuthenticationEntryPoint;
import com.jenikmax.game.library.config.jwt.JwtAuthenticationFilter;
import com.jenikmax.game.library.config.security.CustomAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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
        http
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .csrf().disable()
            .exceptionHandling()
                .accessDeniedHandler(new CustomAccessDeniedHandler())
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/images/**").permitAll()
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                .antMatchers("/resources/**").permitAll()
                .antMatchers("/css/**").permitAll()
                .antMatchers("/img/**").permitAll()
                .antMatchers("/js/**").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/register").permitAll()
                .antMatchers("/changeLocale").permitAll()

                .antMatchers(HttpMethod.GET, "/profile").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/profile").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/profile/pass").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/profile/update").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/profile/pass_reset").hasRole("ADMIN")

                .antMatchers(HttpMethod.GET, "/library").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.GET, "/library/game/{id}").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.GET, "/library/game/{id}/edit").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/library/game/{id}/edit").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/library/game/{id}/download").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/library/game/{id}/grab").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/filter").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/sort").hasAnyRole("ADMIN", "USER")
                .antMatchers(HttpMethod.POST, "/scan").hasRole("ADMIN")

                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/scan").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/games/{id}/edit").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/games/{id}/grab").hasRole("ADMIN")

                .antMatchers("/api/**").hasAnyRole("ADMIN", "USER")

                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/library").permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
