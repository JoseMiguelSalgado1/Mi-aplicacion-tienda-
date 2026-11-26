package com.mitienda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------- TIPOGRAFÍA ----------
val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 2.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
)

// ---------- TEMA: aquí se definen los colores (el único lugar con colores directos) ----------
@Composable
fun PixelForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C4DFF),
            onPrimary = Color.White,
            secondary = Color(0xFF00E5FF),
            tertiary = Color(0xFFFF2E93),
            background = Color(0xFF0B0F1A),
            onBackground = Color.White,
            surface = Color(0xFF141A2B),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFF9AA3B8)
        ),
        typography = AppTypography,
        content = content
    )
}

// ---------- DATOS (todo texto es un id de recurso) ----------
data class Game(
    val id: Int,
    @param:StringRes val title: Int,
    @param:StringRes val genre: Int,
    @param:StringRes val platform: Int,
    val price: Double
)

val games = listOf(
    Game(0, R.string.game_neon, R.string.genre_racing, R.string.platform_ps5, 59.99),
    Game(1, R.string.game_dungeon, R.string.genre_rpg, R.string.platform_pc, 39.99),
    Game(2, R.string.game_void, R.string.genre_action, R.string.platform_xbox, 49.99),
    Game(3, R.string.game_farm, R.string.genre_casual, R.string.platform_switch, 29.99),
    Game(4, R.string.game_cyber, R.string.genre_action, R.string.platform_pc, 44.99),
    Game(5, R.string.game_kingdom, R.string.genre_rpg, R.string.platform_ps5, 54.99)
)

val genres = listOf(R.string.genre_all, R.string.genre_action, R.string.genre_rpg, R.string.genre_racing, R.string.genre_casual)

data class NavItem(@param:StringRes val label: Int, val emoji: String)

val navItems = listOf(
    NavItem(R.string.nav_home, "🏠"), NavItem(R.string.nav_search, "🔎"),
    NavItem(R.string.nav_cart, "🛒"), NavItem(R.string.nav_profile, "👤")
)

// Degradado de cada juego, tomado del tema
@Composable
fun gameBrush(id: Int): Brush {
    val cs = MaterialTheme.colorScheme
    val pairs = listOf(cs.primary to cs.tertiary, cs.secondary to cs.primary, cs.tertiary to cs.secondary)
    val p = pairs[id % 3]
    return Brush.linearGradient(listOf(p.first, p.second))
}

// ---------- LOGO ----------
@Composable
fun PixelLogo(logoSize: Dp = 40.dp) {
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.secondary
    val boltColor = MaterialTheme.colorScheme.background
    Canvas(Modifier.size(logoSize)) {
        val w = size.width
        val h = size.height
        val hex = Path().apply {
            moveTo(w * 0.5f, 0f); lineTo(w, h * 0.25f); lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h); lineTo(0f, h * 0.75f); lineTo(0f, h * 0.25f); close()
        }
        drawPath(hex, Brush.linearGradient(listOf(c1, c2)))
        val bolt = Path().apply {
            moveTo(w * 0.55f, h * 0.18f); lineTo(w * 0.30f, h * 0.55f); lineTo(w * 0.47f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.82f); lineTo(w * 0.70f, h * 0.43f); lineTo(w * 0.53f, h * 0.43f); close()
        }
        drawPath(bolt, boltColor)
    }
}

// ---------- ACTIVITY ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PixelForgeTheme { PixelForgeApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelForgeApp() {
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).cartDao() }
    val scope = rememberCoroutineScope()
    val cartItems by dao.observeAll().collectAsState(initial = emptyList())
    val cart = cartItems.mapNotNull { item -> games.find { it.id == item.gameId }?.let { item to it } }
    val add: (Game) -> Unit = { g -> scope.launch { dao.insert(CartItem(gameId = g.id)) } }
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            NavigationBar(containerColor = cs.surface) {
                navItems.forEachIndexed { i, item ->
                    val label = stringResource(item.label)
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i }, label = { Text(label) },
                        icon = {
                            if (i == 2 && cart.isNotEmpty()) {
                                BadgedBox(badge = { Badge(containerColor = cs.tertiary) { Text("${cart.size}") } }) {
                                    Text(item.emoji, fontSize = 22.sp)
                                }
                            } else Text(item.emoji, fontSize = 22.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = cs.secondary, indicatorColor = cs.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(add)
                1 -> SearchScreen(add)
                2 -> CartScreen(cart, onRemove = { scope.launch { dao.delete(it.id) } }, onClear = { scope.launch { dao.clear() } })
                else -> ProfileScreen()
            }
        }
    }
}

// ---------- PANTALLA 1: INICIO ----------
@Composable
fun HomeScreen(onAdd: (Game) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var genre by remember { mutableIntStateOf(R.string.genre_all) }
    val filtered = if (genre == R.string.genre_all) games else games.filter { it.genre == genre }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelLogo(44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.store_name), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.store_tagline), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(cs.primary, cs.tertiary))).padding(20.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text(stringResource(R.string.promo_label), color = cs.secondary, style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.promo_title), color = cs.onPrimary, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.promo_subtitle), color = cs.onPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres) { g ->
                    FilterChip(
                        selected = genre == g, onClick = { genre = g }, label = { Text(stringResource(g)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary, selectedLabelColor = cs.onPrimary
                        )
                    )
                }
            }
        }
        item { Text(stringResource(R.string.featured), style = MaterialTheme.typography.titleMedium) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered) { GameCard(it, onAdd) }
            }
        }
        item { Text(stringResource(R.string.catalog), style = MaterialTheme.typography.titleMedium) }
        items(filtered) { GameRow(it, onAdd) }
    }
}

@Composable
fun GameCard(game: Game, onAdd: (Game) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.width(160.dp).clip(RoundedCornerShape(16.dp)).background(cs.surface)) {
        Box(Modifier.fillMaxWidth().height(100.dp).background(gameBrush(game.id))) {
            Text("🎮", Modifier.align(Alignment.Center), fontSize = 38.sp)
        }
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(game.title), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                stringResource(R.string.genre_platform, stringResource(game.genre), stringResource(game.platform)),
                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.price_format, game.price), color = cs.secondary, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onAdd(game) }, Modifier.size(32.dp).background(cs.primary, RoundedCornerShape(10.dp))) {
                    Text("+", color = cs.onPrimary, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun GameRow(game: Game, onAdd: (Game) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(gameBrush(game.id))) {
            Text("🎮", Modifier.align(Alignment.Center), fontSize = 24.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(game.title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.genre_platform, stringResource(game.genre), stringResource(game.platform)),
                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(stringResource(R.string.price_format, game.price), color = cs.secondary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { onAdd(game) }) { Text("🛒", fontSize = 22.sp) }
    }
}

// ---------- PANTALLA 2: BUSCAR ----------
@Composable
fun SearchScreen(onAdd: (Game) -> Unit) {
    var query by remember { mutableStateOf("") }
    val titles = games.associateWith { stringResource(it.title) }
    val results = games.filter { titles[it].orEmpty().contains(query, ignoreCase = true) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) }, leadingIcon = { Text("🔎") },
            shape = RoundedCornerShape(16.dp), singleLine = true
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(results) { GameRow(it, onAdd) } }
    }
}

// ---------- CARRITO ----------
@Composable
fun CartScreen(cart: List<Pair<CartItem, Game>>, onRemove: (CartItem) -> Unit, onClear: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.cart_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (cart.isEmpty()) {
            Text(stringResource(R.string.cart_empty), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cart) { (item, g) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(g.title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.price_format, g.price), color = cs.secondary)
                        IconButton(onClick = { onRemove(item) }) { Text("🗑️", fontSize = 20.sp) }
                    }
                }
            }
            Text(stringResource(R.string.total_format, cart.sumOf { it.second.price }), style = MaterialTheme.typography.titleMedium, color = cs.secondary)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClear, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) { Text(stringResource(R.string.checkout), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

// ---------- PERFIL ----------
@Composable
fun ProfileScreen() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        PixelLogo(90.dp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.profile_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.profile_level), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}