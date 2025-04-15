package md.drivestudio.drivestudio.controller;

import md.drivestudio.drivestudio.dto.UserRequest;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.dto.LoginResponse;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.security.JwtUtil;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request) {
        try {
            // Autentificare
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Setare context securizat
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generare token
            User user = userService.getUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(user);

            // Returnăm răspunsul JSON
            LoginResponse response = new LoginResponse(token, user.getRole());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Eroare la login (ex: parola greșită)
            return ResponseEntity
                    .status(401)
                    .body("{\"error\": \"Autentificare eșuată: " + e.getMessage() + "\"}");
        }
    }
}
