// ViewModel for the scan flow: coordinates on-device detection and Gemini vision, merges results, and manages UI state.
package com.example.pantrychef.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrychef.core.CandidateItem
import com.example.pantrychef.core.GeminiHttp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val detector: OnDeviceDetector,
    private val gemini: GeminiHttp
) : ViewModel() {

    private val _candidates = MutableStateFlow<List<CandidateItem>>(emptyList())
    val candidates: StateFlow<List<CandidateItem>> = _candidates

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbar

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    fun simulateDetection() {
        _candidates.value = listOf(
            CandidateItem(name = "cucumber", count = 2, unit = "pcs", confidence = 0.92f, source = CandidateItem.Source.ON_DEVICE),
            CandidateItem(name = "pasta", count = 1, unit = "box", confidence = 0.88f, source = CandidateItem.Source.OCR),
            CandidateItem(name = "noodles", count = 3, unit = "pack", confidence = 0.83f, source = CandidateItem.Source.GEMINI)
        )
    }

    fun clear() {
        _candidates.value = emptyList()
    }

    fun removeCandidate(name: String) {
        _candidates.update { list -> list.filterNot { it.name.equals(name, ignoreCase = true) } }
    }

    fun increment(name: String) {
        _candidates.update { list ->
            list.map { if (it.name.equals(name, ignoreCase = true)) it.copy(count = it.count + 1) else it }
        }
    }

    fun decrement(name: String) {
        _candidates.update { list ->
            list.map {
                if (it.name.equals(name, ignoreCase = true)) it.copy(count = max(0, it.count - 1)) else it
            }.filter { it.count > 0 }
        }
    }

    fun setUnit(name: String, unit: String) {
        _candidates.update { list ->
            list.map { if (it.name.equals(name, ignoreCase = true)) it.copy(unit = unit) else it }
        }
    }

    fun handleCapture(image: ImageProxy) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val detected = withContext(Dispatchers.Default) { detector.detect(image) }
                if (detected.isEmpty()) {
                    _snackbar.value = "Nothing detected"
                } else {
                    mergeDetections(detected)
                }
            } finally {
                image.close()
                _isScanning.value = false
            }
        }
    }

    fun handleCapturedUri(uri: Uri, previewBitmap: Bitmap?) {
        viewModelScope.launch(Dispatchers.Default) {
            _isScanning.value = true
            try {
                val localDeferred = async { runCatching { detector.detectFromUri(appContext, uri) }.getOrDefault(emptyList()) }
                val visionDeferred = async {
                    if (previewBitmap != null) {
                        runCatching { gemini.identifyItemCounts(listOf(previewBitmap)) }.getOrDefault(emptyList())
                    } else emptyList()
                }

                val local = localDeferred.await()
                val vision = visionDeferred.await()
                val merged = local + vision

                if (merged.isEmpty()) {
                    _snackbar.value = "Nothing detected"
                } else {
                    mergeDetections(merged)
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    private fun mergeDetections(newOnes: List<CandidateItem>) {
        val combined = (_candidates.value + newOnes)
            .groupBy { it.name.lowercase() }
            .map { (_, items) ->
                val maxConf = items.maxBy { it.confidence }
                maxConf.copy(count = items.sumOf { it.count })
            }
        _candidates.value = combined
    }
}
