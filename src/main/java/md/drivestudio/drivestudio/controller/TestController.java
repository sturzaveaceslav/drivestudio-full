package md.drivestudio.drivestudio.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // Folosește o rută diferită, ca să nu existe conflict cu / din PageController
    @GetMapping("/test")
    public String testHome() {
        return "Hello from test home!";
    }
}
