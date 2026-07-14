package com.sharcky.klientt.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page pública (apresentação do produto). A app autenticada vive em /app.
 */
@Controller
public class LandingController {

    @GetMapping("/")
    public String landing() {
        return "landing";
    }
}
