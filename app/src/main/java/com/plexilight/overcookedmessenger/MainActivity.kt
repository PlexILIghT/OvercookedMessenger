package com.plexilight.overcookedmessenger

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        val offlinePluginFactory = StreamOfflinePluginFactory(appContext = applicationContext)
        val statePluginFactory = StreamStatePluginFactory(config = StatePluginConfig(), appContext = this)

        // Инициализация Stream
        client = ChatClient.Builder("kpjpryjdzfmt", applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()

        setContent {
            val firebaseUser = auth.currentUser

            when {
                firebaseUser != null -> MainChatScreen()
                else -> LoginScreen { email, password ->
                    signInWithFirebase(email, password)
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

    @Composable
    fun MainChatScreen() {
        val user = auth.currentUser?.firebaseUserToChatUser() // Конвертация в Stream User

        LaunchedEffect(user) {
            user?.let {
                client.connectUser(
                    user = it,
                    token = client.devToken(it.id) // Только для разработки!
                ).enqueue()
            }
        }

        ChatTheme {
            val clientInitialisationState by client.clientState.initializationState.collectAsState()
            when (clientInitialisationState) {
                InitializationState.COMPLETE -> ChannelsScreen(
                    title = "Stream Chat",
                    onChannelClick = { channel ->
                        startActivity(ChannelActivity.getIntent(this, channel.cid))
                    },
                    onBackPressed = { finish() }
                )
                else -> CircularProgressIndicator()
            }
        }
    }
}

// Расширение для Firebase User
fun FirebaseUser.firebaseUserToChatUser(): User {
    return User(
        id = uid,
        name = displayName ?: email?.substringBefore("@") ?: "Unknown",
        image = photoUrl?.toString() ?: "https://postimg.cc/dLq8Txk1"
    )
}
