package md.drivestudio.drivestudio.service;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.UserRequest;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.model.UserRole;
import md.drivestudio.drivestudio.repository.UserRepository;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    public UserResponse register(UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setCountry(request.getCountry());
        user.setEmail(request.getEmail());

        if ("slavon".equalsIgnoreCase(user.getUsername()) || "vlad".equalsIgnoreCase(user.getUsername())) {
            user.setRole(UserRole.ADMIN);
            user.setMaxUploadSize(Long.MAX_VALUE);
        } else {
            user.setRole(UserRole.USER);
            user.setMaxUploadSize(2L * 1024 * 1024 * 1024); // 2 GB
        }

        userRepository.save(user);

        // trimitem email real
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("Bine ai venit pe DriveStudio!");
            msg.setText("Salut, " + user.getFirstName() + "!\n\n" +
                    "Îți mulțumim că te-ai înregistrat pe DriveStudio. " +
                    "Poți începe să încarci fișiere imediat.\n\n" +
                    "Toate cele bune,\nEchipa DriveStudio\n\nPowered by Slavon ❤️");
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Eroare la trimiterea emailului: " + e.getMessage());
        }

        // generăm token după înregistrare
        String token = jwtUtil.generateToken(user);

        return new UserResponse(user.getId(), token, user.getUsername(), user.getRole().name());
    }

    public UserResponse login(UserRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        return new UserResponse(user.getId(), token, user.getUsername(), user.getRole().name());
    }

    public User getUserFromToken(String token) {
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
