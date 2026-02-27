package com.bangreedy.splitsync.presentation.common

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import com.bangreedy.splitsync.domain.model.NfcInvitePayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges NFC intents and deep link intents from Activity to Compose ViewModels.
 * Singleton managed by Koin.
 */
class NfcCoordinator {

    private val _pendingInvites = MutableSharedFlow<NfcInvitePayload>(extraBufferCapacity = 1)
    val pendingInvites: SharedFlow<NfcInvitePayload> = _pendingInvites.asSharedFlow()

    /**
     * Emit a parsed payload directly (e.g. from NFC reader mode).
     */
    fun emitPayload(payload: NfcInvitePayload) {
        _pendingInvites.tryEmit(payload)
    }

    /**
     * Called from Activity.onNewIntent / onCreate for any intent that might
     * contain a friend invite (NFC or deep link).
     */
    fun onIntentReceived(intent: Intent) {
        // Try NFC first
        if (tryParseNfc(intent)) return
        // Then try deep link
        tryParseDeepLink(intent)
    }

    /**
     * Create an NDEF message for the given payload (for NFC push).
     */
    fun createNdefMessage(payload: NfcInvitePayload): NdefMessage {
        val uriRecord = NdefRecord.createUri(payload.toUriString())
        return NdefMessage(arrayOf(uriRecord))
    }

    private fun tryParseNfc(intent: Intent): Boolean {
        if (intent.action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            intent.action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return false

        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return false

        for (raw in rawMessages) {
            val ndefMessage = raw as? NdefMessage ?: continue
            for (record in ndefMessage.records) {
                val uriString = parseNdefRecordUri(record) ?: continue
                val payload = NfcInvitePayload.fromUri(uriString) ?: continue
                _pendingInvites.tryEmit(payload)
                return true
            }
        }
        return false
    }

    private fun tryParseDeepLink(intent: Intent): Boolean {
        if (intent.action != Intent.ACTION_VIEW) return false
        val uri = intent.data ?: return false
        val uriString = uri.toString()

        // Handle splitsync:// scheme
        val normalized = if (uri.scheme == "splitsync" && uri.host == "friend") {
            // Convert splitsync://friend?v=1&uid=X&t=Y -> https://splitsync.app/friend?...
            "https://splitsync.app/friend?${uri.query ?: ""}"
        } else {
            uriString
        }

        val payload = NfcInvitePayload.fromUri(normalized) ?: return false
        _pendingInvites.tryEmit(payload)
        return true
    }

    private fun parseNdefRecordUri(record: NdefRecord): String? {
        return try {
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_URI)
            ) {
                val payload = record.payload
                if (payload.isEmpty()) return null
                val prefixCode = payload[0].toInt()
                val prefix = URI_PREFIX_MAP.getOrElse(prefixCode) { "" }
                val suffix = String(payload, 1, payload.size - 1, Charsets.UTF_8)
                prefix + suffix
            } else if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                String(record.payload, Charsets.UTF_8)
            } else {
                record.toUri()?.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val URI_PREFIX_MAP = mapOf(
            0x00 to "",
            0x01 to "http://www.",
            0x02 to "https://www.",
            0x03 to "http://",
            0x04 to "https://",
            0x05 to "tel:",
            0x06 to "mailto:"
        )
    }
}



