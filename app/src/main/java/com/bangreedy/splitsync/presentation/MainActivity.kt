package com.bangreedy.splitsync.presentation

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bangreedy.splitsync.domain.model.NfcInvitePayload
import com.bangreedy.splitsync.presentation.common.NfcCoordinator
import com.bangreedy.splitsync.ui.theme.SplitSyncTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val nfcCoordinator: NfcCoordinator by inject()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var nfcPendingIntent: PendingIntent
    private lateinit var nfcIntentFilters: Array<IntentFilter>
    private lateinit var nfcTechLists: Array<Array<String>>

    /** When true, we use reader-mode to read HCE tags from the other phone. */
    var nfcReaderModeEnabled = false
        set(value) {
            field = value
            if (value) enableReaderMode() else disableReaderMode()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        nfcPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataScheme("https")
                addDataAuthority("splitsync.app", null)
                addDataPath("/friend", android.os.PatternMatcher.PATTERN_PREFIX)
            } catch (_: Exception) { }
        }
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        nfcIntentFilters = arrayOf(ndefFilter, tagFilter)
        nfcTechLists = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(IsoDep::class.java.name))

        handleIntent(intent)

        setContent {
            SplitSyncTheme {
                com.bangreedy.splitsync.presentation.app.AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this, nfcPendingIntent, nfcIntentFilters, nfcTechLists
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        disableReaderMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        nfcCoordinator.onIntentReceived(intent)
    }

    // ── Reader-mode (Scan tab): reads HCE from another phone ──

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        Log.d("NfcReader", "Tag discovered in reader mode")
        readNdefFromTag(tag)
    }

    private fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        adapter.enableReaderMode(
            this,
            readerCallback,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) }
        )
        Log.d("NfcReader", "Reader mode enabled")
    }

    private fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(this)
    }

    /**
     * Read NDEF from an HCE tag using ISO-DEP (Type 4 Tag protocol).
     */
    private fun readNdefFromTag(tag: Tag) {
        // First try normal NDEF
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val message = ndef.ndefMessage
                if (message != null) {
                    parseNdefMessage(message)
                    return
                }
            } catch (e: Exception) {
                Log.e("NfcReader", "NDEF read failed", e)
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        }

        // Then try ISO-DEP (HCE)
        val isoDep = IsoDep.get(tag) ?: return
        try {
            isoDep.connect()
            isoDep.timeout = 5000

            // 1) SELECT NDEF Tag Application AID
            val selectAid = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                0x07, // AID length
                0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
                0x00 // Le
            )
            var resp = isoDep.transceive(selectAid)
            if (!isSuccess(resp)) { Log.e("NfcReader", "SELECT AID failed"); return }

            // 2) SELECT CC file (E103)
            val selectCc = byteArrayOf(
                0x00, 0xA4.toByte(), 0x00, 0x0C,
                0x02, 0xE1.toByte(), 0x03
            )
            resp = isoDep.transceive(selectCc)
            if (!isSuccess(resp)) { Log.e("NfcReader", "SELECT CC failed"); return }

            // 3) READ CC to find NDEF file info
            val readCc = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x0F)
            resp = isoDep.transceive(readCc)
            if (!isSuccess(resp)) { Log.e("NfcReader", "READ CC failed"); return }

            // 4) SELECT NDEF file (E104)
            val selectNdef = byteArrayOf(
                0x00, 0xA4.toByte(), 0x00, 0x0C,
                0x02, 0xE1.toByte(), 0x04
            )
            resp = isoDep.transceive(selectNdef)
            if (!isSuccess(resp)) { Log.e("NfcReader", "SELECT NDEF failed"); return }

            // 5) READ first 2 bytes to get NDEF message length
            val readLen = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x02)
            resp = isoDep.transceive(readLen)
            if (!isSuccess(resp) || resp.size < 4) { Log.e("NfcReader", "READ LEN failed"); return }
            val ndefLen = ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF)
            if (ndefLen == 0 || ndefLen > 4096) { Log.e("NfcReader", "Bad NDEF len=$ndefLen"); return }

            // 6) READ the NDEF message bytes (offset 2)
            val ndefBytes = ByteArray(ndefLen)
            var offset = 0
            while (offset < ndefLen) {
                val chunkSize = minOf(ndefLen - offset, 59) // MLe from CC
                val fileOffset = offset + 2 // skip 2-byte length prefix in NDEF file
                val readChunk = byteArrayOf(
                    0x00, 0xB0.toByte(),
                    ((fileOffset shr 8) and 0xFF).toByte(),
                    (fileOffset and 0xFF).toByte(),
                    chunkSize.toByte()
                )
                resp = isoDep.transceive(readChunk)
                if (!isSuccess(resp)) { Log.e("NfcReader", "READ NDEF chunk failed at $offset"); return }
                val dataLen = resp.size - 2 // strip SW
                System.arraycopy(resp, 0, ndefBytes, offset, dataLen)
                offset += dataLen
            }

            val message = NdefMessage(ndefBytes)
            parseNdefMessage(message)

        } catch (e: Exception) {
            Log.e("NfcReader", "ISO-DEP read failed", e)
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    private fun parseNdefMessage(message: NdefMessage) {
        for (record in message.records) {
            val uri = record.toUri()?.toString() ?: continue
            val payload = NfcInvitePayload.fromUri(uri)
            if (payload != null) {
                Log.d("NfcReader", "Parsed invite: uid=${payload.uid}")
                nfcCoordinator.emitPayload(payload)
                return
            }
        }
    }

    private fun isSuccess(response: ByteArray): Boolean {
        return response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()
    }
}
