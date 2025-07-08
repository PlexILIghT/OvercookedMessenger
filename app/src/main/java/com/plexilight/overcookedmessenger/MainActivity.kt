package com.plexilight.overcookedmessenger

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QueryChannelRequest
import io.getstream.chat.android.client.channel.ChannelClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.compose.ui.channels.ChannelsScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.compose.viewmodel.channels.ChannelViewModelFactory
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.InitializationState
import io.getstream.chat.android.models.User
import io.getstream.chat.android.models.querysort.QuerySorter
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.uuid.Uuid.Companion.random

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var client: ChatClient

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
        val statePluginFactory =
            StreamStatePluginFactory(config = StatePluginConfig(), appContext = this)

        client = ChatClient.Builder("kpjpryjdzfmt", applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()

        auth.currentUser?.let { firebaseUser ->
            registerStreamUser(firebaseUser)
        }

        setContent {
            val navController = rememberNavController()
            val firebaseUser = auth.currentUser

            NavHost(
                navController = navController,
                startDestination = if (firebaseUser != null) Screen.MainChat.route else Screen.Login.route
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = { navController.navigate(Screen.MainChat.route) },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        onRegisterSuccess = { navController.navigate(Screen.MainChat.route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MainChat.route) {
                    MainChatScreen(
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onSearchUsersClick = { navController.navigate(Screen.UserSearch.route) }
                    )
                }

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

//                composable(Screen.UserSearch.route) {
//                    UserSearchScreen(
//                        onBack = { navController.popBackStack() },
//                        onUserSelected = { userId ->
//                            {
//                                val myUuid = UUID.randomUUID()
//                                val myUuidAsString = myUuid.toString()
//
//                                client.createChannel(
//                                    "messaging",
//                                    channelId = myUuidAsString,
//                                    memberIds = listOf(giveString(), userId),
//                                    extraData = mapOf(
//                                        "name" to "Direct chat",
//                                        "image" to "https://i.postimg.cc/vHnXCRGW/jufufu.webp"
//                                    ),
//                                )
//
//                            }
//                        }
//                    )
//                }

                composable(Screen.UserSearch.route) {
                    UserSearchScreen(
                        onBack = { navController.popBackStack() },
                        onUserSelected = { userId ->

                            lifecycleScope.launch(Dispatchers.IO) {
                                val currentUserId = auth.currentUser?.uid
                                if (currentUserId == null) {
                                    return@launch
                                }

                                val channelId = listOf(currentUserId, userId).sorted().joinToString("_")
                                try {
                                    val channel = client.createChannel(
                                        channelType = "messaging",
                                        channelId = channelId,
                                        memberIds = listOf(currentUserId, userId),
                                        extraData = mapOf(
                                            "name" to "Direct chat",
                                            "image" to "https://i.postimg.cc/vHnXCRGW/jufufu.webp",
                                            "members" to listOf(
                                                mapOf("userId" to currentUserId, "role" to "member"),
                                                mapOf("userId" to userId, "role" to "member")
                                            )
                                        )
                                    ).await().getOrNull()

                                    withContext(Dispatchers.Main) {
                                        startActivity(
                                            ChannelActivity.getIntent(
                                                this@MainActivity,
                                                channel!!.cid
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("Channel", "Error creating channel", e)

                                }
                            }
                        }
                    )
                }


            }
        }
    }

    private fun registerStreamUser(firebaseUser: FirebaseUser) {
        val user = firebaseUser.firebaseUserToChatUser2()
        client.connectUser(
            user = user,
            token = client.devToken(user.id)
        ).enqueue { result ->
            if (result.isSuccess) {
                Log.d("MainActivity", "User connected to Stream")
            } else {
                // Используем result.error().message вместо result.error()
                Log.e("MainActivity", "Connection failed: ${result.getOrNull()}")
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
        val user = auth.currentUser?.firebaseUserToChatUser2()

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
                    onBackPressed = { finish() },
                    viewModelFactory = ChannelViewModelFactory(
                        filters = Filters.and(
                            Filters.eq("type", "messaging"),
                            Filters.`in`("members", listOf(auth.currentUser!!.uid))
                        )
                    )
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

fun FirebaseUser.firebaseUserToChatUser2(): User {
    return User(
        id = uid,
        name = displayName ?: email?.substringBefore("@") ?: "Unknown",
        image = photoUrl?.toString() ?: "https://i.postimg.cc/vHnXCRGW/jufufu.webp"
    )
}

