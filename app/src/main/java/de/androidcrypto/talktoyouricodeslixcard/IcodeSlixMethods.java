package de.androidcrypto.talktoyouricodeslixcard;

import static de.androidcrypto.talktoyouricodeslixcard.Utils.printData;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IcodeSlixMethods {

    private static final String TAG = IcodeSlixMethods.class.getName();

    private final Tag tag;
    private final Activity activity;
    private final TextView textView;

    //
    private NfcV nfcV;
    private String[] techList;

    private byte[] tagUid;
    private int maxTranceiveLength;
    private byte responseFlags;
    private byte dsfId;
    private byte[] responseInventory;
    private SystemInformation systemInformation;
    private Inventory inventory;
    private boolean isInitialized = false;

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
    private byte errorCode;
    private String errorCodeReason;
    private static final byte OPERATION_OK = (byte) 0x00;
    public static final byte RESPONSE_OK = (byte) 0x00;
    private static final String RESPONSE_OK_STRING = "SUCCESS";
    private static final byte RESPONSE_PARAMETER_ERROR = (byte) 0xFE; // failure because of wrong parameter
    private static final byte RESPONSE_FAILURE = (byte) 0xFF; // general, undefined failure
    private static final String RESPONSE_FAILURE_STRING = "FAILURE";
    // constants
    private int MAXIMUM_BLOCK_NUMBER; // fixed for ICODE SLIX S with 28 blocks * 4 bytes = 108 bytes user memory = 27
    // from system information
    private int NUMBER_OF_BLOCKS;
    private int BYTES_PER_BLOCK;
    private int MEMORY_SIZE;
    private byte MANUFACTURER_CODE;

    public IcodeSlixMethods(Tag tag, Activity activity, TextView textView) {
        this.tag = tag;
        this.activity = activity;
        this.textView = textView;
        Log.d(TAG, "IcodeSlixMethods initializing");
        boolean success = initializeCard();
        if (success) {
            errorCode = RESPONSE_OK;
            errorCodeReason = RESPONSE_OK_STRING;
        } else {
            errorCode = RESPONSE_FAILURE;
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
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "getInventory IOException: " + e.getMessage();
            Log.e(TAG, "getInventory IOException: " + e.getMessage());
            return null;
        }
        //writeToUiAppend(textView, printData("readMultipleBlocks", response));
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "inventory read successfully");
        errorCode = RESPONSE_OK;
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
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "Could not get the random number, aborted";
            Log.e(TAG, "Could not get the random number, aborted");
            return false;
        }
        byte[] randomNumberFull = new byte[4];
        System.arraycopy(randomNumber, 0, randomNumberFull, 0, 2);
        System.arraycopy(randomNumber, 0, randomNumberFull, 2, 2);
        byte[] passwordXor = xor(password, randomNumberFull);
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ SET_PASSWORD_COMMAND, //(byte)0xb3, // command set password
                /* MANUF ID*/ MANUFACTURER_CODE, // manufacturer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* PASS ID */ passwordIdentifier,
                /* PASSWORD*/ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        System.arraycopy(passwordXor, 0, cmd, 12, 4); // copy xored password
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "setPassword IOException: " + e.getMessage();
            Log.e(TAG, "setPassword IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "password set successfully");
        errorCode = RESPONSE_OK;
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
                /* MANUF ID*/ MANUFACTURER_CODE, // manufacturer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* PASS ID */ passwordIdentifier,
                /* PASSWORD*/ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        System.arraycopy(password, 0, cmd, 12, 4); // copy xored password
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "writePassword IOException: " + e.getMessage();
            Log.e(TAG, "writePassword IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "password written successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean passwordProtectEas() {
        // Once the EAS password protection is enabled, it is not possible to
        // change back to unprotected EAS.
        // Option flag set to logic 0: EAS will be password protected
        // Option flag set to logic 1: AFI will be password protected

        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ PASSWORD_PROTECT_EAS_AFI_COMMAND, //(byte)0xa6, // command password protection eas/afi
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "passwordProtectEas IOException: " + e.getMessage();
            Log.e(TAG, "passwordProtectEas IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "passwordProtectEas successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean passwordProtectAfi() {
        // Once the AFI password protection is enabled, it is not possible to
        // change back to unprotected AFI.
        // Option flag set to logic 0: EAS will be password protected
        // Option flag set to logic 1: AFI will be password protected

        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x60, // flags: addressed (= UID field present), use default Option 1 for AFI protection
                /* COMMAND */ PASSWORD_PROTECT_EAS_AFI_COMMAND, //(byte)0xa6, // command password protection eas/afi
                /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufacturer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "passwordProtectAfi IOException: " + e.getMessage();
            Log.e(TAG, "passwordProtectAfi IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "passwordProtectAfi successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
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
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "setEas IOException: " + e.getMessage();
            Log.e(TAG, "setEas IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "eas set successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean resetEas() {
        // no sanity checks
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ RESET_EAS_COMMAND, //(byte)0xa3, // command reset eas
                /* MANUF ID*/ MANUFACTURER_CODE, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "resetEas IOException: " + e.getMessage();
            Log.e(TAG, "resetEas IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "eas reset successfully");
        errorCode = RESPONSE_OK;
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
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "easAlarm IOException: " + e.getMessage();
            Log.e(TAG, "easAlarm IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public boolean lockEas() {
        // no sanity checks
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ LOCK_EAS_COMMAND, //(byte)0xa4, // command lock eas
                /* MANUF ID*/ MANUFACTURER_CODE, // manufactorer code is 0x04h for NXP
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "lockEas IOException: " + e.getMessage();
            Log.e(TAG, "lockEas IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "eas locked successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public byte[] inventoryRead(int firstBlockNumber, int numberOfBlocks) {
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
        if (!checkBlockNumber(firstBlockNumber)) return null;
        if (!checkNumberOfBlocks(numberOfBlocks, firstBlockNumber)) return null;

        byte[] cmd = new byte[]{
                /* FLAGS   */ (byte) 0x04,
                /* COMMAND */ INVENTORY_READ_COMMAND, //(byte)0xa0, // command inventory read
                /* MANUF ID*/ MANUFACTURER_CODE,
                //      /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                //          /* AFI     */ afi,
                /* MASK LEN*/ (byte) 0x00,
                //          /* MASK VAL*/ maskValue,
                /* 1st BLK */ (byte) (firstBlockNumber & 0xff),
                /* BLK NBR */ (byte) (numberOfBlocks & 0xff),
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "inventoryRead IOException: " + e.getMessage();
            Log.e(TAG, "inventoryRead IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    public byte[] inventoryRead(byte afi, int firstBlockNumber, int numberOfBlocks) {
        // if the wrong afi is presented an IOException occurs
        // for flags see TRF7960 Evaluation Module ISO 15693 Host Commands Sloa141.pdf pages 20 + 21
/*
bit table
bit 1 0 RFU
bit 2 0 Option flag default
bit 3 0 Nr of slots 0 = all slots
bit 4 0 AFI flag 0 = don't use afi
bit 5 0 Protocol Extension (always 0)
bit 6 1 Inventory flag (1 = see table 4) [32]
bit 7 0 Uplink data read (0 = low, 1 = high) [64]
bit 8 0 subcarrier (0 = ask, 1 = fsk)

 */
        // sanity checks
        if (!checkBlockNumber(firstBlockNumber)) return null;
        if (!checkNumberOfBlocks(numberOfBlocks, firstBlockNumber)) return null;
        byte[] cmd = new byte[]{
                /* FLAGS   */ (byte) 0x14, // inventory with AFI option
                /* COMMAND */ INVENTORY_READ_COMMAND, //(byte)0xa0, // command inventory read
                /* MANUF ID*/ MANUFACTURER_CODE,
                //      /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                /* AFI     */ afi,
                /* MASK LEN*/ (byte) 0x00,
                //          /* MASK VAL*/ maskValue,
                /* 1st BLK */ (byte) (firstBlockNumber & 0xff),
                /* BLK NBR */ (byte) (numberOfBlocks & 0xff),
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "inventoryRead IOException: " + e.getMessage();
            Log.e(TAG, "inventoryRead IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK;
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
bit 7 0 Uplink data read (0 = low, 1 = high) [64]
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
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "inventoryRead IOException: " + e.getMessage();
            Log.e(TAG, "inventoryRead IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    /* Note: this is NOT WORKING */
    public byte[] fastInventoryRead(int firstBlockNumber, int numberOfBlocks) {
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
        if (!checkBlockNumber(firstBlockNumber)) return null;
        if (!checkNumberOfBlocks(numberOfBlocks, firstBlockNumber)) return null;
        byte[] cmd = new byte[]{
                /* FLAGS   */ (byte) 0x04,
                /* COMMAND */ FAST_INVENTORY_READ_COMMAND, //(byte)0xa0, // command inventory read
                /* MANUF ID*/ MANUFACTURER_CODE,
                //      /* MANUF ID*/ MANUFACTURER_CODE_NXP, // manufactorer code is 0x04h for NXP
                //          /* AFI     */ afi,
                /* MASK LEN*/ (byte) 0x00,
                //          /* MASK VAL*/ maskValue,
                /* 1st BLK */ (byte) (firstBlockNumber & 0xff),
                /* BLK NBR */ (byte) (numberOfBlocks & 0xff),
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "fastInventoryRead IOException: " + e.getMessage();
            Log.e(TAG, "fastInventoryRead IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "set eas alarm successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    /**
     * This is a service method to return the status of all blocks
     */
    public byte[] getAllMultipleBlockSecurityStatus() {
        return getMultipleBlockSecurityStatus(0, NUMBER_OF_BLOCKS);
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
                /* OFFSET  */ (byte)(blockNumber & 0xff),
                /* NUMBER  */ (byte)((numberOfBlocks - 1) & 0xff)
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0xff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "getMultipleBlockSecurityStatus IOException: " + e.getMessage();
            Log.e(TAG, "getMultipleBlockSecurityStatus IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "security data from blocks " + blockNumber + " ff read successfully");
        errorCode = RESPONSE_OK;
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
        cmd[10] = (byte)((blockNumber) & 0xff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "readSingleBlock IOException: " + e.getMessage();
            Log.e(TAG, "readSingleBlock IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "data from block " + blockNumber + " read successfully");
        errorCode = RESPONSE_OK;
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
                /* OFFSET  */ (byte)(blockNumber & 0xff),
                /* NUMBER  */ (byte)((numberOfBlocks - 1) & 0xff)
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        cmd[10] = (byte)((blockNumber) & 0xff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "readMultipleBlocks IOException: " + e.getMessage();
            Log.e(TAG, "readMultipleBlocks IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        Log.d(TAG, "data from blocks " + blockNumber + " ff read successfully");
        errorCode = RESPONSE_OK;
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
        cmd[10] = (byte)((blockNumber) & 0xff); // copy block number
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "writeSingleBlock IOException: " + e.getMessage();
            Log.e(TAG, "writeSingleBlock IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "data written to block " + blockNumber + " successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    /**
     * This is not a command natively supported by the ICODE SLIX tag.
     * It will write 0x00 to the complete tag memory of the tag ("erasing")
     */

    public boolean writeClearTagMemory() {
        byte[] emptyTag = new byte[MEMORY_SIZE];
        return writeMultipleBlocks(0, emptyTag);
    }

    /**
     * This is not a command natively supported by the ICODE SLIX tag.
     * It divides the data into parts of 4 bytes each and writes them to the tag, beginning at blockNumber
     * @param blockNumber
     * @param data
     * @return true if all data was written with success
     */
    public boolean writeMultipleBlocks(int blockNumber, byte[] data) {
        if (!checkBlockNumber(blockNumber)) return false;
        if(!checkData(data)) return false;
        int dataLen = data.length;
        // if dataLen is not a multiple of BYTES_PER_BLOCK create a larger array
        byte[] newData;
        int newDataLen;
        if (dataLen % BYTES_PER_BLOCK != 0) {
            // dataLen is not a multiple of BYTES_PER_BLOCK
            newDataLen = dataLen + (BYTES_PER_BLOCK - (dataLen % BYTES_PER_BLOCK));
            newData  = new byte[newDataLen];
            System.arraycopy(data, 0, newData, 0, dataLen);
        } else {
            newData = data;
            newDataLen = dataLen;
        }
        if (newDataLen > MEMORY_SIZE) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data length is > " + MEMORY_SIZE + ", found length " + newDataLen + " , aborted)";
            return false;
        }
        // if we don't start at the first blockNumber we don't have the full memory size to store
        if (newDataLen > (MEMORY_SIZE - (blockNumber * BYTES_PER_BLOCK))) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data length is too large for memory, aborted)";
            return false;
        }
        List<byte[]> dataList = Utils.divideArrayToList(newData,BYTES_PER_BLOCK);
        boolean resultMultipleWrite = true;
        for (int i = 0; i < dataList.size(); i++){
            boolean resultSingleWrite = writeSingleBlock(blockNumber + i, dataList.get(i));
            if (resultSingleWrite == false) resultMultipleWrite = false; // if one write fails the method reports false
        }
        return resultMultipleWrite;
    }

    public boolean lockBlock(int blockNumber) {
        // sanity check
        if (!checkBlockNumber(blockNumber)) return false;
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ LOCK_BLOCK_COMMAND, //(byte)0x28, // command lock afi
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                /* BLK NBR */ (byte) (blockNumber & 0xff)
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "lockBlock IOException: " + e.getMessage();
            Log.e(TAG, "lockBlock IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "block locked successfully");
        errorCode = RESPONSE_OK;
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
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "writeAfi IOException: " + e.getMessage();
            Log.e(TAG, "writeAfi IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "afi written successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean lockAfi() {
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ LOCK_AFI_COMMAND, //(byte)0x28, // command lock afi
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "lockAfi IOException: " + e.getMessage();
            Log.e(TAG, "lockAfi IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "afi locked successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean writeDsfId(byte dsfId) {
        // no sanity check
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
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "writeDsfId IOException: " + e.getMessage();
            Log.e(TAG, "writeDsfId IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "dsfId written successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean lockDsfId() {
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags: addressed (= UID field present), use default OptionSet
                /* COMMAND */ LOCK_DSFID_COMMAND, //(byte)0x2A, // command lock dsfId
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        };
        System.arraycopy(tagUid, 0, cmd, 2, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "lockDsfId IOException: " + e.getMessage();
            Log.e(TAG, "lockDsfId IOException: " + e.getMessage());
            return false;
        }
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "dsfId locked successfully");
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return true;
    }

    public boolean writeNdefMessage(String message) {
        byte[] blockNdef = createNdefTextMessage(message);
        //byte[] block00 = Utils.hexStringToByteArrayBlanks("E1 40 0E 01"); // NDEF message is readable and writable
        byte[] block00 = Utils.hexStringToByteArrayBlanks("E1 43 0E 01"); // NDEF message is readable but NOT writable

        boolean success;
        success = writeSingleBlock(0, block00);
        if (success) {
            success = writeMultipleBlocks(1, blockNdef);
        } else {
            return false;
        }
        return true;

/*
        byte[] block01 = Utils.hexStringToByteArrayBlanks("03 00 FE 00"); // this is an empty NDEF message
        boolean success;
        success = writeSingleBlock(0, block00);
        if (success) {
            success = writeSingleBlock(1, block01);
        } else {
            return false;
        }
        return true;
        // correct e1400e000311d1010d5402656e6d79206d657373616765fe00... my message
        // header  e1400e
        //               00 ??
        //                 0311
        //                   11h = 17d
        //                     d1010d5402656e6d79206d657373616765fe
        //                     |          17 bytes              |
        //                                                         00..

 */
    }

    private byte[] createNdefTextMessage(String message) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", message);
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        byte[] ndefMessageByte = ndefMessage.toByteArray();
        int ndefMessageLength = ndefMessageByte.length;
        int ndefMessageTotalLength = ndefMessageLength + 3;
        byte[] fullNdefMessage = new byte[ndefMessageTotalLength];
        fullNdefMessage[0] = (byte) (0x03);
        fullNdefMessage[1] = (byte) (ndefMessageLength & 0xff);
        System.arraycopy(ndefMessageByte, 0, fullNdefMessage, 2, ndefMessageLength);
        fullNdefMessage[ndefMessageTotalLength - 1] = (byte) 0xFE; // terminator
        return fullNdefMessage;
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
                /* COMMAND */ GET_SYSTEM_INFORMATION_COMMAND, //(byte)0x2B, // command get system information
        };
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return new SystemInformation(response);
    }

    private byte[] getRandomNumber() {
        byte[] cmd = new byte[] {
                /* FLAGS   */ (byte)0x20, // flags
                /* COMMAND */ GET_RANDOM_NUMBER_COMMAND, //(byte)0xb2, // command get random number
                /* MANUF ID*/ MANUFACTURER_CODE,
                /* UID     */ (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };
        // manufacturer code is 04 for NXP tags
        System.arraycopy(tagUid, 0, cmd, 3, 8); // copy tagId to UID
        byte[] response;
        try {
            response = nfcV.transceive(cmd);
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        }
        if (!checkResponse(response)) return null; // errorCode and reason are setup
        errorCode = RESPONSE_OK;
        errorCodeReason = RESPONSE_OK_STRING;
        return trimFirstByte(response);
    }

    private boolean initializeCard() {
        String methodName = "initializeCard";
        if (tag == null) {
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "tag is NULL, aborted";
            return false;
        }
        tagUid = tag.getId();
        Log.d(TAG, printData("tagUid", tagUid));
        writeToUiAppend(textView, printData("tagUid", tagUid));
        nfcV = NfcV.get(tag);
        if (nfcV == null) {
            errorCode = RESPONSE_FAILURE;
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
            inventory = new Inventory(systemInformation.getDsfId(), systemInformation.getUid());
            if (systemInformation == null) {
                writeToUiAppend(textView, "Could not retrieve system information, aborted");
                errorCode = RESPONSE_FAILURE;
                errorCodeReason = "Could not retrieve system information, aborted";
            } else {
                MANUFACTURER_CODE = inventory.getUid6IcManufacturerCode();
                NUMBER_OF_BLOCKS = systemInformation.getNumberOfBlocks();
                BYTES_PER_BLOCK = systemInformation.getBytesPerBlock();
                MEMORY_SIZE = systemInformation.getMemorySizeInt();
                MAXIMUM_BLOCK_NUMBER = NUMBER_OF_BLOCKS - 1;
                Log.d(TAG, systemInformation.dump());
                writeToUiAppend(textView, systemInformation.dump());
                errorCode = RESPONSE_OK;
                errorCodeReason = RESPONSE_OK_STRING;
                isInitialized = true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Error on connecting the tag: " + e.getMessage());
            writeToUiAppend(textView, "Error on connecting the tag: " + e.getMessage());
            errorCode = RESPONSE_FAILURE;
            errorCodeReason = "IOException: " + e.getMessage();
            return false;
        }
        Utils.vibrateShort(activity.getApplicationContext());
        return true;
    }

    /**
     * service methods
     */

    private boolean checkPassword(byte[] password) {
        if (password == null) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "password is NULL";
            return false;
        }
        if (password.length != 4) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "password has wrong length (correct 4 bytes, found " + password.length + "bytes)";
            return false;
        }
        return true;
    }

    private boolean checkPasswordIdentifier(byte passwordIdentifier) {
        if (passwordIdentifier != (byte) 0x10) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "passwordIdentifier is not 0x10h, aborted";
            return false;
        }
        return true;
    }

    private boolean checkTagUid(byte[] tagUid) {
        if (tagUid == null) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "tagUid is NULL";
            return false;
        }
        if (tagUid.length != 8) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "tagUid has wrong length (correct 8 bytes, found " + tagUid.length + "bytes)";
            return false;
        }
        return true;
    }

    private boolean checkBlockNumber(int blockNumber) {
        if (blockNumber < 0) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "blockNumber is < 0, aborted";
            return false;
        }
        if (blockNumber > MAXIMUM_BLOCK_NUMBER) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "blockNumber is > " + MAXIMUM_BLOCK_NUMBER + ", aborted)";
            return false;
        }
        return true;
    }

    private boolean checkNumberOfBlocks(int numberOfBlocks, int blockNumber) {
        if (blockNumber < 0) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "blockNumber is < 0, aborted";
            return false;
        }
        if (blockNumber > MAXIMUM_BLOCK_NUMBER) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "blockNumber is > " + MAXIMUM_BLOCK_NUMBER + ", aborted)";
            return false;
        }
        if (numberOfBlocks < 1) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "numberOfBlocks is < 1, aborted";
        }
        if ((blockNumber + numberOfBlocks) > (MAXIMUM_BLOCK_NUMBER + 1)) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "numberOfBlocks + blockNumber is > MAXIMUM NUMBER OF BLOCKS (28), aborted";
        }
        return true;
    }

    private boolean checkBlockData(byte[] data4Byte) {
        if (data4Byte == null) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data4Byte is NULL, aborted";
            return false;
        }
        if (data4Byte.length != 4) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data4Byte is not of length 4, found " + data4Byte.length + ", aborted)";
            return false;
        }
        return true;
    }

    private boolean checkData(byte[] data) {
        if (data == null) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data is NULL, aborted";
            return false;
        }
        if (data.length == 0) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "data is of length 0, aborted)";
            return false;
        }
        return true;
    }

    private boolean checkResponse(byte[] response) {
        // check first byte
        if (getResponseByte(response) == OPERATION_OK) {
            errorCode = RESPONSE_OK;
            errorCodeReason = RESPONSE_OK_STRING;
            return true;
        }
        errorCode = getResponseBytes(response)[0];
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
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "xor - dataA or dataB is NULL, aborted";
            return null;
        }
        // sanity check - both arrays need to be of the same length
        int dataALength = dataA.length;
        int dataBLength = dataB.length;
        if (dataALength != dataBLength) {
            errorCode = RESPONSE_PARAMETER_ERROR;
            errorCodeReason = "xor - dataA and dataB lengths are different, aborted (dataA: \" + dataALength + \" dataB: \" + dataBLength + \" bytes)";
            return null;
        }
        for (int i = 0; i < dataALength; i++) {
            dataA[i] ^= dataB[i];
        }
        return dataA;
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

    public byte getErrorCode() {
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

    public boolean isInitialized() {
        return isInitialized;
    }
}
