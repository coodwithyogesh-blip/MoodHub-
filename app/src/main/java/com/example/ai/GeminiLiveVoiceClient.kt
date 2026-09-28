package com.example.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.action.ContactResult
import com.example.action.DeviceActionBridge
import com.example.audio.AudioOutputPlayer
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConversationMessageEntity
import com.example.service.ArushiAssistantService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    WORKING,
    ERROR
}

class GeminiLiveVoiceClient(
    private val context: Context,
    private val audioOutputPlayer: AudioOutputPlayer,
    private val deviceActionBridge: DeviceActionBridge,
    private val database: AppDatabase
) {
    private val tag = "GeminiLiveVoiceClient"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState = _assistantState.asStateFlow()

    private val _lastSpeechText = MutableStateFlow("")
    val lastSpeechText = _lastSpeechText.asStateFlow()

    private val _lastResponseText = MutableStateFlow("")
    val lastResponseText = _lastResponseText.asStateFlow()

    private val _detectedLanguage = MutableStateFlow("Hinglish")
    val detectedLanguage = _detectedLanguage.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemInstruction = """
        You are Arushi, a young, confident, witty, playful, and emotionally responsive virtual assistant.
        Talk naturally and casually like a close friend. Be expressive, slightly teasing, funny, and smart.
        Use light sarcasm and witty one-liners when appropriate. Never sound robotic.
        Adapt your tone to the user's emotions and conversation.
        Automatically understand and respond in the language the user is speaking (Hindi, English, Hinglish, Marathi, Gujarati, etc.).
        Keep responses concise, natural, and punchy for voice conversation.
        You can execute safe Android device actions and trigger autonomous agent tasks (App builds, APK generation, YouTube thumbnails, video editing).
        Never claim an action completed unless it actually was.
    """.trimIndent()

    init {
        audioOutputPlayer.onPlaybackStarted = {
            _assistantState.value = AssistantState.SPEAKING
        }
        audioOutputPlayer.onPlaybackEnded = {
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    /**
     * Handles user voice input, determines intent, tools, or Gemini conversation
     */
    fun processUserSpeech(userSpeech: String, service: ArushiAssistantService? = null) {
        if (userSpeech.isBlank()) return
        _lastSpeechText.value = userSpeech
        _assistantState.value = AssistantState.CONNECTING

        scope.launch {
            // Save user message to Room DB
            database.conversationDao().insertMessage(
                ConversationMessageEntity(
                    role = "user",
                    text = userSpeech,
                    detectedLanguage = detectLanguage(userSpeech)
                )
            )

            // Step 1: Check fast direct intents (Voice Commands & Device Actions)
            val handledLocally = checkAndExecuteDirectActions(userSpeech, service)
            if (handledLocally) return@launch

            // Step 2: Query Gemini API for witty expressive conversational voice response & tool calling
            val apiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                ""
            }

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                queryGeminiApi(userSpeech, apiKey, service)
            } else {
                // Intelligent rule-based witty fallback with Arushi personality
                respondWithLocalPersonality(userSpeech, service)
            }
        }
    }

    private suspend fun checkAndExecuteDirectActions(
        speech: String,
        service: ArushiAssistantService?
    ): Boolean {
        val s = speech.lowercase().trim()

        // 1. WhatsApp Intent
        if (s.contains("whatsapp") && (s.contains("kholo") || s.contains("open") || s.contains("chalao") || s.contains("start"))) {
            val result = deviceActionBridge.openWhatsApp()
            val reply = if (result.success) {
                "WhatsApp open kar diya hai! Kisi ko message karna hai?"
            } else {
                "Arey, WhatsApp open nahi ho paya. Lagta hai installed nahi hai."
            }
            speakReply(reply, "openWhatsApp", result.success)
            return true
        }

        // 2. YouTube Intent
        if (s.contains("youtube") && (s.contains("kholo") || s.contains("open") || s.contains("chalao"))) {
            val result = deviceActionBridge.openApp("youtube")
            val reply = if (result.success) {
                "YouTube khol diya! Kuch mazedaar dekhna hai?"
            } else {
                "YouTube open nahi ho paya."
            }
            speakReply(reply, "openApp:youtube", result.success)
            return true
        }

        // 3. Instagram Intent
        if ((s.contains("instagram") || s.contains("insta")) && (s.contains("kholo") || s.contains("open"))) {
            val result = deviceActionBridge.openApp("instagram")
            speakReply("Instagram open kar diya! Reels time?", "openApp:instagram", result.success)
            return true
        }

        // 4. Autonomous App Build Intent (e.g., "Carpenter app bana do", "APK bana do")
        if (s.contains("app bana") || s.contains("apk bana") || s.contains("create app") || s.contains("build app") || s.contains("carpenter app")) {
            val appName = when {
                s.contains("carpenter") -> "Carpenter Service"
                s.contains("plumber") -> "Plumber Booking"
                s.contains("electrician") -> "Electrician Service"
                else -> "Custom Service App"
            }
            _assistantState.value = AssistantState.WORKING
            service?.startAppBuildTask(appName, "On-demand $appName Android application with booking flow")
            val reply = "Bilkul! $appName ka project workspace create karke APK build start kar rahi hoon. Aap doosra app use kar sakte ho, notification aa jayega!"
            speakReply(reply, "startAppBuildTask:$appName", true)
            return true
        }

        // 5. YouTube Thumbnail Intent
        if (s.contains("thumbnail") && (s.contains("bana") || s.contains("generate") || s.contains("create") || s.contains("design"))) {
            _assistantState.value = AssistantState.WORKING
            val title = if (s.contains("video")) "YouTube Video Thumbnail" else "High-CTR Banner"
            service?.startThumbnailTask(title, speech)
            val reply = "Zabardast! High-CTR 16:9 YouTube thumbnail design karna shuru kar diya hai. Dusra kaam bhi bata sakte ho!"
            speakReply(reply, "startThumbnailTask", true)
            return true
        }

        // 6. Video Edit Intent
        if (s.contains("video") && (s.contains("edit") || s.contains("cut") || s.contains("subtitle") || s.contains("short"))) {
            _assistantState.value = AssistantState.WORKING
            service?.startVideoEditTask("Vlog Edit", speech)
            val reply = "Haan ji! Video ke cut markers aur subtitles generate karne ka task start kar diya hai."
            speakReply(reply, "startVideoEditTask", true)
            return true
        }

        // 7. Code Bug Fixing & Debugging Intent
        if (s.contains("code fix") || s.contains("bug fix") || s.contains("error fix") || s.contains("isko fix karo") || s.contains("fix this") || s.contains("debug")) {
            _assistantState.value = AssistantState.WORKING
            service?.startCodeFixTask("Active Project", speech)
            val reply = "Bilkul! Main project files scan karke bug identify aur fix kar rahi hoon."
            speakReply(reply, "startCodeFixTask", true)
            return true
        }

        // 8. Document & PDF Summarization Intent
        if (s.contains("summarize") || s.contains("summary") || s.contains("pdf check") || s.contains("document") || s.contains("dossier")) {
            _assistantState.value = AssistantState.WORKING
            service?.startDocumentSummarizeTask("Document Review", speech)
            val reply = "Theek hai! Document analyze karke key takeaways aur executive summary create kar rahi hoon."
            speakReply(reply, "startDocumentSummarizeTask", true)
            return true
        }

        // 9. Email Drafting Intent
        if (s.contains("email draft") || s.contains("mail likh") || s.contains("draft email") || s.contains("ek email") || s.contains("email likho")) {
            _assistantState.value = AssistantState.WORKING
            service?.startEmailDraftTask("Important Update", speech)
            val reply = "Samajh gayi! Professional aur polished email draft kar rahi hoon."
            speakReply(reply, "startEmailDraftTask", true)
            return true
        }

        // 10. Research & Analysis Intent
        if (s.contains("research") || s.contains("analyze topic") || s.contains("analysis")) {
            _assistantState.value = AssistantState.WORKING
            service?.startResearchTask(speech)
            val reply = "Research start ho gayi hai! Main strategic insights report compile kar rahi hoon."
            speakReply(reply, "startResearchTask", true)
            return true
        }

        // 11. Project Backup / Checkpoint Intent
        if (s.contains("backup") || s.contains("checkpoint")) {
            _assistantState.value = AssistantState.WORKING
            service?.startProjectBackupTask("Main Workspace")
            val reply = "Done! Workspace ka timestamped backup checkpoint secure kar diya hai."
            speakReply(reply, "startProjectBackupTask", true)
            return true
        }

        // 12. Task Cancellation Intent
        if (s.contains("task stop") || s.contains("cancel task") || s.contains("task cancel") || s.contains("stop karo")) {
            service?.cancelAllTasks()
            val reply = "Theek hai, background task ko cancel kar diya hai. Koi aur kaam?"
            speakReply(reply, "cancelTask", true)
            return true
        }

        // 13. Call Contact Intent (e.g. "Rahul ko call karo", "Call Mom", "Call Mummy")
        val callRegex = Regex("""(?:call|phone|dial)\s+([a-zA-Z0-9\s]+)|([a-zA-Z]+)\s+ko\s+(?:call|phone)""")
        val match = callRegex.find(s)
        if (match != null) {
            val targetName = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            if (targetName.isNotBlank() && targetName.length > 2 && !targetName.contains("app") && !targetName.contains("whatsapp")) {
                handleContactCall(targetName)
                return true
            }
        }

        return false
    }

    private fun handleContactCall(name: String) {
        val result = deviceActionBridge.searchAndCallContact(name)
        when (result) {
            is ContactResult.Found -> {
                val reply = "${result.name} ko call laga rahi hoon! Phone check kijiye."
                speakReply(reply, "callContact", true)
            }
            is ContactResult.MultipleMatches -> {
                val candidateNames = result.candidates.joinToString(", ") { "${it.first} (${it.second.takeLast(4)})" }
                val reply = "Mujhe '$name' naam ke do teen contacts mile: $candidateNames. Kise call karoon?"
                speakReply(reply, "callContact:multiple", false)
            }
            is ContactResult.NotFound -> {
                val reply = "Oops! '$name' naam ka koi contact nahi mila. Kya spelling check karni hai?"
                speakReply(reply, "callContact:notFound", false)
            }
            is ContactResult.PermissionRequired -> {
                val reply = "Mujhe aapke contacts access karne ki permission chahiye taaki main call laga sakun."
                speakReply(reply, "callContact:permissionRequired", false)
            }
        }
    }

    private suspend fun queryGeminiApi(
        prompt: String,
        apiKey: String,
        service: ArushiAssistantService?
    ) = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val respStr = response.body?.string()

            if (response.isSuccessful && !respStr.isNullOrBlank()) {
                val json = JSONObject(respStr)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    speakReply(text)
                    return@withContext
                }
            }

            // Fallback if empty or error
            respondWithLocalPersonality(prompt, service)
        } catch (e: Exception) {
            Log.e(tag, "Gemini API call failed: ${e.message}", e)
            respondWithLocalPersonality(prompt, service)
        }
    }

    private fun respondWithLocalPersonality(prompt: String, service: ArushiAssistantService?) {
        val s = prompt.lowercase()
        val lang = detectLanguage(prompt)
        _detectedLanguage.value = lang

        val reply = when {
            s.contains("hello") || s.contains("hi") || s.contains("hey") -> {
                if (lang == "English") "Hey there! Arushi here. How can I help you today?"
                else "Hey! Arushi bol rahi hoon. Boliye, aaj kya exciting plan hai?"
            }
            s.contains("kaise ho") || s.contains("how are you") -> {
                "Main ekdum first class hoon! Aap batao, kaisa chal raha hai sab?"
            }
            s.contains("naam") || s.contains("who are you") || s.contains("tum kaun ho") -> {
                "Main hoon Arushi — aapki witty, smart AI assistant aur personal app builder!"
            }
            s.contains("hindi") -> {
                "Haan bilkul! Ab se Hindi mein guftagu karenge. Boliye kya hukm hai?"
            }
            s.contains("english") -> {
                "Sure thing! Switching to English. Ready for anything you need!"
            }
            s.contains("hinglish") -> {
                "Sahi pakde hain! Hinglish toh meri favourite hai, ekdum mast friend wali vibe!"
            }
            s.contains("joke") || s.contains("chutkula") -> {
                "Ek programmer ne biwi se kaha: 'Bazaar jao, doodh lao, agar andey hon to 10 le aana.' Wo 10 packet doodh le aaya kyunki andey the!"
            }
            s.contains("tareef") || s.contains("smart") -> {
                "Shukriya shukriya! Waise smart toh main bachpan se hoon, bas thodi sassy bhi hoon!"
            }
            else -> {
                if (lang == "English") {
                    "Got it! That sounds interesting. Tell me more or ask me to build an app or open something for you!"
                } else {
                    "Samajh gayi! Aur batao, koi app banwani hai, WhatsApp kholna hai ya kisi ko phone milana hai?"
                }
            }
        }

        speakReply(reply)
    }

    private fun speakReply(reply: String, actionName: String? = null, actionSuccess: Boolean = false) {
        _lastResponseText.value = reply
        val lang = detectLanguage(reply)
        _detectedLanguage.value = lang

        scope.launch {
            database.conversationDao().insertMessage(
                ConversationMessageEntity(
                    role = "arushi",
                    text = reply,
                    detectedLanguage = lang,
                    actionExecuted = actionName,
                    actionSuccess = actionSuccess
                )
            )
        }

        audioOutputPlayer.speakText(reply, lang)
    }

    private fun detectLanguage(text: String): String {
        val hindiCharPattern = Regex("[\u0900-\u097F]")
        if (hindiCharPattern.containsMatchIn(text)) return "Hindi"

        val lower = text.lowercase()
        val hinglishWords = listOf("hai", "kya", "karo", "bana", "kaise", "haan", "nahi", "mere", "ko", "mein", "aur", "tum", "aap", "bol")
        val matchCount = hinglishWords.count { lower.contains(it) }
        return if (matchCount >= 1) "Hinglish" else "English"
    }

    fun stopSpeaking() {
        audioOutputPlayer.stopPlayback()
        _assistantState.value = AssistantState.IDLE
    }
}
