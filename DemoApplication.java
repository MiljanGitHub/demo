package com.example.demo;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import net.sf.ehcache.util.NamedThreadFactory;

@SpringBootApplication
@Slf4j
public class DemoApplication {
	@Autowired
	private SchedulerConfiguration schedulerConfiguration;

	@Autowired
	private SmsAdapterService smsAdapterService;

	public static void main(String[] args) {
		exposeTrustStore();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void exposeTrustStore() {
		try {
			File tmpFile = File.createTempFile("cacerts", "pem");
			InputStream inputStream = DemoApplication.class.getResourceAsStream("/jks/cacerts");
			if (inputStream == null) {
				log.error("Cacerts files is empty");
			} else {
				IOUtils.write(IOUtils.toByteArray(inputStream), new FileOutputStream(tmpFile));
				System.setProperty("javax.net.ssl.trustStore", tmpFile.getAbsolutePath());
				System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
			}
		} catch (IOException e) {
			log.error("There is an issue exposing cacerts file");
		}
	}


	//@Scheduled(cron = "0 0/1 * * * *")
	private void testJob(){
		ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(schedulerConfiguration.getPoolSize(), new NamedThreadFactory("ParallelSMS"));
		List<SMSMessage> threadSafeMessages = Collections.synchronizedList(getMessages());

		List<List<SMSMessage>> messageList = new ArrayList<List<SMSMessage>>();
		if (schedulerConfiguration.getPoolSize() < threadSafeMessages.size()) {
			messageList = Lists.partition(threadSafeMessages, (threadSafeMessages.size() / schedulerConfiguration.getPoolSize()));
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
								//log.error("Exception occurred while sendSmsList.",  e);
								System.out.println("Error");
							}
						}
					});
		}
		executorService.shutdown();

	}



	private void sendSmsList(List<SMSMessage> messagesList) throws Exception {

		for (SMSMessage smsMessage : messagesList){
			String msgId = smsAdapterService.sendSMS(smsMessage.getTo(), smsMessage.getFrom(), smsMessage.getText(), false, null);
		}

	}

	private List<SMSMessage> getMessages(){
		return List.of(new SMSMessage());
	}

}
