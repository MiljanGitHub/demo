package com.example.demo;

import java.io.IOException;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;

public class SmppSessionFactory {

    /**
     * Creates a valid SMPP Session. Session is bound within the constructor.
     *
     * @param host is the SMSC host address.
     * @param port is the SMSC listen port.
     * @param systemId is the system id.
     * @param password is the password.
     * @param timeout is the timeout.
     * @param bindType is the bind type.
     * @param systemType is the system type.
     * @param addrTon is the address TON.
     * @param addrNpi is the address NPI.
     * @param addressRange is the address range.
     * @throws IOException if there is an IO error found.
     */
    public static SMPPSession createSession(
            String host,
            int port,
            String systemId,
            String password,
            long timeout,
            int enquireLinkTimer,
            BindType bindType,
            String systemType,
            TypeOfNumber addrTon,
            NumberingPlanIndicator addrNpi,
            String addressRange)
            throws IOException {
        SMPPSession session = null;
        try {
            BindParameter bindParameter = new BindParameter(bindType, systemId, password, systemType, addrTon, addrNpi, addressRange);

            session = new SMPPSession();
            session.setEnquireLinkTimer(enquireLinkTimer);
            session.connectAndBind(host, port, bindParameter, timeout);
            /*if (!session.getSessionState().isBound()) {
                throw new IOException("A valid session could not be created!");
            }*/
        } catch (Exception e){
            System.out.println("a");
            e.printStackTrace();
        }

        return session;
    }
}

