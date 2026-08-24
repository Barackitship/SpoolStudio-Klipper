package com.spoolstudio.app.hardware.nfc

data class NfcTagReadResult(
    val data: String?,
    val cardUid: String
)
