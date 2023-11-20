package de.androidcrypto.talktoyouricodeslixcard;

import java.util.Arrays;

public class Inventory {

    private byte[] response;
    private byte dsfId;
    private byte[] uid;
    private byte[] uidReversed;
    // analyzed data
    private byte uid7;
    private byte uid6IcManufacturerCode;
    private byte uid5TagType;
    private byte[] uid4SerialNumber;
    private byte uid4Bit37; // separated for bit 4 (37) for additional tag type
    private boolean isBit37; // used for additional tag type
    private boolean isIcodeFamily = false;
    private boolean isManufacturerNxp;
    private String manufacturerName = "unknown";
    private String tagType;

    private boolean isValid = false;

/*
https://stackoverflow.com/a/37098162/8166854 answer by Michael Roland, May 8, 2016
The Android NFC stack automatically handles polling (searching for tags various tag technologies/protocols),
anti-collision (enumeration of multiple tags within one tag technology/protocol) and activation (intitiating
communication with one specific tag) for you. You should, therefore, never send commands used for anti-collision
and activation yourself. The Inventory command is one such command (that is used to discover tags in range).

With regard to the Inventory command, there is typically no need to send this command. All the information that
you would get from this command is already provided by the Android NFC API:

You can get the UID using tag.getId().
You can get the DSFID using tech.getDsfId().
 */

    public Inventory(byte[] response) {
        this.response = response.clone();
        if ((response == null) || (response.length != 9)) {
            isValid = false;
            return;
        }
        dsfId = response[0];
        uid = Arrays.copyOfRange(response, 1, 9);
        // analyze data
        analyzeUid();
    }

    public Inventory(byte dsfId, byte[] uid) {
        this.dsfId = dsfId;
        this.uidReversed = uid.clone();
        if ((uidReversed == null) || (uidReversed.length != 8)) {
            isValid = false;
            return;
        }
        // analyze data
        analyzeUid();
    }

    private void analyzeUid() {
        // we need to reverse the array
        uid = Utils.reverseByteArray(uidReversed);
        uid7 = uid[0];
        if (uid7 == (byte) 0xe0) isIcodeFamily = true;
        uid6IcManufacturerCode = uid[1];
        // todo lookup table
        if (uid6IcManufacturerCode == (byte) 0x04) {
            isManufacturerNxp = true;
        }
        lookupIcManufacturerCode();
        uid5TagType = uid[2];
        uid4SerialNumber = Arrays.copyOfRange(uid, 4, 8);
        uid4Bit37 = uid[3];
        isBit37 = Utils.testBit(uid4Bit37, 4);
        lookupTagType();
        isValid = true;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("INVENTORY").append("\n");
        sb.append("response: ").append(Utils.bytesToHex(response)).append("\n");
        sb.append("dsfId: ").append(Utils.byteToHex(dsfId)).append("\n");
        sb.append("uid: ").append(Utils.bytesToHex(uid)).append("\n");
        sb.append("uid7: ").append(Utils.byteToHex(uid7)).append("\n");
        sb.append("uid6IcManufacturerCode: ").append(Utils.byteToHex(uid6IcManufacturerCode)).append("\n");
        sb.append("uid5TagType: ").append(Utils.byteToHex(uid5TagType)).append("\n");
        sb.append("uid4Bit37: ").append(Utils.byteToHex(uid4Bit37)).append("\n");
        sb.append("isBit37: ").append(isBit37).append("\n");
        sb.append("tagType: ").append(tagType).append("\n");
        sb.append("uid4SerialNumber: ").append(Utils.bytesToHex(uid4SerialNumber)).append("\n");
        sb.append("isIcodeFamily: ").append(isIcodeFamily).append("\n");
        sb.append("isManufacturerNxp: ").append(isManufacturerNxp).append("\n");
        sb.append("manufacturerName: ").append(manufacturerName).append("\n");
        sb.append("Inventory isValid: ").append(isValid).append("\n");
        sb.append("*** dump ended ***").append("\n");
        return sb.toString();
    }

    /**
     * section for internal
     */

    private void lookupTagType() {
        // see ICODE SLIX tag types: ICODE as NFC Type ICODE Tag AN11042.pdf, page 7
        if (uid5TagType == (byte) 0x01) {
            if (!isBit37) {
                tagType = "SL2 ICS2001 (ICODE SLI)";
            } else {
                tagType = "SL2 S2002/SL S2102 (ICODE SLIX)";
            }
        } else if (uid5TagType == (byte) 0x02) {
            if (!isBit37) {
                tagType = "SL2 ICS5301/SL2 S5402 (ICODE SLI-S)";
            } else {
                tagType = "SL2 S5302/SL2 S5402 (ICODE SLIX-S)";
            }
        } else if (uid5TagType == (byte) 0x03) {
            if (!isBit37) {
                tagType = "SL2 ICS5001/SL2 ICS5101 (ICODE SLI-I-L)";
            } else {
                tagType = "SL2 S5002/SL2 S5102 (ICODE SLIX-L";
            }
        } else {
            tagType = "unknown";
        }
    }

    private void lookupIcManufacturerCode() {
        // see https://en.wikipedia.org/wiki/ISO/IEC_15693
        manufacturerName = "unknown";
        switch (uid6IcManufacturerCode) {
            case (byte) 0x01:
                manufacturerName = "Motorola";
                break;
            case (byte) 0x02:
                manufacturerName = "ST Microelectronics";
                break;
            case (byte) 0x03:
                manufacturerName = "Hitachi";
                break;
            case (byte) 0x04:
                manufacturerName = "NXP";
                break;
            case (byte) 0x05:
                manufacturerName = "Infineon Technologies";
                break;
            case (byte) 0x06:
                manufacturerName = "Cylinc";
                break;
            case (byte) 0x07:
                manufacturerName = "Texas Instruments Tag-it\n";
                break;
            case (byte) 0x08:
                manufacturerName = "Fujitsu Limited";
                break;
            case (byte) 0x09:
                manufacturerName = "Matsushita Electric Industrial\n";
                break;
            case (byte) 0x0A:
                manufacturerName = "NEC";
                break;
            case (byte) 0x0B:
                manufacturerName = "Oki Electric";
                break;
            case (byte) 0x0C:
                manufacturerName = "Toshiba";
                break;
            case (byte) 0x0D:
                manufacturerName = "Mitsubishi Electric";
                break;
            case (byte) 0x0E:
                manufacturerName = "Samsung Electronics";
                break;
            case (byte) 0x0F:
                manufacturerName = "Hyundai Electronics";
                break;
            case (byte) 0x10:
                manufacturerName = "LG Semiconductors";
                break;
            case (byte) 0x12:
                manufacturerName = "WISeKey";
                break;
            case (byte) 0x16:
                manufacturerName = "EM Microelectronic-Marin";
                break;
            case (byte) 0x1F:
                manufacturerName = "Melexis [fr]";
                break;
            case (byte) 0x2B:
                manufacturerName = "Maxim Integrated";
                break;
            case (byte) 0x33:
                manufacturerName = "AMIC";
                break;
            case (byte) 0x39:
                manufacturerName = "Silicon Craft Technology";
                break;
            case (byte) 0x44:
                manufacturerName = "GenTag, Inc (USA)\n";
                break;
            case (byte) 0x45:
                manufacturerName = "Invengo Information Technology Co.Ltd\n";
                break;
        }
    }

    /**
     * section for getter
     */

    public byte[] getResponse() {
        return response;
    }

    public byte getDataUnknon() {
        return dsfId;
    }

    public byte[] getUid() {
        return uid;
    }

    public byte[] getUidReversed() {
        return uidReversed;
    }

    public byte getDsfId() {
        return dsfId;
    }

    public byte getUid7() {
        return uid7;
    }

    public byte getUid6IcManufacturerCode() {
        return uid6IcManufacturerCode;
    }

    public byte getUid5TagType() {
        return uid5TagType;
    }

    public byte[] getUid4SerialNumber() {
        return uid4SerialNumber;
    }

    public byte getUid4Bit37() {
        return uid4Bit37;
    }

    public boolean isBit37() {
        return isBit37;
    }

    public boolean isIcodeFamily() {
        return isIcodeFamily;
    }

    public boolean isManufacturerNxp() {
        return isManufacturerNxp;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public String getTagType() {
        return tagType;
    }

    public boolean isValid() {
        return isValid;
    }
}
