package md.drivestudio.drivestudio.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.security.SessionAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService customUserDetailsService;
    private final SessionAuthFilter sessionAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                        // ✅ Pagini publice și statice
                        .requestMatchers(
                                "/", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**", "/video/**",
                                "/index.html", "/login.html", "/register.html",
                                "/upload.html", "/files.html", "/admin.html", "/user.html",
                                "/user-files.html", "/download.html",
                                "/s/**", "/download",
                                "/preview.html", "/gallery/**", "/gallery/*/download-zip",
                                "/h2-console/**",
                                "/api/auth/**", "/api/files/info/**",
                                "/api/gallery/**"
                        ).permitAll()

                        // ✅ Upload anonim
                        .requestMatchers("/upload").permitAll()

                        // ✅ Rute pentru useri logați
                        .requestMatchers("/api/user/files/**").permitAll()
                        .requestMatchers("/api/user/space").permitAll()
                        .requestMatchers("/api/user/gallery/**").permitAll()
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        // ✅ Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 🔐 Restul necesită autentificare
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                )
                .headers(headers -> headers.frameOptions().disable())

                // ✅ Doar filtrul de sesiune (nu JWT!)
                .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
