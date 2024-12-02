package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

class KeepContextAliveRunner implements ApplicationRunner {

    @Value("${keepaliveseconds:}")
    Integer keepAliveSeconds;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (keepAliveSeconds != null) {
            Thread.sleep(keepAliveSeconds * 1000);
        }
    }

}