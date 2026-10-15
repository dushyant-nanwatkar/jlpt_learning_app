package com.nihongo.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nihongo.app.ui.grammar.GrammarScreen
import com.nihongo.app.ui.home.HomeScreen
import com.nihongo.app.ui.results.ResultsScreen
import com.nihongo.app.ui.results.TestDetailScreen
import com.nihongo.app.ui.settings.SettingsScreen
import com.nihongo.app.ui.study.StudyScreen
import com.nihongo.app.ui.tests.TestTakerScreen
import com.nihongo.app.ui.tests.TestsScreen
import com.nihongo.app.ui.theme.NihongoTheme
import com.nihongo.app.ui.vocabulary.VocabularyScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NihongoTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Study : Screen("study", "Study", Icons.Default.School)
    object Vocabulary : Screen("vocabulary", "Vocab", Icons.Default.Book)
    object Grammar : Screen("grammar", "Grammar", Icons.AutoMirrored.Filled.MenuBook)
    object Tests : Screen("tests", "Tests", Icons.Default.Quiz)
    object Results : Screen("results", "Results", Icons.Default.Assessment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showRightShelf by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        Screen.Home,
        Screen.Study,
        Screen.Vocabulary,
        Screen.Grammar,
        Screen.Tests,
        Screen.Results,
        Screen.Settings
    )

    val bottomBarItems = listOf(
        Screen.Home,
        Screen.Study,
        Screen.Vocabulary,
        Screen.Grammar,
        Screen.Tests
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDetailOrTestPage = currentRoute?.startsWith("test_taker") == true || currentRoute?.startsWith("test_detail") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isDetailOrTestPage,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0A0A),
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Left Sandwich Menu Drawer Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                    border = BorderStroke(1.dp, Color(0xFF222222))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "日本語 Nihongo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "JLPT Mastery & Learning",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFF1A1A1A)
                )

                Text(
                    text = "NAVIGATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Menu items
                navigationItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                screen.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White
                            )
                        },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
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
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF181818),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Scaffold(
                containerColor = Color.Black,
                topBar = {
                    val currentTitle = when {
                        currentRoute?.startsWith("test_taker") == true -> "Take Test"
                        currentRoute?.startsWith("test_detail") == true -> "Test Review"
                        else -> navigationItems.find { it.route == currentRoute }?.title ?: "Nihongo"
                    }

                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    currentTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        },
                        navigationIcon = {
                            if (isDetailOrTestPage) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            } else {
                                // Sandwich Menu Button
                                IconButton(onClick = {
                                    scope.launch { drawerState.open() }
                                }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Open Navigation Menu",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isDetailOrTestPage) {
                                // Right Shelf Button
                                IconButton(onClick = { showRightShelf = !showRightShelf }) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Open Right Shelf",
                                        tint = if (showRightShelf) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black,
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    if (!isDetailOrTestPage) {
                        NavigationBar(
                            containerColor = Color(0xFF0A0A0A),
                            contentColor = Color.White
                        ) {
                            bottomBarItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF1E1E1E)
                                    ),
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
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .padding(innerPadding)
                        .background(Color.Black)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToStudy = {
                                navController.navigate(Screen.Study.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToTests = {
                                navController.navigate(Screen.Tests.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    composable(Screen.Study.route) { StudyScreen() }
                    composable(Screen.Vocabulary.route) { VocabularyScreen() }
                    composable(Screen.Grammar.route) { GrammarScreen() }
                    composable(Screen.Tests.route) {
                        TestsScreen(onNavigateToTest = { testId ->
                            navController.navigate("test_taker/$testId")
                        })
                    }
                    composable("test_taker/{testId}") { backStackEntry ->
                        val testId = backStackEntry.arguments?.getString("testId")?.toLongOrNull() ?: 0L
                        TestTakerScreen(testId = testId, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Results.route) {
                        ResultsScreen(onNavigateToDetail = { testId ->
                            navController.navigate("test_detail/$testId")
                        })
                    }
                    composable("test_detail/{testId}") { backStackEntry ->
                        val testId = backStackEntry.arguments?.getString("testId")?.toLongOrNull() ?: 0L
                        TestDetailScreen(testId = testId, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Settings.route) { SettingsScreen() }
                }
            }

            // Right Shelf Section Panel
            AnimatedVisibility(
                visible = showRightShelf,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                RightShelfPanel(
                    onClose = { showRightShelf = false },
                    onNavigate = { route ->
                        showRightShelf = false
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RightShelfPanel(
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = Color(0xFF0F0F0F),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Right Shelf",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Shelf", tint = Color.Gray)
                }
            }

            HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "QUICK ACCESS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            RightShelfShortcutItem(
                title = "Grammar Section",
                subtitle = "Study N5-N3 rules & examples",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = { onNavigate(Screen.Grammar.route) }
            )

            RightShelfShortcutItem(
                title = "Vocabulary Hub",
                subtitle = "Browse & review word list",
                icon = Icons.Default.Book,
                onClick = { onNavigate(Screen.Vocabulary.route) }
            )

            RightShelfShortcutItem(
                title = "Flashcard Study",
                subtitle = "Spaced Repetition mode",
                icon = Icons.Default.School,
                onClick = { onNavigate(Screen.Study.route) }
            )

            RightShelfShortcutItem(
                title = "Practice Tests",
                subtitle = "Generated JLPT quizzes",
                icon = Icons.Default.Quiz,
                onClick = { onNavigate(Screen.Tests.route) }
            )

            HorizontalDivider(color = Color(0xFF1E1E1E), modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "THEME & MODE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                border = BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pitch Black AMOLED",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "True #000000 background enabled for maximum contrast and battery saving.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { onNavigate(Screen.Settings.route) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings", color = Color.LightGray)
            }
        }
    }
}

@Composable
fun RightShelfShortcutItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    subtitle,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
