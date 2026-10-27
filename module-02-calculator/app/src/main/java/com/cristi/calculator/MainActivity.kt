package com.cristi.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The app's entry point (declared in AndroidManifest.xml as LAUNCHER).
 * Its job is small: apply the theme and show the calculator screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // The theme: a dark palette suited to the car. Every component
            // inside (Surface, Button, Text) takes its colors from here.
            MaterialTheme(colorScheme = darkColorScheme()) {
                // Surface paints the background and gives its children the text color.
                // (Without it we had "black text on black".)
                Surface(modifier = Modifier.fillMaxSize()) {
                    // viewModel() creates the ViewModel the first time and reuses it
                    // when the Activity is recreated.
                    val viewModel: CalculatorViewModel = viewModel()
                    CalculatorScreen(
                        state = viewModel.state,
                        onKey = viewModel::onKey,
                    )
                }
            }
        }
    }
}
