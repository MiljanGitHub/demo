package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmscConfig {

    @Value("${messenger.smsc.host1.ip}")
    public String ip;

    @Value("${messenger.smsc.host1.port}")
    public int port;

    @Value("${messenger.smsc.host1.systemId}")
    public String systemId;

    @Value("${messenger.smsc.host1.password}")
    public String password;

    @Value("${messenger.smsc.host2.ip}")
    public String host2_ip;

    @Value("${messenger.smsc.host2.port}")
    public int host2_port;

    @Value("${messenger.smsc.host2.systemId}")
    public String host2_systemId;

    @Value("${messenger.smsc.host2.password}")
    public String host2_password;

    //    @Bean
    //    public SmppClient smppClient() throws Exception {
    //        return new SmppClient();
    //    }

    @Bean(name = "mainHostCfg")
    public SmscHostConfig mainSmscHostConfig() throws Exception {
        return new SmscHostConfig(ip, port, systemId, password);
    }

    @Bean(name = "alternativeHostCfg")
    public SmscHostConfig alternativeSmscHostConfig() throws Exception {
        return new SmscHostConfig(host2_ip, host2_port, host2_systemId, host2_password);
    }
}

