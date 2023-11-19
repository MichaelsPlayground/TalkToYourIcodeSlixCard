package de.androidcrypto.talktoyouricodeslixcard;

public class Iso15693Flags {
    // see TRF7960 Evaluation Module ISO 15693 Host Commands Sloa141.pdf pages 20 + 21
    // bits 1 to 4 see table 2
    // if bit 3 is NOT set see bits 5 to 8 in table 3
    // if bit 3 IS set see bits 5 to 8 in table 4
    private boolean bit1Subcarrier;
    private boolean bit2UplinkDataRate;
    private boolean bit3InventoryFlag; // false = 0 Inventory0, true = 1 Inventory1
    private boolean bit4ProtocolExtension;
    private boolean bit5Inventory0SelectFlag;
    private boolean bit6Inventory0AddressFlag;
    private boolean bit7Inventory0OptionFlag;
    private boolean bit8Inventory0Rfu; // always false
    private boolean bit5Inventory1AfiFlag;
    private boolean bit6Inventory1NoOfSlotsFlag;
    private boolean bit7Inventory1OptionFlag;
    private boolean bit8Inventory1Rfu; // always false

    private byte flagsByte;
    private boolean isValid = false; // don't forget to run the part 2 constructor

    // setting of bit meanings depends on bit3 Inventory flag


    public Iso15693Flags(boolean bit1Subcarrier, boolean bit2UplinkDataRate, boolean bit3InventoryFlag, boolean bit4ProtocolExtension) {
        this.bit1Subcarrier = bit1Subcarrier;
        this.bit2UplinkDataRate = bit2UplinkDataRate;
        this.bit3InventoryFlag = bit3InventoryFlag;
        this.bit4ProtocolExtension = bit4ProtocolExtension;
        isValid = false;
    }

    public boolean setInventory0Flags(boolean bit5Inventory0SelectFlag, boolean bit6Inventory0AddressFlag, boolean bit7Inventory0OptionFlag, boolean bit8Inventory0Rfu) {
        // check for bit3InventoryFlag, if TRUE abort
        if (bit3InventoryFlag == true) return false;
        this.bit5Inventory0SelectFlag = bit5Inventory0SelectFlag;
        this.bit6Inventory0AddressFlag = bit6Inventory0AddressFlag;
        this.bit7Inventory0OptionFlag = bit7Inventory0OptionFlag;
        this.bit8Inventory0Rfu = false; // bit8 is always RFU = 0 = false
        calculateFlagsInventory0();

        isValid = true;
        return true;
    }

    public boolean setInventory1Flags(boolean bit5Inventory1AfiFlag, boolean bit6Inventory1NoOfSlotsFlag, boolean bit7Inventory1OptionFlag, boolean bit8Inventory1Rfu) {
        // check for bit3InventoryFlag, if FALSE abort
        if (bit3InventoryFlag == false) return false;
        this.bit5Inventory1AfiFlag = bit5Inventory1AfiFlag;
        this.bit6Inventory1NoOfSlotsFlag = bit6Inventory1NoOfSlotsFlag;
        this.bit7Inventory1OptionFlag = bit7Inventory1OptionFlag;
        this.bit8Inventory1Rfu = false; // bit8 is always RFU = 0 = false
        calculateFlagsInventory1();
        isValid = true;
        return true;
    }

    private void calculateFlagsInventory0() {
        byte flags = 0;
        if (bit1Subcarrier) flags = Utils.setBitInByte(flags, 0);
        if (bit2UplinkDataRate) flags = Utils.setBitInByte(flags, 1);
        if (bit3InventoryFlag) flags = Utils.setBitInByte(flags, 2);
        if (bit4ProtocolExtension) flags = Utils.setBitInByte(flags, 3);
        if (bit5Inventory0SelectFlag) flags = Utils.setBitInByte(flags, 4);
        if (bit6Inventory0AddressFlag) flags = Utils.setBitInByte(flags, 5);
        if (bit7Inventory0OptionFlag) flags = Utils.setBitInByte(flags, 6);
        if (bit8Inventory0Rfu) flags = Utils.setBitInByte(flags, 7);
        flagsByte = flags;
    }

    private void calculateFlagsInventory1() {
        byte flags = 0;
        if (bit1Subcarrier) flags = Utils.setBitInByte(flags, 0);
        if (bit2UplinkDataRate) flags = Utils.setBitInByte(flags, 1);
        if (bit3InventoryFlag) flags = Utils.setBitInByte(flags, 2);
        if (bit4ProtocolExtension) flags = Utils.setBitInByte(flags, 3);
        if (bit5Inventory1AfiFlag) flags = Utils.setBitInByte(flags, 4);
        if (bit6Inventory1NoOfSlotsFlag) flags = Utils.setBitInByte(flags, 5);
        if (bit7Inventory1OptionFlag) flags = Utils.setBitInByte(flags, 6);
        if (bit8Inventory1Rfu) flags = Utils.setBitInByte(flags, 7);
        flagsByte = flags;
    }

    /**
     * section for getter
     */

    public byte getFlagsByte() {
        return flagsByte;
    }
}
