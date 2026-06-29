package com.jenikmax.game.library.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value(value = "${spring.datasource.url}")
    private String url;

    @Value(value = "${spring.datasource.username}")
    private String user;

    @Value(value = "${spring.datasource.password}")
    private String pass;

    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(pass);
        dataSource.setMaxActive(4);
        dataSource.setMaxIdle(2);
        dataSource.setMinIdle(1);
        dataSource.setMaxWait(10000);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }

}
