package com.example.app.web;

import com.example.app.Greeter;
import java.util.Optional;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Routes historiques, utilisées par les vérifications de déploiement de la CI. */
@RestController
class GreetingController {

    private final Greeter greeter = new Greeter();
    private final String version;

    GreetingController(Optional<BuildProperties> buildProperties) {
        this.version = buildProperties.map(BuildProperties::getVersion).orElse("dev");
    }

    @GetMapping(path = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    String hello(@RequestParam(required = false) String name) {
        return greeter.greet(name);
    }

    @GetMapping(path = "/version", produces = MediaType.TEXT_PLAIN_VALUE)
    String version() {
        return version;
    }
}
