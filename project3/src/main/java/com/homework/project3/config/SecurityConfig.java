package com.homework.project3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JdbcTemplate jdbcTemplate;

    public SecurityConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        try {
            logger.info("Testing database connection...");
            jdbcTemplate.execute("SELECT 1");
            logger.info("Database connection successful.");
        } catch (Exception e) {
            logger.error("Database connection failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection failed during SecurityConfig initialization", e);
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webjars/**", "/css/**", "/js/**", "/images/**", "/Uploads/**", "/favicon.ico").permitAll()
                        .requestMatchers("/login", "/perform_login", "/logout").permitAll()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/students/add", "/students/edit/**", "/students/delete/**").hasRole("ADMIN")
                        .requestMatchers("/courses/add", "/courses/edit/**", "/courses/delete/**").hasRole("ADMIN")
                        .requestMatchers("/enrollments/add", "/enrollments/edit/**", "/enrollments/delete/**").hasRole("ADMIN")
                        .requestMatchers("/students/**", "/courses/**", "/enrollments/**", "/profile/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/students", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access_denied")
                );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            logger.info("Attempting to load user: {}", username);
            String sql = "SELECT username, password, role FROM users WHERE username = ?";
            try {
                return jdbcTemplate.query(sql, new Object[]{username}, rs -> {
                    if (rs.next()) {
                        String dbUsername = rs.getString("username");
                        String dbPassword = rs.getString("password");
                        String dbRole = rs.getString("role");
                        logger.info("User found - Username: {}, Password: {}, Role: {}", dbUsername, dbPassword, dbRole);
                        if (dbPassword == null || dbPassword.trim().isEmpty()) {
                            logger.error("Password is null or empty for user: {}", dbUsername);
                            throw new RuntimeException("Password cannot be null or empty");
                        }
                        String role = dbRole.startsWith("ROLE_") ? dbRole.substring(5) : dbRole;
                        return User.withUsername(dbUsername)
                                .password(dbPassword)
                                .roles(role)
                                .build();
                    } else {
                        logger.warn("User not found: {}", username);
                        throw new UsernameNotFoundException("User not found with username: " + username);
                    }
                });
            } catch (Exception e) {
                logger.error("Error while loading user {}: {}", username, e.getMessage(), e);
                throw new RuntimeException("Failed to load user: " + username, e);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Configuring NoOpPasswordEncoder for plain text passwords.");
        return NoOpPasswordEncoder.getInstance();
    }
}