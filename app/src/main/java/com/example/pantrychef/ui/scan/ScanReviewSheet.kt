// Bottom sheet to review detected scan candidates and confirm adding them to the pantry.
package com.example.pantrychef.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrychef.core.CandidateItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewSheet(
    items: List<CandidateItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<CandidateItem>) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Review detected items")
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { it ->
                    Text("- ${it.name} x${it.count} ${it.unit}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                Button(onClick = { onConfirm(items) }, modifier = Modifier.weight(1f)) { Text("Add selected") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
