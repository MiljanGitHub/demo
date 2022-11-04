package com.example.demo;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@Data
@Slf4j
public class MessageFilter {

    @Value("${messenger.filter.sms.useTestMsisdn}")
    private String useTestMsisdn;

    @Value("${messenger.filter.sms.testMsisdn}")
    private String testMsisdn;

    @Value("${messenger.filter.sms.whiteList}")
    private String whiteList;

    @Value("${messenger.filter.sms.blackList}")
    private String blackList;

    @Value("${messenger.filter.mms.useTestMsisdn}")
    private String mmsUseTestMsisdn;

    @Value("${messenger.filter.mms.testMsisdn}")
    private String mmsTestMsisdn;

    @Value("${messenger.filter.mms.whiteList}")
    private String mmsWhiteList;

    @Value("${messenger.filter.mms.blackList}")
    private String mmsBlackList;

    @Value("${messenger.filter.email.useTestEmail}")
    private String useTestEmail;

    @Value("${messenger.filter.email.testEmail}")
    private String testEmail;

    @Value("${messenger.filter.email.whiteList}")
    private String emailWhiteList;

    @Value("${messenger.filter.email.blackList}")
    private String emailBlackList;

    @Value("${messenger.filter.sender.email.whiteList}")
    private String senderEmailWhiteList;

    @Value("${messenger.filter.sender.sms.whiteList}")
    private String senderSmsWhiteList;

    @Value("${messenger.filter.sender.mms.whiteList}")
    private String senderMmsWhiteList;

    @Value("${messenger.sendsms.quota}")
    private int quota;

    @Value("${messenger.sendsms.quotaDuration}")
    private int quotaDuration;

    @Value("${messenger.sendmms.quota}")
    private int mmsQuota;

    @Value("${messenger.sendmms.quotaDuration}")
    private int mmsQuotaDuration;

    @Value("${messenger.systemlimit.quota}")
    private int systemQuota;

    @Value("${messenger.systemlimit.quotaDuration}")
    private int systemQuotaDuration;

    @Value("${messenger.systemlimit.mms.quota}")
    private int mmsSystemQuota;

    @Value("${messenger.systemlimit.mms.quotaDuration}")
    private int mmsSystemQuotaDuration;

    @Value("${messenger.filter.cacheLifetime}")
    private int cacheLifetime;

    @Value("${messenger.sendsms.retryCount}")
    private int retryCount;

    @Value("${messenger.sendmms.retryCount}")
    private int mmsRetryCount;

    @Value("${messenger.sendsms.validFor}")
    private long validFor;

    public String[] filterSms(String... to) {

        if (!"true".equalsIgnoreCase(getUseTestMsisdn())) {
            log.info("UseTestMsisdn is [{}] ", getUseTestMsisdn());
            return to;
        }

        Set<String> processedTo = new HashSet<String>();
        log.info("Check whitelist : " + getWhiteList());
        for (int i = 0; i < to.length; i++) {
            if (getMsisdnList(getWhiteList()).contains(to[i])) {
                log.info("MSISDN " + to[i] + " is in the whitelist");
                processedTo.add(to[i]);
            } else {
                log.info("SMS not sent to the requested msisdn [" + to[i] + "]! " + " Instead the testMsisdn [" + getTestMsisdn() + "] is used.");
                processedTo.add(getTestMsisdn());
            }
        }

        return processedTo.toArray(new String[processedTo.size()]);
    }

    public List<String> filterMms(List<String> recipients) {

        if (!"true".equalsIgnoreCase(getMmsUseTestMsisdn())) {
            return recipients;
        }

        ArrayList<String> processedTo = new ArrayList<String>();

        for (String recipient : recipients) {

            if (getMsisdnList(getMmsWhiteList()).contains(recipient)) {
                processedTo.add(recipient);
            } else {
                processedTo.add(getMmsTestMsisdn());
            }
        }
        return processedTo;
    }

    public String filterEmails(String recipients) {
        if (!"true".equalsIgnoreCase(getUseTestEmail())) {
            return recipients;
        }
        ArrayList<String> processedTo = new ArrayList<>();

        if (recipients == null || recipients.isEmpty()) {
            return "";
        }
        for (String recipient : recipients.split(",")) {

            if (getEmailList(getEmailWhiteList()).contains(recipient)) {
                log.info("Email " + recipient + " is in the whitelist");
                processedTo.add(recipient);
            } else {
                log.info("Email not sent to the requested email address [" + recipient + "]! " + " Instead the testEmail [" + getTestEmail() + "] is used.");
                processedTo.add(getTestEmail());
            }
        }
        return String.join(",", processedTo);
    }

    public boolean isBlackListMsisdn(String number, boolean isSms) {
        if (isSms && getMsisdnList(getBlackList()).contains(number)) {
            log.info("msisdn [" + number + "]! " + " is in SMS black list.");
            return true;
        } else if (!isSms && getMsisdnList(getMmsBlackList()).contains(number)) {
            log.info("msisdn [" + number + "]! " + " is in MMS black list.");
            return true;
        } else {
            return false;
        }
    }

    public boolean isBlackListEmail(String email) {
        if (getEmailList(getEmailBlackList()).contains(email)) {
            log.info("email [" + email + "]! " + " is in email black list.");
            return true;
        }
        return false;
    }

    private List<String> getMsisdnList(String numbers) {

        List<String> result = new ArrayList<>();
        String separator = ",";

        if (numbers != null && numbers.length() > 0) {
            if (separator != null && separator.length() > 0) {
                result.addAll(Arrays.asList(numbers.split(separator)));
            } else {
                result.add(numbers);
            }
        }

        return result;
    }

    private List<String> getEmailList(String str) {

        List<String> result = new ArrayList<>();
        String separator = ",";

        if (str != null && str.length() > 0) {
            result.addAll(Arrays.asList(str.split(separator)));
        }
        return result;
    }

    public boolean isWhiteListSender(String sender, boolean isSms) {
        List<String> whiteListSmsMsisdn = getMsisdnList(getSenderSmsWhiteList());
        whiteListSmsMsisdn.replaceAll(String::toUpperCase);
        List<String> whiteListMmsMsisdn = getMsisdnList(getSenderMmsWhiteList());
        whiteListMmsMsisdn.replaceAll(String::toUpperCase);
        if (isSms && whiteListSmsMsisdn.contains(sender.toUpperCase())) {
            return true;
        } else if (!isSms && whiteListMmsMsisdn.contains(sender.toUpperCase())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isWhiteListEmailSender(String sender) {
        List<String> whiteListEmailSenders = getEmailList(getSenderEmailWhiteList());
        whiteListEmailSenders.replaceAll(String::toUpperCase);
        return whiteListEmailSenders.contains(sender.toUpperCase());
    }

    public String getSenderNameFromWhiteList(String sender, boolean isSms) {
        if (isSms) {
            for (String senderName : getMsisdnList(getSenderSmsWhiteList())) {
                if (sender.equalsIgnoreCase(senderName)) return senderName;
            }
        } else {
            for (String senderName : getMsisdnList(getSenderMmsWhiteList())) {
                if (sender.equalsIgnoreCase(senderName)) return senderName;
            }
        }
        return sender;
    }
}
