package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.TransactionEntity
import com.example.ui.screens.ai.AiAssistantScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.emergency.EmergencyApprovalScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.AppRestrictionSettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.WhatsAppLinkingScreen
import com.example.ui.screens.transactions.AddEditTransactionScreen
import com.example.ui.screens.transactions.TransactionListScreen
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.sbi.AutomationSettingsScreen
import com.example.ui.screens.sbi.BalanceScreen
import com.example.ui.screens.sbi.PendingPopupHost
import com.example.ui.screens.sbi.PendingTransactionFormScreen
import com.example.ui.screens.sbi.PendingTransactionsScreen
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.AnalyticsViewModel
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.TransactionViewModel

@Composable
fun FinPulseNavHost(
  mainViewModel: MainViewModel,
  deepLinkPendingId: String? = null,
  onDeepLinkConsumed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val navController = rememberNavController()
  val userSettings by mainViewModel.userSettings.collectAsState()
  val syncMessage by mainViewModel.syncMessage.collectAsState()
  val authError by mainViewModel.authError.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  val dashboardViewModel: DashboardViewModel = viewModel()
  val transactionViewModel: TransactionViewModel = viewModel()
  val analyticsViewModel: AnalyticsViewModel = viewModel()
  val aiAssistantViewModel: AiAssistantViewModel = viewModel()
  val settingsViewModel: SettingsViewModel = viewModel()
  val statementViewModel: com.example.viewmodel.StatementAutomationViewModel = viewModel()

  var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

  val appContext = androidx.compose.ui.platform.LocalContext.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  val loggedIn = userSettings.userId.isNotBlank() && userSettings.onboardingCompleted
  val statementAuthRequired by statementViewModel.authRequired.collectAsState()

  LaunchedEffect(statementAuthRequired) {
    if (statementAuthRequired) snackbarHostState.showSnackbar("Session expired. Please log in again to keep receiving transaction reminders.")
  }

  // Background sync (works while the app is closed / phone was off) + foreground polling while visible.
  LaunchedEffect(loggedIn) {
    if (loggedIn) {
      com.example.util.NotificationHelper.createChannels(appContext)
      com.example.worker.SyncScheduler.schedule(appContext)
    } else {
      com.example.worker.SyncScheduler.cancelAll(appContext)
    }
  }
  LaunchedEffect(loggedIn, lifecycleOwner) {
    if (loggedIn) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
        while (true) {
          statementViewModel.syncAndNotify()
          kotlinx.coroutines.delay(30_000)
        }
      }
    }
  }

  // Account opened on another phone (or disabled): sign out here and say why.
  LaunchedEffect(Unit) {
    com.example.data.remote.SessionEvents.signedOut.collect { msg ->
      mainViewModel.showNotice(msg)
      mainViewModel.logout()
      runCatching { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }
    }
  }

  // Notification tap -> open the exact transaction form (original amount/date), never a blank screen.
  LaunchedEffect(deepLinkPendingId, loggedIn) {
    val id = deepLinkPendingId
    if (id != null && loggedIn) {
      runCatching {
        if (id == "whatsapp") navController.navigate(Screen.WhatsAppLinking.route)
        else if (id == "add") navController.navigate(Screen.AddTransaction.route)
        else if (id == "emergency") navController.navigate(Screen.EmergencyApproval.route)
        else if (id == "list") navController.navigate(Screen.PendingTransactions.route)
        else navController.navigate(Screen.PendingForm.create(id))
      }
      onDeepLinkConsumed()
    }
  }

  // Sync notification
  LaunchedEffect(syncMessage) {
    syncMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      mainViewModel.clearSyncMessage()
    }
  }

  // Auth error notification
  LaunchedEffect(authError) {
    authError?.let { err ->
      snackbarHostState.showSnackbar(err)
      mainViewModel.clearAuthError()
    }
  }

  // Determine starting destination
  val startDestination = when {
    userSettings.userId.isBlank() -> Screen.Login.route
    !userSettings.onboardingCompleted -> Screen.Onboarding.route
    else -> Screen.Dashboard.route
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val isBottomNavVisible = currentRoute in Screen.bottomNavItems.map { it.route }

  PendingPopupHost(
    viewModel = statementViewModel,
    enabled = loggedIn && currentRoute != null &&
      currentRoute != Screen.Login.route && currentRoute != Screen.Register.route && currentRoute != Screen.Onboarding.route &&
      currentRoute != Screen.PendingForm.route,
    onOpenForm = { id -> navController.navigate(Screen.PendingForm.create(id)) }
  )

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    bottomBar = {
      if (isBottomNavVisible) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          modifier = Modifier.testTag("finpulse_bottom_navigation")
        ) {
          Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
              selected = isSelected,
              onClick = {
                if (currentRoute != screen.route) {
                  navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                      saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              },
              icon = {
                screen.icon?.let { icon ->
                  Icon(imageVector = icon, contentDescription = screen.title)
                }
              },
              label = {
                Text(
                  text = screen.title,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
              ),
              modifier = Modifier.testTag("nav_${screen.route}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
      ) {
        // 1. Login (Email + Password + Device Binding)
        composable(Screen.Login.route) {
          LoginScreen(
            viewModel = mainViewModel,
            onLoginSuccess = { isFirstTime ->
              if (isFirstTime) {
                navController.navigate(Screen.Onboarding.route) {
                  popUpTo(Screen.Login.route) { inclusive = true }
                }
              } else {
                navController.navigate(Screen.Dashboard.route) {
                  popUpTo(Screen.Login.route) { inclusive = true }
                }
              }
            },
            onNavigateToRegister = {
              navController.navigate(Screen.Register.route)
            }
          )
        }

        // 2. Register (One Device = One Account)
        composable(Screen.Register.route) {
          RegisterScreen(
            viewModel = mainViewModel,
            onRegisterSuccess = { isFirstTime ->
              navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
              }
            },
            onNavigateToLogin = {
              navController.popBackStack()
            }
          )
        }

        // 3. Onboarding
        composable(Screen.Onboarding.route) {
          OnboardingScreen(
            viewModel = mainViewModel,
            onFinished = { mode ->
              navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
              }
            }
          )
        }

        // 4. Main Dashboard
        composable(Screen.Dashboard.route) {
          DashboardScreen(
            viewModel = dashboardViewModel,
            statementViewModel = statementViewModel,
            onPendingTransactionsClick = { navController.navigate(Screen.PendingTransactions.route) },
            onSbiBalanceClick = { navController.navigate(Screen.Balance.route) },
            onOpenPending = { id -> navController.navigate(Screen.PendingForm.create(id)) },
            onLinkWhatsAppClick = { navController.navigate(Screen.WhatsAppLinking.route) },
            trackingMode = userSettings.trackingMode,
            onAddTransactionClick = {
              transactionToEdit = null
              navController.navigate(Screen.AddTransaction.route)
            },
            onViewAllTransactionsClick = {
              navController.navigate(Screen.Transactions.route)
            },
            onTransactionClick = { tx ->
              transactionToEdit = tx
              navController.navigate(Screen.EditTransaction.route)
            },
            onAiAssistantClick = {
              navController.navigate(Screen.AiAssistant.route)
            },
            onConfigureLimitsClick = {
              navController.navigate(Screen.Settings.route)
            }
          )
        }

        // 5. Transactions List
        composable(Screen.Transactions.route) {
          TransactionListScreen(
            viewModel = transactionViewModel,
            onAddTransactionClick = {
              transactionToEdit = null
              navController.navigate(Screen.AddTransaction.route)
            },
            onEditTransactionClick = { tx ->
              transactionToEdit = tx
              navController.navigate(Screen.EditTransaction.route)
            }
          )
        }

        // 6. Analytics
        composable(Screen.Analytics.route) {
          AnalyticsScreen(viewModel = analyticsViewModel)
        }

        // 7. AI Assistant
        composable(Screen.AiAssistant.route) {
          AiAssistantScreen(
            viewModel = aiAssistantViewModel,
            onEmergencyRequestClick = {
              navController.navigate(Screen.EmergencyApproval.route)
            }
          )
        }

        // 8. Settings
        composable(Screen.Settings.route) {
          SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateToWhatsApp = {
              navController.navigate(Screen.WhatsAppLinking.route)
            },
            onNavigateToAppRestrictions = {
              navController.navigate(Screen.AppRestrictions.route)
            },
            onNavigateToAutomationSettings = { navController.navigate(Screen.AutomationSettings.route) },
            onNavigateToPendingTransactions = { navController.navigate(Screen.PendingTransactions.route) },
            onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
            mainViewModel = mainViewModel,
            onLogout = {
              mainViewModel.logout()
              navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
              }
            }
          )
        }

        composable(Screen.Profile.route) {
          ProfileScreen(
            viewModel = mainViewModel,
            onBack = { navController.popBackStack() },
            onSignedOut = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }
          )
        }

        // 9. App Restrictions & Accessibility Service (Task 5)
        composable(Screen.AppRestrictions.route) {
          BackHandler { navController.popBackStack() }
          AppRestrictionSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
          )
        }

        // 10. Add Transaction
        composable(Screen.AddTransaction.route) {
          BackHandler { navController.popBackStack() }
          AddEditTransactionScreen(
            viewModel = transactionViewModel,
            transactionToEdit = null,
            onNavigateBack = { navController.popBackStack() }
          )
        }

        // 11. Edit Transaction
        composable(Screen.EditTransaction.route) {
          BackHandler { navController.popBackStack() }
          AddEditTransactionScreen(
            viewModel = transactionViewModel,
            transactionToEdit = transactionToEdit,
            onNavigateBack = { navController.popBackStack() }
          )
        }

        // 12. WhatsApp Linking (Meta Cloud API)
        composable(Screen.WhatsAppLinking.route) {
          BackHandler { navController.popBackStack() }
          WhatsAppLinkingScreen(
            viewModel = settingsViewModel,
            automationViewModel = statementViewModel,
            onNavigateBack = { navController.popBackStack() }
          )
        }

        // SBI automation screens
        composable(Screen.AutomationSettings.route) {
          BackHandler { navController.popBackStack() }
          AutomationSettingsScreen(
            viewModel = statementViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenWhatsAppLink = { navController.navigate(Screen.WhatsAppLinking.route) },
            onOpenBalance = { navController.navigate(Screen.Balance.route) }
          )
        }
        composable(Screen.Balance.route) {
          BackHandler { navController.popBackStack() }
          BalanceScreen(viewModel = statementViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.PendingTransactions.route) {
          BackHandler { navController.popBackStack() }
          PendingTransactionsScreen(
            viewModel = statementViewModel,
            onNavigateBack = { navController.popBackStack() },
            onOpenForm = { id -> navController.navigate(Screen.PendingForm.create(id)) }
          )
        }
        composable(
          route = Screen.PendingForm.route,
          arguments = listOf(androidx.navigation.navArgument("pendingId") { type = androidx.navigation.NavType.StringType })
        ) { entry ->
          val id = entry.arguments?.getString("pendingId").orEmpty()
          BackHandler { navController.popBackStack() }
          PendingTransactionFormScreen(
            pendingId = id,
            viewModel = statementViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNext = { nextId ->
              navController.navigate(Screen.PendingForm.create(nextId)) { popUpTo(Screen.PendingForm.route) { inclusive = true } }
            }
          )
        }

        // 13. Emergency Approval
        composable(Screen.EmergencyApproval.route) {
          BackHandler { navController.popBackStack() }
          EmergencyApprovalScreen(
            onNavigateBack = { navController.popBackStack() },
            onGrantTemporaryAllowance = { extraAllowance ->
              val currentLimit = userSettings.dailySpendingLimit
              dashboardViewModel.updateDailyLimit(currentLimit + extraAllowance)
            }
          )
        }
      }
    }
  }
}
