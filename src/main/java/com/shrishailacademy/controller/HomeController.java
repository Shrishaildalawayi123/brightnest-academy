package com.shrishailacademy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Forward to static homepage to avoid absolute redirect scheme issues.
        return "forward:/index.html";
    }
}
