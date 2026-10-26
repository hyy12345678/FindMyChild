package com.findmychild.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
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

                    // 必须等首帧路由决定完再挂 NavHost：startDestination 只在建图时生效，
                    // 挂上去之后再改它不会把用户带到已配对的那一页（冷启动回到角色选择页），
                    // 而反复改则会让 NavHost 重建。先挂再改两头都不对。
                    val route = startupRoute
                    if (route != null) {
                        AppNavGraph(
                            navController = navController,
                            startDestination = route
                        )
                    } else {
                        // 读 DataStore 的几毫秒，避免闪一下空白
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
