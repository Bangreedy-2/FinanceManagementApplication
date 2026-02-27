package com.bangreedy.splitsync.presentation.friends

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.presentation.MainActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcFriendScreen(
    onBack: () -> Unit = {},
    vm: NfcFriendViewModel = koinViewModel()
) {
    val state by vm.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Detect NFC support
    LaunchedEffect(Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        vm.setNfcSupported(adapter != null && adapter.isEnabled)
    }

    // Generate token when screen opens
    LaunchedEffect(Unit) {
        vm.generateShareToken()
    }

    // Share tab: set HCE payload so this phone acts as an NFC tag
    val payload = state.sharePayload
    DisposableEffect(payload) {
        if (payload != null) {
            com.bangreedy.splitsync.presentation.common.NfcCardService.currentPayloadUri =
                payload.toUriString()
        }
        onDispose {
            com.bangreedy.splitsync.presentation.common.NfcCardService.currentPayloadUri = null
        }
    }

    // Scan tab: enable reader mode when on the scan page
    val currentPage = pagerState.currentPage
    DisposableEffect(currentPage) {
        val activity = context as? MainActivity
        if (currentPage == 1) {
            activity?.nfcReaderModeEnabled = true
        }
        onDispose {
            activity?.nfcReaderModeEnabled = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Friend") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("My QR Code") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Add by Link") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ShareTab(state = state, onRefresh = { vm.generateShareToken() })
                    1 -> ScanTab(
                        state = state,
                        onManualUri = { vm.onManualUri(it) },
                        onResetScan = { vm.resetScanResult() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareTab(
    state: NfcFriendUiState,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Share your invite",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Let your friend scan this QR code\nwith their camera or QR scanner app",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            state.shareLoading -> {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                Text("Generating invite…")
            }

            state.shareError != null -> {
                Text(
                    text = state.shareError,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onRefresh) {
                    Text("Retry")
                }
            }

            state.sharePayload != null -> {
                val uriString = state.sharePayload.toUriString()

                // QR code
                val qrBitmap = remember(state.sharePayload) {
                    generateQrBitmap(uriString, 512)
                }

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(250.dp)
                    )
                }

                // Copy link button
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("SplitSync Invite", uriString)
                    clipboard.setPrimaryClip(clip)
                }) {
                    Text("📋 Copy invite link")
                }

                if (state.nfcSupported) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📱 You can also hold phones together (NFC)",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(onClick = onRefresh) {
                    Text("Generate new code")
                }

                Text(
                    text = "Code expires in 5 minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScanTab(
    state: NfcFriendUiState,
    onManualUri: (String) -> Unit,
    onResetScan: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enter friend's invite",
            style = MaterialTheme.typography.headlineSmall
        )

        when (val result = state.scanResult) {
            is ScanResult.Idle -> {
                Text(
                    text = "Ask your friend to share their QR code.\nScan it with your camera app, or paste the link below.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Invite link") },
                    placeholder = { Text("https://splitsync.app/friend?...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paste from clipboard
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.contains("splitsync.app/friend")) {
                                    manualInput = text
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📋 Paste")
                    }

                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                onManualUri(manualInput.trim())
                            }
                        },
                        enabled = manualInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Friend")
                    }
                }

                if (state.nfcSupported) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📱 NFC: Hold phones together if your friend has their QR screen open",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ScanResult.Processing -> {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                Text("Processing invite…")
            }

            is ScanResult.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✅", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    onResetScan()
                    manualInput = ""
                }) {
                    Text("Add another")
                }
            }

            is ScanResult.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "❌", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    onResetScan()
                    manualInput = ""
                }) {
                    Text("Try again")
                }
            }
        }
    }
}

/**
 * Generate a QR code Bitmap from a string using ZXing.
 */
private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}






