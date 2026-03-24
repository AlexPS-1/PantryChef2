package com.example.pantrychef.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrychef.data.local.PantryDao
import com.example.pantrychef.data.local.PantryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryDao: PantryDao
) : ViewModel() {

    val items: StateFlow<List<PantryItem>> = pantryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun insertItem(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            pantryDao.insert(
                PantryItem(
                    id = 0L,
                    name = name.trim().ifBlank { "Unnamed" },
                    quantity = quantity,
                    unit = unit.trim().ifBlank { "pcs" }
                )
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            pantryDao.deleteById(id)
        }
    }

    fun updateItem(id: Long, name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            pantryDao.updateById(
                id = id,
                name = name.trim().ifBlank { "Unnamed" },
                quantity = quantity,
                unit = unit.trim().ifBlank { "pcs" }
            )
        }
    }
}