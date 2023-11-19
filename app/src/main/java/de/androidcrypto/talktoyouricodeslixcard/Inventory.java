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
    private boolean isIcodeFamily = false;
    private boolean isManufacturerNxp;
    private String manufacturerName = "unknown";

    private boolean isValid = false;
    public Inventory(byte[] response) {
        this.response = response;
        if ((response == null) || (response.length !=0)) {
            isValid = false;
            return;
        }
        data = response[0];
        uid = Arrays.copyOfRange(response, 1, 8);
        // analyze data
        analyzeData();
    }

    private void analyzeData() {
        uid7 = uid[0];
        if (uid7 == (byte) 0xe0) isIcodeFamily = true;
        uid6IcManufacturerCode = uid[1];
        // todo lookup table
        if (uid6IcManufacturerCode == (byte) 0x04) {
            isManufacturerNxp = true;
            manufacturerName = "NXP";
        }
        uid5TagType = uid[2];
        // todo tag type lookup table
    }

}
