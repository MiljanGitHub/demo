package com.example.demo;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.util.DeliveryReceiptState;

public class SmscUtils {

    public static Address[] numbersToAddresses(String[] numbers, boolean removeInternationalFmt) throws IllegalArgumentException {

        Set<Address> addressSet = new HashSet<Address>();
        for (String number : numbers) {
            addressSet.add(numberToAddress(number, removeInternationalFmt));
        }

        Address[] addresses = addressSet.toArray(new Address[addressSet.size()]);
        return addresses;
    }

    /**
     * Builds an Address object (which wrap a number, a TON and a NPI) out of a number. If specified, the international "+" symbol is removed from the number.
     *
     * @param number the number to wrap.
     * @param removeInternationalFmt remove the international "+" symbol if set to true.
     * @return the Address object.
     * @throws IllegalArgumentException if one of the numbers has an invalid format.
     */
    public static Address numberToAddress(String number, boolean removeInternationalFmt) throws IllegalArgumentException {

        TypeOfNumber ton = TypeOfNumber.UNKNOWN;
        NumberingPlanIndicator npi = NumberingPlanIndicator.ISDN; // Always ISDN
        String address = removeInternationalFmt ? removeInternationalFormat(number) : number;

        /*
         * Unknown
         *
         * Starts with '0'
         * Has length > 7 chars
         */
        if (address.startsWith("0") && address.matches("\\d{7,}")) {
            ton = TypeOfNumber.UNKNOWN;
        }

        /*
         * National (Short Number)
         *
         * Does not start with '0'
         * Has length between 3 and 7 chars
         */
        else if (!address.startsWith("0") && address.matches("\\d{3,7}")) {
            ton = TypeOfNumber.NATIONAL;
        }

        /*
         * International
         *
         * Does not start with '0'
         * Has length > 7 chars
         */
        else if (!address.startsWith("0") && address.matches("\\d{8,}")) {
            ton = TypeOfNumber.INTERNATIONAL;
        }

        /*
         * Numbers from 0 to 2 digits are invalid.
         */
        else if (address.length() == 0 || address.matches("\\d{1,2}")) {
            throw new IllegalArgumentException(number + " is not a valid number");
        }

        /*
         * Alphanumeric
         *
         * Contains alphanumeric characters.
         */
        else {
            ton = TypeOfNumber.ALPHANUMERIC;
        }

        return new Address(ton, npi, address);
    }

    public static Address numberToAddress(String number, boolean removeInternationalFmt, TypeOfNumber forceTON) throws IllegalArgumentException {

        NumberingPlanIndicator npi = NumberingPlanIndicator.ISDN; // Always ISDN
        String address = removeInternationalFmt ? removeInternationalFormat(number) : number;

        return new Address(forceTON, npi, address);
    }

    public static boolean isEmpty(String number) {
        return number != null ? number.length() == 0 : true;
    }

    public static boolean containsEmpty(String[] numbers) {
        for (String number : numbers) {
            if (isEmpty(number)) {
                return true;
            }
        }
        return false;
    }

    private static String removeInternationalFormat(String number) {
        if (number != null ? number.startsWith("+") : false) {
            return number.substring(1);
        }
        return number;
    }

    // Parses a date in the SMPP format
    private static final SimpleDateFormat SMPP_DATE_FORMATTER = new SimpleDateFormat(SmscConnectionConfig.DEFAULT_SMPP_DATE_FORMAT);

    public static MessageType getMessageType(DeliverSm deliverSm) {
        if (deliverSm == null) {
            return null;
        }
        for (MessageType type : MessageType.values()) {
            if (type.containedIn(deliverSm.getEsmClass())) {
                return type;
            }
        }
        return null;
    }

    public static OptionalParameter extractOptionalParameter(DeliverSm deliverSm, OptionalParameter.Tag tag) {
        if (deliverSm.getOptionalParameters() != null) {
            for (OptionalParameter op : deliverSm.getOptionalParameters()) {
                if (op.tag == tag.code()) {
                    return op;
                }
            }
        }
        return null;
    }

    public static DeliveryReceiptState getDeliveryReceiptStateValue(String attrName, String source) {
        String value = getDeliveryReceiptValue(attrName, source);
        return DeliveryReceiptState.valueOf(value);
    }

    public static Date getDeliveryReceiptDateValue(String attrName, String source) throws ParseException {
        String value = getDeliveryReceiptValue(attrName, source);
        return SMPP_DATE_FORMATTER.parse(value);
    }

    public static String getDeliveryReceiptValue(String attrName, String source) {
        String tmpAttr = attrName + ":";
        int startIndex = source.indexOf(tmpAttr);
        if (startIndex < 0) return null;
        startIndex = startIndex + tmpAttr.length();
        int endIndex = source.indexOf(" ", startIndex);
        if (endIndex > 0) return source.substring(startIndex, endIndex);
        return source.substring(startIndex);
    }
}

