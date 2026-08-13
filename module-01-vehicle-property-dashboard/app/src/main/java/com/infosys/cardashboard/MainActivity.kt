package com.infosys.cardashboard

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.infosys.cardashboard.ui.DashboardScreen
import com.infosys.cardashboard.ui.theme.AutomotiveCarPropAppTheme

/**
 * The one and only screen of the app.
 *
 * Its job is small:
 *   1. Ask the user for the vehicle-data permissions.
 *   2. Tell the ViewModel to connect to the car.
 *   3. Show the dashboard.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: VehicleDashboardViewModel by viewModels()

    // Runtime permissions the user must approve (CAR_INFO is granted at install).
    private val requiredPermissions = arrayOf(
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_POWERTRAIN",
        "android.car.permission.CAR_ENERGY",
        "android.car.permission.CAR_MILEAGE"
    )

    // Shows the system permission dialog; when the user answers, we connect.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.connect()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Day/night theme toggle (an optional-enhancement from the brief).
            var night by rememberSaveable { mutableStateOf(true) }
            AutomotiveCarPropAppTheme(darkTheme = night) {
                // Reading viewModel.uiState here means Compose redraws this
                // screen automatically every time a vehicle value changes.
                DashboardScreen(
                    state = viewModel.uiState,
                    night = night,
                    onToggleTheme = { night = !night }
                )
            }
        }

        if (hasAllPermissions()) {
            viewModel.connect()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
