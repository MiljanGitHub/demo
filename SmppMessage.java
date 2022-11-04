package com.example.demo;



import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.ReplaceIfPresentFlag;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
//import sunrise.messenger.ms.domain.SmsAdapterException;


/** Message wrapper used only to send SMPP SMS. It contains some logic for character encoding, address and long text management, */
public class SmppMessage implements SmscConnectionConfig {

    // -------------------------------------------------------------------------
    // PUBLIC CONSTANTS
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // PROTECTED AND PRIVATE VARIABLES AND CONSTANTS
    // -------------------------------------------------------------------------

    private final TimeFormatter timeFormatter = new AbsoluteTimeFormatter(); // JSMPP time formatter

    private String encoding;
    private int maxLenght = SmscConnectionConfig.DEFAULT_MAX_MSG_LENGHT;

    private String serviceType = null;

    private Address sourceAddress;
    private Address[] destinationAddresses;

    private String body; // represents the actual body message
    private String messageText; // used to handle the message text (set the body or the payload)

    private ESMClass esmClass;
    private DataCoding dataCoding;
    private boolean deliveryReceipt;

    private byte protocolId;
    private byte priority;
    private Date scheduleDeliveryTime;
    private Date validityPeriod;
    private RegisteredDelivery registeredDelivery = new RegisteredDelivery();
    private ReplaceIfPresentFlag replaceIfPresentFlag;
    private byte smDefaultMsgId;

    private Map<Short, OptionalParameter> optionalParameters = new HashMap<Short, OptionalParameter>();

    // -------------------------------------------------------------------------
    // CONSTRUCTORS
    // -------------------------------------------------------------------------

    /*
     * public SmppMessage(String encoding) throws UnsupportedCharsetException {
     * this.encoding = encoding; Charset.forName(encoding); // test charset }
     */
    // -------------------------------------------------------------------------
    // PUBLIC METHODS
    // -------------------------------------------------------------------------

    public static SmppMessage getInstance() {
        SmppMessage msg = new SmppMessage();
        msg.encoding = SmscConnectionConfig.encodingCharset;
        msg.serviceType = SmscConnectionConfig.serviceType;
        msg.esmClass = SmscConnectionConfig.esmClass;
        msg.protocolId = SmscConnectionConfig.protocolId;
        msg.priority = SmscConnectionConfig.priority;

        msg.replaceIfPresentFlag = SmscConnectionConfig.replaceIfPresentFlag;

        msg.setDataCoding(new GeneralDataCoding(SmscConnectionConfig.encodingAlphabet));
        msg.setSmDefaultMsgId(SmscConnectionConfig.defaultMsgId);

        return msg;
    }

    public void setText(String text) throws Exception {
        messageText = text != null ? text : "";
        if (messageText.length() > maxLenght) {
            setPayload(messageText, encoding); // put text in the payload
            body = ""; // clear the message body
        } else {
            body = messageText; // put the text in the message body
        }
    }

    public String getText() {
        return messageText;
    }

    public byte[] getMsgBytes() throws UnsupportedCharsetException {
        try {
            return body.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedCharsetException(encoding);
        }
    }

    @Override
    public String toString() {
        String message =
                "serviceType >> "
                        + serviceType
                        + "\n"
                        + "source >> "
                        + sourceAddress.getAddress()
                        + "\n"
                        + "sourceAddTON >> "
                        + sourceAddress.getTypeOfNumber()
                        + "\n"
                        + "sourceAddrNPI >> "
                        + sourceAddress.getNpi()
                        + "\n"
                        + "destinations >> "
                        + Arrays.toString(getDestinations())
                        + "\n"
                        + "message >> "
                        + messageText
                        + (messageText.length() > maxLenght ? " (in the payload)" : "")
                        + "\n"
                        + "esmClass >> "
                        + esmClass.value()
                        + "\n"
                        + "protocolId >> "
                        + protocolId
                        + "\n"
                        + "scheduleDeliveryTime >> "
                        + scheduleDeliveryTime
                        + "\n"
                        + "validityPeriod >> "
                        + validityPeriod
                        + "\n"
                        + "registeredDelivery >> "
                        + (registeredDelivery != null ? registeredDelivery.value() : "null")
                        + "\n"
                        + "replaceIfPresentFlag >> "
                        + replaceIfPresentFlag
                        + "\n"
                        + (dataCoding instanceof GeneralDataCoding
                        ? "dataCoding.compressed >> "
                        + ((GeneralDataCoding) dataCoding).isCompressed()
                        + "\n"
                        + "dataCoding.messageClass >> "
                        + ((GeneralDataCoding) dataCoding).getMessageClass()
                        + "\n"
                        + "dataCoding.alphabet >> "
                        + ((GeneralDataCoding) dataCoding).getAlphabet()
                        + "\n"
                        : "")
                        + "smDefaultMsgId >> "
                        + smDefaultMsgId;

        return message;
    }

    // -------------------------------------------------------------------------
    // PROTECTED METHODS
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // PRIVATE METHODS
    // -------------------------------------------------------------------------

    /**
     * For long messages (msg.length > maxLenght) the text is set as a payload in the optional parameters instead of the message text field. Here we create the optional parameter of type "payload".
     */
    private void setPayload(String msg, String encoding) throws UnsupportedCharsetException {
        try {
            putOptionalParameter(new OctetString(OptionalParameter.Tag.MESSAGE_PAYLOAD.code(), msg, encoding));
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedCharsetException(encoding);
        }
    }

    /**
     * Allow
     *
     * @param optionalParameter
     */
    private void putOptionalParameter(OptionalParameter optionalParameter) {
        optionalParameters.put(optionalParameter.tag, optionalParameter);
    }

    // -------------------------------------------------------------------------
    // PUBLIC ACCESSORS (GETTERS / SETTERS)
    // -------------------------------------------------------------------------

    public String getSource() {
        return sourceAddress.getAddress();
    }

    public TypeOfNumber getSourceAddTON() {
        return sourceAddress.getTypeOfNumber();
    }

    public NumberingPlanIndicator getSourceAddrNPI() {
        return sourceAddress.getNumberingPlanIndicator();
    }

    public String[] getDestinations() {
        String[] destinations = new String[destinationAddresses.length];
        for (int i = 0; i < destinationAddresses.length; i++) {
            destinations[i] = destinationAddresses[i].getAddress();
        }
        return destinations;
    }

    public Address[] getDestinationAddresses() {
        return destinationAddresses;
    }

    public String getServiceType() {
        return serviceType;
    }

    public ESMClass getEsmClass() {
        return esmClass;
    }

    public DataCoding getDataCoding() {
        return dataCoding;
    }

    public boolean isDeliveryReceipt() {
        return deliveryReceipt;
    }

    public byte getProtocolId() {
        return protocolId;
    }

    public byte getPriority() {
        return priority;
    }

    public String getScheduleDeliveryTimeString() {
        return scheduleDeliveryTime != null ? timeFormatter.format(scheduleDeliveryTime) : null;
    }

    public String getValidityPeriodString() {
        return validityPeriod != null ? timeFormatter.format(validityPeriod) : null;
    }

    public RegisteredDelivery getRegisteredDelivery() {
        return registeredDelivery;
    }

    public ReplaceIfPresentFlag getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }

    public byte getSmDefaultMsgId() {
        return smDefaultMsgId;
    }

    /**
     * Optional parameters are currently used only to set the payload for long messages. If you need to use them for something else consider the order of the parameters in the array.
     *
     * @return the optionalParameters array.
     */
    public OptionalParameter[] getOptionalParameters() {
        return optionalParameters.values().toArray(new OptionalParameter[0]);
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setSourceAddress(Address sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public void setDestinationAddresses(Address[] destinationAddresses) {
        this.destinationAddresses = destinationAddresses;
    }

    public void setEsmClass(ESMClass esmClass) {
        this.esmClass = esmClass;
    }

    public void setDataCoding(DataCoding dataCoding) {
        this.dataCoding = dataCoding;
    }

    public void setDeliveryReceipt(boolean deliveryReceipt) {
        this.deliveryReceipt = deliveryReceipt;
    }

    public void setProtocolId(byte protocolId) {
        this.protocolId = protocolId;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public void setScheduleDeliveryTime(Date scheduleDeliveryTime) {
        this.scheduleDeliveryTime = scheduleDeliveryTime;
    }

    public void setValidityPeriod(Date validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public void setRegisteredDelivery(RegisteredDelivery registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public void setReplaceIfPresentFlag(ReplaceIfPresentFlag replaceIfPresentFlag) {
        this.replaceIfPresentFlag = replaceIfPresentFlag;
    }

    public void setSmDefaultMsgId(byte smDefaultMsgId) {
        this.smDefaultMsgId = smDefaultMsgId;
    }
}
