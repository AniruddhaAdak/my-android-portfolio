package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class DeveloperTone(val title: String, val subtitle: String, val quote: String, val tonePrompt: String) {
    PRAGMATIC(
        "Pragmatic Architect",
        "Deterministic O(1) Algorithms",
        "Optimizing PostgreSQL sub-query paths, indexing heuristics, and thread handshakes.",
        "Your tone is deeply analytical, precise, and highly structured. Speak with expert authority on performance profiling, database constraints, thread locks, garbage collection overhead, and design-by-contract."
    ),
    CREATIVE(
        "Creative Visionary",
        "Organic Motion & Human Touch",
        "Where beautiful science meets edge-to-edge screens and tactile interface physics.",
        "Your tone is inspiring, warm, and highly visual. Talk about dynamic shadows, Material 3 dynamic color schemes, typography letter-spacing, visual focus hierarchies, and empathetic accessibility standards."
    ),
    ACCELERATED(
        "Sprint Builder",
        "Continuous Deployment Loop",
        "Shipping secure end-to-end features, dockerized microservices, and live socket routes.",
        "Your tone is fast-paced, highly enthusiastic, and practical. Focus on direct deployment speeds, Docker container isolation, live system monitoring, web sockets, and robust production-ready release pipelines."
    )
}

data class TriviaQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class SkillItem(
    val id: String,
    val name: String,
    val category: String,
    val initialLevel: Float, // 0.0f to 1.0f
    val expLevel: String,
    val info: String,
    val iconName: String,
    var levelUpCount: Int = 0
)

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val techStack: List<String>,
    val stats: String,
    val simulationType: String, // "blog", "ecommerce", "task"
    val isExpanded: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val guestbookDao = db.guestbookDao()

    // Guestbook Flow
    val guestbookEntries: StateFlow<List<GuestbookEntity>> = guestbookDao.getAllEntriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current page navigation state index: 0 = Home/Timeline, 1 = Projects/Sandbox, 2 = AI twin chat, 3 = Guestbook Endorses
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Active Developer Tone
    private val _selectedPersona = MutableStateFlow(DeveloperTone.PRAGMATIC)
    val selectedPersona: StateFlow<DeveloperTone> = _selectedPersona.asStateFlow()

    // Dynamic Commit Count Metrics
    private val _gitCommitsCount = MutableStateFlow(412)
    val gitCommitsCount: StateFlow<Int> = _gitCommitsCount.asStateFlow()

    // Tech Trivia state variables
    private val _triviaQuestions = createTriviaQuestions()
    val triviaQuestions: List<TriviaQuestion> = _triviaQuestions

    private val _currentTriviaIndex = MutableStateFlow(0)
    val currentTriviaIndex: StateFlow<Int> = _currentTriviaIndex.asStateFlow()

    private val _userTriviaScore = MutableStateFlow(0)
    val userTriviaScore: StateFlow<Int> = _userTriviaScore.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(-1)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    private val _isTriviaAnswered = MutableStateFlow(false)
    val isTriviaAnswered: StateFlow<Boolean> = _isTriviaAnswered.asStateFlow()

    // Skills Matrix State
    private val _skills = MutableStateFlow(createInitialSkills())
    val skills: StateFlow<List<SkillItem>> = _skills.asStateFlow()

    private val _selectedSkillId = MutableStateFlow<String?>("kotlin")
    val selectedSkillId: StateFlow<String?> = _selectedSkillId.asStateFlow()

    // Projects State
    private val _projects = MutableStateFlow(createInitialProjects())
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()

    // Chatbot States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Greetings! I am Aniruddha's Digital Twin. Ask me anything about his technical expertise, work, or credentials!", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    val isLiveAi: Boolean = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    // Sandbox Project Simulation Local States
    val simBlogRating = MutableStateFlow(4)
    val simBlogComments = MutableStateFlow<List<String>>(listOf("Amazing backend responsiveness!", "Beautiful reactive frontend!"))
    val simCartItems = MutableStateFlow(0)
    val simCartTotal = MutableStateFlow(0)
    val simTasks = MutableStateFlow<List<String>>(listOf("Review system architecture", "Deploy CI/CD pipe"))

    // Endorse Form inputs
    val guestName = MutableStateFlow("")
    val guestEmail = MutableStateFlow("")
    val guestMessage = MutableStateFlow("")
    val guestFormError = MutableStateFlow<String?>(null)
    val guestSubmitSuccess = MutableStateFlow(false)

    // Select skill
    fun selectSkill(skillId: String) {
        _selectedSkillId.value = skillId
    }

    // Level up skill interaction
    fun levelUpSkill(skillId: String) {
        val currentList = _skills.value
        val updatedList = currentList.map {
            if (it.id == skillId) {
                val newLevel = (it.initialLevel + 0.05f).coerceAtMost(1.0f)
                it.copy(initialLevel = newLevel, levelUpCount = it.levelUpCount + 1)
            } else {
                it
            }
        }
        _skills.value = updatedList
    }

    // Toggle Project expansion
    fun toggleProjectExpansion(projectId: String) {
        val currentList = _projects.value
        val updatedList = currentList.map {
            if (it.id == projectId) {
                it.copy(isExpanded = !it.isExpanded)
            } else {
                it
            }
        }
        _projects.value = updatedList
    }

    // Page selector
    fun setPage(index: Int) {
        _currentPage.value = index.coerceIn(0, 3)
    }

    // Tone switcher
    fun setPersona(persona: DeveloperTone) {
        _selectedPersona.value = persona
        // Reset assistant thread with dynamic welcome tone greeting
        val initialText = when (persona) {
            DeveloperTone.PRAGMATIC -> "Dual system thread initialized. I am Aniruddha's Digital Architect Twin, optimized for algorithmic efficiency. Ask about algorithmic latency or system architectural bottlenecks!"
            DeveloperTone.CREATIVE -> "Aura design system synchronized! I am Aniruddha's Creative Twin, prepared to review stunning responsive views, touch interaction classes, and user emotion vectors."
            DeveloperTone.ACCELERATED -> "Continuous integration sprint started! I am Aniruddha's Sprint Twin. Let's discuss microservice scalability, secure Docker layers, and high-velocity shipping schedules!"
        }
        _chatMessages.value = listOf(ChatMessage(initialText, false))
    }

    // Git simulation commits
    fun incrementGitCommits() {
        _gitCommitsCount.value = _gitCommitsCount.value + (1..4).random()
    }

    // Trivia game controllers
    fun selectTriviaAnswer(optionIndex: Int) {
        _selectedAnswerIndex.value = optionIndex
    }

    fun submitTriviaAnswer() {
        if (_isTriviaAnswered.value) return
        val currentQuestion = triviaQuestions[_currentTriviaIndex.value]
        if (_selectedAnswerIndex.value == currentQuestion.correctIndex) {
            _userTriviaScore.value = _userTriviaScore.value + 10
        }
        _isTriviaAnswered.value = true
    }

    fun nextTriviaQuestion() {
        val nextIdx = (_currentTriviaIndex.value + 1) % triviaQuestions.size
        _currentTriviaIndex.value = nextIdx
        _selectedAnswerIndex.value = -1
        _isTriviaAnswered.value = false
    }

    // Simulation Interactions
    fun addSimBlogComment(comment: String) {
        if (comment.isNotBlank()) {
            simBlogComments.value = simBlogComments.value + comment
        }
    }

    fun addSimCartItem(price: Int) {
        simCartItems.value = simCartItems.value + 1
        simCartTotal.value = simCartTotal.value + price
    }

    fun clearSimCart() {
        simCartItems.value = 0
        simCartTotal.value = 0
    }

    fun addSimTask(task: String) {
        if (task.isNotBlank()) {
            simTasks.value = simTasks.value + task
        }
    }

    fun removeSimTask(index: Int) {
        val current = simTasks.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            simTasks.value = current
        }
    }

    // Chatbot response logic
    fun sendChatMessage(messageStr: String) {
        if (messageStr.isBlank()) return

        val userMsg = ChatMessage(messageStr, true)
        _chatMessages.value = _chatMessages.value + userMsg

        _isChatLoading.value = true

        viewModelScope.launch {
            val response = try {
                if (isLiveAi) {
                    callLiveGemini(messageStr)
                } else {
                    simulatePersonaResponse(messageStr)
                }
            } catch (e: Exception) {
                "Digital Twin Connection offline. Exception: ${e.localizedMessage}. Check your GEMINI_API_KEY settings."
            }

            _chatMessages.value = _chatMessages.value + ChatMessage(response, false)
            _isChatLoading.value = false
        }
    }

    private suspend fun callLiveGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val persona = _selectedPersona.value
        val systemInstruction = "You are the AI Digital Twin of Aniruddha Adak. He is a highly skilled Full Stack Software Engineer and Android developer from West Bengal, India. " +
                "You are currently operating in status mode: [${persona.title}]. " +
                persona.tonePrompt + 
                " He builds production systems using Django, React/Next.js, PostgreSQL, Node.js, and Android Jetpack Compose. Keep responses helpful, concise, nicely formatted with bold markers, and respond in the tone of the selected persona state."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstruction))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7,
                maxOutputTokens = 800
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, my neural link had an issue resolving that. Please ask again!"
        } catch (e: Exception) {
            "An error occurred while reaching Gemini: ${e.localizedMessage}. Falling back to Simulator context: " + simulatePersonaResponse(prompt)
        }
    }

    private fun simulatePersonaResponse(prompt: String): String {
        val clean = prompt.lowercase(Locale.getDefault())
        val tone = _selectedPersona.value
        val prefix = "[${tone.title} Node]: "
        
        val content = when {
            clean.contains("experience") || clean.contains("background") || clean.contains("work") || clean.contains("history") -> {
                when (tone) {
                    DeveloperTone.PRAGMATIC -> "Aniruddha has 3+ years of professional full-stack expertise optimizing PostgreSQL transactions, structuring Django API contracts, and profiling memory usage in Jetpack Compose configurations."
                    DeveloperTone.CREATIVE -> "He has spent 3+ years painting fluid edge-to-edge layouts, designing intuitive micro-interactions, and tailoring dynamic Material schemes based on natural light models."
                    DeveloperTone.ACCELERATED -> "He has shipped complete systems in 3+ years: modular docker-compose files, automated test frameworks, and high-frequency real-time web socket communication handlers, deploying at high velocities."
                }
            }
            clean.contains("skill") || clean.contains("tech") || clean.contains("stack") || clean.contains("languages") -> {
                when (tone) {
                    DeveloperTone.PRAGMATIC -> "Deterministic core languages: Kotlin, TypeScript, Python, alongside SQL/NoSQL storage. Familiar with database sharding, caching, and algorithmic depth."
                    DeveloperTone.CREATIVE -> "Sensory tactile systems: high-fidelity Jetpack Compose Canvas drawing, Tailwind CSS microstyles, custom vector assets, and typography pair scaling."
                    DeveloperTone.ACCELERATED -> "Full throughput mechanics: Express/Node.js socket routes, Django asynchronous pipelines, Docker orchestration, and Git hooks automations."
                }
            }
            clean.contains("project") || clean.contains("portfolio") || clean.contains("built") -> {
                when (tone) {
                    DeveloperTone.PRAGMATIC -> "Aniruddha developed an AI-EmailAssistant capable of filtering/routing drafts based on prompt analytics under 200ms latency, and NexusChat using web sockets and Room persistent cache pools."
                    DeveloperTone.CREATIVE -> "He crafted AuraStore with beautiful interactive sales dashboards, visual dynamic metrics dashboards, and a glorious vector canvas profile portrait."
                    DeveloperTone.ACCELERATED -> "He engineered NexusChat (handling over 10k messages per second safely under load) and deployed rapid docker assets for immediate CI/CD pipelines."
                }
            }
            clean.contains("hire") || clean.contains("recruit") || clean.contains("job") || clean.contains("why") -> {
                when (tone) {
                    DeveloperTone.PRAGMATIC -> "Aniruddha brings extreme precision under pressure, clean MVVM standards, scalable DB architectures, and zero-leak memory paradigms."
                    DeveloperTone.CREATIVE -> "He ensures each component triggers joy: dynamic dynamicColors colors, highly accessible visual touch targets, and balanced spacing structures."
                    DeveloperTone.ACCELERATED -> "He builds resiliently, delivers features continuously, avoids bloated secondary dependencies, and pushes checked, secure builds."
                }
            }
            clean.contains("contact") || clean.contains("email") || clean.contains("find") -> {
                "Reach him immediately at aniruddhaadak80@gmail.com, or leave a verified Room endorsement in the dynamic Guestbook Page!"
            }
            clean.contains("hello") || clean.contains("hi") || clean.contains("greetings") -> {
                "Hello developer guest! I am prepared to explain his technical architecture, responsive styles, or deployment metrics. What can we discuss?"
            }
            else -> {
                "That's an interesting technical query! Aniruddha focuses strictly on performance optimization. Is there a project stack, backend model, or specific certification database detail you would like to explore?"
            }
        }
        return prefix + content
    }

    // Guestbook DB Interactions
    fun addGuestbookEntry() {
        val name = guestName.value.trim()
        val email = guestEmail.value.trim()
        val message = guestMessage.value.trim()

        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            guestFormError.value = "All fields are required to sign the Guestbook!"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            guestFormError.value = "Please enter a valid email address!"
            return
        }

        guestFormError.value = null

        viewModelScope.launch {
            val entry = GuestbookEntity(
                name = name,
                email = email,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            guestbookDao.insertEntry(entry)
            
            // Reset fields
            guestName.value = ""
            guestEmail.value = ""
            guestMessage.value = ""
            guestSubmitSuccess.value = true
        }
    }

    fun incrementEntryLikes(id: Int) {
        viewModelScope.launch {
            guestbookDao.incrementLikes(id)
        }
    }

    fun resetSuccessState() {
        guestSubmitSuccess.value = false
    }

    // Seeds
    private fun createInitialSkills(): List<SkillItem> {
        return listOf(
            SkillItem("kotlin", "Kotlin / Android", "Mobile Dev", 0.90f, "Lead Architect", "Android SDK, Jetpack Compose, Coroutines, Flow, MVVM architecture, Edge-to-Edge Design, Room Persistence.", "ic_android"),
            SkillItem("python", "Python / Django", "Backend", 0.85f, "Full Stack Engineer", "Django REST Framework, Flask, Celery asynchronous pipelines, REST APIs, JSON integrations.", "ic_code"),
            SkillItem("react", "React / Typescript", "Frontend", 0.88f, "Core Frontend", "Single Page Applications, Redux/Context state management, Tailwind custom styles, responsive layouts.", "ic_computer"),
            SkillItem("database", "PostgreSQL & Mongo", "Data Storage", 0.82f, "DB Administrator", "Relational constraints, complex transaction logic, indexes tuning, B-tree search optimization, non-relational modeling.", "ic_storage"),
            SkillItem("docker", "Docker / DevOps", "Deployment", 0.80f, "Automation Lead", "Containerized deployment configurations, local test automation, Docker Compose files, secure build configs.", "ic_cloud")
        )
    }

    private fun createInitialProjects(): List<ProjectItem> {
        return listOf(
            ProjectItem(
                id = "ai-helper",
                name = "AI-EmailAssistant",
                description = "An automated, intelligent email orchestration system using the Gemini 3.5 API to filter, tag, summarize, and draft contextual replies under high loads.",
                category = "Artificial Intelligence",
                techStack = listOf("Python", "Django", "React", "Gemini API", "Celery"),
                stats = "94% Accuracy • <200ms Latency",
                simulationType = "blog"
            ),
            ProjectItem(
                id = "nexus-chat",
                name = "NexusChat Encrypted Messaging",
                description = "High-frequency real-time chat platform leveraging secure web socket handshakes and local SQLite/Room caching to enable fluid end-to-end messaging.",
                category = "Cybersecurity & Web",
                techStack = listOf("Node.js", "WebSockets", "React", "MongoDB", "Redux"),
                stats = "10k+ Messages/sec • Zero Downtime",
                simulationType = "ecommerce"
            ),
            ProjectItem(
                id = "aurastore",
                name = "AuraStore E-Commerce",
                description = "Full featured modern e-commerce storefront containing real dynamic checkout flows, detailed sales inventory charts, and admin metrics dashboard panels.",
                category = "Enterprise MERN",
                techStack = listOf("React.js", "Express", "Node.js", "MongoDB", "ChartJS"),
                stats = "$45k+ Simulated GMV • 4.9 App Rating",
                simulationType = "task"
            )
        )
    }

    private fun createTriviaQuestions(): List<TriviaQuestion> {
        return listOf(
            TriviaQuestion(
                1,
                "Which indexing technique optimizes queries on columns with high cardinality in PostgreSQL?",
                listOf("GIN Index", "B-Tree Index", "Hash Index", "BRIN Index"),
                1,
                "B-Tree is the default and most efficient PostgreSQL index structure for comparison operations (<, <=, =, >=, >) on high-cardinality values."
            ),
            TriviaQuestion(
                2,
                "In Jetpack Compose, which recomposition optimizer forces evaluations to skip unless state dependencies change?",
                listOf("remember", "derivedStateOf", "rememberUpdatedState", "produceState"),
                1,
                "derivedStateOf is used to buffer and monitor state values, preventing excessive recomposition trigger storms when parent elements shift."
            ),
            TriviaQuestion(
                3,
                "What is the main benefit of utilizing GIN indexes in PostgreSQL databases?",
                listOf("Fast single lookup", "Indexing composite geometric points", "Fast search within arrays and document JSON payloads", "Ordered sequential scanning"),
                2,
                "Generalized Inverted Indexes (GIN) are optimized for multi-value elements like arrays and JSONB documents containing key-value pairs."
            ),
            TriviaQuestion(
                4,
                "Which component in Jetpack Compose retains parameters securely across configuration changes?",
                listOf("rememberSaveable", "remember", "mutableStateOf", "derivedStateOf"),
                0,
                "rememberSaveable automatically persists composite and structural values using a Bundle across Activity rebuild loops."
            )
        )
    }
}
