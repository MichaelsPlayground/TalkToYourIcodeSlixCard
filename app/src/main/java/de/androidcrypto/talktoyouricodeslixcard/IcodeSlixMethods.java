package de.androidcrypto.talktoyouricodeslixcard;

import static de.androidcrypto.talktoyouricodeslixcard.Utils.printData;

import android.app.Activity;
import android.icu.lang.UScript;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcV;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;

public class IcodeSlixMethods {

    private static final String TAG = IcodeSlixMethods.class.getName();

    private final Tag tag;
    private final Activity activity;
    private final TextView textView; // used for displaying information's from the methods

    //
    private NfcV nfcV;
    private String[] techList;

    private byte[] tagUid;
    private int maxTranceiveLength;
    private byte responseFlags;
    private byte dsfId;
    private byte[] responseInventory;
    private SystemInformation systemInformation;
    private byte[] responseGetSystemInfoFrame1bytesAddress;

    // mandatory commands as defined in ISO/IEC 15693-3
    // https://developer.apple.com/documentation/corenfc/nfciso15693tag
    private static final byte GET_RANDOM_NUMBER_COMMAND = (byte) 0xB2;
    private static final byte INVENTORY_COMMAND = (byte) 0x01; // todo check
    private static final byte STAY_QUIET_COMMAND = (byte) 0x02;
    private static final byte READ_SINGLE_BLOCK_COMMAND = (byte) 0x20;
    private static final byte WRITE_SINGLE_BLOCK_COMMAND = (byte) 0x21;
    private static final byte LOCK_BLOCK_COMMAND = (byte) 0x22;
    private static final byte READ_MULTIPLE_BLOCKS_COMMAND = (byte) 0x23;
    private static final byte SELECT_COMMAND = (byte) 0x25;
    private static final byte RESET_TO_READY_COMMAND = (byte) 0x26;
    private static final byte WRITE_AFI_CCOMMAND = (byte) 0x27;
    private static final byte LOCK_AFI_COMMAND = (byte) 0x28;
    private static final byte WRITE_DSFID_COMMAND = (byte) 0x29;
    private static final byte LOCK_DSFID_COMMAND = (byte) 0x2A;
    private static final byte GET_SYSTEM_INFORMATION_COMMAND = (byte) 0x2B;
    private static final byte GET_MULTIPLE_BLOCK_SECURITY_STATUS_COMMAND = (byte) 0x2C;

    // custom commands
    private static final byte SET_PASSWORD_COMMAND = (byte) 0xB3;
    private static final byte WRITE_PASSWORD_COMMAND = (byte) 0xB4;
    private static final byte LOCK_PASSWORD_COMMAND = (byte) 0xB5;
    private static final byte INVENTORY_READ_COMMAND = (byte) 0xA0;
    private static final byte FAST_INVENTORY_READ_COMMAND = (byte) 0xA1;

    private static final byte SET_EAS_COMMAND = (byte) 0xA2;
    private static final byte RESET_EAS_COMMAND = (byte) 0xA3;
    private static final byte LOCK_EAS_COMMAND = (byte) 0xA4;
    private static final byte EAS_ALARM_COMMAND = (byte) 0xA5;
    private static final byte PASSWORD_PROTECT_EAS_AFI_COMMAND = (byte) 0xA6;
    private static final byte MANUFACTURER_CODE_NXP = (byte) 0x04;
    private static final byte PASSWORD_IDENTIFIER_EAS_AFI = (byte) 0x10;
    // Response codes
    private boolean isTagIcodeSlix = false;
    private boolean isApplicationSelected = false;
    private boolean printToLog = true; // print data to log
    private String logData;
    private byte[] errorCode = new byte[2];
    private String errorCodeReason;
    private static final byte OPERATION_OK = (byte) 0x00;
    private static final byte[] RESPONSE_OK = new byte[]{(byte) 0x00};
    private static final String RESPONSE_OK_STRING = "SUCCESS";
    private static final byte[] RESPONSE_ISO_OK = new byte[]{(byte) 0x90, (byte) 0x00};
    private static final byte[] RESPONSE_MORE_DATA_AVAILABLE = new byte[]{(byte) 0x91, (byte) 0xAF};
    private static final byte[] RESPONSE_LENGTH_ERROR = new byte[]{(byte) 0x91, (byte) 0x7E};
    private static final byte[] RESPONSE_PARAMETER_ERROR = new byte[]{(byte) 0x91, (byte) 0xFE}; // failure because of wrong parameter
    private static final byte[] RESPONSE_FAILURE = new byte[]{(byte) 0x91, (byte) 0xFF}; // general, undefined failure
    private static final String RESPONSE_FAILURE_STRING = "FAILURE";
    // constants
    private int MAXIMUM_BLOCK_NUMBER = 27; // fixed for ICODE SLIX S with 28 blocks * 4 bytes = 108 bytes user memory
    // from system information
    private int NUMBER_OF_BLOCKS;
    private int BYTES_PER_BLOCK;
    private int MEMORY_SIZE;

    public IcodeSlixMethods(Tag tag, Activity activity, TextView textView) {
        this.tag = tag;
        this.activity = activity;
        this.textView = textView;
        Log.d(TAG, "IcodeSlixMethods initializing");
        boolean success = initializeCard();
        if (success) {
            errorCode = RESPONSE_OK.clone();
            errorCodeReason = RESPONSE_OK_STRING;
        } else {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = RESPONSE_FAILURE_STRING;
        }
    }

    public byte[] getInventory() {
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
        byte[] cmd = new byte[] {
/*
bit table
bit 1 0 RFU
bit 2 1 Option flag default
bit 3 0 Nr of slots 0 = all slots
bit 4 0 AFI flag 0 = don't use afi
bit 5 0 Protocol Extension (always 0)
bit 6 0 Inventory flag (1 = see table 4) [32]
bit 7 0 Uplink data read (0 = low, 1 = high) [64]
bit 8 0 subcarrier (0 = ask, 1 = fsk), ask = Amplitude-shift keying, fsk = Frequency-shift keying

sum = 32 + 64 = 96 = 60h
 */
                // 26h is 38d
                // 38 in binary is 100110
                /* FLAGS   */ (byte)0x26, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ INVENTORY_COMMAND, //(byte)0x01, // command get Inventory
                /* ???     */ (byte)0x00
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readMultipleBlocks", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "inventory read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public boolean setPasswordEasAfi(byte[] password) {
        return setPassword(PASSWORD_IDENTIFIER_EAS_AFI, password);
    }

    private boolean setPassword(byte passwordIdentifier, byte[] password) {
        // sanity checks
        if (!checkPassword(password)) {
            return false;
        }
        if (!checkPasswordIdentifier(passwordIdentifier)) {
            return false;
        }
        byte[] randomNumber = getRandomNumber();
        if (randomNumber == null) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "Could not get the random number, aborted";
            Log.e(TAG, "Could not get the random number, aborted");
            return false;
        }
        //Log.d(TAG, printData("password", password));
        byte[] randomNumberFull = new byte[4];
        System.arraycopy(randomNumber, 0, randomNumberFull, 0, 2);
        System.arraycopy(randomNumber, 0, randomNumberFull, 2, 2);
        byte[] passwordXor = xor(password, randomNumberFull);
        //Log.d(TAG, printData("randomNumber", randomNumber));
        //Log.d(TAG, printData("randomNumberFull", randomNumberFull));
        //Log.d(TAG, printData("passwordXor", passwordXor));
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ SET_PASSWORD_COMMAND, //(byte)0xb3, // command set password
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* PASS ID */ passwordIdentifier,
                /* PASSWORD*/ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        System.arraycopy(passwordXor, 0, cmd, 12, 4); // copy xored password
        //Log.d(TAG, printData("tagUid", tagUid));
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "setPassword IOException: " + e.getMessage();
            Log.e(TAG, "setPassword IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "password set successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean writePasswordEasAfi(byte[] password) {
        return writePassword(PASSWORD_IDENTIFIER_EAS_AFI, password);
    }

    private boolean writePassword(byte passwordIdentifier, byte[] password) {
        // sanity checks
        if (!checkPassword(password)) {
            return false;
        }
        if (!checkPasswordIdentifier(passwordIdentifier)) {
            return false;
        }
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ WRITE_PASSWORD_COMMAND, //(byte)0xb4, // command write password
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* PASS ID */ passwordIdentifier,
                /* PASSWORD*/ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        System.arraycopy(password, 0, cmd, 12, 4); // copy xored password
        //Log.d(TAG, printData("tagUid", tagUid));
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "writePassword IOException: " + e.getMessage();
            Log.e(TAG, "writePassword IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "password written successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean passwordProtectEas() {
        // Once the EAS password protection is enabled, it is not possible to
        // change back to unprotected EAS.
        // Option flag set to logic 0: EAS will be password protected

        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ PASSWORD_PROTECT_EAS_AFI_COMMAND, //(byte)0xa6, // command password protection eas/afi
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean passwordProtectAfi() {
        // Once the AFI password protection is enabled, it is not possible to
        // change back to unprotected AFI.
        // Option flag set to logic 1: AFI will be password protected


        return false;
    }

    public boolean setEas() {
        // sanity checks
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ SET_EAS_COMMAND, //(byte)0xa2, // command set eas
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "eas set successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean resetEas() {
        // sanity checks
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ RESET_EAS_COMMAND, //(byte)0xa3, // command reset eas
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "eas reset successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public byte[] easAlarm() {
        // sanity checks
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ EAS_ALARM_COMMAND, //(byte)0xa5, // command eas alarm
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public byte[] inventoryRead(byte afi, byte maskLength, byte maskValue, byte firstBlockNumber, byte numberOfBlocks) {
        // for flags see TRF7960 Evaluation Module ISO 15693 Host Commands Sloa141.pdf pages 20 + 21
/*
bit table
bit 1 0 RFU
bit 2 0 Option flag default
bit 3 0 Nr of slots 0 = all slots
bit 4 0 AFI flag 0 = don't use afi
bit 5 0 Protocol Extension (always 0)
bit 6 1 Inventory flag (1 = see table 4) [32]
bit 7 1 Uplink data read (0 = low, 1 = high) [64]
bit 8 0 subcarrier (0 = ask, 1 = fsk)

sum = 32 + 64 = 96 = 60h
 */
        // sanity checks
        byte[] cmd = new byte[]{
                /* FLAGS   */ (byte) 0x04,
                /* COMMAND */ INVENTORY_READ_COMMAND, //(byte)0xa0, // command inventory read
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
      //          /* AFI     */ afi,
                /* MASK LEN*/ (byte) 0x00,
      //          /* MASK VAL*/ maskValue,
                /* 1st BLK */ firstBlockNumber,
                /* BLK NBR */ numberOfBlocks
        };
        //Log.d(TAG, printData("cmd", cmd));
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public byte[] getMultipleBlockSecurityStatus(int blockNumber, int numberOfBlocks) {
        // sanity check
        if (!checkBlockNumber(blockNumber)) {
            return null;
        }
        if (!checkNumberOfBlocks(numberOfBlocks, blockNumber)) {
            return null;
        }
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ GET_MULTIPLE_BLOCK_SECURITY_STATUS_COMMAND, //(byte)0x2c, // command get Multiple Block Security Status
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ (byte)(blockNumber & 0x0ff),
                /* NUMBER  */ (byte)((numberOfBlocks - 1) & 0x0ff)
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0x0ff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "getMultipleBlockSecurityStatus IOException: " + e.getMessage();
            Log.e(TAG, "getMultipleBlockSecurityStatus IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readMultipleBlocks", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "security data from blocks " + blockNumber + " ff read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }


    public byte[] readSingleBlock(int blockNumber) {
        // sanity check
        if (!checkBlockNumber(blockNumber)) {
            return null;
        }
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ READ_SINGLE_BLOCK_COMMAND, //(byte)0x20, // command read single block
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ (byte)0x00
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0x0ff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "readSingleBlock IOException: " + e.getMessage();
            Log.e(TAG, "readSingleBlock IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "data from block " + blockNumber + " read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public byte[] readMultipleBlocks(int blockNumber, int numberOfBlocks) {
        // sanity check
        if (!checkBlockNumber(blockNumber)) {
            return null;
        }
        if (!checkNumberOfBlocks(numberOfBlocks, blockNumber)) {
            return null;
        }
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ READ_MULTIPLE_BLOCKS_COMMAND, //(byte)0x23, // command read multiple blocks
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ (byte)(blockNumber & 0x0ff),
                /* NUMBER  */ (byte)((numberOfBlocks - 1) & 0x0ff)
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0x0ff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "readMultipleBlocks IOException: " + e.getMessage();
            Log.e(TAG, "readMultipleBlocks IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readMultipleBlocks", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "data from blocks " + blockNumber + " ff read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public boolean writeSingleBlock(int blockNumber, byte[] data4Byte) {
        // sanity check
        if (!checkBlockNumber(blockNumber)) {
            return false;
        }
        if (!checkBlockData(data4Byte)) {
            return false;
        }
        byte[] cmd = new byte[] {
                ///* FLAGS   */ (byte)0x60, // flags: addressed (= UID field present), use default OptionSet
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ WRITE_SINGLE_BLOCK_COMMAND, //(byte)0x21, // command write single block
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ (byte)0x00,
                /* DATA    */ data4Byte[0], data4Byte[1], data4Byte[2], data4Byte[3]
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0x0ff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "writeSingleBlock IOException: " + e.getMessage();
            Log.e(TAG, "writeSingleBlock IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "data written to block " + blockNumber + " successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean writeAfi(byte afi) {
        // sanity check

        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ WRITE_AFI_CCOMMAND, //(byte)0x27, // command write afi
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ afi
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "afi written successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean writeDsfId(byte dsfId) {
        // sanity check

        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ WRITE_DSFID_COMMAND, //(byte)0x29, // command write dsfId
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* OFFSET  */ dsfId
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "dsfId written successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean formatTagNdef() {
        //byte[] block00 = Utils.hexStringToByteArrayBlanks("E1 40 F2 09"); // F2 defines the total memory of the tag
        // 112 bytes = 70h
        byte[] block00 = Utils.hexStringToByteArrayBlanks("E1 40 70 09");
        byte[] block01 = Utils.hexStringToByteArrayBlanks("03 0B D1 01");
        byte[] block02 = Utils.hexStringToByteArrayBlanks("07 54 02 65");
        byte[] block03 = Utils.hexStringToByteArrayBlanks("6E 41 42 43");
        byte[] block04 = Utils.hexStringToByteArrayBlanks("44 FE 00 00");
        byte[] block05 = Utils.hexStringToByteArrayBlanks("00 00 00 00");
        boolean success;
        success = writeSingleBlock(0, block00);
        if (success) {
            success = writeSingleBlock(1, block01);
        } else {
            return false;
        }
        if (success) {
            success = writeSingleBlock(2, block02);
        } else {
            return false;
        }
        if (success) {
            success = writeSingleBlock(3, block03);
        } else {
            return false;
        }
        if (success) {
            success = writeSingleBlock(4, block04);
        } else {
            return false;
        }
        if (success) {
            success = writeSingleBlock(5, block05);
        } else {
            return false;
        }
        return true;
    }

    private SystemInformation getSystemInformation() {
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x02, // flags
                /* COMMAND */ GET_SYSTEM_INFORMATION_COMMAND, //(byte)0x20, // command read single block
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "system information read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return new SystemInformation(response);
    }

    private byte[] getRandomNumber() {
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags
                /* COMMAND */ GET_RANDOM_NUMBER_COMMAND, //(byte)0xb2, // command get random number
                /* MANUF ID*/ MANUFACTURER_CODE_NXP,
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        // manufacturer code is 04 for NXP tags
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        writeToUiAppend(textView, printData("response", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "random number read successfully");
        errorCode = RESPONSE_OK.clone();
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    /**
     * service methods
     */

    private boolean checkPassword(byte[] password) {
        if (password == null) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "password is NULL";
            return false;
        }
        if (password.length != 4) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "password has wrong length (correct 4 bytes, found " + password.length + "bytes)";
            return false;
        }
        return true;
    }

    private boolean checkPasswordIdentifier(byte passwordIdentifier) {
        if (passwordIdentifier != (byte) 0x10) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "passwordIdentifier is not 0x10h, aborted";
            return false;
        }
        return true;
    }

    private boolean checkTagUid(byte[] tagUid) {
        if (tagUid == null) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "tagUid is NULL";
            return false;
        }
        if (tagUid.length != 8) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "tagUid has wrong length (correct 8 bytes, found " + tagUid.length + "bytes)";
            return false;
        }
        return true;
    }

    private boolean checkBlockNumber(int blockNumber) {
        if (blockNumber < 0) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "blockNumber is < 0, aborted";
            return false;
        }
        if (blockNumber > MAXIMUM_BLOCK_NUMBER) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "blockNumber is > " + MAXIMUM_BLOCK_NUMBER + ", aborted)";
            return false;
        }
        return true;
    }

    private boolean checkNumberOfBlocks(int numberOfBlocks, int blockNumber) {
        if (blockNumber < 0) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "blockNumber is < 0, aborted";
            return false;
        }
        if (blockNumber > MAXIMUM_BLOCK_NUMBER) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "blockNumber is > " + MAXIMUM_BLOCK_NUMBER + ", aborted)";
            return false;
        }
        if (numberOfBlocks < 1) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "numberOfBlocks is < 1, aborted";
        }
        if ((blockNumber + numberOfBlocks) > (MAXIMUM_BLOCK_NUMBER + 1)) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "numberOfBlocks + blockNumber is > MAXIMUM NUMBER OF BLOCKS (28), aborted";
        }
        return true;
    }

    private boolean checkBlockData(byte[] data4Byte) {
        if (data4Byte == null) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "data4Byte is NULL, aborted";
            return false;
        }
        if (data4Byte.length != 4) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "data4Byte is not of length 4, found " + data4Byte.length + ", aborted)";
            return false;
        }
        return true;
    }

    private boolean checkResponse(byte[] response) {
        // check first byte
        if (getResponseByte(response) == OPERATION_OK) {
            return true;
        }
        errorCode = getResponseBytes(response);
        errorCodeReason = "Failure after transceive, aborted";
        return false;
    }

    private byte getResponseByte(byte[] input) {
        return input[0];
    }

    private byte[] getResponseBytes(byte[] input) {
        return Arrays.copyOfRange(input, 0, 2);
    }

    private byte[] trimFirstByte(byte[] input) {
        return Arrays.copyOfRange(input, 1, (input.length));
    }

    private byte[] xor(byte[] dataA, byte[] dataB) {
        // sanity checks
        if ((dataA == null) || (dataB == null)) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "xor - dataA or dataB is NULL, aborted";
            return null;
        }
        // sanity check - both arrays need to be of the same length
        int dataALength = dataA.length;
        int dataBLength = dataB.length;
        if (dataALength != dataBLength) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "xor - dataA and dataB lengths are different, aborted (dataA: \" + dataALength + \" dataB: \" + dataBLength + \" bytes)";
            return null;
        }
        for (int i = 0; i < dataALength; i++) {
            dataA[i] ^= dataB[i];
        }
        return dataA;
    }

    private boolean initializeCard() {
        String methodName = "initializeCard";
        if (tag == null) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "tag is NULL, aborted";
            return false;
        }
        tagUid = tag.getId();
        Log.d(TAG, printData("tagUid", tagUid));
        writeToUiAppend(textView, printData("tagUid", tagUid));
        nfcV = NfcV.get(tag);
        if (nfcV == null) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "NFCV is NULL (maybe it is not an Icode SLIX tag ?), aborted";
            return false;
        }
        // get data from the tag
        techList = tag.getTechList();
        Log.d(TAG, "techList: " + Arrays.toString(techList));
        writeToUiAppend(textView, "techList: " + Arrays.toString(techList));
        maxTranceiveLength = nfcV.getMaxTransceiveLength();
        Log.d(TAG, "maxTranceiveLength: " + maxTranceiveLength + " bytes");
        writeToUiAppend(textView, "maxTranceiveLength: " + maxTranceiveLength + " bytes");
        responseFlags = nfcV.getResponseFlags();
        Log.d(TAG, "responseFlags: " + Utils.byteToHex(responseFlags));
        writeToUiAppend(textView, "responseFlags: " + Utils.byteToHex(responseFlags));
        dsfId = nfcV.getDsfId();
        Log.d(TAG, "dsfId: " + Utils.byteToHex(dsfId));
        writeToUiAppend(textView, "dsfId: " + Utils.byteToHex(dsfId));
        // connect to the tag
        try {
            nfcV.connect();
            Log.d(TAG, "tag is connected");
            writeToUiAppend(textView, "tag is connected");
            // get the inventory
            // inventory
            byte[] UIDFrame = new byte[] { (byte) 0x26, (byte) 0x01, (byte) 0x00 };
            responseInventory = nfcV.transceive(UIDFrame);
            Log.d(TAG, printData("responseInventory", responseInventory));
            writeToUiAppend(textView, printData("responseInventory", responseInventory));
            systemInformation = getSystemInformation();
            if (systemInformation == null) {
                writeToUiAppend(textView, "Could not retrieve system information, aborted");
                errorCode = RESPONSE_FAILURE.clone();
                errorCodeReason = "Could not retrieve system information, aborted";
            } else {
                NUMBER_OF_BLOCKS = systemInformation.getNumberOfBlocks();
                BYTES_PER_BLOCK = systemInformation.getBytesPerBlock();
                MEMORY_SIZE = systemInformation.getMemorySizeInt();
                Log.d(TAG, systemInformation.dump());
                writeToUiAppend(textView, systemInformation.dump());
                errorCode = RESPONSE_OK.clone();
                errorCodeReason = RESPONSE_OK_STRING;
            }

        } catch (IOException e) {
            Log.e(TAG, "Error on connecting the tag: " + e.getMessage());
            writeToUiAppend(textView, "Error on connecting the tag: " + e.getMessage());
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            return false;
        }

        Utils.vibrateShort(activity.getApplicationContext());
        return true;

    }

    /**
     * section for UI related tasks
     */

    private void writeToUiAppend(TextView textView, String message) {
        activity.runOnUiThread(() -> {
            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    /**
     * getter and setter
     */

    public byte[] getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeReason() {
        return errorCodeReason;
    }

    public byte[] getTagUid() {
        return tagUid;
    }

    public byte getDsfId() {
        return dsfId;
    }
}
