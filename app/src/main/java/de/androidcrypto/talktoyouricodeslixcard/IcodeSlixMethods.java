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
    private byte[] responseGetSystemInfoFrame1bytesAddress;

    // mandatory commands as defined in ISO/IEC 15693-3
    // https://developer.apple.com/documentation/corenfc/nfciso15693tag
    private static final byte GET_RANDOM_NUMBER_COMMAND = (byte) 0xB2;
    private static final byte INVENTORY_COMMAND = (byte) 0x00;
    private static final byte STAY_QUIET_COMMAND = (byte) 0x00;
    private static final byte READ_SINGLE_BLOCK_COMMAND = (byte) 0x20;
    private static final byte WRITE_SINGLE_BLOCK_COMMAND = (byte) 0x21;
    private static final byte LOCK_BLOCK_COMMAND = (byte) 0x00;
    private static final byte READ_MULTIPLE_BLOCKS_COMMAND = (byte) 0x23;
    private static final byte SELECT_COMMAND = (byte) 0x00;
    private static final byte RESET_TO_READY_COMMAND = (byte) 0x00;
    private static final byte WRITE_AFI_CCOMMAND = (byte) 0x00;
    private static final byte LOCK_AFI_COMMAND = (byte) 0x00;
    private static final byte WRITE_DSFID_COMMAND = (byte) 0x00;
    private static final byte LOCK_DSFID_COMMAND = (byte) 0x00;
    private static final byte GET_SYSTEM_COMMAND = (byte) 0x00;
    private static final byte GET_MULTIPLE_BLOCK_SECURITY_STATUS_COMMAND = (byte) 0x00;

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
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
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
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
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
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, "IOException: " + e.getMessage());
            return false;
        }
        //writeToUiAppend(textView, printData("readSingleBlock", response));
        if (!checkResponse(response)) return false; // errorCode and reason are setup
        Log.d(TAG, "data written to block " + blockNumber + " successfully");
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

    /**
     * service methods
     */

    private boolean checkTagUid(byte[] tagUid) {
        if (tagUid == null) {
            errorCode = RESPONSE_PARAMETER_ERROR.clone();
            errorCodeReason = "tagUid is NULL";
            return false;
        }
        if (tagUid.length == 8) {
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
            byte[] GetSystemInfoFrame1bytesAddress = new byte[] { (byte) 0x02, (byte) 0x2B };
            responseGetSystemInfoFrame1bytesAddress = nfcV.transceive(GetSystemInfoFrame1bytesAddress);
            Log.d(TAG, printData("responseGetSystemInfoFrame1bytesAddress", responseGetSystemInfoFrame1bytesAddress));
            writeToUiAppend(textView, printData("responseGetSystemInfoFrame1bytesAddress", responseGetSystemInfoFrame1bytesAddress));



        } catch (IOException e) {
            Log.e(TAG, "Error on connecting the tag: " + e.getMessage());
            writeToUiAppend(textView, "Error on connecting the tag: " + e.getMessage());
            return false;
        }


        Utils.vibrateShort(activity.getApplicationContext());
        return true;

        /*
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                errorCode = RESPONSE_FAILURE.clone();
                errorCodeReason = "isoDep is NULL (maybe it is not a NTAG424DNA tag ?), aborted";
                return false;
            }
            Log.d(TAG, "tag is connected: " + isoDep.isConnected());
            isoDep.connect();
            Log.d(TAG, "tag is connected: " + isoDep.isConnected());
            if (isoDep.isConnected()) {
                isIsoDepConnected = true;
                Log.d(TAG, "tag is connected to isoDep");
            } else {
                Log.d(TAG, "could not connect to isoDep, aborted");
                isIsoDepConnected = false;
                errorCode = RESPONSE_FAILURE.clone();
                errorCodeReason = "could not connect to isoDep, aborted";
                isoDep.close();
                return false;
            }
            // initialize the Communication Adapter
            communicationAdapter = new CommunicationAdapterNtag424Dna(isoDep, printToLog);
            // get the version information
            versionInfo = getVersionInfo();
            if (versionInfo == null) {
                errorCode = RESPONSE_FAILURE.clone();
                errorCodeReason = "could not retrieve VersionInfo (maybe it is not a NTAG424DNA tag ?), aborted";
                return false;
            }

            lrpAuthentication = new LrpAuthentication(isoDep);

            if (versionInfo.getHardwareType() == (byte) 0x04) {
                isTagNtag424Dna = true;
                Log.d(TAG, "tag is identified as NTAG424DNA");
                log(methodName, versionInfo.dump());
                errorCode = RESPONSE_OK.clone();
                errorCodeReason = "SUCCESS";
                writeToUiAppend(textView, versionInfo.dump());
                Utils.vibrateShort(activity.getBaseContext());
                return true;
            } else {
                isTagNtag424Dna = false;
                Log.d(TAG, "tag is NOT identified as NTAG424DNA, aborted");
                writeToUiAppend(textView, "tag is NOT identified as NTAG424DNA, aborted");
                log(methodName, versionInfo.dump());
                errorCode = RESPONSE_FAILURE.clone();
                errorCodeReason = "could not retrieve VersionInfo (maybe it is not a NTAG424DNA tag ?), aborted";
                writeToUiAppend(textView, versionInfo.dump());
                return false;
            }
        } catch (IOException e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "IOException: " + e.getMessage();
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            errorCode = RESPONSE_FAILURE.clone();
            errorCodeReason = "Exception: " + e.getMessage();
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            return false;
        }
        */
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
}
