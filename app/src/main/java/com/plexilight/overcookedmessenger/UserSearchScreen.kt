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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.common.collect.Iterables.limit
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isLoading = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val results = searchUsersInBackend(searchQuery)
                    withContext(Dispatchers.Main) {
                        searchResults = results
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
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
        AsyncImage(
            model = user.image,
            contentDescription = "User avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private suspend fun searchUsersInBackend(query: String): List<User> = withContext(Dispatchers.IO) {
    val db = Firebase.firestore
    try {

        val querySnapshot = db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get()
            .await()

        querySnapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: ""
            val uid = doc.getString("uid") ?: ""
            val email = doc.getString("email") ?: ""

            User(
                id = uid,
                name = name,
                image = "https://i.postimg.cc/vHnXCRGW/jufufu.webp"
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}