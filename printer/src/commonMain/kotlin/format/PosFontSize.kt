package format

enum class PosFontSize(val bytes: ByteArray) {
    NORMAL(byteArrayOf(0x1D, 0x21, 0x00)),
    LARGE(byteArrayOf(0x1D, 0x21, 0x11))
}


