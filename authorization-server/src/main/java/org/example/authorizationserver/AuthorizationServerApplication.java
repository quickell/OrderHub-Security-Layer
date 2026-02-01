package org.example.authorizationserver;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
public class AuthorizationServerApplication {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerApplication.class);

    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return registry -> registry.addErrorPages(
                new ErrorPage("/error"),
                new ErrorPage(HttpStatus.NOT_FOUND, "/error"),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error"));
    }

    public static void main(String[] args) {
        Dotenv dotenv = loadDotenv();
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }

    private static Dotenv loadDotenv() {
        String userDir = System.getProperty("user.dir");
        Path cwd = Paths.get("").toAbsolutePath();
        String[] paths = {
                userDir,
                Paths.get(userDir, "authorization-server").toString(),
                cwd.toString(),
                cwd.resolve("authorization-server").toString()
        };
        for (String dir : paths) {
            Dotenv dotenv = Dotenv.configure().directory(dir).ignoreIfMissing().load();
            if (!dotenv.entries().isEmpty()) {
                log.info("Loaded .env from: {}", dir);
                return dotenv;
            }
        }
        log.debug("No .env found, using defaults or env vars");
        return Dotenv.configure().ignoreIfMissing().load();
    }
}
