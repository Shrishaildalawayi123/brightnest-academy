package com.shrishailacademy.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MarketingPageController {

    private static final Resource LANDING_PAGE = new ClassPathResource("static/tuition-classes-in-bangalore.html");

    @GetMapping(value = {"/tuition-classes-in-bangalore", "/tuition-classes-in-bangalore/"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<Resource> tuitionClassesInBangalore() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(LANDING_PAGE);
    }
}
