package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GuestbookEntity
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        val backgroundGradient = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF0C0907), // Deepest warm obsidian black
            Color(0xFF21150A), // Deep warm toasted hazelnut amber-espresso glow
            Color(0xFF0C0907)  // Deepest warm obsidian black
          )
        )
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color.Transparent
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(backgroundGradient)
          ) {
            PortfolioScreen()
          }
        }
      }
    }
  }
}

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = viewModel()) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  
  // Collect state flows
  val skills by viewModel.skills.collectAsState()
  val selectedSkillId by viewModel.selectedSkillId.collectAsState()
  val projects by viewModel.projects.collectAsState()
  val chatMessages by viewModel.chatMessages.collectAsState()
  val isChatLoading by viewModel.isChatLoading.collectAsState()
  val guestbookEntries by viewModel.guestbookEntries.collectAsState()

  val currentPage by viewModel.currentPage.collectAsState()
  val selectedPersona by viewModel.selectedPersona.collectAsState()
  val gitCommitsCount by viewModel.gitCommitsCount.collectAsState()

  // Guestbook and contact states
  val gName by viewModel.guestName.collectAsState()
  val gEmail by viewModel.guestEmail.collectAsState()
  val gMessage by viewModel.guestMessage.collectAsState()
  val gError by viewModel.guestFormError.collectAsState()
  val gSuccess by viewModel.guestSubmitSuccess.collectAsState()

  // Dynamic Accent Theme Colors based on Persona hot-swap Shifter selection!
  val activeAccentColor = when (selectedPersona) {
    DeveloperTone.PRAGMATIC -> ElegantDarkPrimary // Bright Glowing Amber Gold (0xFFFBBF24)
    DeveloperTone.CREATIVE -> Color(0xFFF97316)   // Radiant Sunset Orange (0xFFF97316)
    DeveloperTone.ACCELERATED -> Color(0xFFEA580C) // Burning Deep Orange (0xFFEA580C)
  }

  // Toast recommendation acknowledgement
  LaunchedEffect(gSuccess) {
    if (gSuccess) {
      Toast.makeText(context, "Thank you for endorsing my portfolio!", Toast.LENGTH_SHORT).show()
      viewModel.resetSuccessState()
    }
  }

  Scaffold(
    containerColor = Color.Transparent,
    bottomBar = {
      NavigationBar(
        containerColor = ElegantDarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("portfolio_bottom_nav")
      ) {
        NavigationBarItem(
          selected = currentPage == 0,
          onClick = { viewModel.setPage(0) },
          icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = if (currentPage == 0) activeAccentColor else Color.Gray) },
          label = { Text("Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (currentPage == 0) activeAccentColor else Color.Gray) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeAccentColor,
            selectedTextColor = activeAccentColor,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray,
            indicatorColor = ElegantDarkSecondary
          )
        )
        NavigationBarItem(
          selected = currentPage == 1,
          onClick = { viewModel.setPage(1) },
          icon = { Icon(Icons.Default.Star, contentDescription = "Projects", tint = if (currentPage == 1) activeAccentColor else Color.Gray) },
          label = { Text("Projects", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (currentPage == 1) activeAccentColor else Color.Gray) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeAccentColor,
            selectedTextColor = activeAccentColor,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray,
            indicatorColor = ElegantDarkSecondary
          )
        )
        NavigationBarItem(
          selected = currentPage == 2,
          onClick = { viewModel.setPage(2) },
          icon = { Icon(Icons.Default.AccountCircle, contentDescription = "AI Twin", tint = if (currentPage == 2) activeAccentColor else Color.Gray) },
          label = { Text("AI Twin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (currentPage == 2) activeAccentColor else Color.Gray) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeAccentColor,
            selectedTextColor = activeAccentColor,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray,
            indicatorColor = ElegantDarkSecondary
          )
        )
        NavigationBarItem(
          selected = currentPage == 3,
          onClick = { viewModel.setPage(3) },
          icon = { Icon(Icons.Default.Email, contentDescription = "Endorse", tint = if (currentPage == 3) activeAccentColor else Color.Gray) },
          label = { Text("Endorse", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (currentPage == 3) activeAccentColor else Color.Gray) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = activeAccentColor,
            selectedTextColor = activeAccentColor,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray,
            indicatorColor = ElegantDarkSecondary
          )
        )
      }
    },
    contentWindowInsets = WindowInsets.systemBars
  ) { paddingValues ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      val isTablet = maxWidth >= 720.dp

      AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
          if (targetState > initialState) {
            slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
                slideOutHorizontally { width -> -width / 2 } + fadeOut()
          } else {
            slideInHorizontally { width -> -width / 2 } + fadeIn() togetherWith
                slideOutHorizontally { width -> width / 2 } + fadeOut()
          }.using(
            SizeTransform(clip = false)
          )
        },
        label = "tab_navigation_transitions"
      ) { pageIndex ->
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(24.dp),
          contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
          when (pageIndex) {
            0 -> {
              // ==========================================
              // PAGE 0: HOME PORTFOLIO BIO, GIT MAP, CRITICAL SKILLS, CAREER TIMELINE, TRIVIA GAME
              // ==========================================
              item {
                HeroHeaderSection(activeAccentColor)
              }

              item {
                GitContributionChartCard(
                  commitsCount = gitCommitsCount,
                  accentColor = activeAccentColor,
                  onTriggerCommit = { viewModel.incrementGitCommits() }
                )
              }

              if (isTablet) {
                item {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                  ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                      InteractiveSkillsCard(
                        skills = skills,
                        selectedId = selectedSkillId,
                        accentColor = activeAccentColor,
                        onSelect = { viewModel.selectSkill(it) },
                        onLevelUp = { viewModel.levelUpSkill(it) }
                      )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                      SystemDesignTriviaCard(
                        viewModel = viewModel,
                        accentColor = activeAccentColor
                      )
                    }
                  }
                }
              } else {
                item {
                  InteractiveSkillsCard(
                    skills = skills,
                    selectedId = selectedSkillId,
                    accentColor = activeAccentColor,
                    onSelect = { viewModel.selectSkill(it) },
                    onLevelUp = { viewModel.levelUpSkill(it) }
                  )
                }
                item {
                  SystemDesignTriviaCard(
                    viewModel = viewModel,
                    accentColor = activeAccentColor
                  )
                }
              }

              item {
                CareerTimelineCard(activeAccentColor)
              }
            }

            1 -> {
              // ==========================================
              // PAGE 1: DETAILED PROJECTS WORK AND METRICS SIMULATOR SANDBOX
              // ==========================================
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(24.dp),
                  colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
                  border = BorderStroke(1.5.dp, ElegantDarkBorder)
                ) {
                  Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                      text = "Technical Sandbox Showcase",
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "Explore actual simulated runtimes. Click on each case study node below to run dynamic mock Microservices.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.LightGray,
                      lineHeight = 18.sp
                    )
                  }
                }
              }

              item {
                FeaturedProjectsSection(
                  projects = projects,
                  onExpandToggle = { viewModel.toggleProjectExpansion(it) },
                  viewModel = viewModel,
                  accentColor = activeAccentColor
                )
              }
            }

            2 -> {
              // ==========================================
              // PAGE 2: DIGITAL AI TWIN PLATFORM (SWAPPABLE PERSONAS)
              // ==========================================
              item {
                PersonaAlignmentSwapper(
                  selectedPersona = selectedPersona,
                  onSelect = { viewModel.setPersona(it) },
                  accentColor = activeAccentColor
                )
              }

              item {
                DigitalTwinChatCard(
                  messages = chatMessages,
                  isLoading = isChatLoading,
                  isLiveAi = viewModel.isLiveAi,
                  onSendMessage = { viewModel.sendChatMessage(it) },
                  accentColor = activeAccentColor
                )
              }
            }

            3 -> {
              // ==========================================
              // PAGE 3: GUESTBOOK ENDORSEMENTS & SQLite ROOM REGISTER FEED
              // ==========================================
              item {
                GuestbookAndContactSection(
                  name = gName,
                  email = gEmail,
                  message = gMessage,
                  errorStr = gError,
                  onNameChange = { viewModel.guestName.value = it },
                  onEmailChange = { viewModel.guestEmail.value = it },
                  onMessageChange = { viewModel.guestMessage.value = it },
                  onSubmit = {
                    viewModel.addGuestbookEntry()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                  },
                  entries = guestbookEntries,
                  onLikeEntry = { viewModel.incrementEntryLikes(it) },
                  accentColor = activeAccentColor
                )
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// 1. HERO HEADER SECTION
// ==========================================
@Composable
fun HeroHeaderSection(accentColor: Color) {
  var pulses by remember { mutableStateOf(0) }
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  
  // Avatar scale on click effect
  val scaleAnim by animateFloatAsState(
    targetValue = if (pulses % 2 == 1) 1.08f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "scale"
  )

  // Rotating background gradient angle
  val rotationAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(25000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .scale(scaleAnim)
      .testTag("hero_header_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(accentColor, ElegantDarkTertiary)))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Location Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ElegantDarkSecondary,
          border = BorderStroke(1.dp, ElegantDarkBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .background(Color(0xFFFBBF24), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open to Roles", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
          }
        }

        // Active Status
        Text("UTC 2026", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Custom drawn vector avatar of Aniruddha Adak
      Box(
        modifier = Modifier
          .size(160.dp)
          .pointerInput(Unit) {
            detectTapGestures { pulses++ }
          }
          .clip(CircleShape)
          .drawBehind {
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 2

            // Rotating back gradient
            drawCircle(
              brush = Brush.sweepGradient(
                colors = listOf(accentColor, ElegantDarkTertiary, accentColor),
                center = centerOffset
              ),
              radius = baseRadius,
              style = Stroke(width = 8.dp.toPx())
            )

            // Inner glowing color disk
            drawCircle(
              color = ElegantDarkBg,
              radius = baseRadius - 4.dp.toPx()
            )
          }
          .padding(8.dp)
          .clip(CircleShape)
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .testTag("drawn_avatar")
        ) {
          translate(left = 0f, top = 10f) {
            val w = size.width
            val h = size.height

            // 1. Draw Blazer (Crimson/Red Blazer)
            val jacketPath = Path().apply {
              moveTo(w * 0.15f, h * 0.95f)
              quadraticTo(w * 0.2f, h * 0.65f, w * 0.35f, h * 0.62f)
              lineTo(w * 0.65f, h * 0.62f)
              quadraticTo(w * 0.8f, h * 0.65f, w * 0.85f, h * 0.95f)
              close()
            }
            drawPath(
              path = jacketPath,
              color = Color(0xFFA11010)
            )

            // Blazer Lapels
            val lapelLeft = Path().apply {
              moveTo(w * 0.35f, h * 0.62f)
              lineTo(w * 0.42f, h * 0.88f)
              lineTo(w * 0.28f, h * 0.82f)
              close()
            }
            drawPath(path = lapelLeft, color = Color(0xFF7F0606))

            val lapelRight = Path().apply {
              moveTo(w * 0.65f, h * 0.62f)
              lineTo(w * 0.58f, h * 0.88f)
              lineTo(w * 0.72f, h * 0.82f)
              close()
            }
            drawPath(path = lapelRight, color = Color(0xFF7F0606))

            // 2. Blue Shirt
            val shirtPath = Path().apply {
              moveTo(w * 0.38f, h * 0.62f)
              lineTo(w * 0.5f, h * 0.82f)
              lineTo(w * 0.62f, h * 0.62f)
              close()
            }
            drawPath(
              path = shirtPath,
              color = Color(0xFF1E88E5)
            )

            // Shirt collar
            drawPath(
              path = Path().apply {
                moveTo(w * 0.38f, h * 0.62f)
                lineTo(w * 0.44f, h * 0.70f)
                lineTo(w * 0.47f, h * 0.62f)
                close()
              },
              color = Color(0xFF1565C0)
            )
            drawPath(
              path = Path().apply {
                moveTo(w * 0.62f, h * 0.62f)
                lineTo(w * 0.56f, h * 0.70f)
                lineTo(w * 0.53f, h * 0.62f)
                close()
              },
              color = Color(0xFF1565C0)
            )

            // 3. Dark tie
            val tiePath = Path().apply {
              moveTo(w * 0.47f, h * 0.69f)
              lineTo(w * 0.53f, h * 0.69f)
              lineTo(w * 0.55f, h * 0.95f)
              lineTo(w * 0.50f, h * 0.99f)
              lineTo(w * 0.45f, h * 0.95f)
              close()
            }
            drawPath(
              path = tiePath,
              color = Color(0xFF0D1B2A)
            )
            
            drawCircle(color = Color(0xFF3A86C8), radius = 2f, center = Offset(w * 0.50f, h * 0.75f))
            drawCircle(color = Color(0xFF3A86C8), radius = 2f, center = Offset(w * 0.48f, h * 0.82f))
            drawCircle(color = Color(0xFF3A86C8), radius = 2f, center = Offset(w * 0.52f, h * 0.88f))

            // 4. Face / Neck
            val neckPath = Path().apply {
              moveTo(w * 0.43f, h * 0.48f)
              lineTo(w * 0.43f, h * 0.63f)
              lineTo(w * 0.57f, h * 0.63f)
              lineTo(w * 0.57f, h * 0.48f)
              close()
            }
            drawPath(path = neckPath, color = Color(0xFFE5A687))

            drawCircle(
              color = Color(0xFFF3C1A1),
              radius = w * 0.23f,
              center = Offset(w * 0.5f, h * 0.38f)
            )

            // 5. Beard
            val beardPath = Path().apply {
              moveTo(w * 0.28f, h * 0.35f)
              quadraticTo(w * 0.29f, h * 0.56f, w * 0.5f, h * 0.63f)
              quadraticTo(w * 0.71f, h * 0.56f, w * 0.72f, h * 0.35f)
              lineTo(w * 0.67f, h * 0.35f)
              quadraticTo(w * 0.64f, h * 0.51f, w * 0.5f, h * 0.55f)
              quadraticTo(w * 0.36f, h * 0.51f, w * 0.33f, h * 0.35f)
              close()
            }
            drawPath(path = beardPath, color = Color(0xFF1E1E1E))

            // Mustache
            val mustachePath = Path().apply {
              moveTo(w * 0.38f, h * 0.46f)
              quadraticTo(w * 0.5f, h * 0.49f, w * 0.62f, h * 0.46f)
              quadraticTo(w * 0.5f, h * 0.52f, w * 0.38f, h * 0.46f)
            }
            drawPath(path = mustachePath, color = Color(0xFF1A1A1A))

            // Friendly lips
            drawPath(
              path = Path().apply {
                moveTo(w * 0.45f, h * 0.49f)
                quadraticTo(w * 0.5f, h * 0.53f, w * 0.55f, h * 0.49f)
              },
              color = Color(0xFFD07D7D),
              style = Stroke(width = 3f)
            )

            // 6. Hair cut
            val hairPath = Path().apply {
              moveTo(w * 0.26f, h * 0.35f)
              quadraticTo(w * 0.23f, h * 0.16f, w * 0.5f, h * 0.12f)
              quadraticTo(w * 0.77f, h * 0.16f, w * 0.74f, h * 0.35f)
              lineTo(w * 0.70f, h * 0.32f)
              quadraticTo(w * 0.5f, h * 0.20f, w * 0.30f, h * 0.32f)
              close()
            }
            drawPath(path = hairPath, color = Color(0xFF151515))

            // 7. Sharp focused eyes & eyebrows
            drawPath(
              path = Path().apply {
                moveTo(w * 0.36f, h * 0.31f)
                quadraticTo(w * 0.41f, h * 0.29f, w * 0.45f, h * 0.32f)
              },
              color = Color(0xFF111111),
              style = Stroke(width = 3.dp.toPx())
            )
            drawPath(
              path = Path().apply {
                moveTo(w * 0.55f, h * 0.32f)
                quadraticTo(w * 0.59f, h * 0.29f, w * 0.64f, h * 0.31f)
              },
              color = Color(0xFF111111),
              style = Stroke(width = 3.dp.toPx())
            )

            // Eyes
            drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.41f, h * 0.34f))
            drawCircle(color = Color(0xFF1A1A1A), radius = 3.5f, center = Offset(w * 0.41f, h * 0.34f))
            drawCircle(color = Color.White, radius = 1.5f, center = Offset(w * 0.40f, h * 0.33f))

            drawCircle(color = Color.White, radius = 6f, center = Offset(w * 0.59f, h * 0.34f))
            drawCircle(color = Color(0xFF1A1A1A), radius = 3.5f, center = Offset(w * 0.59f, h * 0.34f))
            drawCircle(color = Color.White, radius = 1.5f, center = Offset(w * 0.58f, h * 0.33f))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Balanced display typography heading
      Text(
        text = "ANIRUDDHA ADAK",
        color = Color.White,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Full Stack Software Engineer & Android Architect",
        color = accentColor,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )

      Text(
        text = "Engineering robust micro-components, highly transactional database plan schemas, and seamless responsive client platforms.",
        color = Color(0xFFCCCCCC),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Core parameters index
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        StatItem("3+ Yrs", "Experience", accentColor)
        StatDivider()
        StatItem("15+", "Projects", accentColor)
        StatDivider()
        StatItem("10K+", "Lines", accentColor)
        StatDivider()
        StatItem("99.9%", "Uptime", accentColor)
      }
    }
  }
}

@Composable
fun StatItem(valStr: String, labelStr: String, accentColor: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = valStr,
      color = accentColor,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = labelStr,
      color = Color(0xFF888888),
      style = MaterialTheme.typography.labelSmall
    )
  }
}

@Composable
fun StatDivider() {
  Box(
    modifier = Modifier
      .height(32.dp)
      .width(1.dp)
      .background(Color(0xFF333333))
  )
}

// ==========================================
// INTERACTIVE GIT CONTRIBS CHART CANVAS
// ==========================================
@Composable
fun GitContributionChartCard(
  commitsCount: Int,
  accentColor: Color,
  onTriggerCommit: () -> Unit
) {
  var clickedState by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (clickedState) 1.05f else 1.0f,
    animationSpec = spring(stiffness = Spring.StiffnessHigh),
    finishedListener = { clickedState = false },
    label = "git_button_press"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .scale(scale)
      .testTag("git_contribution_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Live Activity Deck",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Total Sim Commits: $commitsCount",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
          )
        }
        
        Button(
          onClick = {
            clickedState = true
            onTriggerCommit()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = ElegantDarkSecondary,
            contentColor = accentColor
          ),
          border = BorderStroke(1.2.dp, accentColor),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Commit Test Point", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sim Commit Push", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Git contribution visualizer grids
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Less", color = Color.Gray, fontSize = 9.sp, style = MaterialTheme.typography.labelSmall)
        
        Canvas(
          modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .padding(horizontal = 10.dp)
        ) {
          val columns = 14
          val rows = 4
          val gap = 4.dp.toPx()
          val boxSize = (size.width - (columns - 1) * gap) / columns
          val verticalGap = (size.height - (rows - 1) * gap) / rows
          
          for (c in 0 until columns) {
            for (r in 0 until rows) {
              val mockIntensity = ((c * r + commitsCount) % 5)
              val boxColor = when (mockIntensity) {
                0 -> Color(0xFF1E1E24)
                1 -> Color(0xFF0E4429)
                2 -> Color(0xFF006D38)
                3 -> Color(0xFF26A641)
                else -> Color(0xFF39D353)
              }
              
              drawRoundRect(
                color = boxColor,
                topLeft = Offset(c * (boxSize + gap), r * (boxSize + gap)),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
              )
            }
          }
        }
        
        Text("More", color = Color.Gray, fontSize = 9.sp, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

// ==========================================
// 2. INTERACTIVE TECHNICAL SKILLS MATRIX
// ==========================================
@Composable
fun InteractiveSkillsCard(
  skills: List<SkillItem>,
  selectedId: String?,
  accentColor: Color,
  onSelect: (String) -> Unit,
  onLevelUp: (String) -> Unit
) {
  val activeSkill = skills.find { it.id == selectedId }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("skills_matrix_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Skills",
            tint = accentColor,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Skills Matrix",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "Tap to Learn",
          color = accentColor,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold
        )
      }
      
      Spacer(modifier = Modifier.height(16.dp))

      // Horizontal Row of skill category selector items
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(skills) { skill ->
          val isSelected = skill.id == selectedId
          Surface(
            modifier = Modifier.clickable { onSelect(skill.id) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) ElegantDarkActivePill else ElegantDarkSecondary,
            border = BorderStroke(1.5.dp, if (isSelected) accentColor else Color.Transparent)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              val vectorIcon = when (skill.iconName) {
                "ic_android" -> Icons.Default.PlayArrow
                "ic_code" -> Icons.Default.Build
                "ic_computer" -> Icons.Default.Home
                "ic_storage" -> Icons.Default.Star
                else -> Icons.Default.Check
              }
              Icon(
                imageVector = vectorIcon,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color.White,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = skill.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Active Details Panel
      AnimatedContent(
        targetState = activeSkill,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "skills_panel_fade"
      ) { skill ->
        if (skill != null) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(ElegantDarkBg, RoundedCornerShape(16.dp))
              .border(1.dp, ElegantDarkBorder, RoundedCornerShape(16.dp))
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = skill.category,
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = skill.expLevel,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text detailed description
            Text(
              text = skill.info,
              color = Color.LightGray,
              style = MaterialTheme.typography.bodyLarge,
              lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Proficiencies progress scale
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Competency Index: ${(skill.initialLevel * 100).toInt()}%",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = "+${skill.levelUpCount} Boosted",
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
              progress = { skill.initialLevel },
              color = accentColor,
              trackColor = ElegantDarkSecondary,
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Boost proficiency
            Button(
              onClick = { onLevelUp(skill.id) },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkActivePill, contentColor = Color.White),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Enhance Metric", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Calibrate Competency Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// ==========================================
// TIMELINE CRITICAL CAREER JOURNAL
// ==========================================
@Composable
fun CareerTimelineCard(accentColor: Color) {
  var selectedMilestone by remember { mutableStateOf(0) }

  val milestones = listOf(
    Milestone(
      year = "2024 - Present",
      role = "Lead Architect & SaaS Engineer",
      title = "AI Orchestrator Delivery",
      description = "Spearheaded low-latency Gemini integration pipelines. Configured Docker-driven microservices reducing server start latencies and ensuring horizontal container safety.",
      stack = listOf("Django", "React.js", "Docker", "Celery", "PostgreSQL")
    ),
    Milestone(
      year = "2023 - 2024",
      role = "Full Stack Engineer",
      title = "Nexus Encrypted Hub Development",
      description = "Engineered real-time chat infrastructure supporting high concurrent connections. Implemented Room SQLite synchronization with local storage, boosting offline responsiveness.",
      stack = listOf("Node.js", "WebSockets", "MongoDB", "Redux", "Room DB")
    ),
    Milestone(
      year = "2021 - 2023",
      role = "Research & Engineering Core",
      title = "Computer Science & CSE specialization",
      description = "Delivered clean research, optimizing index trees (B-Tree query planning), algorithmic benchmarks, and materializing clean, edge-to-edge Jetpack Compose apps.",
      stack = listOf("Kotlin", "Jetpack Compose", "Python", "SQL Optimization")
    )
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("career_timeline_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = "Career", tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Milestones Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
      }
      Text(
        text = "Tap on any career node to unpack technical solutions and stacks delivered:",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
      )
      
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(IntrinsicSize.Min)
      ) {
        // Vertical Timeline line
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.width(32.dp)
        ) {
          milestones.forEachIndexed { idx, ms ->
            val isSel = selectedMilestone == idx
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isSel) accentColor else ElegantDarkSecondary)
                .border(2.dp, if (isSel) accentColor else ElegantDarkBorder, CircleShape)
                .clickable { selectedMilestone = idx }
            )
            
            if (idx < milestones.lastIndex) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .width(2.dp)
                  .background(ElegantDarkBorder)
              )
            }
          }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Active milestone details panel
        val ms = milestones[selectedMilestone]
        Column(
          modifier = Modifier
            .weight(1f)
            .background(ElegantDarkBg, RoundedCornerShape(16.dp))
            .border(1.dp, ElegantDarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Text(ms.year, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
          Text(ms.role, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
          Text(ms.title, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
          Spacer(modifier = Modifier.height(10.dp))
          Text(ms.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBBBBCC), lineHeight = 16.sp)
          Spacer(modifier = Modifier.height(12.dp))
          
          Text("Primary Core Stack:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(ms.stack) { st ->
              Box(
                modifier = Modifier
                  .background(ElegantDarkSecondary, RoundedCornerShape(6.dp))
                  .border(0.5.dp, ElegantDarkBorder, RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Text(st, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
              }
            }
          }
        }
      }
    }
  }
}

data class Milestone(
  val year: String,
  val role: String,
  val title: String,
  val description: String,
  val stack: List<String>
)

// ==========================================
// SYSTEM DESIGN TRIVIA ALIGNMENT CHALLENGE
// ==========================================
@Composable
fun SystemDesignTriviaCard(
  viewModel: PortfolioViewModel,
  accentColor: Color
) {
  val questions = viewModel.triviaQuestions
  val currentIdx by viewModel.currentTriviaIndex.collectAsState()
  val score by viewModel.userTriviaScore.collectAsState()
  val selectedAnswerIdx by viewModel.selectedAnswerIndex.collectAsState()
  val answered by viewModel.isTriviaAnswered.collectAsState()

  val currentQ = questions.getOrNull(currentIdx) ?: return

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("trivia_challenge_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Build, contentDescription = "Quiz", tint = accentColor, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("System Alignment Quiz", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text("Score: $score", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
      }
      
      Spacer(modifier = Modifier.height(10.dp))
      
      Text(
        text = "Test alignment on high-throughput performance architecture:",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFBBBBCC)
      )
      
      Spacer(modifier = Modifier.height(14.dp))
      
      Surface(
        color = ElegantDarkBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, ElegantDarkBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Q${currentQ.id}: ${currentQ.question}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          
          Spacer(modifier = Modifier.height(12.dp))
          
          currentQ.options.forEachIndexed { idx, opt ->
            val isSelected = selectedAnswerIdx == idx
            val btnColor = when {
              answered && idx == currentQ.correctIndex -> Color(0xFF1E4620)
              answered && isSelected && idx != currentQ.correctIndex -> Color(0xFF631515)
              isSelected -> ElegantDarkActivePill
              else -> ElegantDarkSecondary
            }
            val borderClr = when {
              answered && idx == currentQ.correctIndex -> Color(0xFF00FF87)
              answered && isSelected && idx != currentQ.correctIndex -> Color(0xFFFF5252)
              isSelected -> accentColor
              else -> ElegantDarkBorder
            }

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = !answered) { viewModel.selectTriviaAnswer(idx) },
              shape = RoundedCornerShape(8.dp),
              color = btnColor,
              border = BorderStroke(1.dp, borderClr)
            ) {
              Text(
                text = opt,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
          
          if (answered) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Heuristics: ${currentQ.explanation}",
              color = Color.LightGray,
              style = MaterialTheme.typography.bodyMedium,
              lineHeight = 16.sp
            )
          }
        }
      }
      
      Spacer(modifier = Modifier.height(14.dp))
      
      if (!answered) {
        Button(
          onClick = { viewModel.submitTriviaAnswer() },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkTertiary, contentColor = ElegantDarkOnPrimary),
          shape = RoundedCornerShape(10.dp),
          enabled = selectedAnswerIdx != -1
        ) {
          Text("Verify Architecture Alignment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      } else {
        Button(
          onClick = { viewModel.nextTriviaQuestion() },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkActivePill, contentColor = Color.White),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Next Challenge Query", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

// ==========================================
// 3. FEATURED CASE PROJECTS WITH MINISIMULATOR SANDBOXES
// ==========================================
@Composable
fun FeaturedProjectsSection(
  projects: List<ProjectItem>,
  onExpandToggle: (String) -> Unit,
  viewModel: PortfolioViewModel,
  accentColor: Color
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("featured_projects_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Projects",
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "System Deliveries",
          color = Color.White,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "Interactive architectural reviews. Expand nodes to execute microservice simulations.",
        color = Color(0xFF888888),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        projects.forEach { project ->
          ProjectItemCard(
            project = project,
            onExpandToggle = onExpandToggle,
            viewModel = viewModel,
            accentColor = accentColor
          )
        }
      }
    }
  }
}

@Composable
fun ProjectItemCard(
  project: ProjectItem,
  onExpandToggle: (String) -> Unit,
  viewModel: PortfolioViewModel,
  accentColor: Color
) {
  val isEx = project.isExpanded

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("project_item_${project.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSecondary),
    border = BorderStroke(1.5.dp, if (isEx) accentColor else ElegantDarkBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onExpandToggle(project.id) }
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = ElegantDarkActivePill
          ) {
            Text(
              text = project.category,
              color = accentColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              fontFamily = FontFamily.Monospace
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = project.name,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }

        IconButton(
          onClick = { onExpandToggle(project.id) },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (isEx) Icons.Default.Close else Icons.Default.PlayArrow,
            contentDescription = "Expand",
            tint = if (isEx) accentColor else Color.White,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = project.description,
        color = Color(0xFFBBBBCC),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Technology Labels
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(project.techStack) { tech ->
          Box(
            modifier = Modifier
              .background(ElegantDarkBg, RoundedCornerShape(6.dp))
              .border(0.5.dp, ElegantDarkBorder, RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(tech, color = Color(0xFF9999AA), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          }
        }
      }

      // Project Simulation sandboxes
      AnimatedVisibility(
        visible = isEx,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(ElegantDarkBorder)
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Engine Operational Metrics:", color = Color(0xFF888888), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(6.dp))
            Text(project.stats, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "LIVE SANDBOX MICROSERVICE SIMULATOR",
            color = accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(8.dp))

          when (project.simulationType) {
            "blog" -> {
              val comments by viewModel.simBlogComments.collectAsState()
              var typedText by remember { mutableStateOf("") }
              
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(ElegantDarkBg, RoundedCornerShape(12.dp))
                  .border(1.dp, ElegantDarkBorder, RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Text("Inbound simulated request payloads queue:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  comments.forEach { comm ->
                    Text(
                      text = "→ {payload_body: \"$comm\", state: \"OK_200\"}",
                      color = Color(0xFF55C080),
                      fontSize = 9.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  TextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Write dynamic mock trace...", color = Color.Gray, fontSize = 10.sp) },
                    modifier = Modifier
                      .weight(1f)
                      .height(44.dp),
                    colors = TextFieldDefaults.colors(
                      focusedContainerColor = ElegantDarkSecondary,
                      unfocusedContainerColor = ElegantDarkSecondary,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent,
                      focusedTextColor = Color.White
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(8.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Button(
                    onClick = {
                      if (typedText.isNotBlank()) {
                        viewModel.addSimBlogComment(typedText)
                        typedText = ""
                      }
                    },
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkTertiary, contentColor = ElegantDarkOnPrimary),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text("Secure Send", color = ElegantDarkOnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            "ecommerce" -> {
              val tasks by viewModel.simTasks.collectAsState()
              var taskText by remember { mutableStateOf("") }
              val isProducing = false

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(ElegantDarkBg, RoundedCornerShape(12.dp))
                  .border(1.dp, ElegantDarkBorder, RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Text("Secure chat over WebSockets:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  tasks.forEachIndexed { idx, t ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("• $t", color = Color.LightGray, fontSize = 10.sp)
                      Text(
                        "[Connected]",
                        color = accentColor,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { viewModel.removeSimTask(idx) }
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                  TextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    placeholder = { Text("Stream text parameter...", color = Color.Gray, fontSize = 10.sp) },
                    modifier = Modifier
                      .weight(1f)
                      .height(44.dp),
                    colors = TextFieldDefaults.colors(
                      focusedTextColor = Color.White,
                      unfocusedTextColor = Color.White,
                      focusedContainerColor = ElegantDarkSecondary,
                      unfocusedContainerColor = ElegantDarkSecondary,
                      focusedIndicatorColor = Color.Transparent,
                      unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(8.dp)
                  )

                  Spacer(modifier = Modifier.width(6.dp))

                  Button(
                    onClick = {
                      if (taskText.isNotBlank()) {
                        viewModel.addSimTask(taskText)
                        taskText = ""
                      }
                    },
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkTertiary, contentColor = ElegantDarkOnPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isProducing
                  ) {
                    Text("Push WebSocket", color = ElegantDarkOnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            "task" -> {
              val cartCount by viewModel.simCartItems.collectAsState()
              val cartTotal by viewModel.simCartTotal.collectAsState()

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(ElegantDarkBg, RoundedCornerShape(12.dp))
                  .border(1.dp, ElegantDarkBorder, RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("MERN Store checkout sandbox", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                  Text("Cart Total: $$cartTotal", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  StoreProductButton("Red Blazer ($120)", 120, viewModel)
                  StoreProductButton("Database Dev ($80)", 80, viewModel)
                  StoreProductButton("Express Setup ($50)", 50, viewModel)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { viewModel.clearSimCart() },
                  modifier = Modifier.fillMaxWidth(),
                  colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkActivePill, contentColor = Color.White),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text("Reset Cart ($cartCount items)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun StoreProductButton(pName: String, price: Int, viewModel: PortfolioViewModel) {
  Button(
    onClick = { viewModel.addSimCartItem(price) },
    modifier = Modifier.height(32.dp),
    colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkSecondary, contentColor = Color.White),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(0.5.dp, ElegantDarkBorder),
    contentPadding = PaddingValues(horizontal = 6.dp)
  ) {
    Text(pName, fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
  }
}

// ==========================================
// PERSONA SWAPPER SELECTION BOARD
// ==========================================
@Composable
fun PersonaAlignmentSwapper(
  selectedPersona: DeveloperTone,
  onSelect: (DeveloperTone) -> Unit,
  accentColor: Color
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("persona_swapper_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Menu, contentDescription = "Tones", tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Twin Behavioral Shifter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
      }
      Text(
        text = "Hot-swap Aniruddha's twin AI behavioral directive matrix instantly inside active runtime:",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )
      
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DeveloperTone.values().forEach { tone ->
          val isSel = selectedPersona == tone
          val itemBg = if (isSel) ElegantDarkActivePill else ElegantDarkBg
          val borderClr = if (isSel) accentColor else ElegantDarkBorder
          
          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable { onSelect(tone) },
            shape = RoundedCornerShape(12.dp),
            color = itemBg,
            border = BorderStroke(1.5.dp, borderClr)
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = tone.title.split(" ").first(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
              )
              Text(
                text = tone.subtitle.split(" ").first(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSel) accentColor else Color.Gray,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(14.dp))
      
      Surface(
        color = ElegantDarkBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, ElegantDarkBorder)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Active Persona Core Directive:",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = selectedPersona.quote,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp
          )
        }
      }
    }
  }
}

// ==========================================
// 4. DIGITAL TWIN CHATBOT ENGINE
// ==========================================
@Composable
fun DigitalTwinChatCard(
  messages: List<ChatMessage>,
  isLoading: Boolean,
  isLiveAi: Boolean,
  onSendMessage: (String) -> Unit,
  accentColor: Color
) {
  var chatInput by remember { mutableStateOf("") }
  
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("chat_assistant_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "AI Assistant",
            tint = accentColor,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Digital AI Twin",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
        }
        
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = ElegantDarkSecondary,
          border = BorderStroke(1.2.dp, accentColor)
        ) {
          Text(
            text = if (isLiveAi) "Live AI Mode" else "Simulated AI Mode",
            color = accentColor,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
      
      Text(
        text = "Consult Aniruddha's twin regarding system bounds, architectural planning, and design logic.",
        color = Color(0xFF888888),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )

      // Scrolling Chats Frame
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .background(ElegantDarkBg, RoundedCornerShape(16.dp))
          .padding(8.dp)
      ) {
        val chatScroll = rememberScrollState()

        LaunchedEffect(messages.size, isLoading) {
          chatScroll.animateScrollTo(chatScroll.maxValue)
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(chatScroll),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          messages.forEach { msg ->
            val align = if (msg.isUser) Alignment.End else Alignment.Start
            val bubbleBg = if (msg.isUser) ElegantDarkSecondary else ElegantDarkActivePill
            val outlineColor = if (msg.isUser) ElegantDarkBorder else accentColor
            
            Column(modifier = Modifier.align(align)) {
              Box(
                modifier = Modifier
                  .background(bubbleBg, RoundedCornerShape(12.dp))
                  .border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
                  .padding(10.dp)
                  .widthIn(max = 240.dp)
              ) {
                Text(
                  text = msg.text,
                  color = Color.White,
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )
              }
              Text(
                text = msg.timestamp,
                color = Color(0xFF555566),
                fontSize = 8.sp,
                modifier = Modifier
                  .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                  .align(align),
                fontFamily = FontFamily.Monospace
              )
            }
          }

          if (isLoading) {
            Box(
              modifier = Modifier
                .background(ElegantDarkActivePill, RoundedCornerShape(12.dp))
                .border(0.5.dp, accentColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
                .align(Alignment.Start)
            ) {
              Text("Analyzing response...", color = accentColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text("Suggestions:", color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(4.dp))
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val suggestions = listOf("Why hire you?", "Show technical skills", "Tell about Email Assistant", "Experience background")
        items(suggestions) { keyword ->
          Surface(
            modifier = Modifier.clickable { onSendMessage(keyword) },
            shape = RoundedCornerShape(8.dp),
            color = ElegantDarkSecondary
          ) {
            Text(keyword, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Chat input row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextField(
          value = chatInput,
          onValueChange = { chatInput = it },
          placeholder = { Text("Write dynamic instruction message...", color = Color.Gray, fontSize = 12.sp) },
          colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = ElegantDarkBg,
            unfocusedContainerColor = ElegantDarkBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("chat_input_text_field"),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = {
            if (chatInput.isNotBlank()) {
              onSendMessage(chatInput)
              chatInput = ""
            }
          })
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = {
            if (chatInput.isNotBlank()) {
              onSendMessage(chatInput)
              chatInput = ""
            }
          },
          modifier = Modifier
            .size(50.dp)
            .background(ElegantDarkTertiary, RoundedCornerShape(12.dp))
            .testTag("chat_send_button")
        ) {
          Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = ElegantDarkOnPrimary)
        }
      }
    }
  }
}

// ==========================================
// 5. GUESTBOOK ROOM DB PERSIST REGISTER
// ==========================================
@Composable
fun GuestbookAndContactSection(
  name: String,
  email: String,
  message: String,
  errorStr: String?,
  onNameChange: (String) -> Unit,
  onEmailChange: (String) -> Unit,
  onMessageChange: (String) -> Unit,
  onSubmit: () -> Unit,
  entries: List<GuestbookEntity>,
  onLikeEntry: (Int) -> Unit,
  accentColor: Color
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("guestbook_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurface),
    border = BorderStroke(1.5.dp, ElegantDarkBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Email,
          contentDescription = "Contact Form",
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Guestbook Sign-in",
          color = Color.White,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "Endorse my projects or write recommendations. Signatures are persisted inside the local Android Room Database.",
        color = Color(0xFF888888),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        TextField(
          value = name,
          onValueChange = onNameChange,
          placeholder = { Text("Your public credential name...", color = Color.Gray, fontSize = 12.sp) },
          colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = ElegantDarkBg,
            unfocusedContainerColor = ElegantDarkBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("guest_name_field")
        )

        TextField(
          value = email,
          onValueChange = onEmailChange,
          placeholder = { Text("Your verified email address...", color = Color.Gray, fontSize = 12.sp) },
          colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = ElegantDarkBg,
            unfocusedContainerColor = ElegantDarkBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("guest_email_field"),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        TextField(
          value = message,
          onValueChange = onMessageChange,
          placeholder = { Text("Leave your recommendation note or review feedback...", color = Color.Gray, fontSize = 12.sp) },
          colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = ElegantDarkBg,
            unfocusedContainerColor = ElegantDarkBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .testTag("guest_message_field")
        )

        if (errorStr != null) {
          Text(errorStr, color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Button(
          onClick = onSubmit,
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("guest_submit_button"),
          colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkTertiary, contentColor = ElegantDarkOnPrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Sign & Persist Profile", color = ElegantDarkOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Verified Endorsements (${entries.size})",
          color = Color.White,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
      
      Spacer(modifier = Modifier.height(10.dp))

      if (entries.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(ElegantDarkBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("Be the first to endorse Aniruddha's portfolio!", color = Color.Gray, fontSize = 11.sp)
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          entries.forEach { entry ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(ElegantDarkBg, RoundedCornerShape(12.dp))
                .border(0.5.dp, ElegantDarkBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = entry.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(entry.timestamp)),
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = entry.message,
                  color = Color(0xFFBBBBCC),
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )
              }

              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                  onClick = { onLikeEntry(entry.id) },
                  modifier = Modifier
                    .size(32.dp)
                    .background(ElegantDarkSecondary, CircleShape)
                ) {
                  Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Like", tint = accentColor, modifier = Modifier.size(12.dp))
                }
                Text("${entry.likes}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp), fontFamily = FontFamily.Monospace)
              }
            }
          }
        }
      }
    }
  }
}
