package com.project.readingactivities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            val showBottomBar = currentRoute in listOf(Screen.Books.route, Screen.Goals.route, Screen.Stats.route)
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(Screen.Books, Screen.Goals, Screen.Stats)
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
            composable(Screen.Goals.route) {
                GoalsScreen(viewModel)
            }
            composable(Screen.Stats.route) {
                StatsScreen(viewModel)
            }
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
    val books by viewModel.books.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Books", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Book")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books) { book ->
                BookCard(book, onClick = { onBookClick(book.id) })
            }
        }
    }

    if (showAddSheet) {
        AddBookSheet(
            onDismiss = { showAddSheet = false },
            onSave = { title, author, pages ->
                viewModel.addBook(title, author, pages)
                showAddSheet = false
            }
        )
    }
}

@Composable
fun BookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(book.author, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${book.readPages} / ${book.totalPages} pages",
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookSheet(onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add New Book", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title*") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = totalPages,
                onValueChange = { totalPages = it },
                label = { Text("Total pages*") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Text("Please fill required fields with valid data", color = Color.Red, fontSize = 12.sp)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && totalPages.toIntOrNull() != null,
                onClick = {
                    val pages = totalPages.toIntOrNull()
                    if (title.isNotBlank() && pages != null) {
                        onSave(title, author, pages)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(viewModel: ReadingViewModel, bookId: String?, onBack: () -> Unit) {
    val books by viewModel.books.collectAsState()
    val book = books.find { it.id == bookId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (book == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val animatedProgress by animateFloatAsState(
        targetValue = book.progress,
        label = "progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(book.author, fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text("${(book.progress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Text("${book.readPages} of ${book.totalPages} pages read", fontSize = 16.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { viewModel.updateProgress(book.id, 10) }) {
                    Text("+10 pages")
                }
                Button(onClick = { viewModel.updateProgress(book.id, 20) }) {
                    Text("+20 pages")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete this book?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book.id)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: ReadingViewModel) {
    val goals by viewModel.goals.collectAsState()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Goals", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(goals) { goal ->
                ListItem(
                    headlineContent = { Text(goal.description) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.toggleGoal(goal.id) }) {
                            Icon(
                                if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (goal.isCompleted) Color.Green else Color.Gray
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: ReadingViewModel) {
    val totalPages = viewModel.totalPagesRead
    
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Stats", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Pages Read", fontSize = 14.sp)
                    Text("$totalPages", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reading Streak", fontSize = 14.sp)
                    Text("5 days", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text("Pages per day (Last 7 days)", fontWeight = FontWeight.Bold)
            
            // Simple Bar Chart Placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f, 0.8f)
                heights.forEach { h ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .width(20.dp)
                            .fillMaxHeight(h),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    ) {}
                }
            }
        }
    }
}
