package com.example.demo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SchedulerConfiguration {

    @Value("${job.MessageProcessingJob.cronExpression}")
    private String cronExpression;

    @Value("${messenger.sendsms.deliveryReceipt}")
    private boolean deliveryReceipt;

    @Value("${messenger.sendsms.batchSize}")
    private int batchSize;

    @Value("${messenger.sendsms.poolSize}")
    private int poolSize;

    @Value("${messenger.sendsms.awaitTime}")
    private int awaitTime;
}
