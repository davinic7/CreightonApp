package com.devdavinic.creightonapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.devdavinic.creightonapp.data.AppDatabase
import com.devdavinic.creightonapp.data.RecordRepository
import com.devdavinic.creightonapp.model.ProfileType
import com.devdavinic.creightonapp.model.UserProfile
import com.devdavinic.creightonapp.ui.screens.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.*

enum class Screen {
    HOME, REGISTRATION, PLANILLA, PREDICTION, SPICE,
    AI_CHAT, SPECIAL_CASES, PARTNER, PROFILE, CALENDAR,
    SIMULATOR, NOTIFICATIONS, SYMPTOMS, EXPORT, VIDEOS, TRIVIA
}

class MainActivity : ComponentActivity() {

    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "creighton_database")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
    }
    private val repository        by lazy { RecordRepository(db.recordDao()) }
    private val profileDao        by lazy { db.userProfileDao() }
    private val symptomRepository by lazy {
        com.devdavinic.creightonapp.data.SymptomRepository(db.symptomDao())
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(profileDao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context   = LocalContext.current
            var skin      by remember { mutableStateOf(SkinManager.loadSkin(context)) }
            val isFirst   = remember { SkinManager.isFirstLaunch(context) }
            var showOnboarding by remember { mutableStateOf(isFirst) }

            CreightonAppTheme(skin = skin) {
                if (showOnboarding) {
                    SkinOnboardingScreen { chosenSkin ->
                        skin = chosenSkin
                        SkinManager.saveSkin(context, chosenSkin)
                        showOnboarding = false
                    }
                } else {
                    val authState by authViewModel.authState.collectAsState()
                    val profile   by authViewModel.currentProfile.collectAsState()
                    var mainViewModel by remember { mutableStateOf<MainViewModel?>(null) }

                    LaunchedEffect(authState) {
                        when (val state = authState) {
                            is AuthState.LoggedIn -> {
                                val uid    = state.user.uid
                                val isTest = profile?.isTestMode ?: false
                                if (mainViewModel == null) {
                                    mainViewModel = object : ViewModelProvider.Factory {
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            @Suppress("UNCHECKED_CAST")
                                            return MainViewModel(
                                                repository, symptomRepository,
                                                uid, isTest, applicationContext
                                            ) as T
                                        }
                                    }.create(MainViewModel::class.java)
                                }
                            }
                            is AuthState.LoggedOut,
                            is AuthState.Error -> mainViewModel = null
                            else -> {}
                        }
                    }

                    when (authState) {
                        is AuthState.Loading ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        is AuthState.LoggedOut,
                        is AuthState.Error ->
                            LoginScreen(authViewModel = authViewModel)
                        is AuthState.LoggedIn -> {
                            val vm = mainViewModel
                            if (vm == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                AppContent(
                                    mainViewModel = vm,
                                    authViewModel = authViewModel,
                                    profile       = profile,
                                    isPartner     = profile?.profileType == ProfileType.MASCULINE.name,
                                    skin          = skin,
                                    onSkinChange  = { newSkin ->
                                        skin = newSkin
                                        SkinManager.saveSkin(context, newSkin)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// APP CONTENT
// =============================================================================

@Composable
private fun AppContent(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    profile: UserProfile?,
    isPartner: Boolean,
    skin: AppSkin,
    onSkinChange: (AppSkin) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    LaunchedEffect(isPartner) {
        if (isPartner) currentScreen = Screen.PARTNER
    }

    // Screens where floating FAB is hidden
    val hideFab = currentScreen in listOf(Screen.AI_CHAT, Screen.REGISTRATION)

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Animated screen transitions ────────────────────────────────────────
        AnimatedContent(
            targetState  = currentScreen,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                if (forward)
                    (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut(tween(180)))
                else
                    (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
            },
            label = "screenTransition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                Screen.HOME -> HomeScreen(
                    viewModel         = mainViewModel,
                    profile           = profile,
                    onRegistroParcial = { currentScreen = Screen.REGISTRATION },
                    onVerPlanilla     = { currentScreen = Screen.PLANILLA },
                    onPrediccion      = { currentScreen = Screen.PREDICTION },
                    onSpice           = { currentScreen = Screen.SPICE },
                    onAsistente       = { currentScreen = Screen.AI_CHAT },
                    onCasosEspeciales = { currentScreen = Screen.SPECIAL_CASES },
                    onPartner         = { currentScreen = Screen.PARTNER },
                    onProfile         = { currentScreen = Screen.PROFILE },
                    onCalendario      = { currentScreen = Screen.CALENDAR },
                    onSimulator       = { currentScreen = Screen.SIMULATOR },
                    onNotifications   = { currentScreen = Screen.NOTIFICATIONS },
                    onSymptoms        = { currentScreen = Screen.SYMPTOMS },
                    onExport          = { currentScreen = Screen.EXPORT },
                    onVideos          = { currentScreen = Screen.VIDEOS },
                    onTrivia          = { currentScreen = Screen.TRIVIA },
                    modifier          = Modifier.fillMaxSize()
                )
                Screen.REGISTRATION -> RegistrationScreen(
                    viewModel = mainViewModel,
                    onClose   = { currentScreen = Screen.HOME }
                )
                Screen.PLANILLA -> PlanillaScreen(
                    viewModel   = mainViewModel,
                    onBack      = { currentScreen = Screen.HOME },
                    onRegistrar = { currentScreen = Screen.REGISTRATION }
                )
                Screen.PREDICTION -> PredictionScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.SPICE -> SpiceScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.AI_CHAT -> AiChatScreen(
                    mainViewModel = mainViewModel,
                    onBack        = { currentScreen = Screen.HOME }
                )
                Screen.SPECIAL_CASES -> SpecialCasesScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.PARTNER -> PartnerScreen(
                    viewModel     = mainViewModel,
                    authViewModel = authViewModel,
                    onBack        = { currentScreen = Screen.HOME },
                    onGoToAiChat  = { currentScreen = Screen.AI_CHAT }
                )
                Screen.PROFILE -> ProfileScreen(
                    authViewModel = authViewModel,
                    mainViewModel = mainViewModel,
                    currentSkin   = skin,
                    onSkinChange  = onSkinChange,
                    onBack        = { currentScreen = Screen.HOME },
                    onLogout      = { currentScreen = Screen.HOME }
                )
                Screen.CALENDAR -> CalendarScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.SIMULATOR -> TestSimulatorScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.NOTIFICATIONS -> NotificationSettingsScreen(
                    onBack = { currentScreen = Screen.HOME }
                )
                Screen.SYMPTOMS -> SymptomsScreen(
                    viewModel = mainViewModel,
                    onBack    = { currentScreen = Screen.HOME }
                )
                Screen.VIDEOS -> VideoScreen(
                    onBack = { currentScreen = Screen.HOME }
                )
                Screen.TRIVIA -> TriviaScreen(
                    onBack = { currentScreen = Screen.HOME }
                )
                Screen.EXPORT -> ExportScreen(
                    viewModel     = mainViewModel,
                    authViewModel = authViewModel,
                    onBack        = { currentScreen = Screen.HOME }
                )
            }
        }

        // ── Floating AI assistant — hidden on AI_CHAT and REGISTRATION ─────────
        AnimatedVisibility(
            visible  = !hideFab,
            enter    = scaleIn(tween(300)) + fadeIn(tween(300)),
            exit     = scaleOut(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            FloatingAssistantButton(
                skin    = skin,
                modifier = Modifier,
                onClick  = { currentScreen = Screen.AI_CHAT }
            )
        }
    }
}