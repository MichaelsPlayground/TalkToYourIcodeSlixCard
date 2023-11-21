# ICODE SLIX command overview

This is an extract of the official datasheet for *ICODE SLIX SL2S2002_SL2S2102* and other sources.

For these kind of tags there are 2 possible options - the regular and the addressed one. In my 
examples I'm using always the addressed option; for this mode the tags needs the tag's UID as 
variable.

Below you find all supported commands and examples:

# Mandatory commands

## Inventory (Command code = 01h)

## Stay quiet (Command code = 02h)

# Optional commands

## Read single block (Command code = 20h)

## Write single block (Command code = 21h)

## Write multiple blocks (not natively supported by tag)

This is method that divides the data in blocks of tag's block size (4 bytes) and writes the divided data to 
subsequent blocks using the "Write single block" command.

## Lock block (Command code = 22h)

For an addressed Lock Block command, the Option and Address flags (0x90) must be set.

**This is a permanent = irrevocable setting !**

## Read multiple blocks (Command code = 23h)

## Select (Command code = 25h)

## Reset to ready (Command code = 26h)

## Write AFI (Command code = 27h)

The AFI ("Application Family Identifier") is a one byte value defining a group of same articles. You can write 
a new value by running this command. If the command is password protected you need to run a **set password** command 
before running this command.

## Lock AFI (Command code = 28h)

After running this command the value of AFI it permanently locked.

**This is a permanent = irrevocable setting !**

## Write DSFID (Command code = 29h)

The 8-bit Data Structure Format Identifier (DSFID) field is a location to write information to about the structure 
of the User memory. You can write a new value by running this command.

## Lock DSFID (Command code = 2Ah)

After running this command the value of DSFID it permanently locked.

**This is a permanent = irrevocable setting !**

## Get System Information (Command code = 2Bh)

## Get Multiple Block Security Status (Command code = 2Ch)

# Custom commands

## Get Random number (Command code = B2h)

The GET RANDOM NUMBER command is required to receive a random number from the label IC. 
The passwords that will be transmitted with the SET PASSWORD command have to be 
calculated with the password and the random number (see "SET PASSWORD").

## Set Password (Command code = B3h)

The SET PASSWORD command enables the EAS/AFI password to be transmitted to the label 
to access EAS and/or AFI (if the EAS and/or AFI password is enabled). The SET PASSWORD 
command has to be executed just once for the related password if the label is powered.

Remark: If the IC receives an invalid password, it will not execute any following command 
until a Power-On Reset (POR) (RF reset) is executed.

## Write Password (Command code = B4h)

The WRITE PASSWORD command enables a new password to be written into the related memory 
if the related old password has already been transmitted with a SET PASSWORD command and 
the addressed password is not locked (see Section "LOCK PASSWORD").

Remark: The WRITE PASSWORD command can only be executed in addressed or selected mode. 
The new password takes effect immediately which means that the new password has to be 
transmitted with the SET PASSWORD command to access protected blocks.

The EAS/AFI password is addressed with the password identifier (0x10).

## Lock Password (Command code = B5h)

The LOCK PASSWORD command enables the addressed password to be locked if the related 
password has already been transmitted with a SET PASSWORD command. A locked password 
cannot be changed.

The EAS/AFI password is addressed with the password identifier (0x10).

**This is a permanent = irrevocable setting !**

## Inventory Read (Command code = A0h)

When receiving the INVENTORY READ request, the ICODE SLIX IC performs the same as the 
anti collision sequence, with the difference that instead of the UID and the DSFID, 
the requested memory content is re-transmitted from the ICODE SLIX IC.

## Fast Inventory Read (Command code = A1h)

When receiving the FAST INVENTORY READ command the ICODE SLIX IC behaves the same as the 
INVENTORY READ command with the following exceptions:

The data rate in the direction ICODE SLIX IC to the interrogator is twice that defined 
in ISO/IEC 15693-3 depending on the Datarate_flag 53 kbit (high data rate) or 13 kbit 
(low data rate).

## Set EAS (Command code = A2h)

The SET EAS command enables the EAS mode if the EAS mode is not locked. If the EAS mode 
is password protected the EAS password has to be first transmitted with the SET PASSWORD 
command.

## Reset EAS (Command code = A3h)

The RESET EAS command disables the EAS mode if the EAS mode is not locked. If the EAS 
mode is password protected the EAS password has to be first transmitted with the SET 
PASSWORD command.

## Lock EAS (Command code = A4h)

The LOCK EAS command locks the current state of the EAS mode and the EAS ID. If the EAS 
mode is password protected the EAS password has to be first transmitted with the SET 
PASSWORD command.

**This is a permanent = irrevocable setting !**

## EAS Alarm (Command code = A5h)

If the EAS mode is enabled, the EAS sequence is returned from the ICODE SLIX IC.
If the EAS mode is disabled the ICODE SLIX IC remains silent.


If the *Option Flag* is 0/false, EAS will be password protected, if the *Option Flag* is
## Password Protect EAS or AFI (Command code = A6h)

The PASSWORD PROTECT EAS/AFI command enables the password protection for EAS and/or AFI
if the EAS/AFI password is first transmitted with the SET PASSWORD command.
1/true, AFI will be password protected.

After an activation you can write to AFI or EAS after a successfully "set password" run.

**Once the EAS/AFI password protection is enabled, it is not possible to change back to 
unprotected EAS and/or AFI writing.**

## 




