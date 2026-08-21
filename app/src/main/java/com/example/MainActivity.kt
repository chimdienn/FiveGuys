package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BiomateApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.SessionViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as BiomateApplication).container

        setContent {
            MyApplicationTheme {
                val sessionViewModel: SessionViewModel =
                    viewModel(factory = SessionViewModelFactory(container))
                val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

                BiomateApp(
                    container = container,
                    sessionViewModel = sessionViewModel,
                    sessionState = sessionState
                )
            }
        }
    }
}
