package format

enum class PosFontType(val bytes: ByteArray) {
    NORMAL(byteArrayOf(0x1B, 0x4D, 0x00)),
    COMPRESSED(byteArrayOf(0x1B, 0x4D, 0x01))
}
