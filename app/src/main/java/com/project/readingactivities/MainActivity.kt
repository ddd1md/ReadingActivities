package com.project.readingactivities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.readingactivities.ui.theme.ReadingActivitiesTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadingActivitiesTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Books : Screen("books", "Books", Icons.AutoMirrored.Filled.MenuBook)
    object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    object Finished : Screen("finished", "Library", Icons.Default.CollectionsBookmark)
    object Stats : Screen("stats", "Stats", Icons.Default.BarChart)
    object BookDetail : Screen("book_detail/{bookId}", "Detail", Icons.Default.Info)
}

@Composable
fun MainScreen(viewModel: ReadingViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute in listOf(Screen.Books.route, Screen.Goals.route, Screen.Finished.route, Screen.Stats.route)
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(Screen.Books, Screen.Goals, Screen.Finished, Screen.Stats)
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
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
            startDestination = Screen.Books.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Books.route) {
                BooksScreen(viewModel, onBookClick = { bookId ->
                    navController.navigate("book_detail/$bookId")
                })
            }
            composable(Screen.Goals.route) { GoalsScreen(viewModel) }
            composable(Screen.Finished.route) { FinishedBooksScreen(viewModel) }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
            composable(Screen.BookDetail.route) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")
                BookDetailScreen(viewModel, bookId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(viewModel: ReadingViewModel, onBookClick: (String) -> Unit) {
    val allBooks by viewModel.books.collectAsState()
    val books = allBooks.filter { !it.isFinished }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reading Now", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Book", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
            )
        }
    ) { padding ->
        if (books.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.LibraryBooks, "No active books", "Tap + to start a new journey")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(books) { book -> BookCard(book, onClick = { onBookClick(book.id) }) }
            }
        }
    }
    if (showAddSheet) {
        AddBookSheet(onDismiss = { showAddSheet = false }, onSave = { t, a, p -> viewModel.addBook(t, a, p); showAddSheet = false })
    }
}

@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Book, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(book.author, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookSheet(onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("New Book", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = totalPages, onValueChange = { totalPages = it }, label = { Text("Total pages*") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Button(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), enabled = title.isNotBlank() && totalPages.toIntOrNull() != null, onClick = { onSave(title, author, totalPages.toInt()) }) {
                Text("Add to Library", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(viewModel: ReadingViewModel, bookId: String?, onBack: () -> Unit) {
    val books by viewModel.books.collectAsState()
    val book = books.find { it.id == bookId }
    var showFinishDialog by remember { mutableStateOf(false) }
    var rating by remember { mutableFloatStateOf(5f) }
    var review by remember { mutableStateOf("") }

    if (book == null) { LaunchedEffect(Unit) { onBack() } ; return }

    var sliderPosition by remember { mutableFloatStateOf(book.readPages.toFloat()) }
    val animatedProgress by animateFloatAsState(targetValue = book.progress, label = "progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (book.progress >= 0.99f) {
                        TextButton(onClick = { showFinishDialog = true }) { Text("Finish", fontWeight = FontWeight.Bold) }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(book.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), strokeWidth = 16.dp, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Read: ${sliderPosition.roundToInt()} pages", fontWeight = FontWeight.Bold)
                    Text("of ${book.totalPages}", color = MaterialTheme.colorScheme.secondary)
                }
                Slider(value = sliderPosition, onValueChange = { sliderPosition = it; viewModel.updateReadPages(book.id, it.roundToInt()) }, valueRange = 0f..book.totalPages.toFloat())
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Book") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("How would you rate this book?")
                    Slider(value = rating, onValueChange = { rating = it }, valueRange = 0f..10f, steps = 9)
                    Text("Rating: ${rating.toInt()}/10", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    OutlinedTextField(value = review, onValueChange = { review = it }, placeholder = { Text("Write a short review...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { viewModel.finishBook(book.id, rating.toInt(), review); onBack() }) { Text("Done") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: ReadingViewModel) {
    val allGoals by viewModel.goals.collectAsState()
    val activeGoals = allGoals.filter { !it.isCompleted }
    val completedGoals = allGoals.filter { it.isCompleted }
    var showAddDialog by remember { mutableStateOf(false) }
    var newGoalDesc by remember { mutableStateOf("") }
    var showCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.ExtraBold) },
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.AddCircle, "Add", tint = MaterialTheme.colorScheme.primary) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Active Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (activeGoals.isEmpty()) item { Text("No active goals", color = Color.Gray, fontSize = 14.sp) }
            items(activeGoals) { goal -> GoalItem(goal, onToggle = { viewModel.toggleGoal(goal.id) }) }
            
            item { 
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().clickable { showCompleted = !showCompleted }, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed Collection (${completedGoals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(if (showCompleted) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }
            if (showCompleted) {
                items(completedGoals) { goal -> GoalItem(goal, onToggle = { viewModel.toggleGoal(goal.id) }) }
            }
        }
    }
    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("New Goal") }, text = { OutlinedTextField(value = newGoalDesc, onValueChange = { newGoalDesc = it }, placeholder = { Text("E.g. Read 50 pages today") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) },
            confirmButton = { Button(enabled = newGoalDesc.isNotBlank(), onClick = { viewModel.addGoal(newGoalDesc); newGoalDesc = ""; showAddDialog = false }) { Text("Add") } })
    }
}

@Composable
fun GoalItem(goal: Goal, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text(goal.description, style = if (goal.isCompleted) MaterialTheme.typography.bodyLarge.copy(color = Color.Gray) else MaterialTheme.typography.bodyLarge) }, trailingContent = { Checkbox(checked = goal.isCompleted, onCheckedChange = { onToggle() }) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedBooksScreen(viewModel: ReadingViewModel) {
    val allBooks by viewModel.books.collectAsState()
    val finishedBooks = allBooks.filter { it.isFinished }.sortedByDescending { it.rating }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Finished Collection", fontWeight = FontWeight.ExtraBold) }) }) { padding ->
        if (finishedBooks.isEmpty()) {
            EmptyState(Icons.Default.CollectionsBookmark, "Empty Collection", "Finish your first book to see it here!")
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(finishedBooks) { book ->
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("${book.rating}/10", color = Color.White, modifier = Modifier.padding(4.dp)) }
                            }
                            Text(book.author, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            if (!book.review.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("“${book.review}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: ReadingViewModel) {
    val totalPages = viewModel.totalPagesRead
    val stats by viewModel.weeklyStats.collectAsState()
    val streak = viewModel.readingStreak
    val maxPages = stats.maxOfOrNull { it.pages }?.coerceAtLeast(1) ?: 1

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Activity", fontWeight = FontWeight.ExtraBold) }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Total Pages", "$totalPages", Icons.Default.AutoStories, Modifier.weight(1f))
                StatCard("Streak", "$streak days", Icons.Default.Whatshot, Modifier.weight(1f))
            }
            Text("Weekly Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    stats.forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                            Text("${stat.pages}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Box(modifier = Modifier.width(30.dp).fillMaxHeight((stat.pages.toFloat() / maxPages).coerceIn(0.05f, 0.9f)).clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stat.day, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(subtitle, color = Color.Gray.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
