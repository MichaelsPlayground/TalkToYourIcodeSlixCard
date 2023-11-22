package de.androidcrypto.talktoyouricodeslixcard;

import static de.androidcrypto.talktoyouricodeslixcard.Utils.printData;

import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

public class MainActivitySample {

    private String TAG = MainActivity.class.getSimpleName();

    public void onTagDiscovered(Tag tag) {
        if (tag == null) {
            Log.e(TAG, "tag is NULL, aborted");
            return;
        }
        byte[] tagUid = tag.getId();
        String[] techList = tag.getTechList();
        Log.d(TAG, "techList: " + Arrays.toString(techList));
        // get access to the NfcV technology class
        NfcV nfcV = NfcV.get(tag);
        if (nfcV == null) {
            Log.e(TAG, "nfcV is NULL, aborted");
            return;
        }
        byte dsfId = nfcV.getDsfId();
        Log.d(TAG, "dsfId: " + dsfId);
        int maxTranceiveLength = nfcV.getMaxTransceiveLength();
        Log.d(TAG, "maxTranceiveLength: " + maxTranceiveLength + " bytes");
        byte responseFlags = nfcV.getResponseFlags();
        Log.d(TAG, "responseFlags: " + responseFlags);
        // connect to the tag
        try {
            nfcV.connect();
            Log.d(TAG, "tag is connected");
            // the ICODE SLIX logic starts here
            // ...
        } catch (IOException e) {
            Log.e(TAG, "Error on connecting the tag: " + e.getMessage());
            return;
        }
    }


}
