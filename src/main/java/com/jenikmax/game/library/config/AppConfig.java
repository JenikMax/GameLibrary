package com.jenikmax.game.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jenikmax.game.library.service.utils.StringUtils;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Основная конфигурация приложения: JDBC-шаблоны, HTTP-клиент, ObjectMapper.
 */
@Configuration
public class AppConfig {

    @Autowired
    private DataSource dataSource;

    /** JdbcTemplate для прямых SQL-запросов (статистика, трекер). */
    @Bean
    public JdbcTemplate jdbcTemplate(){
        return new JdbcTemplate(dataSource);
    }

    /** NamedParameterJdbcTemplate для запросов с именованными параметрами. */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(){
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /** Единый HTTP-клиент OkHttp для всех скраперов и AI-сервиса. */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }

    /** ObjectMapper с pretty-print для отладки JSON. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

}
