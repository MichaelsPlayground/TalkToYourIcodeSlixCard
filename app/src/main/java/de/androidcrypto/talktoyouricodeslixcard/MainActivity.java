package de.androidcrypto.talktoyouricodeslixcard;

import static de.androidcrypto.talktoyouricodeslixcard.Utils.byteToHex;
import static de.androidcrypto.talktoyouricodeslixcard.Utils.bytesToHexNpeUpperCase;
import static de.androidcrypto.talktoyouricodeslixcard.Utils.bytesToHexNpeUpperCaseBlank;
import static de.androidcrypto.talktoyouricodeslixcard.Utils.hexStringToByteArray;
import static de.androidcrypto.talktoyouricodeslixcard.Utils.printData;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final String TAG = MainActivity.class.getName();

    private com.google.android.material.textfield.TextInputEditText output, errorCode;
    private com.google.android.material.textfield.TextInputLayout errorCodeLayout;


    //private FileSettings selectedFileSettings;

    /**
     * section for application handling
     */

    private com.google.android.material.textfield.TextInputEditText numberOfKeys, applicationId, applicationSelected;
    private Button applicationList, applicationSelect;

    // experimental:

    private byte[] selectedApplicationId = null;

    /**
     * section for files
     */

    private Button fileList, fileSelect, getFileSettings, changeFileSettings, getFileSettingsMac;
    private com.google.android.material.textfield.TextInputEditText fileSelected;
    private String selectedFileId = "";
    private int selectedFileSize;
    private FileSettings selectedFileSettings;

    /**
     * section for EV2 authentication and communication
     */

    private Button selectApplicationEv2, getAllFileIdsEv2, getAllFileSettingsEv2, completeFileSettingsEv2;

    private Button authD0AEv2, authD2AEv2, authD3AEv2, authD3ACEv2;
    private Button getCardUidEv2, getFileSettingsEv2;
    private Button fileStandardCreateEv2, fileStandardWriteEv2, fileStandardReadEv2;

    private Button fileCreateFileSetPlain, fileCreateFileSetMaced, fileCreateFileSetEnciphered;

    private Button changeKeyD3AtoD3AC, changeKeyD3ACtoD3A;
    private Button enableTransactionTimerEv2;

    /**
     * section for auth using LRP with default keys
     */

    private Button authD0LEv2, authD2LEv2, authD3LEv2, authD4LEv2;

    /**
     * section for SDM tasks
     */

    private Button createNdefFile256Ev2, sdmChangeFileSettingsEv2, sdmTestFileSettingsEv2;
    private Button sdmGetFileSettingsEv2, sdmDecryptNdefManualEv2;

    /**
     * section for standard file handling
     */

    private Button fileStandardWrite2, fileStandardWrite3, fileStandardRead, fileStandardRead2, fileStandardRead3;
    private com.google.android.material.textfield.TextInputEditText fileStandardFileId, fileStandardSize, fileStandardData;
    RadioButton rbFileFreeAccess, rbFileKeySecuredAccess;

    /**
     * section for authentication
     */
    private Button authKey0D, authKey1D, authKey2D, authKey3D, authKey4D; // default keys
    private Button authKey0C, authKey1C, authKey2C, authKey3C, authKey4C; // changed keys

    private Button testEnableLrpMode;
    private byte KEY_NUMBER_USED_FOR_AUTHENTICATION; // the key number used for a successful authentication
    // var used by EV2 auth
    private byte[] SES_AUTH_ENC_KEY; // filled in by authenticateEv2
    private byte[] SES_AUTH_MAC_KEY; // filled in by authenticateEv2
    private byte[] TRANSACTION_IDENTIFIER; // filled in by authenticateEv2
    private int CMD_COUNTER; // filled in by authenticateEv2, LSB encoded when in byte[


    /**
     * section for key handling
     */

    private Button changeKey0ToC, changeKey1ToC, changeKey2ToC, changeKey3ToC, changeKey4ToC; // change key to CHANGED
    private Button changeKey0ToD, changeKey1ToD, changeKey2ToD, changeKey3ToD, changeKey4ToD; // change key to DEFAULT

    /**
     * section for general
     */

    private Button getCardUidDes, getCardUidAes; // get cardUID * encrypted
    private Button getTagVersion, formatPicc;
    private Button getKeyVersion;

    private Button testGetSesAuthKeys;
    /**
     * section for visualizing DES authentication
     */

    private Button selectApplicationDesVisualizing, authDesVisualizing, readDesVisualizing, writeDesVisualizing;
    private Button changeKeyDes00ToChangedDesVisualizing, changeKeyDes00ToDefaultDesVisualizing;
    private Button changeKeyDes01ToChangedDesVisualizing, changeKeyDes01ToDefaultDesVisualizing, authDesVisualizingC;
    private Button changeKeyDes01ToChanged2DesVisualizing, changeKeyDes01ToDefault2DesVisualizing, authDesVisualizingC2;

    /**
     * section for tests
     */

    private Button testNdefTemplate;

    /**
     * section for constants
     */

    private final byte[] APPLICATION_IDENTIFIER = Utils.hexStringToByteArray("D0D1D2"); // AID 'D0 D1 D2'
    private final byte APPLICATION_NUMBER_OF_KEYS = (byte) 0x05; // maximum 5 keys for secured access
    private final byte APPLICATION_MASTER_KEY_SETTINGS = (byte) 0x0F; // 'amks'
    /**
     * for explanations on Master Key Settings see M075031_desfire.pdf page 35:
     * left '0' = Application master key authentication is necessary to change any key (default)
     * right 'f' = bits 3..0
     * bit 3: 1: this configuration is changeable if authenticated with the application master key (default setting)
     * bit 2: 1: CreateFile / DeleteFile is permitted also without application master key authentication (default setting)
     * bit 1: 1: GetFileIDs, GetFileSettings and GetKeySettings commands succeed independently of a preceding application master key authentication (default setting)
     * bit 0: 1: Application master key is changeable (authentication with the current application master key necessary, default setting)
     */

    private final byte FILE_COMMUNICATION_SETTINGS = (byte) 0x00; // plain communication
    /**
     * for explanations on File Communication Settings see M075031_desfire.pdf page 15:
     * byte = 0: Plain communication
     * byte = 1: Plain communication secured by DES/3DES/AES MACing
     * byte = 3: Fully DES/3DES/AES enciphered communication
     */

    private final byte STANDARD_FILE_FREE_ACCESS_ID = (byte) 0x00; // file ID with free access
    private final byte STANDARD_FILE_KEY_SECURED_ACCESS_ID = (byte) 0x01; // file ID with key secured access
    // settings for key secured access depend on RadioButtons rbFileFreeAccess, rbFileKeySecuredAccess
    // key 0 is the  Application Master Key
    private final byte ACCESS_RIGHTS_RW_CAR_FREE = (byte) 0xEE; // Read&Write Access (free) & ChangeAccessRights (free)
    private final byte ACCESS_RIGHTS_R_W_FREE = (byte) 0xEE; // Read Access (free) & Write Access (free)
    private final byte ACCESS_RIGHTS_RW_CAR_SECURED = (byte) 0x12; // Read&Write Access (key 01) & ChangeAccessRights (key 02)
    private final byte ACCESS_RIGHTS_R_W_SECURED = (byte) 0x34; // Read Access (key 03) & Write Access (key 04)
    private int MAXIMUM_FILE_SIZE = 32; // do not increase this value to avoid framing !

    /**
     * section for predefined EV2 authentication file numbers
     */

    private final byte STANDARD_FILE_ENCRYPTED_NUMBER = (byte) 0x03;
    private final byte CYCLIC_RECORD_FILE_ENCRYPTED_NUMBER = (byte) 0x05;
    private final byte TRANSACTION_MAC_FILE_NUMBER = (byte) 0x0F;

    /**
     * section for application keys
     */

    private final byte[] APPLICATION_KEY_MASTER_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    private final byte[] APPLICATION_KEY_MASTER_AES = Utils.hexStringToByteArray("A08899AABBCCDD223344556677889911");
    private final byte APPLICATION_KEY_MASTER_NUMBER = (byte) 0x00;

    private final byte[] APPLICATION_KEY_1_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    private final byte[] APPLICATION_KEY_1_AES = Utils.hexStringToByteArray("A1000000000000000000000000000000");
    private final byte APPLICATION_KEY_1_NUMBER = (byte) 0x01;
    private final byte[] APPLICATION_KEY_2_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    private final byte[] APPLICATION_KEY_2_AES = Utils.hexStringToByteArray("A2000000000000000000000000000000");
    private final byte APPLICATION_KEY_2_NUMBER = (byte) 0x02;
    private final byte[] APPLICATION_KEY_3_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    private final byte[] APPLICATION_KEY_3_AES = Utils.hexStringToByteArray("A3000000000000000000000000000000");
    private final byte APPLICATION_KEY_3_NUMBER = (byte) 0x03;
    private final byte[] APPLICATION_KEY_4_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000"); // default AES key with 16 nulls
    private final byte[] APPLICATION_KEY_4_AES = Utils.hexStringToByteArray("A4000000000000000000000000000000");
    private final byte APPLICATION_KEY_4_NUMBER = (byte) 0x04;

    // see Mifare DESFire Light Features and Hints AN12343.pdf, page 83-84
    private final byte[] TRANSACTION_MAC_KEY_AES = Utils.hexStringToByteArray("F7D23E0C44AFADE542BFDF2DC5C6AE02"); // taken from Mifare DESFire Light Features and Hints AN12343.pdf, pages 83-84

    /**
     * section for commands and responses
     */

    private final byte CREATE_APPLICATION_COMMAND = (byte) 0xCA;
    private final byte SELECT_APPLICATION_COMMAND = (byte) 0x5A;
    private final byte CREATE_STANDARD_FILE_COMMAND = (byte) 0xCD;
    private final byte READ_STANDARD_FILE_COMMAND = (byte) 0xBD;
    private final byte WRITE_STANDARD_FILE_COMMAND = (byte) 0x3D;
    private final byte GET_FILE_SETTINGS_COMMAND = (byte) 0xF5;
    private final byte CHANGE_FILE_SETTINGS_COMMAND = (byte) 0x5F;
    private final byte CHANGE_KEY_COMMAND = (byte) 0xC4;


    private final byte MORE_DATA_COMMAND = (byte) 0xAF;

    private final byte APPLICATION_CRYPTO_DES = 0x00; // add this to number of keys for DES
    private final byte APPLICATION_CRYPTO_AES = (byte) 0x80; // add this to number of keys for AES

    private final byte GET_CARD_UID_COMMAND = (byte) 0x51;
    private final byte GET_VERSION_COMMAND = (byte) 0x60;

    private final byte[] RESPONSE_OK = new byte[]{(byte) 0x91, (byte) 0x00};
    private final byte[] RESPONSE_AUTHENTICATION_ERROR = new byte[]{(byte) 0x91, (byte) 0xAE};
    private final byte[] RESPONSE_MORE_DATA_AVAILABLE = new byte[]{(byte) 0x91, (byte) 0xAF};
    private final byte[] RESPONSE_FAILURE = new byte[]{(byte) 0x91, (byte) 0xFF};

    /**
     * general constants
     */

    int COLOR_GREEN = Color.rgb(0, 255, 0);
    int COLOR_RED = Color.rgb(255, 0, 0);
    private final String outputDivider = "--------------";

    // variables for NFC handling

    private NfcAdapter mNfcAdapter;
    //private CommunicationAdapter adapter;
    private IcodeSlixMethods icodeSlixMethods;


    private IsoDep isoDep;
    private byte[] tagIdByte;
    private byte dsfId;
    private byte[] uid;

    private String exportString = "Desfire Authenticate Legacy"; // takes the log data for export
    private String exportStringFileName = "auth.html"; // takes the log data for export
    public static String NDEF_BACKEND_URL = "https://sdm.nfcdeveloper.com/tag"; // filled by writeStandardFile2

    // DesfireAuthentication is used for all authentication tasks. The constructor needs the isoDep object so it is initialized in 'onTagDiscovered'
    DesfireAuthenticate desfireAuthenticate;

    // DesfireAuthenticationProximity is used for old DES d40 authenticate tasks. The constructor needs the isoDep object so it is initialized in 'onTagDiscovered'
    //DesfireAuthenticateProximity desfireAuthenticateProximity;
    DesfireAuthenticateLegacy desfireAuthenticateLegacy;
    DesfireAuthenticateEv2 desfireAuthenticateEv2;

    private Activity activity;
    private Ntag424DnaMethods ntag424DnaMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        output = findViewById(R.id.etOutput);
        errorCode = findViewById(R.id.etErrorCode);
        errorCodeLayout = findViewById(R.id.etErrorCodeLayout);

        // application handling
        applicationSelect = findViewById(R.id.btnSelectApplication);
        applicationSelected = findViewById(R.id.etSelectedApplicationId);
        numberOfKeys = findViewById(R.id.etNumberOfKeys);
        applicationId = findViewById(R.id.etApplicationId);


        // file handling

        fileList = findViewById(R.id.btnListFiles); // this is misused for getSesAuthEncKey
        fileSelect = findViewById(R.id.btnSelectFile);
        getFileSettings = findViewById(R.id.btnGetFileSettings);
        getFileSettingsMac = findViewById(R.id.btnGetFileSettingsMac);
        changeFileSettings = findViewById(R.id.btnChangeFileSettings);
        fileSelected = findViewById(R.id.etSelectedFileId);
        rbFileFreeAccess = findViewById(R.id.rbFileAccessTypeFreeAccess);
        rbFileKeySecuredAccess = findViewById(R.id.rbFileAccessTypeKeySecuredAccess);

        // section for EV2 auth & communication

        selectApplicationEv2 = findViewById(R.id.btnSelectApplicationEv2);
        getAllFileIdsEv2 = findViewById(R.id.btnGetAllFileIdsEv2);
        getAllFileSettingsEv2 = findViewById(R.id.btnGetAllFileSettingsEv2);
        completeFileSettingsEv2 = findViewById(R.id.btnCompleteFileSettingsEv2); // run all 3 commands above

        authD0AEv2 = findViewById(R.id.btnAuthD0AEv2);
        authD2AEv2 = findViewById(R.id.btnAuthD2AEv2);
        authD3AEv2 = findViewById(R.id.btnAuthD3AEv2);
        authD3ACEv2 = findViewById(R.id.btnAuthD3ACEv2);
        getCardUidEv2 = findViewById(R.id.btnGetCardUidEv2);
        getFileSettingsEv2 = findViewById(R.id.btnGetFileSettingsEv2);

        testEnableLrpMode = findViewById(R.id.btnTestLrpEnable);
        authD0LEv2 = findViewById(R.id.btnAuthD0LEv2);

        // methods for sdm
        createNdefFile256Ev2 = findViewById(R.id.btnCreateNdef256);
        sdmChangeFileSettingsEv2 = findViewById(R.id.btnSdmChangeFileSettings);
        sdmTestFileSettingsEv2 = findViewById(R.id.btnSdmTestFileSettings);
        sdmGetFileSettingsEv2 = findViewById(R.id.btnSdmGetFileSettingsEv2);
        sdmDecryptNdefManualEv2 = findViewById(R.id.btnSdmDecryptNdefManualEv2);

        //fileCreateEv2 = findViewById(R.id.btnCreateFilesEv2);

        fileStandardCreateEv2 = findViewById(R.id.btnCreateStandardFileEv2);
        fileStandardReadEv2 = findViewById(R.id.btnReadStandardFileEv2);
        fileStandardWriteEv2 = findViewById(R.id.btnWriteStandardFileEv2);

        fileCreateFileSetEnciphered = findViewById(R.id.btnCreateFileSetEncipheredEv2);

        changeKeyD3AtoD3AC = findViewById(R.id.btnChangeKeyD3AtoD3ACEv2); // change AES key 03 from DEFAULT to CHANGED
        changeKeyD3ACtoD3A = findViewById(R.id.btnChangeKeyD3ACtoD3AEv2); // change AES key 03 from CHANGED to DEFAULT
        enableTransactionTimerEv2 = findViewById(R.id.btnEnableTransactionTimerEv2);

        // standard files
        fileStandardRead = findViewById(R.id.btnReadStandardFile);
        fileStandardRead2 = findViewById(R.id.btnReadStandardFile2);
        fileStandardRead3 = findViewById(R.id.btnReadStandardFile3);
        fileStandardWrite3 = findViewById(R.id.btnWriteStandardFile3);
        fileStandardWrite2 = findViewById(R.id.btnWriteStandardFile2);
        fileStandardFileId = findViewById(R.id.etFileStandardFileId);
        fileStandardSize = findViewById(R.id.etFileStandardSize);
        fileStandardData = findViewById(R.id.etFileStandardData);

        // authentication handling DEFAULT keys
        authKey0D = findViewById(R.id.btnAuthKey0D);
        authKey1D = findViewById(R.id.btnAuthKey1D);
        authKey2D = findViewById(R.id.btnAuthKey2D);
        authKey3D = findViewById(R.id.btnAuthKey3D);
        authKey4D = findViewById(R.id.btnAuthKey4D);

        // authentication handling CHANGED keys
        authKey0C = findViewById(R.id.btnAuthKey0C);
        authKey1C = findViewById(R.id.btnAuthKey1C);
        authKey2C = findViewById(R.id.btnAuthKey2C);
        authKey3C = findViewById(R.id.btnAuthKey3C);
        authKey4C = findViewById(R.id.btnAuthKey4C);

        // key handling
        // change key from DEFAULT to CHANGED
        changeKey0ToC = findViewById(R.id.btnChangeKey0ToC);
        changeKey1ToC = findViewById(R.id.btnChangeKey1ToC);
        changeKey2ToC = findViewById(R.id.btnChangeKey2ToC);
        changeKey3ToC = findViewById(R.id.btnChangeKey3ToC);
        changeKey4ToC = findViewById(R.id.btnChangeKey4ToC);
        // change key from CHANGED to DEFAULT
        changeKey0ToD = findViewById(R.id.btnChangeKey0ToD);
        changeKey1ToD = findViewById(R.id.btnChangeKey1ToD);
        changeKey2ToD = findViewById(R.id.btnChangeKey2ToD);
        changeKey3ToD = findViewById(R.id.btnChangeKey3ToD);
        changeKey4ToD = findViewById(R.id.btnChangeKey4ToD);

        // general handling
        getCardUidDes = findViewById(R.id.btnGetCardUidDes);
        getCardUidAes = findViewById(R.id.btnGetCardUidAes);
        getTagVersion = findViewById(R.id.btnGetTagVersion);
        formatPicc = findViewById(R.id.btnFormatPicc);
        getKeyVersion = findViewById(R.id.btnGetKeyVersion);
        testGetSesAuthKeys = findViewById(R.id.btnTestGetSesAuthKeys);

        // visualize DES authentication
        selectApplicationDesVisualizing = findViewById(R.id.btnDesVisualizeAuthSelect);
        authDesVisualizing = findViewById(R.id.btnDesVisualizeAuthAuthenticate);
        authDesVisualizingC = findViewById(R.id.btnDesVisualizeAuthAuthenticateC);
        authDesVisualizingC2 = findViewById(R.id.btnDesVisualizeAuthAuthenticateC2);
        readDesVisualizing = findViewById(R.id.btnDesVisualizeAuthRead);
        writeDesVisualizing = findViewById(R.id.btnDesVisualizeAuthWrite);
        changeKeyDes00ToChangedDesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes00ToChanged);
        changeKeyDes00ToDefaultDesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes00ToDefault);
        changeKeyDes01ToChangedDesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes01ToChanged);
        changeKeyDes01ToDefaultDesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes01ToDefault);
        // this is for changing the CHANGED key to CHANGED 2 key and from CHANGED 2 key to DEFAULT key
        changeKeyDes01ToChanged2DesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes01ToChanged2);
        changeKeyDes01ToDefault2DesVisualizing = findViewById(R.id.btnDesVisualizeChangeKeyDes01ToDefault2);


        testNdefTemplate = findViewById(R.id.btnTestNdefTemplate);

        // some presets
        applicationId.setText(Utils.bytesToHexNpeUpperCase(APPLICATION_IDENTIFIER));
        numberOfKeys.setText(String.valueOf((int) APPLICATION_NUMBER_OF_KEYS));
        fileStandardFileId.setText(String.valueOf((int) STANDARD_FILE_FREE_ACCESS_ID)); // preset is FREE ACCESS

        activity = MainActivity.this;


        /**
         * just es quick test button
         */
        /*
        Button pad = findViewById(R.id.btncardUIDxx);
        pad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] fileSettings02 = desfireAuthenticateEv2.getFileSettingsEv2((byte) 0x02);
                Log.d(TAG, printData("fileSettings02", fileSettings02));

                byte[] fileSettings05 = desfireAuthenticateEv2.getFileSettingsEv2((byte) 0x05);
                Log.d(TAG, printData("fileSettings05", fileSettings05));

                byte[] fileSettings08 = desfireAuthenticateEv2.getFileSettingsEv2((byte) 0x08);
                Log.d(TAG, printData("fileSettings08", fileSettings08));

                byte[] fileSettings11 = desfireAuthenticateEv2.getFileSettingsEv2((byte) 0x0b);
                Log.d(TAG, printData("fileSettings11", fileSettings11));

                byte[] fileSettings14 = desfireAuthenticateEv2.getFileSettingsEv2((byte) 0x0e);
                Log.d(TAG, printData("fileSettings14", fileSettings14));
            }
        });
       */


        // hide soft keyboard from showing up on startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);


        formatPicc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the free memory on the tag
                clearOutputFields();
                String logString = "format the PICC";
                writeToUiAppend(output, logString);

                // open a confirmation dialog
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked

                                boolean success = desfireAuthenticateLegacy.formatPicc();
                                byte[] responseData = desfireAuthenticateLegacy.getErrorCode();
                                if (success) {
                                    writeToUiAppend(output, logString + " SUCCESS");
                                    writeToUiAppendBorderColor(errorCode, errorCodeLayout, logString + " SUCCESS", COLOR_GREEN);
                                    vibrateShort();
                                } else {
                                    writeToUiAppend(output, logString + " FAILURE with error " + EV3.getErrorCode(responseData));
                                    writeToUiAppendBorderColor(errorCode, errorCodeLayout, logString + " FAILURE with error code: " + Utils.bytesToHexNpeUpperCase(responseData), COLOR_RED);
                                }
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                // nothing to do
                                writeToUiAppend(output, "format of the PICC aborted");
                                break;
                        }
                    }
                };
                final String selectedFolderString = "You are going to format the PICC " + "\n\n" +
                        "Do you want to proceed ?";
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setMessage(selectedFolderString).setPositiveButton(android.R.string.yes, dialogClickListener)
                        .setNegativeButton(android.R.string.no, dialogClickListener)
                        .setTitle("FORMAT the PICC")
                        .show();
        /*
        If you want to use the "yes" "no" literals of the user's language you can use this
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
         */
            }
        });

    }


    private byte[] readFromAStandardFilePlainCommunicationDes(TextView logTextView, byte fileNumber, int fileSize, byte[] methodResponse) {
        final String methodName = "readStandardFilePlainCommunicationDes";
        Log.d(TAG, methodName);
        // sanity checks
        if (logTextView == null) {
            Log.e(TAG, methodName + " logTextView is NULL, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber < 0) {
            Log.e(TAG, methodName + " fileNumber is < 0, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber > 14) {
            Log.e(TAG, methodName + " fileNumber is > 14, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if ((fileSize < 0) || (fileSize > MAXIMUM_FILE_SIZE)) {
            Log.e(TAG, methodName + " fileSize has to be in range 0.." + MAXIMUM_FILE_SIZE + " but found " + fileSize + ", aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if ((isoDep == null) || (!isoDep.isConnected())) {
            writeToUiAppend(logTextView, methodName + " lost connection to the card, aborted");
            Log.e(TAG, methodName + " lost connection to the card, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        // generate the parameter
        int offsetBytes = 0; // read from the beginning
        byte[] offset = Utils.intTo3ByteArrayInversed(offsetBytes); // LSB order
        byte[] length = Utils.intTo3ByteArrayInversed(fileSize); // LSB order
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber);
        baos.write(offset, 0, 3);
        baos.write(length, 0, 3);
        byte[] parameter = baos.toByteArray();
        Log.d(TAG, methodName + printData(" parameter", parameter));
        byte[] response = new byte[0];
        byte[] apdu = new byte[0];
        try {
            apdu = wrapMessage(READ_STANDARD_FILE_COMMAND, parameter);
            Log.d(TAG, methodName + printData(" apdu", apdu));
            // sample: 90bd0000070000000020000000 (13 bytes)
            //       0x903D00002400000000000000
            response = isoDep.transceive(apdu);
            Log.d(TAG, methodName + printData(" response", response));
            // sample: 323032332e30372e32312031373a30343a3034203132333435363738393031329100 (34 bytes)
        } catch (IOException e) {
            Log.e(TAG, methodName + " transceive failed, IOException:\n" + e.getMessage());
            writeToUiAppend(logTextView, "transceive failed: " + e.getMessage());
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        byte[] responseBytes = returnStatusBytes(response);
        System.arraycopy(responseBytes, 0, methodResponse, 0, 2);
        if (checkResponse(response)) {
            Log.d(TAG, methodName + " SUCCESS");

            /*
            // for AES only - update the global IV
            // status NOT working
            // todo this is just for testing the IV "update" when getting the cardUid on AES
            byte[] cmacIv = calculateApduCMAC(apdu, SESSION_KEY_AES, IV.clone());
            writeToUiAppend(output, printData("cmacIv", cmacIv));
            IV = cmacIv.clone();
             */

            // now strip of the response bytes
            // if the card responses more data than expected we truncate the data
            int expectedResponse = fileSize - offsetBytes;
            if (response.length == expectedResponse) {
                return response;
            } else if (response.length > expectedResponse) {
                // more data is provided - truncated
                return Arrays.copyOf(response, expectedResponse);
            } else {
                // less data is provided - we return as much as possible
                return response;
            }
        } else {
            Log.d(TAG, methodName + " FAILURE with error code " + Utils.bytesToHexNpeUpperCase(responseBytes));
            Log.d(TAG, methodName + " error code: " + EV3.getErrorCode(responseBytes));
            return null;
        }
    }

    public byte[] getModifiedKey(byte[] key) {
        String methodName = "getModifiedKey";
        Log.d(TAG, methodName + printData(" key", key));
        if ((key == null) || (key.length != 8)) {
            Log.d(TAG, methodName + " Error: key is NULL or key length is not of 8 bytes length, aborted");
            return null;
        }
        byte[] modifiedKey = new byte[24];
        System.arraycopy(key, 0, modifiedKey, 16, 8);
        System.arraycopy(key, 0, modifiedKey, 8, 8);
        System.arraycopy(key, 0, modifiedKey, 0, key.length);
        Log.d(TAG, methodName + printData(" modifiedKey", modifiedKey));
        return modifiedKey;
    }

    // this is the code as readFromAStandardFilePlainCommunicationDes but we allow a fileNumber 15 (0x0F) for TMAC files
    private byte[] readFromAStandardFilePlainCommunication(TextView logTextView, byte fileNumber, int fileSize, byte[] methodResponse) {
        final String methodName = "createFilePlainCommunication";
        Log.d(TAG, methodName);
        // sanity checks
        if (logTextView == null) {
            Log.e(TAG, methodName + " logTextView is NULL, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber < 0) {
            Log.e(TAG, methodName + " fileNumber is < 0, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber > 15) {
            Log.e(TAG, methodName + " fileNumber is > 15, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        /*
        if ((fileSize < 1) || (fileSize > MAXIMUM_FILE_SIZE)) {
            Log.e(TAG, methodName + " fileSize has to be in range 1.." + MAXIMUM_FILE_SIZE + " but found " + fileSize + ", aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }

         */
        if ((isoDep == null) || (!isoDep.isConnected())) {
            writeToUiAppend(logTextView, methodName + " lost connection to the card, aborted");
            Log.e(TAG, methodName + " lost connection to the card, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        // generate the parameter
        int offsetBytes = 0; // read from the beginning
        byte[] offset = Utils.intTo3ByteArrayInversed(offsetBytes); // LSB order
        byte[] length = Utils.intTo3ByteArrayInversed(fileSize); // LSB order
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber);
        baos.write(offset, 0, 3);
        baos.write(length, 0, 3);
        byte[] parameter = baos.toByteArray();
        Log.d(TAG, methodName + printData(" parameter", parameter));
        byte[] response = new byte[0];
        byte[] apdu = new byte[0];
        try {
            apdu = wrapMessage(READ_STANDARD_FILE_COMMAND, parameter);
            Log.d(TAG, methodName + printData(" apdu", apdu));
            response = isoDep.transceive(apdu);
            Log.d(TAG, methodName + printData(" response", response));
        } catch (IOException e) {
            Log.e(TAG, methodName + " transceive failed, IOException:\n" + e.getMessage());
            writeToUiAppend(logTextView, "transceive failed: " + e.getMessage());
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        byte[] responseBytes = returnStatusBytes(response);
        System.arraycopy(responseBytes, 0, methodResponse, 0, 2);
        if (checkResponse(response)) {
            Log.d(TAG, methodName + " SUCCESS");
            // now strip of the response bytes
            // if the card responses more data than expected we truncate the data
            int expectedResponse = fileSize - offsetBytes;
            if (response.length == expectedResponse) {
                return response;
            } else if (response.length > expectedResponse) {
                // more data is provided - truncated
                return Arrays.copyOf(response, expectedResponse);
            } else {
                // less data is provided - we return as much as possible
                return response;
            }
        } else {
            Log.d(TAG, methodName + " FAILURE with error code " + Utils.bytesToHexNpeUpperCase(responseBytes));
            Log.d(TAG, methodName + " error code: " + EV3.getErrorCode(responseBytes));
            return null;
        }
    }

    private boolean writeToAStandardFilePlainCommunicationDes(TextView logTextView, byte fileNumber, byte[] data, byte[] methodResponse) {
        final String methodName = "writeToAStandardFilePlainCommunicationDes";
        Log.d(TAG, methodName);
        // sanity checks
        if (logTextView == null) {
            Log.e(TAG, methodName + " logTextView is NULL, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        if (fileNumber < 0) {
            Log.e(TAG, methodName + " fileNumber is < 0, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        if (fileNumber > 14) {
            Log.e(TAG, methodName + " fileNumber is > 14, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        if ((data == null) || (data.length < 1) || (data.length > selectedFileSize)) {
            Log.e(TAG, "data length not in range 1.." + MAXIMUM_FILE_SIZE + ", aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        if ((isoDep == null) || (!isoDep.isConnected())) {
            writeToUiAppend(logTextView, methodName + " lost connection to the card, aborted");
            Log.e(TAG, methodName + " lost connection to the card, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        // generate the parameter
        int numberOfBytes = data.length;
        int offsetBytes = 0; // write from the beginning
        byte[] offset = Utils.intTo3ByteArrayInversed(offsetBytes); // LSB order
        byte[] length = Utils.intTo3ByteArrayInversed(numberOfBytes); // LSB order
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber);
        baos.write(offset, 0, 3);
        baos.write(length, 0, 3);
        baos.write(data, 0, numberOfBytes);
        byte[] parameter = baos.toByteArray();
        Log.d(TAG, methodName + printData(" parameter", parameter));
        byte[] response = new byte[0];
        byte[] apdu = new byte[0];
        try {
            apdu = wrapMessage(WRITE_STANDARD_FILE_COMMAND, parameter);
            Log.d(TAG, methodName + printData(" apdu", apdu));
            // sample:  903d00002700000000200000323032332e30372e32312031373a30343a30342031323334353637383930313200 (45 bytes)
            response = isoDep.transceive(apdu);
            Log.d(TAG, methodName + printData(" response", response));
        } catch (IOException e) {
            Log.e(TAG, methodName + " transceive failed, IOException:\n" + e.getMessage());
            writeToUiAppend(logTextView, "transceive failed: " + e.getMessage());
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return false;
        }
        byte[] responseBytes = returnStatusBytes(response);
        System.arraycopy(responseBytes, 0, methodResponse, 0, 2);
        if (checkResponse(response)) {
            Log.d(TAG, methodName + " SUCCESS");
            return true;
        } else {
            return false;
        }
    }

    public byte[] getFileSettingsA(TextView logTextView, byte fileNumber, byte[] methodResponse) {
        final String methodName = "getFileSettings";
        Log.d(TAG, methodName);
        // sanity checks
        if (logTextView == null) {
            Log.e(TAG, methodName + " logTextView is NULL, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber < 0) {
            Log.e(TAG, methodName + " fileNumber is < 0, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        if (fileNumber > 14) {
            Log.e(TAG, methodName + " fileNumber is > 14, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        // generate the parameter
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber);
        byte[] parameter = baos.toByteArray();
        Log.d(TAG, methodName + printData(" parameter", parameter));
        byte[] response = new byte[0];
        byte[] apdu = new byte[0];
        try {
            apdu = wrapMessage(GET_FILE_SETTINGS_COMMAND, parameter);
            Log.d(TAG, methodName + printData(" apdu", apdu));
            response = isoDep.transceive(apdu);
            Log.d(TAG, methodName + printData(" response", response));
        } catch (IOException e) {
            Log.e(TAG, methodName + " transceive failed, IOException:\n" + e.getMessage());
            writeToUiAppend(logTextView, "transceive failed: " + e.getMessage());
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        byte[] responseBytes = returnStatusBytes(response);
        System.arraycopy(responseBytes, 0, methodResponse, 0, 2);
        if (checkResponse(response)) {
            Log.d(TAG, methodName + " SUCCESS");
            return Arrays.copyOf(response, response.length - 2);
        } else {
            Log.d(TAG, methodName + " FAILURE with error code " + Utils.bytesToHexNpeUpperCase(responseBytes));
            Log.d(TAG, methodName + " error code: " + EV3.getErrorCode(responseBytes));
            return null;
        }
    }


    /*
    private boolean changeFileSettingsA(TextView logTextView, byte fileNumber, byte[] methodResponse) {
        // NOTE: don't forget to authenticate with CAR key

        if (SESSION_KEY_DES == null) {
            writeToUiAppend(logTextView, "the SESSION KEY DES is null, did you forget to authenticate with a CAR key first ?");
            return false;
        }

        int selectedFileIdInt = Integer.parseInt(selectedFileId);
        byte selectedFileIdByte = Byte.parseByte(selectedFileId);
        Log.d(TAG, "changeTheFileSettings for selectedFileId " + selectedFileIdInt);
        Log.d(TAG, printData("DES session key", SESSION_KEY_DES));

        // CD | File No | Comms setting byte | Access rights (2 bytes) | File size (3 bytes)
        byte commSettingsByte = 0; // plain communication without any encryption

        // we are changing the keys for R and W from 0x34 to 0x22;
        byte accessRightsRwCar = (byte) 0x12; // Read&Write Access & ChangeAccessRights
        //byte accessRightsRW = (byte) 0x34; // Read Access & Write Access // read with key 3, write with key 4
        byte accessRightsRW = (byte) 0x22; // Read Access & Write Access // read with key 2, write with key 2
        // to calculate the crc16 over the setting bytes we need a 3 byte long array
        byte[] bytesForCrc = new byte[3];
        bytesForCrc[0] = commSettingsByte;
        bytesForCrc[1] = accessRightsRwCar;
        bytesForCrc[2] = accessRightsRW;
        Log.d(TAG, printData("bytesForCrc", bytesForCrc));
        byte[] crc16Value = CRC16.get(bytesForCrc);
        Log.d(TAG, printData("crc16Value", crc16Value));
        // create a 8 byte long array
        byte[] bytesForDecryption = new byte[8];
        System.arraycopy(bytesForCrc, 0, bytesForDecryption, 0, 3);
        System.arraycopy(crc16Value, 0, bytesForDecryption, 3, 2);
        Log.d(TAG, printData("bytesForDecryption", bytesForDecryption));
        // generate 24 bytes long triple des key
        byte[] tripleDES_SESSION_KEY = getModifiedKey(SESSION_KEY_DES);
        Log.d(TAG, printData("tripleDES Session Key", tripleDES_SESSION_KEY));
        byte[] IV_DES = new byte[8];
        Log.d(TAG, printData("IV_DES", IV_DES));
        byte[] decryptedData = TripleDES.decrypt(IV_DES, tripleDES_SESSION_KEY, bytesForDecryption);
        Log.d(TAG, printData("decryptedData", decryptedData));
        // the parameter for wrapping
        byte[] parameter = new byte[9];
        parameter[0] = selectedFileIdByte;
        System.arraycopy(decryptedData, 0, parameter, 1, 8);
        Log.d(TAG, printData("parameter", parameter));
        byte[] wrappedCommand;
        byte[] response;
        try {
            wrappedCommand = wrapMessage(CHANGE_FILE_SETTINGS_COMMAND, parameter);
            Log.d(TAG, printData("wrappedCommand", wrappedCommand));
            response = isoDep.transceive(wrappedCommand);
            Log.d(TAG, printData("response", response));
            System.arraycopy(response, 0, methodResponse, 0, 2);
            if (checkResponse(response)) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            writeToUiAppend(output, "IOException: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

     */

/**
 * section for key handling
 */


    /**
     * section for general handling
     */

    private byte[] getCardUid(TextView logTextView, byte[] methodResponse) {
        final String methodName = "getCardUid";
        Log.d(TAG, methodName);
        // sanity checks
        if (logTextView == null) {
            Log.e(TAG, methodName + " logTextView is NULL, aborted");
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        // no parameter
        byte[] response = new byte[0];
        byte[] apdu = new byte[0];
        try {
            apdu = wrapMessage(GET_CARD_UID_COMMAND, null);
            Log.d(TAG, methodName + printData(" apdu", apdu));
            response = isoDep.transceive(apdu);
            Log.d(TAG, methodName + printData(" response", response));
        } catch (IOException e) {
            Log.e(TAG, methodName + " transceive failed, IOException:\n" + e.getMessage());
            writeToUiAppend(logTextView, "transceive failed: " + e.getMessage());
            System.arraycopy(RESPONSE_FAILURE, 0, methodResponse, 0, 2);
            return null;
        }
        byte[] responseBytes = returnStatusBytes(response);
        System.arraycopy(responseBytes, 0, methodResponse, 0, 2);
        if (checkResponse(response)) {
            Log.d(TAG, methodName + " SUCCESS");
            return Arrays.copyOf(response, response.length - 2);
        } else {
            Log.d(TAG, methodName + " FAILURE with error code " + Utils.bytesToHexNpeUpperCase(responseBytes));
            Log.d(TAG, methodName + " error code: " + EV3.getErrorCode(responseBytes));
            return null;
        }
    }


    /**
     * copied from DESFireEV1.java class
     * necessary for calculation the new IV for decryption of getCardUid
     *
     * @param apdu
     * @param sessionKey
     * @param iv
     * @return
     */
    private byte[] calculateApduCMAC(byte[] apdu, byte[] sessionKey, byte[] iv) {
        Log.d(TAG, "calculateApduCMAC" + printData(" apdu", apdu) +
                printData(" sessionKey", sessionKey) + printData(" iv", iv));
        byte[] block;

        if (apdu.length == 5) {
            block = new byte[apdu.length - 4];
        } else {
            // trailing 00h exists
            block = new byte[apdu.length - 5];
            System.arraycopy(apdu, 5, block, 1, apdu.length - 6);
        }
        block[0] = apdu[1];
        Log.d(TAG, "calculateApduCMAC" + printData(" block", block));
        //byte[] newIv = desfireAuthenticateProximity.calculateDiverseKey(sessionKey, iv);
        //return newIv;
        byte[] cmacIv = CMAC.get(CMAC.Type.AES, sessionKey, block, iv);
        Log.d(TAG, "calculateApduCMAC" + printData(" cmacIv", cmacIv));
        return cmacIv;
    }

    private static byte[] calculateApduCRC32R(byte[] apdu, int length) {
        byte[] data = new byte[length + 1];
        System.arraycopy(apdu, 0, data, 0, length);// response code is at the end
        return CRC32.get(data);
    }


    /**
     * section for command and response handling
     */

    private byte[] wrapMessage(byte command, byte[] parameters) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write((byte) 0x90);
        stream.write(command);
        stream.write((byte) 0x00);
        stream.write((byte) 0x00);
        if (parameters != null) {
            stream.write((byte) parameters.length);
            stream.write(parameters);
        }
        stream.write((byte) 0x00);
        return stream.toByteArray();
    }

    private byte[] returnStatusBytes(byte[] data) {
        return Arrays.copyOfRange(data, (data.length - 2), data.length);
    }

    /**
     * checks if the response has an 0x'9100' at the end means success
     * and the method returns the data without 0x'9100' at the end
     * if any other trailing bytes show up the method returns false
     *
     * @param data
     * @return
     */
    private boolean checkResponse(byte[] data) {
        if (data == null) return false;
        // simple sanity check
        if (data.length < 2) {
            return false;
        } // not ok
        if (Arrays.equals(RESPONSE_OK, returnStatusBytes(data))) {
            return true;
        } else {
            return false;
        }
        /*
        int status = ((0xff & data[data.length - 2]) << 8) | (0xff & data[data.length - 1]);
        if (status == 0x9100) {
            return true;
        } else {
            return false;
        }
         */
    }

    /**
     * checks if the response has an 0x'91AF' at the end means success
     * but there are more data frames available
     * if any other trailing bytes show up the method returns false
     *
     * @param data
     * @return
     */
    private boolean checkResponseMoreData(@NonNull byte[] data) {
        // simple sanity check
        if (data.length < 2) {
            return false;
        } // not ok
        if (Arrays.equals(RESPONSE_MORE_DATA_AVAILABLE, returnStatusBytes(data))) {
            return true;
        } else {
            return false;
        }
        /*
        int status = ((0xff & data[data.length - 2]) << 8) | (0xff & data[data.length - 1]);
        if (status == 0x91AF) {
            return true;
        } else {
            return false;
        }
         */
    }

    /**
     * checks if the response has an 0x'91AE' at the end means
     * that an authentication with an appropriate key is missing
     * if any other trailing bytes show up the method returns false
     *
     * @param data
     * @return
     */
    private boolean checkAuthenticationError(@NonNull byte[] data) {
        // simple sanity check
        if (data.length < 2) {
            return false;
        } // not ok
        if (Arrays.equals(RESPONSE_AUTHENTICATION_ERROR, returnStatusBytes(data))) {
            return true;
        } else {
            return false;
        }
        /*
        int status = ((0xff & data[data.length - 2]) << 8) | (0xff & data[data.length - 1]);
        if (status == 0x91AE) {
            return true;
        } else {
            return false;
        }
         */
    }

    /**
     * section for NFC handling
     */

// This method is run in another thread when a card is discovered
// !!!! This method cannot cannot direct interact with the UI Thread
// Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {

        clearOutputFields();
        invalidateAllSelections();
        writeToUiAppend(output, "NFC tag discovered");

        icodeSlixMethods = new IcodeSlixMethods(tag, activity, output);

        byte[] response;
        boolean success;

        /*
        writeToUiAppend(output, outputDivider);
        response = icodeSlixMethods.getInventory();
        writeToUiAppend(output, printData("getInventory", response));
        Inventory inventory = new Inventory(response);
        writeToUiAppend(output, "inventory:\n" + inventory.dump());
         */

        uid = icodeSlixMethods.getTagUid();
        dsfId = icodeSlixMethods.getDsfId();

        writeToUiAppend(output, outputDivider);
        Inventory inventory = new Inventory(dsfId, uid);
        writeToUiAppend(output, inventory.dump());

        writeToUiAppend(output, outputDivider);
        response = icodeSlixMethods.readSingleBlock(0);
        writeToUiAppend(output, printData("readSingleBlock 00", response));

        int startBlockNumber = 0;
        int numberOfBlocks = 28;
        byte[] multipleBlocksSecurityStatus = getMultipleBlocksSecurityStatus(startBlockNumber, numberOfBlocks);

/*
        byte[] defaultPassword = Utils.hexStringToByteArray("00000000");
        setPasswordEasAfi(defaultPassword);
        passwordProtectAfi();
*/
/*
        byte[] defaultPassword = Utils.hexStringToByteArray("00000000");
        byte[] easAfiPassword = Utils.hexStringToByteArray("12345678");
        setPasswordEasAfi(easAfiPassword);
        writePasswordEasAfi(defaultPassword);
        //writePasswordEasAfi(easAfiPassword);
*/

        byte[] defaultPassword = Utils.hexStringToByteArray("00000000");
        setPasswordEasAfi(defaultPassword);
        byte afi = (byte) 0x06;
        writeAfi(afi);
/*
        byte[] defaultPassword = Utils.hexStringToByteArray("00000000");
        setPasswordEasAfi(defaultPassword);

        passwordProtectAfi();
 */

/*
        byte[] defaultPassword = Utils.hexStringToByteArray("00000000");
        byte[] easAfiPassword = Utils.hexStringToByteArray("12345678");
        setPasswordEasAfi(defaultPassword);

        //byte afi = (byte) 0x01;
        byte afi = (byte) 0x00;
        //byte afi = (byte) 0xAE;
        writeAfi(afi);

        //dsfId = (byte) 0x02;
        byte dsfId = (byte) 0x00;
        //byte dsfId = (byte) 0xD1;
        writeDfsId(dsfId);

        int blockNumber = 0;
        byte[] dataBlock00 = readSingleBlock(blockNumber);

        byte[] dataBlocks00To27 = readMultipleBlocks(blockNumber, numberOfBlocks);

        byte[] data = "ABCD".getBytes(StandardCharsets.UTF_8);
        writeSingleBlock(1, data);

        setEas();
        resetEas();
        easAlarm();
*/

        writeToUiAppend(output, outputDivider);
        // playing with flags
        Iso15693Flags iFlags1 = new Iso15693Flags(false, true, false, false);
        iFlags1.setInventory0Flags(false, false, false, false);
        byte iFlags1Byte = iFlags1.getFlagsByte();
        writeToUiAppend(output, "iFlags1: " + Utils.byteToHex(iFlags1Byte));
        // result 0x02

        // playing with flags
        Iso15693Flags iFlags2 = new Iso15693Flags(false, false, false, false);
        iFlags2.setInventory0Flags(false, true, false, false);
        byte iFlags2Byte = iFlags2.getFlagsByte();
        writeToUiAppend(output, "iFlags2: " + Utils.byteToHex(iFlags2Byte));
        // result 0x20

        // playing with flags
        Iso15693Flags iFlags4 = new Iso15693Flags(false, false, false, false);
        iFlags4.setInventory0Flags(false, true, true, false);
        byte iFlags4Byte = iFlags4.getFlagsByte();
        writeToUiAppend(output, "iFlags4: " + Utils.byteToHex(iFlags4Byte));
        // result 0x60

        // playing with flags for Inventory Read
        Iso15693Flags iFlags5 = new Iso15693Flags(false, false, true, false);
        iFlags5.setInventory1Flags(false, false, false, false);
        byte iFlags5Byte = iFlags5.getFlagsByte();
        writeToUiAppend(output, "iFlags5: " + Utils.byteToHex(iFlags5Byte));
        // result 04

        // playing with flags for Inventory Read with AFI
        Iso15693Flags iFlags6 = new Iso15693Flags(false, false, true, false);
        iFlags6.setInventory1Flags(true, false, false, false);
        byte iFlags6Byte = iFlags6.getFlagsByte();
        writeToUiAppend(output, "iFlags6: " + Utils.byteToHex(iFlags6Byte));
        // result 14

        // playing with flags
        Iso15693Flags iFlags3 = new Iso15693Flags(false, true, true, false);
        iFlags3.setInventory1Flags(false, true, false, false);
        byte iFlags3Byte = iFlags3.getFlagsByte();
        writeToUiAppend(output, "iFlags3: " + Utils.byteToHex(iFlags3Byte));
        // result 0x26

        writeToUiAppend(output, outputDivider);
        //byte afi = (byte) 0x03;

        int startBlock = 0;
        int numberOfBlocksByte = 28;
        inventoryRead(startBlock, numberOfBlocksByte);

        //response = icodeSlixMethods.inventoryRead(startBlock, numberOfBlocksByte);
        //response = icodeSlixMethods.inventoryRead(afi, startBlock, numberOfBlocksByte);
        //response = icodeSlixMethods.inventoryRead(afi, (byte) 0x00, (byte) 0x00, startBlock, numberOfBlocksByte);
        //writeToUiAppend(output, printData("inventoryRead\n", response));

        //response = icodeSlixMethods.fastInventoryRead(startBlock, numberOfBlocksByte);
        //writeToUiAppend(output, printData("fastInventoryRead\n", response));

/*
        writeToUiAppend(output, outputDivider);
        byte maskLength = (byte) (0x01);
        byte maskValue = (byte) (0xff);
        byte firstBlock = (byte) 0x00;
        byte numberOfBlocks = (byte) 0x01;
        response = icodeSlixMethods.inventoryRead(afi, maskLength, maskValue, firstBlock, numberOfBlocks);
        writeToUiAppend(output, printData("inventoryRead\n", response));

 */


        //writeSingleBlock(data);

        // formatTagNdef();

    }


    private byte[] getMultipleBlocksSecurityStatus(int blockNumber, int numberOfBlocks) {
        System.out.println("blockNumber: " + blockNumber + " numberOfBlocks: " + numberOfBlocks);
        System.out.println("icodeSlixMethods: " + icodeSlixMethods.toString());
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.getMultipleBlockSecurityStatus(blockNumber, numberOfBlocks);
        writeToUiAppend(output, printData("getMultipleBlockSecurityStatus 00-27\n", response));
        return response;
    }

    private byte[] readSingleBlock(int blockNumber) {
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.readSingleBlock(0);
        writeToUiAppend(output, printData("readSingleBlock 00", response));
        return response;
    }

    private byte[] readMultipleBlocks(int blockNumber, int numberOfBlocks) {
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.readMultipleBlocks(0, 28);
        writeToUiAppend(output, printData("readMultipleBlocks from " + blockNumber + " read " + numberOfBlocks + " blocks", response));
        return response;
    }

    private boolean writeAfi(byte afi) {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeAfi(afi);
        writeToUiAppend(output, "writeAfi: " + success);
        return success;
    }

    private boolean writeDfsId(byte dfsId) {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeDsfId(dsfId);
        writeToUiAppend(output, "writeDsfId: " + success);
        return success;
    }

    private boolean writeSingleBlock(int blockNumber, byte[] data) {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeSingleBlock(blockNumber, data);
        writeToUiAppend(output, "writeBlock 00: " + success);
        return success;
    }

    private boolean setPasswordEasAfi(byte[] password) {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.setPasswordEasAfi(password);
        writeToUiAppend(output, "setPasswordEasAfi: " + success);
        return success;
    }

    private boolean writePasswordEasAfi(byte[] password) {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writePasswordEasAfi(password);
        writeToUiAppend(output, "writePasswordEasAfi: " + success);
        return success;
    }

    private boolean passwordProtectAfi() {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.passwordProtectAfi();
        writeToUiAppend(output, "passwordProtectAfi: " + success);
        return success;
    }

    private boolean passwordProtectEas() {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.passwordProtectEas();
        writeToUiAppend(output, "passwordProtectEas: " + success);
        return success;
    }

    private boolean setEas() {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.setEas();
        writeToUiAppend(output, "setEas: " + success);
        return success;
    }

    private boolean resetEas() {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.resetEas();
        writeToUiAppend(output, "resetEas: " + success);
        return success;
    }

    private byte[] easAlarm() {
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.easAlarm();
        writeToUiAppend(output, printData("easAlarm\n", response));
        return response;
        // easAlarm length: 32 data: 2fb36270d5a7907fe8b18038d281497682da9a866faf8bb0f19cd112a57237ef
    }

    private byte[] inventoryRead(int firstBlockNumber, int numberOfBlocks) {
        byte[] response = icodeSlixMethods.inventoryRead(firstBlockNumber, numberOfBlocks);
        writeToUiAppend(output, printData("inventoryRead\n", response));
        return response;
    }

    private boolean formatTagNdef() {
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.formatTagNdef();
        writeToUiAppend(output, "formatTagNde: " + success);
        return success;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }


    /**
     * section for layout handling
     */
    private void allLayoutsInvisible() {
        //llApplicationHandling.setVisibility(View.GONE);
        //llStandardFile.setVisibility(View.GONE);
    }

    /**
     * section for UI handling
     */

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
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

    private void writeToUiAppendBorderColor(TextView textView, TextInputLayout textInputLayout, String message, int color) {
        runOnUiThread(() -> {

            // set the color to green
            //Color from rgb
            // int color = Color.rgb(255,0,0); // red
            //int color = Color.rgb(0,255,0); // green
            //Color from hex string
            //int color2 = Color.parseColor("#FF11AA"); light blue
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused}, // focused
                    new int[]{android.R.attr.state_hovered}, // hovered
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{}  //
            };
            int[] colors = new int[]{
                    color,
                    color,
                    color,
                    //color2
                    color
            };
            ColorStateList myColorList = new ColorStateList(states, colors);
            textInputLayout.setBoxStrokeColorStateList(myColorList);

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

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void clearOutputFields() {
        runOnUiThread(() -> {
            output.setText("");
            errorCode.setText("");
        });
        // reset the border color to primary for errorCode
        int color = R.color.colorPrimary;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused}, // focused
                new int[]{android.R.attr.state_hovered}, // hovered
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{}  //
        };
        int[] colors = new int[]{
                color,
                color,
                color,
                color
        };
        ColorStateList myColorList = new ColorStateList(states, colors);
        errorCodeLayout.setBoxStrokeColorStateList(myColorList);
    }


    private void invalidateAllSelections() {
        selectedApplicationId = null;
        selectedFileId = "";
        runOnUiThread(() -> {
            applicationSelected.setText("");
            fileSelected.setText("");
        });
        KEY_NUMBER_USED_FOR_AUTHENTICATION = -1;
    }

    private void invalidateEncryptionKeys() {
        KEY_NUMBER_USED_FOR_AUTHENTICATION = -1;
    }

    private void vibrateShort() {
        // Make a Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50, 10));
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    /**
     * section OptionsMenu export text file methods
     */

    private void exportTextFile() {
        //provideTextViewDataForExport(etLog);
        if (TextUtils.isEmpty(exportString)) {
            writeToUiToast("Log some data before writing files :-)");
            return;
        }
        writeStringToExternalSharedStorage();
    }

    private void writeStringToExternalSharedStorage() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // boolean pickerInitialUri = false;
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // get filename from edittext
        String filename = exportStringFileName;
        // sanity check
        if (filename.equals("")) {
            writeToUiToast("scan a tag before writing the content to a file :-)");
            return;
        }
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        selectTextFileActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> selectTextFileActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                // get file content from edittext
                                String fileContent = exportString;
                                System.out.println("## data to write: " + exportString);
                                writeTextToUri(uri, fileContent);
                                writeToUiToast("file written to external shared storage: " + uri.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                                writeToUiToast("ERROR: " + e.toString());
                                return;
                            }
                        }
                    }
                }
            });

    private void writeTextToUri(Uri uri, String data) throws IOException {
        try {
            System.out.println("** data to write: " + data);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            System.out.println("Exception File write failed: " + e.toString());
        }
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mApplications = menu.findItem(R.id.action_applications);
        mApplications.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                allLayoutsInvisible();
                //llApplicationHandling.setVisibility(View.VISIBLE);
                return false;
            }
        });

        MenuItem mStandardFile = menu.findItem(R.id.action_standard_file);
        mStandardFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                allLayoutsInvisible();
                //llStandardFile.setVisibility(View.VISIBLE);
                return false;
            }
        });

        MenuItem mExportTextFile = menu.findItem(R.id.action_export_text_file);
        mExportTextFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG, "mExportTextFile");
                exportTextFile();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    public void showDialog(Activity activity, String msg) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.logdata);
        TextView text = dialog.findViewById(R.id.tvLogData);
        //text.setMovementMethod(new ScrollingMovementMethod());
        text.setText(msg);
        Button dialogButton = dialog.findViewById(R.id.btnLogDataOk);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}