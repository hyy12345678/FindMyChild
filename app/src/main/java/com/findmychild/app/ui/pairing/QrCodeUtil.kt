package com.findmychild.app.ui.pairing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeUtil @Inject constructor() {

    private val writer = QRCodeWriter()

    /**
     * 将文本编码为 QR 码 Bitmap。
     * @param text 需要编码的文本（sessionId）
     * @param size 输出 Bitmap 的宽高 (px)
     */
    fun generateQrCode(text: String, size: Int = 512): Bitmap {
        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }
}
