package md.drivestudio.drivestudio.controller;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.UserRequest;
import md.drivestudio.drivestudio.dto.UserResponse;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
