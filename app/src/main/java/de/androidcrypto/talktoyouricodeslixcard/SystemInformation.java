package de.androidcrypto.talktoyouricodeslixcard;

import java.util.Arrays;

public class SystemInformation {
    // see TRF7960 Evaluation Module ISO 15693 Host Commands Sloa141.pdf

    private final byte[] response;
    private boolean isValid = false;
    private byte statusBits;
    private byte informationFlags;
    private byte[] uid;
    private byte dsfId; // Data Structure Format Identifier
    private byte afi; // Application Family Identifier
    private byte[] memorySize; // (number of blocks + 1) || (bytes per block + 1)
    private byte icReference;
    private int memorySizeInt;
    private int numberOfBlocks;
    private int bytesPerBlock;

    public SystemInformation(byte[] response) {
        this.response = response;
        if ((response == null) || (response.length != 15)) return;
        isValid = analyzeResponse();
    }

    private boolean analyzeResponse() {
        // get the data from the response
        statusBits = response[0];
        informationFlags = response[1];
        uid = Arrays.copyOfRange(response, 2, 10);
        dsfId = response[10];
        afi = response[11];
        memorySize = new byte[2];
        memorySize[0] = response[12];
        memorySize[1] = response[13];
        icReference = response[14];
        // get some readable data
        numberOfBlocks = (int) memorySize[0] + 1;
        bytesPerBlock = (int) memorySize[1] + 1;
        memorySizeInt = numberOfBlocks * bytesPerBlock;
        return true;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEMINFORMATION").append("\n");
        sb.append("statusBits: ").append(Utils.byteToHex(statusBits)).append("\n");
        sb.append("informationFlags: ").append(Utils.byteToHex(informationFlags)).append("\n");
        sb.append("uid: ").append(Utils.bytesToHex(uid)).append("\n");
        sb.append("dsfId: ").append(Utils.byteToHex(dsfId)).append("\n");
        sb.append("afi: ").append(Utils.byteToHex(afi)).append("\n");
        sb.append("memorySize: ").append(Utils.bytesToHex(memorySize)).append("\n");
        sb.append("numberOfBlocks: ").append(numberOfBlocks).append("\n");
        sb.append("bytesPerBlock: ").append(bytesPerBlock).append("\n");
        sb.append("memorySizeInt: ").append(memorySizeInt).append("\n");
        sb.append("icReference: ").append(Utils.byteToHex(icReference)).append("\n");
        sb.append("*** dump ended ***").append("\n");
        return sb.toString();
    }

    /**
     * section for getter
     */

    public boolean isValid() {
        return isValid;
    }

    public byte[] getResponse() {
        return response;
    }

    public byte getStatusBits() {
        return statusBits;
    }

    public byte getInformationFlags() {
        return informationFlags;
    }

    public byte[] getUid() {
        return uid;
    }

    public byte getDsfId() {
        return dsfId;
    }

    public byte getAfi() {
        return afi;
    }

    public byte[] getMemorySize() {
        return memorySize;
    }

    public byte getIcReference() {
        return icReference;
    }

    public int getMemorySizeInt() {
        return memorySizeInt;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public int getBytesPerBlock() {
        return bytesPerBlock;
    }
}
