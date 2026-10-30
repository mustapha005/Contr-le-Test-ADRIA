package ma.adria.config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.cloud.config.server.EnableConfigServer
public class ConfigApplication {
    public static void main(String[] args) { SpringApplication.run(ConfigApplication.class, args); }
}
