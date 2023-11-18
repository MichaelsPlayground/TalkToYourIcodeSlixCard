# Format the tag to a NDEF tag

see: https://stackoverflow.com/questions/36188341/ti-rf430frl152hevm-nfc-ndef-formatting/36234325#36234325

### Is it that the NDEF formatting we copied from the other tag is not valid for our tag?

That's exactly the problem. Looking at the first 12 bytes of your memory dump, you obviously copied the data blocks from an NXP NTAG203 (or similar) as indicated by the manufacturer code (byte 0: `0x04`) and the memory size in the capability container (byte 13: `0x12`). NXP's NTAG and MIFARE Ultralight series follow the NFC Forum Type 2 Tag Operation specification. However, your TI chip (RF430FRL152H) is based on ISO/IEC 15693 and consequently follow the NFC Forum Type 5 Tag Operation specification. The Tag Operation specifications define the data formats and command sets that turn an NFC tag into an NDEF tag. There are several (currently 5) different such specifications because NFC technology combines several different RF standards (ISO/IEC 14443, FeliCa, ISO/IEC 15693) and uses tag hardware that already existed for those standards before NFC.

### Why does NFC TagInfo see blocks sometimes and then pages for when NDEF is detected? Are blocks and pages the same?

In that case, blocks and pages are equivalent. The different wording simply comes from the terminology used by the chip manufacturers. Note that the RF430FRL152H chip uses the term "pages" to group several blocks and therefore with a different meaning.

### If one simply writes the correct bytes in the correct blocks then can anything be picked up as NDEF, after all, at the low level what is the difference?

The difference is that your TI RF430FRL152H tag chip needs to use a different coding for the NDEF memory area than the NXP tag. This is simply because it uses a different low-level communication technology (modulation, coding, framing, command set) and consequently follows a different NFC Forum Tag Operation specification.

In order to make your tag chip an NDEF tag, you need to use the following coding for the NDEF memory area starting at block 0: Note that the capability container was filled with values that assume a block size of 8 bytes. You can change the ISO block size option using the flag ISOBlockSize in the Firmware Control Register (see section 7.54 "Firmware System Control Register" in the [RF430FRL15xH Firmware User's Guide](http://www.ti.com/lit/ug/slau603/slau603.pdf)).

<pre>
E1 40 F2 09   03 0B D1 01
07 54 02 65   6E 41 42 43
44 FE 00 00   00 00 00 00
</pre>

This would result into an NDEF message containing one Text record with the message "ABCD".

The first 4 bytes (`E1 40 F2 09`) are the capability container:

- `0xE1` is the magic number identifying the capability container for a tag where the complete memory area can be addressed with one byte block addresses.
- `0x40` codes version 1.0 of the Type 5 tag memory mapping and indicates free read/write access to the memory.
- `0xF2` defines the overall NDEF memory area (except for the CC bytes) to have a length of 242 (`0xF2`) times 8 bytes (= 1936 bytes). **See the section "*Theory vs. practice*" below!**
- `0x09` indicates that your tag supports the READ_MULTIPLE_BLOCKS command (bit 0 set) and the LOCK_BLOCK command (bit 3 set).

The next 2 bytes (`03 0B`) are the header of the NDEF Message TLV (tag-length-value coded data structure):

- `0x03`: Header byte indicating NDEF Message TLV.
- `0x0B`: Length of NDEF Message TLV = 11 bytes.

The next 11 bytes (`D1 01 07 54 02 656E 41424344`) are the NDEF message:

- `0xD1`: Record header byte:
    - Bits 7 and 6: This is the only record of the NDEF message.
    - Bit 4: This is a short record (i.e. the payload length is coded with a single byte).
    - Bits 2..0: The record type codes an NFC Forum well-known type.
- `0x01`: The type name field has a length of 1 byte.
- `0x07`: The payload field has a length of 7 bytes.
- `0x54`: The type name (in ASCII: "T") indicating an NFC Forum well-known Text record type (Text RTD).
- `0x02`..`0x44`: The payload field of the Text record:
    - `0x02`: The text is encoded in UTF-8, the language field consists of 2 bytes.
    - `0x65 0x6E`: The language field (in ASCII: "en") indicating the language English.
    - `0x41 0x42 0x43 0x44`: The text payload (in UTF-8: "ABCD").

The next byte (`FE`) is the Terminator TLV indicating the end of the used data area. The remaining bytes of that block should be filled with zeros (`0x00`) in order to avoid problems with some Android devices.

### Does block locking make any difference?

No, block locking does not change the way your tag is detected. It only changes how it can be accessed by the reading (Android) device: read/write access or read-only access.

### Will this tag be detectable on all Android devices?

Unfortunately, no. The NFC Forum Type 5 Tag Operation specification was only finalized in July 2015. While some Android devices implemented NDEF on ISO/IEC 15693 (NFC-V) tags before that date, don't expect this the case for all Android devices. It should work on most devices starting with Android 5.0 though. Some Android devices should be capable of supporting NDEF on certain NFC-V tags even as of Android 4.3.

### Theory vs. practice

After some further testing I found that even devices that do support NDEF on Type 5 (NFC-V) tags seem to have significant limitations (**bugs???**) in the implementations of their NFC stack. I tested a Samsung Galaxy S6 (Android 5.1.1) with two tag types from the TI Tag-it HF-I series:

1. Tag-it HF-I Plus (2048 bit user memory, in 64 x 4 byte blocks)
2. Tag-it HF-I Standard (256 bit user memory, in 8 x 4 byte blocks)

Unfortunately, none of them worked with the Galaxy S6 using the capability container that I described above. The problem was the size of the NDEF memory area (MLEN, stored in the third byte of the CC). Obviously, the size used above is too long for these two tags. Consequently I reduced it to match the size of the tag memory for each tag:

1. Tag-it HF-I Plus:
    - 64 x 4 bytes = 256 bytes
    - 256 bytes / 8 = 32 (MLEN is always calculated as a multiple of 8 bytes)
    - minus 1 block since the CC does not count as part of the data area (according to the Type 5 Tag Operation specification)
    - MLEN = 31 = 0x1F
    - CC: `E1 40 1F 09`
2. Tag-it HF-I Standard:
    - 8 x 4 bytes = 32 bytes
    - 32 bytes / 8 = 4 (MLEN is always calculated as a multiple of 8 bytes)
    - minus 1 block since the CC does not count as part of the data area (according to the Type 5 Tag Operation specification)
    - MLEN = 3 = 0x03
    - CC: `E1 40 03 09`

Still, this did not work. The tags were not detected as NDEF tags (only as `NdefFormatable`). Finally, I found that the Galaxy S6 **only** detects those tags if the MLEN byte reflects **exactly** the size of the **whole memory area** (including the CC bytes). Thus, only the following values worked:

1. Tag-it HF-I Plus:
    - CC: `E1 40 20 09`
2. Tag-it HF-I Standard:
    - CC: `E1 40 04 09`

Even worse, while the CC `E1 40 04 09` worked on a Tag-it HF-I Standard tag, it did *not* work on a Tag-it HF-I Plus tag. Thus, the NFC stack of the Galaxy S6 seems to expect very specific values for the CC on different tag products.

Based on this, the following CC values should work for the RF430FRL152H:

1. when block size is set to 8 bytes: `E1 40 F3 09`
2. when block size is set to 4 bytes: `E1 40 79 09`

"should", since it's unclear how tags are identified and mapped to their expected CC values. Moreover, it's unclear if the Galaxy S6 even knows any expected CC value for that particular chip.

Another way to find the "correct" (= expected) values for the CC bytes would be to write an NDEF message to the tag using the `NdefFormatable` technology and then reading the CC bytes using a tag reader app like NFC TagInfo:

    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    NdefFormatable ndefFormatable = NdefFormatable.get(tag);

    if (ndefFormatable != null) {
        try {
            ndefFormatable.connect();
            ndefFormatable.format(new NdefMessage(NdefRecord.createTextRecord("en", "ABCD")));
        } catch (Exception e) {
        } finally {
            try {
                ndefFormatable.close();
            } catch (Exception e) {
            }
        }
    }

The same could be done using some generic tag writer app (except for NXP TagWriter which seems to be unable to write to the tag).


### Failing all else, how can we implement simple block reading in Android, in the same way that the NFC TagInfo performs its hex-dump?

The RF430FRL152H should be detected as NFC-V (ISO/IEC 15693 in NFC terminology) tag  by Android. So once you receive the NFC intent, you can obtain the tag handle and get an instance of the `NfcV` class for it:

    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    NfcV nfcV = NfcV.get(tag);

You can the connect to the tag and exchange low-level commands (e.g. READ_SINGLE_BLOCK) using the transceive method:

    nfcV.connect();
    byte[] tagUid = tag.getId();  // store tag UID for use in addressed commands

    int blockAddress = 0;
    byte[] cmd = new byte[] {
        (byte)0x60,  // FLAGS
        (byte)0x20,  // READ_SINGLE_BLOCK
        0, 0, 0, 0, 0, 0, 0, 0,
        (byte)(blockAddress & 0x0ff)
    };
    System.arraycopy(tagUid, 0, cmd, 2, 8);

    byte[] response = nfcV.transceive(cmd);

    nfcV.close();

### Where can I get further information on tag formatting, NDEF and low-level commands?

- NFC Forum Type 5 Tag Operation specification: from the [NFC Forum website](http://nfc-forum.org) (unfortunately, NFC Forum specifications are no longer available for free).

**IMPORTANT:** Be careful **not** to mix that with the "NFC Tag Type 5 Specification" offered by open-nfc.org. Even though both specifications talk about a tag "*Type 5*", they refer to a completely **different** tag platform. The specification from open-nfc.org is **not** compatible with the RF430FRL15xH chip.
- A public document that is very close to the final NFC Forum Type 5 Tag Operation specification is the NXP application note [AN11032 NXP Type ICODE Tag Operation](https://www.nxp.com/documents/application_note/AN11032.pdf). However, there are some significant differentces between the NFC Forum Type 5 Tag Operation specification and that application note (specifically with regard to the format of the capability container).
- NFC Data Exchange Format (NDEF) specification: also from the NFC Forum website or from [here](http://www.cardsys.dk/download/NFC_Docs/NFC%20Data%20Exchange%20Format%20(NDEF)%20Technical%20Specification.pdf).
- NFC Record Type Definition (RTD) specification: also from the NFC Forum website or from [here](http://www.cardsys.dk/download/NFC_Docs/NFC%20Record%20Type%20Definition%20(RTD)%20Technical%20Specification.pdf).
- Text Record Type Definition specification: also from the NFC Forum website or from [here](http://www.cardsys.dk/download/NFC_Docs/NFC%20Text%20Record%20Type%20Definition%20Technical%20Specification.pdf).
- NFC-V in the Digital Protocol specification: also from the NFC Forum website.
- ISO/IEC 15693: This standard defines the standard low-level commands.
- [RF430FRL15xH User Guide](http://www.ti.com/lit/ug/slau603b/slau603b.pdf): You can find the custom commands for access to the RAM memory area in sections 4.2ff.

