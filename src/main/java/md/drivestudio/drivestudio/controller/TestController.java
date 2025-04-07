package md.drivestudio.drivestudio.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String home() {
        return "DriveStudio rulează cu succes! 🚀";
    }
}
