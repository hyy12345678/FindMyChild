package com.findmychild.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.ui.navigation.AppNavGraph
import com.findmychild.app.ui.navigation.NavRoute
import com.findmychild.app.ui.theme.FindMyChildTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FindMyChildTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startupViewModel: StartupViewModel = hiltViewModel()
                    val startupRoute by startupViewModel.startupRoute.collectAsState()

                    AppNavGraph(
                        navController = navController,
                        startDestination = startupRoute,
                        onRoleSelected = { role -> }
                    )
                }
            }
        }
    }
}
