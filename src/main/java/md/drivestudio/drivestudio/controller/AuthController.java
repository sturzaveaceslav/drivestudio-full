package md.drivestudio.drivestudio.controller;

import md.drivestudio.drivestudio.dto.RegisterRequest;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }
}
