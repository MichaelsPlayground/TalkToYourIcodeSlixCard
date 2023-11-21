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

    /**
     * section for block operations
     */

    Button btnReadSingleBlock, btnReadMultipleBlocks,
            btnWriteSingleBlock, btnWriteMultipleBlocks,
            btnLockBlock, btnGetMultipleBlockSecurityStatus;
    private com.google.android.material.textfield.TextInputEditText etBlockNumber, etNumberOfBlocks, etDataToWrite;



    /**
     * section general
     */
    private com.google.android.material.textfield.TextInputEditText output, errorCode;
    private com.google.android.material.textfield.TextInputLayout errorCodeLayout;


    /**
     * general constants
     */

    int COLOR_GREEN = Color.rgb(0, 255, 0);
    int COLOR_RED = Color.rgb(255, 0, 0);
    private final String outputDivider = "--------------";

    // variables for NFC handling

    private NfcAdapter mNfcAdapter;
    private IcodeSlixMethods icodeSlixMethods;

    private byte dsfId;
    private byte[] uid;

    private String exportString = "Desfire Authenticate Legacy"; // takes the log data for export
    private String exportStringFileName = "auth.html"; // takes the log data for export

    private Activity activity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        // block operations section
        btnReadSingleBlock = findViewById(R.id.btnReadSingleBlock);
        btnReadMultipleBlocks = findViewById(R.id.btnReadMultipleBlocks);
        btnWriteSingleBlock = findViewById(R.id.btnWriteSingleBlock);
        btnWriteMultipleBlocks = findViewById(R.id.btnWriteMultipleBlocks);
        btnLockBlock = findViewById(R.id.btnLockBlock);
        btnGetMultipleBlockSecurityStatus = findViewById(R.id.btnGetMultipleBlockSecurityStatus);
        etBlockNumber = findViewById(R.id.etBlockNumber);
        etNumberOfBlocks = findViewById(R.id.etNumberOfBlocks);

        // general section
        output = findViewById(R.id.etOutput);
        errorCode = findViewById(R.id.etErrorCode);
        errorCodeLayout = findViewById(R.id.etErrorCodeLayout);

        activity = MainActivity.this;

        // hide soft keyboard from showing up on startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        btnReadSingleBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSingleBlock(Integer.parseInt(etBlockNumber.getText().toString()));
            }
        });

        btnReadMultipleBlocks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readMultipleBlocks(Integer.parseInt(etBlockNumber.getText().toString()), Integer.parseInt(etNumberOfBlocks.getText().toString()));
            }
        });

        btnWriteSingleBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeSingleBlock(Integer.parseInt(etBlockNumber.getText().toString()), etDataToWrite.getText().toString().getBytes(StandardCharsets.UTF_8));
            }
        });

        btnWriteMultipleBlocks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeMultipleBlocks(Integer.parseInt(etBlockNumber.getText().toString()), etDataToWrite.getText().toString().getBytes(StandardCharsets.UTF_8));
            }
        });

/*
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

                                boolean success = true;
                                byte[] responseData = null;
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

            }
        });
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
        byte afi = (byte) 0x00;
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
*/
        //dsfId = (byte) 0x02;
        byte dsfId = (byte) 0x00;
        //byte dsfId = (byte) 0xD1;
        writeDfsId(dsfId);
/*
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
        Iso15693Flags iFlags4b = new Iso15693Flags(iFlags4Byte);
        writeToUiAppend(output, "iFlags4b:\n" + iFlags4b.dump());



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

        byte[] testData1 = Utils.generateTestData(8);
        writeMultipleBlocks(0, testData1);

        byte[] testData2 = Utils.generateTestData(9);
        writeMultipleBlocks(5, testData2);

        byte[] testData3 = Utils.generateTestData(112);
        writeMultipleBlocks(0, testData3);
/*
        success = icodeSlixMethods.lockDsfId();
        writeToUiAppend(output, "lockDsfId: " + success);
*/
        /*
        lockDfsId();
        lockAfi();
         */

        readMultipleBlocks(0, 28);

        //writeSingleBlock(data);

        // formatTagNdef();

    }

    /**
     * section for block operations (read, write and lock blocks)
     */

    private byte[] readSingleBlock(int blockNumber) {
        if (!checkValidTag()) return null;
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.readSingleBlock(blockNumber);
        writeToUiAppend(output, printData("readSingleBlock 00", response));
        if (icodeSlixMethods.getErrorCode() == IcodeSlixMethods.RESPONSE_OK) {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, icodeSlixMethods.getErrorCodeReason(), COLOR_GREEN);
        } else {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, icodeSlixMethods.getErrorCodeReason(), COLOR_RED);
        }
        return response;
    }

    private byte[] readMultipleBlocks(int blockNumber, int numberOfBlocks) {
        if (!checkValidTag()) return null;
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.readMultipleBlocks(blockNumber, numberOfBlocks);
        writeToUiAppend(output, printData("readMultipleBlocks from " + blockNumber + " read " + numberOfBlocks + " blocks", response));
        if (icodeSlixMethods.getErrorCode() == IcodeSlixMethods.RESPONSE_OK) {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, icodeSlixMethods.getErrorCodeReason(), COLOR_GREEN);
        } else {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, icodeSlixMethods.getErrorCodeReason(), COLOR_RED);
        }
        return response;
    }

    private boolean writeSingleBlock(int blockNumber, byte[] data) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeSingleBlock(blockNumber, data);
        writeToUiAppend(output, "writeBlock: " + success);
        return success;
    }

    private boolean writeMultipleBlocks(int blockNumber, byte[] data) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeMultipleBlocks(blockNumber, data);
        writeToUiAppend(output, "writeMultipleData success: " + success);
        return success;
    }

    private boolean lockBlock(int blockNumber) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.lockBlock(blockNumber);
        writeToUiAppend(output, "lockBlock: " + success);
        return success;
    }

    private byte[] getMultipleBlocksSecurityStatus(int blockNumber, int numberOfBlocks) {
        if (!checkValidTag()) return null;
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.getMultipleBlockSecurityStatus(blockNumber, numberOfBlocks);
        writeToUiAppend(output, "Block security status beginning with blockNumber " + blockNumber + " for " + numberOfBlocks + " blocks");
                writeToUiAppend(output, printData("getMultipleBlockSecurityStatus\n", response));
        return response;
    }

    /**
     * section for dsfid
     */

    private boolean writeDfsId(byte dfsId) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeDsfId(dsfId);
        writeToUiAppend(output, "writeDsfId: " + success);
        return success;
    }

    private boolean lockDfsId() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.lockDsfId();
        writeToUiAppend(output, "lockDsfId: " + success);
        return success;
    }

    /**
     * section for afi
     */

    private boolean writeAfi(byte afi) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writeAfi(afi);
        writeToUiAppend(output, "writeAfi: " + success);
        return success;
    }

    private boolean lockAfi() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.lockAfi();
        writeToUiAppend(output, "lockAfi: " + success);
        return success;
    }

    /**
     * section for eas
     */

    private boolean setEas() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.setEas();
        writeToUiAppend(output, "setEas: " + success);
        return success;
    }

    private boolean resetEas() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.resetEas();
        writeToUiAppend(output, "resetEas: " + success);
        return success;
    }

    private byte[] easAlarm() {
        if (!checkValidTag()) return null;
        writeToUiAppend(output, outputDivider);
        byte[] response = icodeSlixMethods.easAlarm();
        writeToUiAppend(output, printData("easAlarm\n", response));
        return response;
        // easAlarm length: 32 data: 2fb36270d5a7907fe8b18038d281497682da9a866faf8bb0f19cd112a57237ef
    }

    /**
     * section for password protection
     */

    private boolean setPasswordEasAfi(byte[] password) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.setPasswordEasAfi(password);
        writeToUiAppend(output, "setPasswordEasAfi: " + success);
        return success;
    }

    private boolean writePasswordEasAfi(byte[] password) {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.writePasswordEasAfi(password);
        writeToUiAppend(output, "writePasswordEasAfi: " + success);
        return success;
    }

    private boolean passwordProtectAfi() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.passwordProtectAfi();
        writeToUiAppend(output, "passwordProtectAfi: " + success);
        return success;
    }

    private boolean passwordProtectEas() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.passwordProtectEas();
        writeToUiAppend(output, "passwordProtectEas: " + success);
        return success;
    }

    /**
     * section for general
     */

    private byte[] inventoryRead(int firstBlockNumber, int numberOfBlocks) {
        if (!checkValidTag()) return null;
        byte[] response = icodeSlixMethods.inventoryRead(firstBlockNumber, numberOfBlocks);
        writeToUiAppend(output, printData("inventoryRead\n", response));
        return response;
    }

    private boolean formatTagNdef() {
        if (!checkValidTag()) return false;
        writeToUiAppend(output, outputDivider);
        boolean success = icodeSlixMethods.formatTagNdef();
        writeToUiAppend(output, "formatTagNde: " + success);
        return success;
    }

    private boolean checkValidTag() {
        if ((icodeSlixMethods == null) || (!icodeSlixMethods.isInitialized())) {
            output.setText("Tag is not available, aborted");
            errorCode.setText("");
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, "Error - tag is not initialied", COLOR_RED);
            return false;
        }
        output.setText("");
        errorCode.setText("");
        return true;
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

/*
    private void invalidateAllSelections() {
        selectedApplicationId = null;
        selectedFileId = "";
        runOnUiThread(() -> {
            applicationSelected.setText("");
            fileSelected.setText("");
        });
        KEY_NUMBER_USED_FOR_AUTHENTICATION = -1;
    }
*/


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