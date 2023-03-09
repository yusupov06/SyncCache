package uz.md.synccache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class SyncCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncCacheApplication.class, args);
    }

}
