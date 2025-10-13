package growdy.mumuri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MumuriApplication {

	public static void main(String[] args) {
		SpringApplication.run(MumuriApplication.class, args);
	}

}
