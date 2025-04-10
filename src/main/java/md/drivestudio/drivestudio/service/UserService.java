package md.drivestudio.drivestudio.service;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.UserRequest;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UserRepository;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponse register(UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setRole("USER");
        userRepository.save(user);

        return new UserResponse(user.getId(), user.getUsername());
    }

    public UserResponse login(UserRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        return new UserResponse(user.getId(), token);
    }
}
