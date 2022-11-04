package com.example.demo;

import java.util.Date;

public interface SmsAdapterService {
    /**
     * Send an SMS message to the given msisdn
     *
     * @param to the msisdn to send the message to
     * @param from the msisdn of the sender
     * @param message the message to send.
     * @param requestReceipt receipt if adapter is registered to listen for receipts
     * @param expireDeliveryOn this date if SMS is not processed by then
     * @throws Exception in case the message was not sent.
     */
    public String sendSMS(String to, String from, String message, boolean requestReceipt, Date expireDeliveryOn) throws Exception;
}
