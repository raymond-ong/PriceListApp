package com.example.pricelistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pricelistapp.data.UserRepository
import com.example.pricelistapp.data.local.AppDatabase
import com.example.pricelistapp.ui.login.LoginScreen
import com.example.pricelistapp.ui.login.LoginViewModel
import com.example.pricelistapp.ui.login.LoginViewModelFactory
import com.example.pricelistapp.ui.main.MainScreen
import com.example.pricelistapp.ui.theme.PriceListAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = UserRepository(database.userDao(), database.deviceCacheDao())
        val viewModelFactory = LoginViewModelFactory(repository)

        setContent {
            PriceListAppTheme {
                val viewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsState()

                val loggedInUser = uiState.loggedInUser
                if (loggedInUser != null) {
                    MainScreen(
                        user = loggedInUser,
                        currentRole = uiState.userRole,
                        onRoleChange = { viewModel.onRoleChange(it) },
                        onLogout = { viewModel.onLogout() }
                    )
                } else {
                    LoginScreen(
                        uiState = uiState,
                        onModeChange = { viewModel.onModeChange(it) },
                        onNewUserNameChange = { viewModel.onNewUserNameChange(it) },
                        onSelectedUserChange = { viewModel.onSelectedUserChange(it) },
                        onSubmit = { viewModel.onSubmit() }
                    )
                }
            }
        }
    }
}
