package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.UserRequest;
import md.drivestudio.drivestudio.dto.LoginResponse;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.security.JwtUtil;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request, HttpSession session) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userService.getUserByUsername(request.getUsername());
            session.setAttribute("user", user);

            String token = jwtUtil.generateToken(user);
            return ResponseEntity.ok(new LoginResponse(token, user.getRole()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("{\"error\": \"Autentificare eșuată: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.ok("{\"message\": \"Înregistrare reușită\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
