package com.example.safescreen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.safescreen.AppContainer
import com.example.safescreen.SafeScreenApp

/** Builds ViewModels from the app-wide [AppContainer]: `viewModel(factory = containerFactory { MyVm(it) })`. */
fun <VM : ViewModel> containerFactory(build: (AppContainer) -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = extras[APPLICATION_KEY] as SafeScreenApp
            return build(app.container) as T
        }
    }
