package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SessionStateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@Slf4j
public class SmppClient {

    @Value("${messenger.smsc.charset}")
    public String charset = "ISO-10646-UCS-2";

    @Autowired
    private MessageReceiverListener smsListener;
    @Autowired private SessionStateListener sessionStateListener;

    public long idleReceiveTimeout = 60000;
    public long checkBindingTimeout = 10000;
    public long sendResponseTimeout = 5000;
    public int enquireLinkTimeout = 15000;

    @Resource(name = "mainHostCfg")
    protected SmscHostConfig mainHostCfg;

    @Resource(name = "alternativeHostCfg")
    protected SmscHostConfig alternativeHostCfg;

    protected SMPPSession session; // the SMPP session
    protected SMPPSession session1; // the SMPP session
    protected SmscHostConfig activeHostCfg;
    protected SmscHostConfig inactiveHostCfg;

    protected SmscHostConfig activeHostCfg2;
    protected SmscHostConfig inactiveHostCfg2;

    /**
     * Tries to bind to the main host and then to the alternative host (if any exists).
     *
     * @throws Exception
     */
    protected synchronized void bindAndConnectDefaultFailover() throws Exception {
        bindAndConnectClient(mainHostCfg, alternativeHostCfg, FailoverType.MAIN_ALTERNATIVE);
    }

    /** Unbinds the client. */
    protected synchronized void unbindAndCloseClient() {
        log.info("Stopping " + getClientName() + "...");
        activeHostCfg = null;
        inactiveHostCfg = null;

        activeHostCfg2 = null;
        inactiveHostCfg2 = null;

        if (session != null) {
            session.unbindAndClose();
        }
        if (session1 != null) {
            session1.unbindAndClose();
        }
    }

    private void bindAndConnectClient(SmscHostConfig host1, SmscHostConfig host2, FailoverType failoverType) throws Exception {

        logBindingMessage(host1, false, failoverType);
        try {
            // Tries to connect to main host
            session = createSession(host1);
            activeHostCfg = host1;
            inactiveHostCfg = host2;
        }
        // Main host connection failed.
        catch (Exception eMain) {
            connectAlternateHost(host1, host2, failoverType, 1, eMain);
        }

        try {
            // Tries to connect to main host
            session1 = createSession(host2);
            activeHostCfg2 = host2;
            inactiveHostCfg2 = host1;
        }
        // Main host connection failed.
        catch (Exception eMain) {
            connectAlternateHost(host2, host1, failoverType, 2, eMain);
        }

        log.info(getClientName() + " successfully bound to host ");
    }

    private void connectAlternateHost(SmscHostConfig host1, SmscHostConfig host2, FailoverType failoverType, int connectSessionNumber, Exception eMain) throws Exception {
        // If an alternative host is specified, it tries to connect to it
        if (host2 != null ? host2.isDefined() : false) {

            logBindingMessage(host2, true, failoverType);
            try {
                // Tries to connect to main host
                if (connectSessionNumber == 1) {
                    session1 = createSession(host2);
                    activeHostCfg = host2;
                    inactiveHostCfg = host1;
                } else {
                    session = createSession(host2);
                    activeHostCfg2 = host2;
                    inactiveHostCfg2 = host1;
                }
            }
            // Alternative host connection failed as well.
            catch (Exception eAlternative) {
                throw new Exception();
            }
        } else {
            throw new Exception();
        }
    }

    private SMPPSession createSession(SmscHostConfig hostCfg) throws IOException {
        SMPPSession session =
                SmppSessionFactory.createSession(
                        hostCfg.getIp(),
                        hostCfg.getPort(),
                        hostCfg.getSystemId(),
                        hostCfg.getPassword(),
                        sendResponseTimeout,
                        enquireLinkTimeout,
                        SmscConnectionConfig.bindType,
                        SmscConnectionConfig.systemType,
                        TypeOfNumber.UNKNOWN,
                        NumberingPlanIndicator.UNKNOWN,
                        SmscConnectionConfig.addressRange);
        session.setMessageReceiverListener(smsListener);
        session.addSessionStateListener(sessionStateListener);
      //  SMPPSession session = new SMPPSession();
        //String host = hostCfg.
       // SmppSessionFactory.createSession();
        //session.
        return session;
    }

    private String getClientName() {
        return this.getClass().getSimpleName();
    }

    private enum FailoverType {
        ACTIVE_INACTIVE,
        MAIN_ALTERNATIVE;
    }

    protected synchronized void bindAndConnectInactiveFailover() throws Exception {
        if (inactiveHostCfg != null) {
            bindAndConnectClient(inactiveHostCfg, activeHostCfg, FailoverType.ACTIVE_INACTIVE);
        } else {
            bindAndConnectDefaultFailover(); // tries the default binding method
        }
    }

    private void logBindingMessage(SmscHostConfig host, boolean otherHostFailed, FailoverType failoverType) {
        log.info("Starting " + getClientName() + "...");
        if (otherHostFailed) {
            log.warn("Failed to connect to host!");
        }
        String mode = "sender";
        if (SmscConnectionConfig.isReceiver()) mode = "receiver";
        else if (SmscConnectionConfig.isTransceiver()) {
            mode = "transceiver";
        }

        log.info("Binding " + getClientName() + (otherHostFailed ? " alternative configuration" : "") + " in " + mode + " mode. Failover type: " + failoverType);
        log.info("System Id : " + host.getSystemId());
        log.info("Password  : " + host.getPassword());
    }

    protected synchronized boolean isBound() {
        return (session != null ? session.getSessionState().isBound() : false) && (session1 != null ? session1.getSessionState().isBound() : false);
    }

    protected SMPPSession getSession() {
        long instanceNumber = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli() % 2;
        /*if(instanceNumber == 0) {
            log.info("Get Session");
        }else {
            log.info("Get Session1");
        }*/
        return instanceNumber == 0 ? session : session1;
    }
}

