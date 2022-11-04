package sunrise.messenger.ms.smsc.service;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.util.NamedThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sunrise.messenger.ms.adapter.kafka.domain.Message;
import sunrise.messenger.ms.adapter.kafka.domain.MessageStatus;
import sunrise.messenger.ms.adapter.kafka.domain.MessageType;
import sunrise.messenger.ms.adapter.kafka.service.MessageProducer;
import sunrise.messenger.ms.configuration.SchedulerConfiguration;
import sunrise.messenger.ms.domain.SMSMessage;
import sunrise.messenger.ms.helper.MessageProcessingServiceHelper;
import sunrise.messenger.ms.repository.SMSMessageRepository;
import sunrise.messenger.ms.sms.model.Status;
import sunrise.messenger.ms.util.ConfigUtil;
import sunrise.messenger.ms.util.DateUtil;
import sunrise.messenger.ms.util.MessageFilter;
import sunrise.messenger.ms.util.RateLimiterUtil;
import sunrise.nute.config.support.ConfigReloadService;
import sunrise.nute.scheduler.support.annotation.JobExecution;
import sunrise.nute.scheduler.support.util.ScheduledTasksRefresher;
import sunrise.nute.scheduler.support.util.Util;

@Service
@Data
@Slf4j
public class MessageProcessingService {

    @Autowired private SMSMessageRepository smsRepository;
    @Autowired private SchedulerConfiguration schedulerConfiguration;
    @Autowired private MessageProcessingServiceHelper processingServiceHelper;
    @Autowired private MessageFilter messageFilter;
    @Autowired private RateLimiterUtil rateLimiterUtil;
    @Autowired private ConfigUtil configUtil;
    @Autowired MessageProducer messageProducer;

    @JobExecution(name = "MessageProcessingJob")
    @Transactional(value = "schedulerTransactionManager")
    public void execute() {
        log.info("Executing MessageProcessingJob  at: " + DateUtil.getLocalTimeOfCET());
        // triggering SMS on schedule
        int countOfMessagesToProcess = processingServiceHelper.updateMessagesToPending();
        if (countOfMessagesToProcess > 0) {
            triggestSMS(new ArrayList<SMSMessage>());
            log.info("Executing MessageProcessingJob: Send SMSs triggerred");
        } else {
            log.info("Executing MessageProcessingJob: Send SMSs NOT triggerred");
        }
    }

    public List<SMSMessage> triggestSMS(List<SMSMessage> smsList) {
        
        List<SMSMessage> messages = new ArrayList<SMSMessage>();
        Pageable pageable = PageRequest.of(0, Integer.valueOf(getSchedulerConfiguration().getBatchSize()));
        try {
            if (smsList != null && !smsList.isEmpty()) {
                log.info("Progressing for sendSmsList without DB lookup, SMS list size - {}", messages.size());
                messages.addAll(smsList);
            } else {
                log.info("Progressing for sendSmsList[MessagesInSending] with SMS list size - {}", messages.size());
                messages = smsRepository.findMessagesInSending(pageable, configUtil.getActiveConfigLong());
                if (messages != null)
                    log.info("Total SMS loaded : [{}] ", messages.size());
            }
            ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(getSchedulerConfiguration().getPoolSize(), new NamedThreadFactory("ParallelSMS"));
            List<SMSMessage> threadSafeMessages = Collections.synchronizedList(messages);

            List<List<SMSMessage>> messageList = new ArrayList<List<SMSMessage>>();
            if (getSchedulerConfiguration().getPoolSize() < threadSafeMessages.size()) {
                messageList = Lists.partition(threadSafeMessages, (threadSafeMessages.size() / getSchedulerConfiguration().getPoolSize()));
            } else {
                messageList = Lists.partition(threadSafeMessages, 1);
            }

            for (List<SMSMessage> smsMessages : messageList) {
                executorService.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    sendSmsList(smsMessages);
                                } catch (Exception e) {
                                    log.error("Exception occurred while sendSmsList.",  e);
                                }
                            }
                        });
            }
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(getSchedulerConfiguration().getAwaitTime(), TimeUnit.SECONDS)) {
                    smsRepository.updateMessagesToPending();
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                smsRepository.updateMessagesToPending();
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Processed all messages");
        } catch (Exception e) {
            log.error("Exception occurred in save all " + e);
        }
        return messages;
    }

    private void sendSmsList(List<SMSMessage> messagesList) {
        final Map<String, RateLimiter> rateLimitList = rateLimiterUtil.getRateLimitListForActiveConfigs();
        RateLimiter rateLimiterGlobal = rateLimiterUtil.getRateLimitGlobal();

        for (SMSMessage smsMessage : messagesList) {
            log.info("Checkpoint for [sendSmsList] for Sender {} and Recipient {}", smsMessage.getSender(), smsMessage.getRecipient());
            try {
                if (rateLimitList.get(smsMessage.getConfigurationId().toString() + "_" + sunrise.messenger.ms.configuration.model.TimeUnit.SECOND.toString()).tryAcquire(1, TimeUnit.SECONDS)
                        && rateLimiterGlobal.tryAcquire(1, TimeUnit.SECONDS)) {
                    log.info("Checking Rate Limiter configuration for Sender {} and Recipient {}", smsMessage.getSender(), smsMessage.getRecipient());
                    processingServiceHelper.sendSms(smsMessage);
                } else if (Status.SENDING.toString().equalsIgnoreCase(smsMessage.getStatus()) && smsMessage.getId() != null) {
                        smsRepository.updateMessageAfterSend(Status.PENDING.toString(), smsMessage.getTargetSystemId(), smsMessage.getId(), smsMessage.getRetryCount());
                        log.info("SMS updated to {} for Sender {} and Recipient {}", Status.PENDING, smsMessage.getSender(), smsMessage.getRecipient());
                }
            } catch (Exception e) {
                log.error("Exception while sending SMS for Sender {} and Recipient {}", smsMessage.getSender(), smsMessage.getRecipient(), e);
                int retryCount = smsMessage.getRetryCount().intValue();
                smsMessage.setRetryCount(Long.valueOf(retryCount + 1));
                if (retryCount > messageFilter.getRetryCount()) {
                    log.info("Retry count [{}/{}] exceeded for Sender {} and Recipient {}", smsMessage.getRetryCount(), messageFilter.getRetryCount(), smsMessage.getSender(), smsMessage.getRecipient());
                    smsRepository.updateMessageAfterSend(Status.FAILED.toString(), smsMessage.getTargetSystemId(), smsMessage.getId(), smsMessage.getRetryCount());
                    messageProducer.sendMessage(
                            Message.builder()
                                    .id(smsMessage.getId().toString())
                                    .orderId(smsMessage.getOrderId())
                                    .messageType(MessageType.SMS.name())
                                    .messageStatus(MessageStatus.FAILED.name())
                                    .externalReferenceId(smsMessage.getExternalReferenceId())
                                    .targetSystemId(smsMessage.getTargetSystemId())
                                    .build());
                    log.info("Kafka Msg published on failure for Sender {} and Recipient {}", smsMessage.getSender(), smsMessage.getRecipient());
                } else {
                    log.info("Retry count [{}/{}], for Sender {} and Recipient {}", smsMessage.getRetryCount(), messageFilter.getRetryCount(), smsMessage.getSender(), smsMessage.getRecipient());
                    if (Status.SENDING.toString().equalsIgnoreCase(smsMessage.getStatus())) {
                        smsRepository.updateMessageAfterSend(Status.PENDING.toString(), smsMessage.getTargetSystemId(), smsMessage.getId(), smsMessage.getRetryCount());
                        log.info("SMS updated to {} after Send and retrying for Sender {} and Recipient {}", Status.PENDING, smsMessage.getSender(), smsMessage.getRecipient());
                    }
                }
            }
            log.info("sendSmsList process is completed for Sender {} and Recipient {} ", smsMessage.getSender(), smsMessage.getRecipient());
        }
    }

    @Configuration
    @EnableScheduling
    class SchedulingConfiguration implements ApplicationListener<EnvironmentChangeEvent>, SchedulingConfigurer {

        @Autowired
        ConfigReloadService reloadService;

        @Override
        public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
            scheduledTaskRegistrar.addTriggerTask(triggerTask());
        }

        @Bean("MessageProcessingTask")
        public TriggerTask triggerTask() {
            return new TriggerTask(
                    new Runnable() {
                        @Override
                        public void run() {
                            execute();
                            reloadService.reloadConfiguration();
                        }
                    },
                    new Trigger() {
                        @Override
                        public Date nextExecutionTime(TriggerContext triggerContext) {
                            return Util.nextDate(triggerContext, getSchedulerConfiguration().getCronExpression(), null, null);
                        }
                    });
        }

        @Override
        public void onApplicationEvent(EnvironmentChangeEvent event) {
            if (event.getKeys().contains("job.EgidImportJob.cronExpression")) {
                reloadService.reloadConfiguration();
                ScheduledTasksRefresher scheduledTasksRefresher = new ScheduledTasksRefresher(triggerTask());
                scheduledTasksRefresher.afterPropertiesSet();
            }
        }
    }
}
