package de.androidcrypto.talktoyouricodeslixcard.lrp;

import static de.androidcrypto.talktoyouricodeslixcard.lrp.Constants.lower;
import static de.androidcrypto.talktoyouricodeslixcard.lrp.Constants.upper;
import static de.androidcrypto.talktoyouricodeslixcard.lrp.Util.encryptWith;

import java.util.ArrayList;
import java.util.List;

public class LrpMultiCipher {

    private byte[] mainKey;
    public int M;
    public List<byte[]> P;

    public LrpMultiCipher(byte[] key, int nibbleSize) {
        mainKey = key;
        M = nibbleSize;
        P = new ArrayList<>();
        reset();
    }

    public LrpMultiCipher(byte[] key) {
        this(key, 4);
    }

    public void reset() {
        int numPlaintexts = 1 << M;
        P = new ArrayList<>(numPlaintexts);
        byte[] h = encryptWith(mainKey, upper);
        for (int i = 0; i < numPlaintexts; i++) {
            P.add(encryptWith(h, lower));
            h = encryptWith(h, upper);
        }
    }

    public LrpCipher cipher(int idx) {
        byte[] h = encryptWith(mainKey, lower);
        for (int i = 0; i < idx; i++) {
            h = encryptWith(h, upper);
        }
        byte[] k = encryptWith(h, lower);
        return new LrpCipher(this, k, 0, true);
    }

}
