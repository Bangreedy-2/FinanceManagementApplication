package com.bangreedy.splitsync.presentation.common

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * Host Card Emulation service that makes this phone act as an NFC tag
 * containing a friend-invite NDEF URI.
 *
 * When another phone in reader-mode touches this phone, the reader
 * sends SELECT AID → we respond with the NDEF message bytes.
 */
class NfcCardService : HostApduService() {

    companion object {
        private const val TAG = "NfcCardService"

        // AID registered in aid_list.xml — must match
        // Using a custom AID in the "F" range (proprietary)
        private val SELECT_AID_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte()
        )
        private val SELECT_CC_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(),
            0x02.toByte(), 0xE1.toByte(), 0x03.toByte()
        )
        private val SELECT_NDEF_APDU = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(),
            0x02.toByte(), 0xE1.toByte(), 0x04.toByte()
        )

        private val SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val FAILURE = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // NDEF Tag Application AID (D2760000850101)
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // Capability Container (CC) file contents for NDEF Type 4 Tag
        // Mapping Version 2.0, MLe=0xFF, MLc=0xFF, NDEF File ID=E104, Max NDEF=0x0FFF, R/W
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, // CCLEN
            0x20,       // Mapping version 2.0
            0x00, 0x3B, // MLe (59)
            0x00, 0x34, // MLc (52)
            0x04, 0x06, // NDEF File Control TLV: T=04, L=06
            0xE1.toByte(), 0x04, // NDEF File ID
            0x0F.toByte(), 0xFF.toByte(), // Max NDEF file size
            0x00,       // Read access: open
            0xFF.toByte() // Write access: no write
        )

        /** Current payload URI string. Set from the UI before the service is active. */
        @Volatile
        var currentPayloadUri: String? = null

        fun buildNdefBytes(): ByteArray? {
            val uri = currentPayloadUri ?: return null
            val record = NdefRecord.createUri(uri)
            val ndefMessage = NdefMessage(arrayOf(record))
            val messageBytes = ndefMessage.toByteArray()
            // NDEF file = 2 bytes length (big-endian) + message bytes
            val length = messageBytes.size
            return byteArrayOf(
                ((length shr 8) and 0xFF).toByte(),
                (length and 0xFF).toByte()
            ) + messageBytes
        }
    }

    private var selectedFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "Received APDU: ${commandApdu.toHexString()}")

        // SELECT AID (NDEF Tag Application)
        if (isSelectAid(commandApdu)) {
            Log.d(TAG, "SELECT AID -> OK")
            selectedFile = null
            return SUCCESS
        }

        // SELECT CC file
        if (commandApdu.startsWith(SELECT_CC_APDU)) {
            Log.d(TAG, "SELECT CC file")
            selectedFile = CC_FILE
            return SUCCESS
        }

        // SELECT NDEF file
        if (commandApdu.startsWith(SELECT_NDEF_APDU)) {
            Log.d(TAG, "SELECT NDEF file")
            selectedFile = buildNdefBytes() ?: run {
                Log.w(TAG, "No NDEF payload set")
                return FAILURE
            }
            return SUCCESS
        }

        // READ BINARY
        if (commandApdu.size >= 5 && commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xB0.toByte()) {
            val file = selectedFile ?: return FAILURE
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val length = commandApdu[4].toInt() and 0xFF

            Log.d(TAG, "READ BINARY offset=$offset len=$length fileSize=${file.size}")

            if (offset >= file.size) return FAILURE

            val end = minOf(offset + length, file.size)
            val slice = file.copyOfRange(offset, end)
            return slice + SUCCESS
        }

        Log.d(TAG, "Unknown APDU")
        return FAILURE
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: reason=$reason")
        selectedFile = null
    }

    private fun isSelectAid(apdu: ByteArray): Boolean {
        if (apdu.size < 5) return false
        // Check CLA=00, INS=A4, P1=04, P2=00
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() ||
            apdu[2] != 0x04.toByte() || apdu[3] != 0x00.toByte()) return false
        val aidLength = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + aidLength) return false
        val aid = apdu.copyOfRange(5, 5 + aidLength)
        return aid.contentEquals(NDEF_AID)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}

