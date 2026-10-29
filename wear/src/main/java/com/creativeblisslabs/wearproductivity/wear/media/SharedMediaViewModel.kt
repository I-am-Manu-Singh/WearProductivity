package com.creativeblisslabs.wearproductivity.wear.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedMediaViewModel(application: Application) : AndroidViewModel(application) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Fake progress ticker for demonstration purposes across screens
    init {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                if (_isPlaying.value) {
                    _progress.value = (_progress.value + 0.01f) % 1f
                }
            }
        }
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun skipToPrevious() {
        _progress.value = 0f
    }

    fun skipToNext() {
        _progress.value = 0f
    }

    fun setProgress(newProgress: Float) {
        _progress.value = newProgress
    }
}
