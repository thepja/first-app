package com.example.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Renvoie index.html pour les routes Angular, afin qu'un rechargement de page fonctionne. */
@Controller
class SpaController {

    // À compléter à chaque nouvelle route déclarée dans frontend/src/app/app.routes.ts
    @GetMapping({"/login", "/register", "/books"})
    String index() {
        return "forward:/index.html";
    }
}
