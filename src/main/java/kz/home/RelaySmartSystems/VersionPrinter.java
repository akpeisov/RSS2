package kz.home.RelaySmartSystems;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class VersionPrinter implements CommandLineRunner {

    @Getter
    @Value("${app.version}")
    private static String appVersion;

    @Override
    public void run(String... args) {
        System.out.println("🚀 Application version: " + appVersion);
    }
}
