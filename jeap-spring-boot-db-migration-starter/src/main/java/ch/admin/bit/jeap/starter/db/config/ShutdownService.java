package ch.admin.bit.jeap.starter.db.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class ShutdownService {

    public void shutdown(ApplicationContext ctx, int status) {
        SpringApplication.exit(ctx, () -> status);
        System.exit(status);
    }
}
