package md.drivestudio.drivestudio;

import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.model.UserRole;
import md.drivestudio.drivestudio.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // ⬅️ Adaugă acest import
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling // ⬅️ Fără asta nu merge @Scheduled!
public class DrivestudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrivestudioApplication.class, args);
    }

    @Bean
    public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.findByUsername("slavon").isEmpty()) {
                User slavon = new User();
                slavon.setUsername("slavon");
                slavon.setPassword(passwordEncoder.encode("1234"));
                slavon.setFirstName("Slavon");
                slavon.setLastName("Admin");
                slavon.setEmail("slavon@drivestudio.md");
                slavon.setRole(UserRole.ADMIN);
                slavon.setMaxUploadSize(Long.MAX_VALUE);
                userRepository.save(slavon);
            }

            if (userRepository.findByUsername("vlad").isEmpty()) {
                User vlad = new User();
                vlad.setUsername("vlad");
                vlad.setPassword(passwordEncoder.encode("1234"));
                vlad.setFirstName("Vlad");
                vlad.setLastName("Admin");
                vlad.setEmail("vlad@drivestudio.md");
                vlad.setRole(UserRole.ADMIN);
                vlad.setMaxUploadSize(Long.MAX_VALUE);
                userRepository.save(vlad);
            }
        };
    }
}
