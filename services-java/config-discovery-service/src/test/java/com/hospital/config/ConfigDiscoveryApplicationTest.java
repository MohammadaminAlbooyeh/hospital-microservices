package com.hospital.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.server.native.search-locations=classpath:/config-repo")
class ConfigDiscoveryApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsConfigServerAndEurekaServer() {
        assertThat(applicationContext.getBean(ConfigServerAutoConfiguration.class)).isNotNull();
        assertThat(applicationContext.getBean(EurekaServerAutoConfiguration.class)).isNotNull();
    }
}
