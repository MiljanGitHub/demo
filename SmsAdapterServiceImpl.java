package com.example.demo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;

@Service
@Data
@Slf4j
public class SmsAdapterServiceImpl extends SmppClient implements SmsAdapterService, SmscConnectionConfig {

    // -------------------------------------------------------------------------
    // PUBLIC CONSTANTS
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // PROTECTED AND PRIVATE VARIABLES AND CONSTANTS
    // -------------------------------------------------------------------------

    // default sender if not specified in the send() method
    private String defaultSender;

    @Autowired
    private MessageFilter messageFilter;

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    public SmsAdapterServiceImpl() {
        super();
    }

    // -------------------------------------------------------------------------
    // PUBLIC METHODS
    // -------------------------------------------------------------------------

    /**
     * Bind the adapter. If connection fails it tries to bind to an alternative host.
     *
     * @throws Exception
     */
    @PostConstruct
    public synchronized void startAdapter() throws Exception {
        super.bindAndConnectDefaultFailover();
    }

    /** Unbind the adapter. */
    @PreDestroy
    public synchronized void stopAdapter() {
        super.unbindAndCloseClient();
    }

    @Override
    public synchronized String sendSMS(String to, String from, String message, boolean requestReceipt, Date expireDeliveryOn) throws Exception {
        return doSendSMS(from, to, message, null, expireDeliveryOn, requestReceipt);
    }

    /** Creates a 1-entry array of recipients out of one single recipients and delegates the sending. */
    private String doSendSMS(String from, String to, String message, Date sendAtDate, Date expireDeliveryOn, boolean requestReceipt) throws Exception {
        return doSendSMS(from, new String[] {to}, message, sendAtDate, expireDeliveryOn, requestReceipt);
    }

    private String doSendSMS(String from, String[] tos, String message, Date sendAtDate, Date expireDeliveryOn, boolean requestReceipt) throws Exception {

        log.info("doSendSMS for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos));
        // check destinations
        if (tos != null ? tos.length == 0 || SmscUtils.containsEmpty(tos) : true) {
            throw new Exception();
        }

        // filter  to numbers
        tos = getMessageFilter().filterSms(tos);

        log.info("Filtered list for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos));

        SmppMessage sms = SmppMessage.getInstance();
        sms.setSourceAddress(SmscUtils.numberToAddress(from, SmscConnectionConfig.removeInternationalFormat));
        sms.setDestinationAddresses(SmscUtils.numbersToAddresses(tos, SmscConnectionConfig.removeInternationalFormat));
        sms.setText(message);
        sms.setDeliveryReceipt(requestReceipt);
        sms.setScheduleDeliveryTime(sendAtDate);
        sms.setValidityPeriod(expireDeliveryOn);

        sms.setServiceType(SmscConnectionConfig.serviceType);
        sms.setEsmClass(SmscConnectionConfig.esmClass);
        sms.setProtocolId(SmscConnectionConfig.protocolId);
        sms.setPriority(SmscConnectionConfig.priority);

        if (requestReceipt) {
            sms.setRegisteredDelivery(new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE));
        }
        sms.setReplaceIfPresentFlag(SmscConnectionConfig.replaceIfPresentFlag);
        sms.setDataCoding(new GeneralDataCoding(SmscConnectionConfig.encodingAlphabet));
        sms.setSmDefaultMsgId(SmscConnectionConfig.defaultMsgId);

        if (!isBound()) {
            try {
                super.bindAndConnectDefaultFailover();
            } catch (Exception e) {
                throw new Exception();
            }
        }

        /*
         * if sending fails, try failover connection by starting from the
         * inactive host
         */
        try {
            log.info("Trying doSendSMS for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos));
            //return doSendSMS(sms);
        }
        catch (Exception smsAdapterException) {
            log.error("Failed to send SMS, trying re-bind and re-send... for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos), smsAdapterException);
            // if session is bound, unbind first
            if (isBound()) {
                super.unbindAndCloseClient();
            }

            // try to re-bind first to inactive host, then to the previously
            // active one
            try {
                super.bindAndConnectInactiveFailover();
            } catch (Exception eRebind) {
                log.error("Failed to send SMS, BindException for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos), eRebind);
                throw new Exception(); // none of the binding worked
            }
        }

        log.info("Final trying to doSendSMS, for Sender {} and Recipient {}", from, StringUtils.arrayToCommaDelimitedString(tos));
        return doSendSMS(sms); // try to re-send
    }

    /** Calls the API send method from jSMPP. */
    private String doSendSMS(SmppMessage sms) throws  Exception {
        // duplicates are removed in advance of calling this method
        boolean multi = sms.getDestinationAddresses().length > 1;
        try {

            if (multi && SmscConnectionConfig.isUseSubmitMulti) {
                SubmitMultiResult result =
                        getSession()
                                .submitMultiple(
                                        sms.getServiceType(),
                                        sms.getSourceAddTON(),
                                        sms.getSourceAddrNPI(),
                                        sms.getSource(),
                                        sms.getDestinationAddresses(),
                                        sms.getEsmClass(),
                                        sms.getProtocolId(),
                                        sms.getPriority(),
                                        sms.getScheduleDeliveryTimeString(),
                                        sms.getValidityPeriodString(),
                                        sms.getRegisteredDelivery(),
                                        sms.getReplaceIfPresentFlag(),
                                        sms.getDataCoding(),
                                        sms.getSmDefaultMsgId(),
                                        sms.getMsgBytes(),
                                        sms.getOptionalParameters());
                log.info("doSendSMS[SubmitMultiResult] SUCCESS {}.", result.getMessageId());
                return result.getMessageId();

            } else {
                String result = null;
                for (Address destinationAddress : sms.getDestinationAddresses()) {
                    TypeOfNumber destinationAddTON = destinationAddress.getTypeOfNumber();
                    NumberingPlanIndicator destinationAddNPI = destinationAddress.getNumberingPlanIndicator();
                    String destination = destinationAddress.getAddress();

                    String messageId =
                            getSession()
                                    .submitShortMessage(
                                            sms.getServiceType(),
                                            sms.getSourceAddTON(),
                                            sms.getSourceAddrNPI(),
                                            sms.getSource(),
                                            destinationAddTON,
                                            destinationAddNPI,
                                            destination,
                                            sms.getEsmClass(),
                                            sms.getProtocolId(),
                                            sms.getPriority(),
                                            sms.getScheduleDeliveryTimeString(),
                                            sms.getValidityPeriodString(),
                                            sms.getRegisteredDelivery(),
                                            sms.getReplaceIfPresentFlag().value(),
                                            sms.getDataCoding(),
                                            sms.getSmDefaultMsgId(),
                                            sms.getMsgBytes(),
                                            sms.getOptionalParameters());
                    if (result == null) {
                        result = messageId;
                    }
                    log.info("doSendSMS[submitShortMessage] SUCCESS Result {}.", result);
                }
                return result;
            }

        } catch (Exception e) {
            throw new Exception();
        }
    }
}
