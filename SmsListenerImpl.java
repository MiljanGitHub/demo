package com.example.demo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Data
public class SmsListenerImpl implements MessageReceiverListener {
    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
        log.info("Checkpoint [onAcceptDeliverSm] receipt for from {} to {}: {}", deliverSm.getSourceAddr(), deliverSm.getDestAddress());
    }

    @Override
    public void onAcceptAlertNotification(AlertNotification alertNotification) {

    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session session) throws ProcessRequestException {
        return null;
    }
}
