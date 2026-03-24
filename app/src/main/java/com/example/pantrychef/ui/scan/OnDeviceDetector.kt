// Interface for on-device detection of pantry items from images.
package com.example.pantrychef.ui.scan

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.example.pantrychef.core.CandidateItem

interface OnDeviceDetector {
    suspend fun detect(image: ImageProxy): List<CandidateItem>
    suspend fun detectFromUri(context: Context, uri: Uri): List<CandidateItem>
}
