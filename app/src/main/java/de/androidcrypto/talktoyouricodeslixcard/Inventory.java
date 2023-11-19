package de.androidcrypto.talktoyouricodeslixcard;

import java.util.Arrays;

public class Inventory {

    private byte[] response;
     private byte data;
     private byte[] uid;
     // analyzed data
    private byte uid7;
    private byte uid6IcManufacturerCode;
    private byte uid5TagType;
    private byte[] uid4SerialNumber;
    private boolean isIcodeFamily;
    private boolean isManufacturerNxp;

    private boolean isValid = false;
    public Inventory(byte[] response) {
        this.response = response;
        if ((response == null) || (response.length !=0)) {
            isValid = false;
            return;
        }
        data = response[0];
        uid = Arrays.copyOfRange(response, 1, 8);
        //

    }
}
