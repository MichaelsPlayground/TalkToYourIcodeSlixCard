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

    public Iso15693Flags(byte flagsByte) {
        this.flagsByte = flagsByte;
        analyzeFlags();
        isValid = true;
    }

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

    private void analyzeFlags() {
        bit1Subcarrier = Utils.testBit(flagsByte, 0);
        bit2UplinkDataRate = Utils.testBit(flagsByte, 1);
        bit3InventoryFlag = Utils.testBit(flagsByte, 2);
        bit4ProtocolExtension = Utils.testBit(flagsByte, 3);
        // InventoryFlag not set/false
        if (!bit3InventoryFlag) {
            bit5Inventory0SelectFlag = Utils.testBit(flagsByte, 4);
            bit6Inventory0AddressFlag = Utils.testBit(flagsByte, 5);
            bit7Inventory0OptionFlag = Utils.testBit(flagsByte, 6);
            bit8Inventory0Rfu = Utils.testBit(flagsByte, 7);
        } else {
            // InventoryFlag set/true
            bit5Inventory1AfiFlag = Utils.testBit(flagsByte, 4);
            bit6Inventory1NoOfSlotsFlag = Utils.testBit(flagsByte, 5);
            bit7Inventory1OptionFlag = Utils.testBit(flagsByte, 6);
            bit8Inventory1Rfu = Utils.testBit(flagsByte, 7);
        }
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("ISO15693FLAGS").append("\n");
        sb.append("bit1 Subcarrier:        ").append(bit1Subcarrier).append("\n");
        sb.append("bit2 Uplink Data Rate:  ").append(bit2UplinkDataRate).append("\n");
        sb.append("bit3 InventoryFlag:     ").append(bit3InventoryFlag).append("\n");
        sb.append("bit4 ProtocolExtension: ").append(bit4ProtocolExtension).append("\n");
        if(!bit3InventoryFlag) {
            sb.append("Inventory Flag NOT set").append("\n");
            sb.append("bit5 Select Flag:       ").append(bit5Inventory0SelectFlag).append("\n");
            sb.append("bit6 Address Flag:      ").append(bit6Inventory0AddressFlag).append("\n");
            sb.append("bit7 Option Flag:       ").append(bit7Inventory0OptionFlag).append("\n");
            sb.append("bit8 Rfu:               ").append(bit8Inventory0Rfu).append("\n");
        } else {
            sb.append("Inventory Flag SET").append("\n");
            sb.append("bit5 AFI Flag:          ").append(bit5Inventory1AfiFlag).append("\n");
            sb.append("bit6 No Of Slots Flag:  ").append(bit6Inventory1NoOfSlotsFlag).append("\n");
            sb.append("bit7 Option Flag:       ").append(bit7Inventory1OptionFlag).append("\n");
            sb.append("bit8 Rfu:               ").append(bit8Inventory1Rfu).append("\n");
        }
        sb.append("flagsByte:              ").append(Utils.byteToHex(flagsByte)).append("\n");
        sb.append("*** dump ended ***");
        return sb.toString();
    }

    /**
     * section for getter
     */

    public byte getFlagsByte() {
        return flagsByte;
    }
}
