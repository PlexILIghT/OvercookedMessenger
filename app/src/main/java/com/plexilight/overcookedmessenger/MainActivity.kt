package com.plexilight.overcookedmessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.token.TokenProvider
import io.getstream.chat.android.compose.ui.channels.ChannelsScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.models.InitializationState
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra("USER_ID") ?: run {
            finish()
            return
        }

        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val userImage = intent.getStringExtra("USER_IMAGE") ?: ""

        val offlinePluginFactory = StreamOfflinePluginFactory(appContext = applicationContext)
        val statePluginFactory = StreamStatePluginFactory(config = StatePluginConfig(), appContext = this)

        val client = ChatClient.Builder("kpjpryjdzfmt", applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()

        val token = client.devToken(userId)

        client.connectUser(
            user = User(
                id = userId,
                name = userName,
                image = userImage
            ),
            token = token
        ).enqueue()

        setContent {
            val clientInitialisationState by client.clientState.initializationState.collectAsState()

            ChatTheme {
                when (clientInitialisationState) {
                    InitializationState.COMPLETE -> {
                        ChannelsScreen(
                            title = stringResource(id = R.string.app_name),
                            isShowingHeader = true,
                            onChannelClick = { channel ->
                                startActivity(ChannelActivity.getIntent(this, channel.cid))
                            },
                            onBackPressed = { finish() }
                        )
                    }
                    InitializationState.INITIALIZING -> {
                        Text(text = "Initializing...")
                    }
                    InitializationState.NOT_INITIALIZED -> {
                        Text(text = "Not initialized...")
                    }
                }
            }
        }
    }
}