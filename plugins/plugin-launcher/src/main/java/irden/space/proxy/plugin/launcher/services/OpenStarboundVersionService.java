package irden.space.proxy.plugin.launcher.services;

import irden.space.proxy.plugin.launcher.exceptions.OSBConfigurationFileNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class OpenStarboundVersionService {
    private static final Path DEFAULT_PATH = Paths.get("./config/plugins/launcher/open-starbound-version.json");


    public String openStarboundVersion() {
        File file = DEFAULT_PATH.toFile();
        if (file.exists()) {
            try {
                return Files.readString(file.toPath());
            } catch (IOException e) {
                throw new OSBConfigurationFileNotFoundException(e.getMessage());
            }
        }
        throw new OSBConfigurationFileNotFoundException("OSB Configuration File Not Found");
    }


}
