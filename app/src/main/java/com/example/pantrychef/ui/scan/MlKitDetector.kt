// On-device detector using ML Kit OCR and barcode scanning to produce generic pantry CandidateItem results.
package com.example.pantrychef.ui.scan

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.example.pantrychef.core.CandidateItem
import com.example.pantrychef.core.ClassLexicon
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitDetector(
    private val appContext: Context
) : OnDeviceDetector {

    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val barcodeScanner by lazy {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(opts)
    }

    override suspend fun detect(image: ImageProxy): List<CandidateItem> {
        val media = image.image ?: return emptyList()
        val degrees = image.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(media, degrees)
        return detectWithInput(input)
    }

    override suspend fun detectFromUri(context: Context, uri: Uri): List<CandidateItem> {
        val input = InputImage.fromFilePath(context, uri)
        return detectWithInput(input)
    }

    private suspend fun detectWithInput(input: InputImage): List<CandidateItem> {
        val ocr: Text? = suspendCancellableCoroutine { cont ->
            textRecognizer.process(input)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
                .addOnCanceledListener { cont.resume(null) }
        }

        val tokens = mutableListOf<String>()
        if (ocr != null) {
            for (block in ocr.textBlocks) {
                val t = block.text
                t.split(Regex("[^A-Za-z]+"))
                    .filter { it.length >= 3 }
                    .forEach { tokens += it.lowercase() }
            }
        }

        val mapped = tokens.mapNotNull { tok ->
            ClassLexicon.resolve(tok)?.let { it.name to it.unit }
        }

        val byName = mapped.groupingBy { it.first }.eachCount()

        val ocrItems = byName.map { (name, count) ->
            val unit = ClassLexicon.resolve(name)?.unit ?: defaultUnitFor(name)
            CandidateItem(
                name = name,
                count = count,
                unit = unit,
                confidence = 0.72f,
                source = CandidateItem.Source.OCR
            )
        }.toMutableList()

        val barcodes: List<Barcode> = suspendCancellableCoroutine { cont ->
            barcodeScanner.process(input)
                .addOnSuccessListener { cont.resume(it ?: emptyList()) }
                .addOnFailureListener { cont.resume(emptyList()) }
                .addOnCanceledListener { cont.resume(emptyList()) }
        }

        if (ocrItems.isEmpty() && barcodes.isNotEmpty()) {
            ocrItems += CandidateItem(
                name = "packaged item",
                count = barcodes.size.coerceAtMost(3),
                unit = "pack",
                confidence = 0.55f,
                source = CandidateItem.Source.BARCODE
            )
        }

        return ocrItems
    }

    private fun defaultUnitFor(name: String): String = when (name) {
        "pasta" -> "box"
        "noodles" -> "pack"
        "rice" -> "bag"
        "beans", "tuna" -> "can"
        "milk" -> "carton"
        "yogurt" -> "cup"
        "cheese", "butter", "flour", "sugar", "coffee", "tea" -> "pack"
        "bread" -> "loaf"
        "eggs" -> "pcs"
        "oil" -> "bottle"
        else -> "pcs"
    }
}
