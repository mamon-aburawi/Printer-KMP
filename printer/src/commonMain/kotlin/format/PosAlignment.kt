package format

enum class PosAlignment(val bytes: ByteArray) {
    LEFT(byteArrayOf(0x1B, 0x61, 0x00)),
    CENTER(byteArrayOf(0x1B, 0x61, 0x01)),
    RIGHT(byteArrayOf(0x1B, 0x61, 0x02))
}
