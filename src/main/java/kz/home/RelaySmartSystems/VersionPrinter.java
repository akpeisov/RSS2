package kz.home.RelaySmartSystems;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class VersionPrinter implements CommandLineRunner {

    @Value("${app.version}")
    private String appVersion;

    @Override
    public void run(String... args) {
        System.out.println("🚀 Application version: " + appVersion);
    }
}
