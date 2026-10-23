package com.onlasdan.comic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.onlasdan.comic.ui.source.SourcePreferences
import com.onlasdan.comic.ui.source.SourceRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val selectedSource: StateFlow<String> =
        SourcePreferences.selectedSourceId(application)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                savedStateHandle.get<String>("selected_source") ?: SourceRegistry.defaultSource.id,
            )

    val defaultSource: StateFlow<String> =
        SourcePreferences.defaultSourceId(application)
            .stateIn(viewModelScope, SharingStarted.Eagerly, SourceRegistry.defaultSource.id)

    fun selectSource(source: String) {
        savedStateHandle.set("selected_source", source)
        viewModelScope.launch {
            SourcePreferences.setSelectedSourceId(getApplication(), source)
        }
    }

    fun setDefaultSource(source: String) {
        viewModelScope.launch {
            SourcePreferences.setDefaultSourceId(getApplication(), source)
        }
    }
}
