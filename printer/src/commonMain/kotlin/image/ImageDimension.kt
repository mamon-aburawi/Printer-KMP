package image

enum class ImageDimension(val mm: Int) {
    MM_5(5),
    MM_10(10),
    MM_15(15),
    MM_20(20),
    MM_25(25),
    MM_30(30),
    MM_35(35),
    MM_40(40),
    MM_45(45);

    val pixels: Int get() = mm * 8
}