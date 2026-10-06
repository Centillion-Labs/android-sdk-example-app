package com.centillion.sample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.centillion.sdk.*
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Sample() } } }
    }
    @Composable private fun Sample() {
        var agent by remember { mutableStateOf(intent.getStringExtra("agentId") ?: "") }
        var baseUrl by remember { mutableStateOf(intent.getStringExtra("baseUrl") ?: "https://app.cntillion.com/api/sdk") }
        var client by remember { mutableStateOf<CentillionClient?>(null) }
        val scope = rememberCoroutineScope()
        if (client == null) {
            Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Centillion", style = MaterialTheme.typography.headlineLarge)
                Text("Android SDK sample", style = MaterialTheme.typography.titleMedium)
                Text("Enable Android SDK in your agent's Overview, then enter its ID and your Centillion app URL.")
                OutlinedTextField(agent, { agent = it }, label = { Text("Agent ID") })
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("SDK base URL") })
                Button(onClick = { client = Centillion.create(this@MainActivity, agent.trim(), baseUrl.trim()) }, enabled = agent.isNotBlank()) { Text("Open support") }
            }
            LaunchedEffect(Unit) { if (intent.getBooleanExtra("smoke", false) && agent.isNotBlank()) client = Centillion.create(this@MainActivity, agent, baseUrl) }
        } else {
            val api = client!!
            val conversation = remember(api) { ConversationState(api) }
            val history = remember(api) { ConversationListState(api) }
            val state by conversation.state.collectAsState()
            val list by history.state.collectAsState()
            val scroll = rememberLazyListState()
            LaunchedEffect(state.messages.lastOrNull()?.text) { if (state.messages.isNotEmpty()) scroll.scrollToItem(state.messages.lastIndex) }
            var text by remember { mutableStateOf("") }
            var showHistory by remember { mutableStateOf(false) }
            var report by remember { mutableStateOf("") }
            DisposableEffect(api) { onDispose { api.close() } }
            LaunchedEffect(api) {
                api.tool("get_app_version") { JSONObject().put("version", "1.0.0").put("platform", "Android") }
                if (intent.getBooleanExtra("smoke", false)) {
                    try {
                        conversation.sendMessage("What can Centillion help me with? Please answer briefly.")
                        check(conversation.state.value.error == null) { conversation.state.value.error?.message ?: "No reply" }
                        val id = conversation.state.value.conversationId ?: error("No conversation")
                        conversation.loadHistory(id, 1)
                        val older = conversation.state.value.hasMoreHistory
                        conversation.loadMoreHistory()
                        val answer = conversation.state.value.messages.last { it.role == "assistant" }
                        conversation.retry(answer.id)
                        check(conversation.state.value.error == null)
                        conversation.sendMessage("What is my app version? Use the native action.")
                        check(conversation.state.value.error == null)
                        check(conversation.state.value.toolResults.isNotEmpty()) { "Native tool did not run" }
                        history.load()
                        report = "PASS · Stream · History · Retry · Native tool · ${history.state.value.conversations.size} conversations"
                        check(older)
                        Log.i("CENTILLION_SMOKE", report)
                    } catch (e: Exception) { report = "FAIL: ${e.message}"; Log.e("CENTILLION_SMOKE", report, e) }
                }
            }
            Column(Modifier.safeDrawingPadding().imePadding().fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Centillion", style = MaterialTheme.typography.headlineSmall); Text("Your in-app support assistant", style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { scope.launch { history.load(); showHistory = true } }) { Text("History") }
                }
                HorizontalDivider()
                if (report.isNotBlank()) Text(report, style = MaterialTheme.typography.labelSmall)
                if (state.hasMoreHistory) TextButton(onClick = { scope.launch { conversation.loadMoreHistory() } }) { Text("Load older messages") }
                LazyColumn(Modifier.weight(1f), state = scroll, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.messages.isEmpty()) item { Text("How can we help?", style = MaterialTheme.typography.titleLarge) }
                    items(state.messages, key = { it.id }) { message ->
                        Card(colors = CardDefaults.cardColors(containerColor = if (message.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(if (message.role == "user") "You" else "Centillion", style = MaterialTheme.typography.labelMedium)
                                if (message.role == "user") Text(message.text)
                                else MarkdownReply(message.text.ifEmpty { "Thinking…" })
                                if (message.role == "assistant" && !state.isSending) TextButton(onClick = { scope.launch { conversation.retry(message.id) } }) { Text("Retry") }
                            }
                        }
                    }
                }
                state.error?.let { Text(it.message ?: "The request failed.", color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Ask a question") }, enabled = !state.isSending)
                    Button(onClick = { val message = text; text = ""; scope.launch { conversation.sendMessage(message) } }, enabled = text.isNotBlank() && !state.isSending) { Text("Send") }
                }
                TextButton(onClick = { conversation.clear() }, enabled = !state.isSending) { Text("New conversation") }
            }
            if (showHistory) AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("Conversations") }, text = {
                LazyColumn { items(list.conversations) { item -> TextButton(onClick = { showHistory = false; scope.launch { conversation.loadHistory(item.id) } }) { Text(item.title) } }
                    if (list.hasMore) item { TextButton(onClick = { scope.launch { history.loadMore() } }) { Text("Load more") } }
                }
            }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } })
        }
    }
}

@Composable private fun MarkdownReply(text: String) {
    val context = LocalContext.current
    val renderer = remember(context) { Markwon.create(context) }
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        factory = { TextView(it).apply { textSize = 16f } },
        update = { view -> view.setTextColor(color); renderer.setMarkdown(view, text) },
    )
}
