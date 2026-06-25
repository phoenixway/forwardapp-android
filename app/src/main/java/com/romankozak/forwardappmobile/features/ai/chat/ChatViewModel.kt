package com.romankozak.forwardappmobile.features.ai.chat

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.romankozak.forwardappmobile.core.data.models.entities.ai.ConversationEntity
import com.romankozak.forwardappmobile.data.repository.ChatRepository
import com.romankozak.forwardappmobile.data.repository.RolesRepository
import com.romankozak.forwardappmobile.data.repository.ScriptRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.aichat.GenerationService
import com.romankozak.forwardappmobile.domain.aichat.Message
import com.romankozak.forwardappmobile.domain.aichat.OllamaRuntimeOptions
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import com.romankozak.forwardappmobile.domain.aichat.RoleItem
import com.romankozak.forwardappmobile.domain.scripts.LuaScriptRunner
import com.romankozak.forwardappmobile.ui.ModelsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "AI_CHAT_DEBUG"
private const val RECENT_MESSAGES_AFTER_COMPRESSION = 8
private const val APPROX_CHARS_PER_TOKEN = 4
private const val SUMMARY_NUM_PREDICT = 1024

data class ChatUiState(
    val currentConversation: ConversationEntity? = null,
    val drawerItems: List<DrawerItem> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val systemPrompt: String = "",
    val roleTitle: String = "Assistant",
    val temperature: Float = 0.8f,
    val rolesHierarchy: List<RoleItem> = emptyList(),
    val smartModel: String = "",
    val availableModels: ModelsState = ModelsState.Success(emptyList()),
)

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val settingsRepo: SettingsRepository,
        private val chatRepo: ChatRepository,
        private val rolesRepo: RolesRepository,
        private val ollamaService: OllamaService,
        private val scriptRepository: ScriptRepository,
        private val luaScriptRunner: LuaScriptRunner,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private val _userInput = MutableStateFlow("")
        val userInput = _userInput.asStateFlow()
        private var modelsJob: Job? = null
        private var currentSendJob: Job? = null
        private var currentAssistantDraftId: Long? = null

        init {
            viewModelScope.launch {
                chatRepo.getDrawerItems().collectLatest { drawerItems ->
                    val filtered =
                        drawerItems.filterNot { item ->
                            item is DrawerItem.Conversation && item.conversationWithLastMessage.conversation.title == "Life Management"
                        }.map { item ->
                            if (item is DrawerItem.Folder) {
                                item.copy(
                                    conversations =
                                        item.conversations.filter { conv ->
                                            conv.conversation.title != "Life Management"
                                        },
                                )
                            } else {
                                item
                            }
                        }

                    _uiState.update { it.copy(drawerItems = filtered) }
                    if (uiState.value.currentConversation == null) {
                        val firstConversation =
                            filtered.firstNotNullOfOrNull { item ->
                                when (item) {
                                    is DrawerItem.Conversation -> item.conversationWithLastMessage.conversation
                                    is DrawerItem.Folder -> item.conversations.firstOrNull()?.conversation
                                }
                            }
                        firstConversation?.let { setCurrentConversation(it.id) } ?: startNewChat()
                    }
                }
            }

            viewModelScope.launch {
                uiState.map { it.currentConversation?.id }.distinctUntilChanged().collectLatest { conversationId ->
                    if (conversationId != null) {
                        chatRepo.getChatHistory(conversationId).collectLatest { history ->
                            _uiState.update { currentState ->
                                val newMessages = history.map { it.toChatMessage(conversationId) }
                                currentState.copy(messages = newMessages)
                            }
                        }
                    }
                }
            }

            viewModelScope.launch {
                combine(
                    settingsRepo.systemPromptFlow,
                    settingsRepo.roleTitleFlow,
                    settingsRepo.temperatureFlow,
                    rolesRepo.rolesHierarchyFlow,
                    settingsRepo.ollamaSmartModelFlow,
                ) { prompt, title, temp, roles, smartModel ->
                    _uiState.update {
                        it.copy(
                            systemPrompt = prompt,
                            roleTitle = title,
                            temperature = temp,
                            rolesHierarchy = roles,
                            smartModel = smartModel,
                        )
                    }
                }.collect {}
            }

            // авто-завантаження моделей щоб не зависав спінер
            loadAvailableModels()
        }

        fun setCurrentConversation(conversationId: Long) {
            viewModelScope.launch {
                setCurrentConversationNow(conversationId)
            }
        }

        private suspend fun setCurrentConversationNow(conversationId: Long) {
            val conversation = chatRepo.getConversationById(conversationId)
            _uiState.update {
                it.copy(
                    currentConversation = conversation,
                    messages = if (conversation == null) emptyList() else it.messages,
                )
            }
        }

        fun translateMessage(messageId: Long) {
            viewModelScope.launch {
                val message = _uiState.value.messages.find { it.id == messageId }
                if (message == null || message.text.isBlank()) return@launch

                _uiState.update { currentState ->
                    currentState.copy(
                        messages =
                            currentState.messages.map {
                                if (it.id == messageId) it.copy(isTranslating = true) else it
                            },
                    )
                }

                try {
                    val options =
                        TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.ENGLISH)
                            .setTargetLanguage(TranslateLanguage.UKRAINIAN)
                            .build()
                    val englishUkrainianTranslator = Translation.getClient(options)

                    val conditions = DownloadConditions.Builder().requireWifi().build()
                    englishUkrainianTranslator.downloadModelIfNeeded(conditions).await()

                    val translatedText = englishUkrainianTranslator.translate(message.text).await()

                    _uiState.update { currentState ->
                        currentState.copy(
                            messages =
                                currentState.messages.map {
                                    if (it.id == messageId) {
                                        it.copy(translatedText = translatedText, isTranslating = false)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                    englishUkrainianTranslator.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Translation failed", e)
                    _uiState.update { currentState ->
                        currentState.copy(
                            messages =
                                currentState.messages.map {
                                    if (it.id == messageId) {
                                        it.copy(
                                            translatedText = "Помилка перекладу: ${e.localizedMessage}",
                                            isTranslating = false,
                                        )
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                }
            }
        }

        fun loadAvailableModels() {
            modelsJob?.cancel()
            modelsJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(availableModels = ModelsState.Loading) }
                    try {
                        val baseUrl =
                            resolveOllamaBaseUrl() ?: run {
                                _uiState.update { it.copy(availableModels = ModelsState.Error("Не знайдено адресу Ollama")) }
                                return@launch
                            }
                        Log.d(TAG, "[Chat] Fetch models from $baseUrl")
                        val result = ollamaService.getAvailableModels(baseUrl)
                        result.onSuccess { models ->
                            Log.d(TAG, "[Chat] Models loaded: ${models.size}")
                            _uiState.update { it.copy(availableModels = ModelsState.Success(models)) }
                        }.onFailure { e ->
                            Log.e(TAG, "[Chat] Model fetch failed: ${e.message}", e)
                            _uiState.update { it.copy(availableModels = ModelsState.Error(e.message ?: "Помилка завантаження моделей")) }
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is CancellationException -> {
                                Log.w(TAG, "[Chat] Model fetch cancelled")
                                _uiState.update { it.copy(availableModels = ModelsState.Error("Запит скасовано")) }
                            }
                            else -> {
                                Log.e(TAG, "[Chat] Model fetch unexpected error: ${e.message}", e)
                                _uiState.update { it.copy(availableModels = ModelsState.Error(e.message ?: "Помилка завантаження моделей")) }
                            }
                        }
                    }
                }
        }

        private suspend fun resolveOllamaBaseUrl(): String? {
            val manualRaw = runCatching { settingsRepo.manualServerIpFlow.first() }.getOrNull().orEmpty()
            val port = runCatching { settingsRepo.ollamaPortFlow.first() }.getOrDefault(11434)

            val manualResolved =
                manualRaw.takeIf { it.isNotBlank() }?.let { url ->
                    if (url.startsWith("http")) url else "http://$url:$port"
                }
            if (!manualResolved.isNullOrBlank()) {
                Log.d(TAG, "[Chat] resolveOllamaBaseUrl manual raw='$manualRaw', resolved=$manualResolved")
                return manualResolved
            }

            val discovered =
                try {
                    withTimeoutOrNull(3_000) {
                        settingsRepo.getOllamaUrl().firstOrNull { !it.isNullOrBlank() }
                    }
                } catch (e: TimeoutCancellationException) {
                    null
                }
            Log.d(TAG, "[Chat] resolveOllamaBaseUrl fallback discovery, discovered=$discovered")
            return discovered
        }

        private suspend fun resolveOllamaModel(): String? {
            val smart = settingsRepo.ollamaSmartModelFlow.first()
            val fast = settingsRepo.ollamaFastModelFlow.first()
            return smart.ifBlank { fast }.takeIf { it.isNotBlank() }
        }

        private suspend fun resolveOllamaRuntimeOptions(): OllamaRuntimeOptions =
            OllamaRuntimeOptions(
                numCtx = settingsRepo.ollamaNumCtxFlow.first(),
                numPredict = settingsRepo.ollamaNumPredictFlow.first(),
                numBatch = settingsRepo.ollamaNumBatchFlow.first(),
                numGpu = settingsRepo.ollamaNumGpuFlow.first(),
                numThread = settingsRepo.ollamaNumThreadFlow.first(),
            )

        private suspend fun prepareMessagesForOllama(
            baseUrl: String,
            model: String,
            systemPrompt: String,
            historyMessages: List<Message>,
            temperature: Float,
            runtimeOptions: OllamaRuntimeOptions,
            autoCompressContext: Boolean,
            compressThresholdPercent: Int,
        ): List<Message> {
            val fullMessages = listOf(Message(role = "system", content = systemPrompt)) + historyMessages
            if (!autoCompressContext || historyMessages.size <= RECENT_MESSAGES_AFTER_COMPRESSION) return fullMessages

            val estimatedTokens = fullMessages.estimatedTokenCount()
            val thresholdTokens = (runtimeOptions.numCtx * compressThresholdPercent.coerceIn(10, 95)) / 100
            if (runtimeOptions.numCtx <= 0 || estimatedTokens < thresholdTokens) return fullMessages

            val recentMessages = historyMessages.takeLast(RECENT_MESSAGES_AFTER_COMPRESSION)
            val olderMessages = historyMessages.dropLast(RECENT_MESSAGES_AFTER_COMPRESSION)
            val summary = summarizeOlderMessages(
                baseUrl = baseUrl,
                model = model,
                messages = olderMessages,
                temperature = temperature,
                runtimeOptions = runtimeOptions,
            ).takeIf { it.isNotBlank() } ?: return fullMessages
            Log.d(
                TAG,
                "[Chat] Auto-compressed context: estimatedTokens=$estimatedTokens threshold=$thresholdTokens " +
                    "older=${olderMessages.size} recent=${recentMessages.size} summaryChars=${summary.length}",
            )
            return listOf(
                Message(
                    role = "system",
                    content =
                        "$systemPrompt\n\n" +
                            "Summary of earlier conversation, generated to keep within the context window:\n$summary",
                ),
            ) + recentMessages
        }

        private suspend fun summarizeOlderMessages(
            baseUrl: String,
            model: String,
            messages: List<Message>,
            temperature: Float,
            runtimeOptions: OllamaRuntimeOptions,
        ): String {
            val transcript =
                messages
                    .joinToString("\n") { "${it.role}: ${it.content}" }
                    .take((runtimeOptions.numCtx.coerceAtLeast(2048)) * APPROX_CHARS_PER_TOKEN)
            val summaryMessages =
                listOf(
                    Message(
                        role = "system",
                        content =
                            "You compress chat history. Preserve durable facts, user preferences, decisions, " +
                                "open tasks, code/file details, and unresolved questions. Be concise.",
                    ),
                    Message(
                        role = "user",
                        content = "Summarize this earlier conversation for future context:\n\n$transcript",
                    ),
                )
            val summaryBuilder = StringBuilder()
            val summaryOptions = runtimeOptions.copy(numPredict = SUMMARY_NUM_PREDICT)
            runCatching {
                ollamaService
                    .generateChatResponseStream(baseUrl, model, summaryMessages, temperature, summaryOptions)
                    .collect { chunk -> summaryBuilder.append(chunk) }
            }.onFailure { error ->
                Log.w(TAG, "[Chat] Context auto-compression failed: ${error.message}", error)
            }
            return summaryBuilder.toString().trim()
        }

        private fun List<Message>.estimatedTokenCount(): Int =
            sumOf { message ->
                (message.role.length + message.content.length).coerceAtLeast(1) / APPROX_CHARS_PER_TOKEN + 1
            }

        fun selectSmartModel(modelName: String) {
            viewModelScope.launch {
                val currentFastModel = settingsRepo.ollamaFastModelFlow.first()
                settingsRepo.saveOllamaModels(fastModel = currentFastModel, smartModel = modelName)
            }
        }

        fun onUserInputChange(text: String) {
            _userInput.value = text
        }

        fun sendMessage() {
            val text = _userInput.value.trim()
            if (text.isBlank()) return
            if (currentSendJob?.isActive == true) return
            currentSendJob =
                viewModelScope.launch {
                    val conversationId = ensureConversation()

                    val userMessage =
                        ChatMessage(
                            conversationId = conversationId,
                            text = text,
                            isFromUser = true,
                        )
                    chatRepo.addMessage(userMessage.toEntity())
                    _userInput.value = ""
                    _uiState.update { it.copy(isLoading = true) }
                    Log.d(TAG, "[Chat] User message queued, id=$conversationId textLen=${text.length}")

                    val baseUrl = resolveOllamaBaseUrl()
                    val model = resolveOllamaModel()
                    if (baseUrl.isNullOrBlank() || model.isNullOrBlank()) {
                        Log.e(TAG, "[Chat] Missing Ollama config. url=$baseUrl model=$model")
                        chatRepo.addMessage(
                            ChatMessage(
                                conversationId = conversationId,
                                text = "Не налаштовано Ollama (URL або модель).",
                                isFromUser = false,
                                isError = true,
                            ).toEntity(),
                        )
                        _uiState.update { it.copy(isLoading = false) }
                        currentSendJob = null
                        return@launch
                    }

                    var assistantDraftId: Long? = null
                    try {
                        val historyEntities = chatRepo.getChatHistory(conversationId).first()
                        val systemPrompt = settingsRepo.systemPromptFlow.first()
                        val temperature = settingsRepo.temperatureFlow.first()
                        val runtimeOptions = resolveOllamaRuntimeOptions()
                        val autoCompressContext = settingsRepo.ollamaAutoCompressContextFlow.first()
                        val compressThresholdPercent = settingsRepo.ollamaCompressThresholdPercentFlow.first()
                        Log.d(TAG, "[Chat] Sending to Ollama url=$baseUrl model=$model temp=$temperature history=${historyEntities.size}")
                        val messages =
                            prepareMessagesForOllama(
                                baseUrl = baseUrl!!,
                                model = model!!,
                                systemPrompt = systemPrompt,
                                historyMessages =
                                    historyEntities.map { msg ->
                                        Message(role = if (msg.isFromUser) "user" else "assistant", content = msg.text)
                                    },
                                temperature = temperature,
                                runtimeOptions = runtimeOptions,
                                autoCompressContext = autoCompressContext,
                                compressThresholdPercent = compressThresholdPercent,
                            )

                        assistantDraftId =
                            chatRepo.addMessage(
                                ChatMessage(
                                    conversationId = conversationId,
                                    text = "",
                                    isFromUser = false,
                                    isStreaming = true,
                                ).toEntity(),
                            )
                        currentAssistantDraftId = assistantDraftId

                        val responseBuilder = StringBuilder()
                        ollamaService
                            .generateChatResponseStream(baseUrl, model, messages, temperature, runtimeOptions)
                            .collect { chunk ->
                                responseBuilder.append(chunk)
                                assistantDraftId?.let { id ->
                                    chatRepo.updateMessageContent(
                                        messageId = id,
                                        text = responseBuilder.toString(),
                                        isStreaming = true,
                                    )
                                }
                            }
                        Log.d(TAG, "[Chat] Ollama response received chars=${responseBuilder.length}")

                        assistantDraftId?.let { id ->
                            chatRepo.updateMessageContent(
                                messageId = id,
                                text = responseBuilder.toString(),
                                isStreaming = false,
                            )
                        }
                        currentAssistantDraftId = null
                    } catch (e: CancellationException) {
                        Log.w(TAG, "[Chat] sendMessage cancelled")
                        assistantDraftId?.let { id ->
                            val latestText = _uiState.value.messages.find { it.id == id }?.text.orEmpty()
                            val stoppedText =
                                if (latestText.isNotBlank()) "$latestText\n\n[Stopped by user]" else "[Stopped by user]"
                            chatRepo.updateMessageContent(
                                messageId = id,
                                text = stoppedText,
                                isStreaming = false,
                                isError = true,
                            )
                        }
                        currentAssistantDraftId = null
                    } catch (e: Exception) {
                        Log.e(TAG, "[Chat] sendMessage failed: ${e.message}", e)
                        assistantDraftId?.let { id ->
                            chatRepo.updateMessageContent(
                                messageId = id,
                                text = e.message ?: "Request failed",
                                isStreaming = false,
                                isError = true,
                            )
                        } ?: chatRepo.addMessage(
                            ChatMessage(
                                conversationId = conversationId,
                                text = e.message ?: "Request failed",
                                isFromUser = false,
                                isError = true,
                            ).toEntity(),
                        )
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                        currentSendJob = null
                        currentAssistantDraftId = null
                    }
                }
        }

        fun regenerateLastResponse() {
            val conversation = _uiState.value.currentConversation
            if (conversation == null) return

            viewModelScope.launch {
                chatRepo.deleteLastAssistantMessage(conversation.id)
                val lastUserMessage = _uiState.value.messages.filter { it.isFromUser }.lastOrNull()
                if (lastUserMessage != null) {
                    _userInput.value = lastUserMessage.text
                    sendMessage()
                }
            }
        }

        fun runScript(scriptId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val conversationId = ensureConversation()
                val script = scriptRepository.getScriptById(scriptId)
                if (script == null) {
                    chatRepo.addMessage(
                        ChatMessage(
                            conversationId = conversationId,
                            text = "Скрипт не знайдено",
                            isFromUser = false,
                            isError = true,
                        ).toEntity(),
                    )
                    return@launch
                }
                val startedAt = System.currentTimeMillis()
                val startMessage =
                    ChatMessage(
                        conversationId = conversationId,
                        text =
                            buildString {
                                appendLine("\uD83E\uDDF9 Запускаю скрипт \"${script.name}\"")
                                appendLine(
                                    "Контекст: input=${_userInput.value.take(
                                        60,
                                    )}, conversation_title=${_uiState.value.currentConversation?.title ?: "Chat"}",
                                )
                                appendLine("Підказка: save_chat_md(folder?, filename?) збереже чат у Markdown")
                            },
                        isFromUser = false,
                        isError = false,
                    )
                chatRepo.addMessage(startMessage.toEntity())
                val contextMap =
                    mapOf(
                        "input" to _userInput.value,
                        "conversation_title" to (_uiState.value.currentConversation?.title ?: "Chat"),
                    )
                val markdown = exportChatMarkdown()
                val saveHelper = createSaveChatHelper(markdown)
                val result = luaScriptRunner.runScript(script.content, contextMap, mapOf("save_chat_md" to saveHelper))
                val isError = result.isFailure
                val durationMs = System.currentTimeMillis() - startedAt
                val messageText =
                    result.fold(
                        onSuccess = { value ->
                            buildString {
                                appendLine("✅ Скрипт \"${script.name}\" виконано за ${durationMs}мс")
                                append("Вивід: $value")
                            }
                        },
                        onFailure = { e ->
                            buildString {
                                appendLine("❌ Помилка виконання \"${script.name}\" за ${durationMs}мс")
                                appendLine(formatScriptErrorMessage(e))
                                append("Перевірте код або контекст і спробуйте ще раз.")
                            }
                        },
                    )
                chatRepo.addMessage(
                    ChatMessage(
                        conversationId = conversationId,
                        text = messageText,
                        isFromUser = false,
                        isError = isError,
                    ).toEntity(),
                )
            }
        }

        private fun createSaveChatHelper(markdown: String): LuaValue =
            object : TwoArgFunction() {
                override fun call(
                    arg1: LuaValue,
                    arg2: LuaValue,
                ): LuaValue {
                    val folder = arg1.optjstring("")
                    val fileName =
                        arg2.optjstring(
                            "chat-" +
                                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date()) +
                                ".md",
                        )
                    val result = saveChatToPath(folder, fileName, markdown)
                    return result.fold(
                        onSuccess = { path -> valueOf(path) },
                        onFailure = { error -> error("Save failed: ${error.message ?: error}") },
                    )
                }
            }

        private fun saveChatToPath(
            folder: String?,
            fileName: String,
            content: String,
        ): Result<String> =
            runCatching {
                val baseDir =
                    if (folder.isNullOrBlank()) {
                        context.getExternalFilesDir(null)
                    } else {
                        val folderFile = File(folder)
                        if (folderFile.isAbsolute) {
                            return@runCatching saveToPublicFolder(folderFile, fileName, content)
                        } else {
                            File(context.getExternalFilesDir(null), folder)
                        }
                    } ?: error("Storage unavailable")
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    error("Cannot create folder: ${baseDir.absolutePath}")
                }
                val target = File(baseDir, fileName)
                target.writeText(content)
                target.absolutePath
            }

        private fun saveToPublicFolder(
            folder: File,
            fileName: String,
            content: String,
        ): String {
            val relativePath =
                buildString {
                    val root = "/storage/emulated/0/"
                    val clean = folder.path.removePrefix(root).trim('/')
                    append("Documents/")
                    if (clean.isNotBlank()) append(clean).append('/')
                }

            val values =
                ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "text/markdown")
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                }
            val resolver = context.contentResolver
            val uri: Uri =
                resolver.insert(MediaStore.Files.getContentUri("external"), values)
                    ?: error("Cannot create media entry")
            resolver.openOutputStream(uri)?.use { stream ->
                stream.writer().use { writer -> writer.write(content) }
            } ?: error("Cannot open output stream")
            return "/storage/emulated/0/$relativePath$fileName"
        }

        suspend fun exportChatMarkdownToConfiguredFolder(): Result<String> =
            runCatching {
                val folderUriString = settingsRepo.chatExportFolderUriFlow.first()
                if (folderUriString.isBlank()) {
                    error("Markdown export folder is not selected in AI settings.")
                }
                val folderUri = Uri.parse(folderUriString)
                val fileName = buildChatExportFileName()
                val markdown = exportChatMarkdown()
                val treeDocumentUri =
                    DocumentsContract.buildDocumentUriUsingTree(
                        folderUri,
                        DocumentsContract.getTreeDocumentId(folderUri),
                    )
                val targetUri =
                    DocumentsContract.createDocument(
                        context.contentResolver,
                        treeDocumentUri,
                        "text/markdown",
                        fileName,
                    ) ?: error("Cannot create markdown file in selected folder.")
                context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                    stream.writer().use { writer -> writer.write(markdown) }
                } ?: error("Cannot open markdown file for writing.")
                fileName
            }

        private fun buildChatExportFileName(): String {
            val title =
                _uiState.value.currentConversation
                    ?.title
                    ?.takeIf { it.isNotBlank() }
                    ?: "chat"
            val safeTitle =
                title
                    .replace(Regex("""[^\p{L}\p{N}._ -]+"""), "_")
                    .trim()
                    .take(60)
                    .ifBlank { "chat" }
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            return "$safeTitle-$timestamp.md"
        }

        fun exportChatMarkdown(): String {
            val roleTitle = _uiState.value.roleTitle
            return _uiState.value.messages.joinToString("\n\n") { msg ->
                val prefix = if (msg.isFromUser) "### USER" else "### ASSISTANT ($roleTitle)"
                "$prefix\n${msg.text}"
            }
        }

        private fun formatScriptErrorMessage(e: Throwable): String {
            val firstTrace = e.stackTrace.firstOrNull()
            return buildString {
                appendLine("Тип: ${e.javaClass.simpleName}")
                appendLine("Деталі: ${e.message ?: "без повідомлення"}")
                firstTrace?.let {
                    append("Стек: ${it.className}:${it.lineNumber}")
                }
            }
        }

        private suspend fun ensureConversation(): Long {
            val existing = _uiState.value.currentConversation?.id
            if (existing != null && chatRepo.getConversationById(existing) != null) return existing
            val newId = chatRepo.createConversation("New Chat")
            setCurrentConversationNow(newId)
            return newId
        }

        fun stopGeneration() {
            currentSendJob?.cancel()
            viewModelScope.launch {
                currentAssistantDraftId?.let { id ->
                    val latestText = _uiState.value.messages.find { it.id == id }?.text.orEmpty()
                    val stoppedText =
                        if (latestText.isNotBlank()) "$latestText\n\n[Stopped by user]" else "[Stopped by user]"
                    chatRepo.updateMessageContent(
                        messageId = id,
                        text = stoppedText,
                        isStreaming = false,
                        isError = true,
                    )
                }
                _uiState.update { it.copy(isLoading = false) }

                val serviceIntent = Intent(context, GenerationService::class.java)
                context.stopService(serviceIntent)
            }
        }

        fun startNewChat() {
            viewModelScope.launch {
                val newConversationId = chatRepo.createConversation("New Chat")
                setCurrentConversationNow(newConversationId)
            }
        }

        fun updateConversationTitle(newTitle: String) {
            viewModelScope.launch {
                _uiState.value.currentConversation?.let {
                    val updatedConversation = it.copy(title = newTitle)
                    chatRepo.updateConversation(updatedConversation)
                    _uiState.update { state -> state.copy(currentConversation = updatedConversation) }
                }
            }
        }

        fun deleteConversation(conversationId: Long) {
            viewModelScope.launch {
                val deletingCurrent = _uiState.value.currentConversation?.id == conversationId
                if (deletingCurrent) {
                    currentSendJob?.cancel()
                    currentSendJob = null
                    currentAssistantDraftId = null
                }
                chatRepo.deleteConversation(conversationId)
                if (deletingCurrent) {
                    _userInput.value = ""
                    _uiState.update {
                        it.copy(
                            currentConversation = null,
                            messages = emptyList(),
                            isLoading = false,
                        )
                    }
                    val newConversationId = chatRepo.createConversation("New Chat")
                    setCurrentConversationNow(newConversationId)
                }
            }
        }

        fun updateSystemPromptAndTitle(
            newPrompt: String,
            newTitle: String,
        ) {
            viewModelScope.launch {
                settingsRepo.setSystemPrompt(newPrompt)
                settingsRepo.setRoleTitle(newTitle)
                updateConversationTitle(newTitle)
            }
        }

        fun updateTemperature(newTemperature: Float) {
            viewModelScope.launch {
                settingsRepo.setTemperature(newTemperature)
            }
        }

        fun exportChat(): String {
            val roleTitle = _uiState.value.roleTitle
            return _uiState.value.messages.joinToString("\n\n") { msg ->
                val prefix = if (msg.isFromUser) "[USER]" else "[ASSISTANT] - $roleTitle"
                "$prefix\n${msg.text}"
            }
        }
    }
