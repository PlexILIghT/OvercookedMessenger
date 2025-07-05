package com.plexilight.overcookedmessenger

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.common.collect.Iterables.limit
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.User
import java.lang.reflect.Member
import java.time.Clock.offset
import java.util.Arrays.sort
import java.util.Locale.filter

@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    onUserSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            isLoading = true
            try {
//                channelClient.queryMembers(offset, limit, filter, sort).enqueue { result ->
//                    when (result) {
//                        is Result.Success -> {
//                            val members: List<Member> = result.value
//                        }
//                        is Result.Failure -> {
//                            Log.e(TAG, String.format("There was an error %s", result.value), result.value.extractCause())
//                        }
//                    }
//                }
                searchResults = searchUsersInBackend(searchQuery)
            } catch (e: Exception) {
                Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            searchResults = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Хедер с поиском
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search users") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }
            )
        }

        // Результаты поиска
        LazyColumn {
            items(searchResults) { user ->
                UserListItem(
                    user = user,
                    onClick = { onUserSelected(user.id) }
                )
            }
        }
    }
}

@Composable
fun UserListItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
//        AsyncImage(
//            model = user.image,
//            contentDescription = "User avatar",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier
//                .size(48.dp)
//                .clip(CircleShape)
//        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// Заглушка для поиска пользователей (реализуйте реальный поиск)
private fun searchUsersInBackend(query: String): List<User> {
    return listOf(
        User(id = "user1", name = "John Doe", image = "https://i.postimg.cc/vHnXCRGW/jufufu.webp"),
        User(id = "user2", name = "Jane Smith", image = "https://i.postimg.cc/vHnXCRGW/jufufu.webp"),
        User(id = "user3", name = "Bob Johnson", image = "https://i.postimg.cc/vHnXCRGW/jufufu.webp"),
    ).filter { it.name.contains(query, ignoreCase = true) }
}