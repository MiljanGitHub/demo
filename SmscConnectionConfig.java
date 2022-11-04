package com.example.demo;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.ReplaceIfPresentFlag;
import org.jsmpp.bean.TypeOfNumber;

public interface SmscConnectionConfig {

    // -------------------------------------------------------------------------
    // PUBLIC CONSTANTS
    // -------------------------------------------------------------------------

    public static final int DEFAULT_MAX_MSG_LENGHT = 65;
    public static final String DEFAULT_GSM_ENCODING = "ISO-10646-UCS-2";
    public static final String DEFAULT_SMPP_DATE_FORMAT = "yyMMddHHmm";

    // -------------------------------------------------------------------------
    // PROTECTED AND PRIVATE VARIABLES AND CONSTANTS
    // -------------------------------------------------------------------------

    public String encodingCharset = DEFAULT_GSM_ENCODING;
    public Alphabet encodingAlphabet = Alphabet.ALPHA_UCS2;
    public MessageClass encodingMsgClass = MessageClass.CLASS1;
    public boolean encodingCompress = false;
    public boolean useSubmitMulti = true;

    public boolean isUseSubmitMulti = true;

    // override if you do not want to remove the "+" from the phone numbers
    public boolean removeInternationalFormat = true;

    public byte protocolId = (byte) 0;
    public byte priority = (byte) 1;
    public ESMClass esmClass = new ESMClass();
    public String serviceType = null;
    public ReplaceIfPresentFlag replaceIfPresentFlag = ReplaceIfPresentFlag.DEFAULT;
    public byte defaultMsgId = (byte) 0;

    public TypeOfNumber forceSenderTON = null;

    /*  from SmscHostConfig  */
    public BindType bindType = BindType.BIND_TRX;
    public String systemType = null;
    public String addressRange = null;

/*    static boolean isSender() {
        return BindType.BIND_TX == bindType;
    }*/

    static boolean isReceiver() {
        return BindType.BIND_RX == bindType;
    }

    static boolean isTransceiver() {
        return BindType.BIND_TRX == bindType;
    }
}