package com.plexilight.overcookedmessenger

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.compose.ui.channels.ChannelsScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.models.InitializationState
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var client: ChatClient

    // Определим маршруты навигации
    sealed class Screen(val route: String) {
        object Login : Screen("login")
        object Register : Screen("register")
        object MainChat : Screen("main_chat")
        object Profile : Screen("profile")
        object UserSearch : Screen("user_search")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        val offlinePluginFactory = StreamOfflinePluginFactory(appContext = applicationContext)
        val statePluginFactory = StreamStatePluginFactory(config = StatePluginConfig(), appContext = this)

        client = ChatClient.Builder("kpjpryjdzfmt", applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()

        setContent {
            val navController = rememberNavController()
            val firebaseUser = auth.currentUser

            NavHost(
                navController = navController,
                startDestination = if (firebaseUser != null) Screen.MainChat.route else Screen.Login.route
            ) {
                // Экран входа
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { navController.navigate(Screen.MainChat.route) },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }

                // Экран регистрации
                composable(Screen.Register.route) {
                    RegisterScreen(
                        onRegisterSuccess = { navController.navigate(Screen.MainChat.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Главный экран чата
                composable(Screen.MainChat.route) {
                    MainChatScreen(
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onSearchUsersClick = { navController.navigate(Screen.UserSearch.route) }
                    )
                }

                // Экран профиля
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.MainChat.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Экран поиска пользователей
                composable(Screen.UserSearch.route) {
                    UserSearchScreen(
                        onBack = { navController.popBackStack() },
                        onUserSelected = { userId ->
                            // TODO: Логика создания канала
                        }
                    )
                }
            }
        }
    }

    private fun signInWithFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("Auth", "Login failed", task.exception)
                }
            }
    }

    private fun logout() {
        auth.signOut()
        client.disconnect(flushPersistence = false).enqueue { disconnectResult ->
            if (disconnectResult.isSuccess) {
//                client.connectUser(user, token).enqueue { /* ... */ }
            } else {
                // Handle Result.Failure
            }
        }
    }

    @Composable
    fun MainChatScreen(
        onProfileClick: () -> Unit,
        onSearchUsersClick: () -> Unit
    ) {
        val user = auth.currentUser?.firebaseUserToChatUser()

        LaunchedEffect(user) {
            user?.let {
                client.connectUser(
                    user = it,
                    token = client.devToken(it.id)
                ).enqueue()
            }
        }

        ChatTheme {
            val clientInitialisationState by client.clientState.initializationState.collectAsState()
            when (clientInitialisationState) {
                InitializationState.COMPLETE -> ChannelsScreen(
                    title = stringResource(id = R.string.app_name),
                    onChannelClick = { channel ->
                        startActivity(ChannelActivity.getIntent(this, channel.cid))
                    },
                    onHeaderActionClick = onSearchUsersClick,
                    onHeaderAvatarClick = onProfileClick,
                    onBackPressed = { finish() }
                )
                else -> {
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

fun FirebaseUser.firebaseUserToChatUser(): User {
    return User(
        id = uid,
        name = displayName ?: email?.substringBefore("@") ?: "Unknown",
        image = photoUrl?.toString() ?: "https://i.postimg.cc/vHnXCRGW/jufufu.webp"
    )
}