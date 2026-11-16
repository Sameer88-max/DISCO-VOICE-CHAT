
package com.disco.voicechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlin.random.Random

private val AppBg = Color(0xFF090B1B)
private val CardBg = Color(0xFF171B38)
private val Purple = Color(0xFF985CFF)
private val Cyan = Color(0xFF38D9FF)
private val Gold = Color(0xFFFFC857)
private val Pink = Color(0xFFFF4FA3)
private val AppWhite = Color(0xFFF5F5FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiscoVoiceChat()
        }
    }
}

@Composable
fun DiscoVoiceChat() {

    var selectedTab by remember { mutableStateOf("Profile") }
    var currentPage by remember { mutableStateOf("main") }

    var selectedCategory by remember { mutableStateOf("Frames") }
    var selectedFilter by remember { mutableStateOf("Featured") }

    var coins by remember { mutableIntStateOf(1000) }
    var diamonds by remember { mutableIntStateOf(0) }

    var charmLevel by remember { mutableIntStateOf(1) }
    var wealthLevel by remember { mutableIntStateOf(1) }

    var equippedFrame by remember {
        mutableStateOf("Basic Blue Frame")
    }

    var equippedTitle by remember {
        mutableStateOf("DISCO Member")
    }

    var ownedItems by remember {
        mutableStateOf(
            setOf(
                "Basic Blue Frame",
                "DISCO Member",
                "Welcome Gift"
            )
        )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            secondary = Cyan,
            background = AppBg,
            surface = CardBg
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBg
        ) {

            if (currentPage == "store") {

                StoreScreen(
                    category = selectedCategory,
                    filter = selectedFilter,
                    ownedItems = ownedItems,
                    equippedFrame = equippedFrame,
                    equippedTitle = equippedTitle,
                    coins = coins,
                    onCategory = { selectedCategory = it },
                    onFilter = { selectedFilter = it },
                    onBack = { currentPage = "main" },
                    onBuy = { item ->
                        if (item !in ownedItems) {
                            ownedItems = ownedItems + item
                        }
                    },
                    onEquip = { item ->
                        if (item in ownedItems) {
                            if (selectedCategory == "Frames") {
                                equippedFrame = item
                            }
                            if (selectedCategory == "Titles") {
                                equippedTitle = item
                            }
                        }
                    }
                )

            } else {

                Scaffold(
                    containerColor = AppBg,
                    bottomBar = {

                        NavigationBar(
                            containerColor = CardBg
                        ) {

                            val tabs = listOf(
                                "Home",
                                "Friends",
                                "Rooms",
                                "Messages",
                                "Profile"
                            )

                            tabs.forEach { tab ->

                                val icon = when (tab) {
                                    "Home" -> Icons.Default.Home
                                    "Friends" -> Icons.Default.People
                                    "Rooms" -> Icons.Default.Mic
                                    "Messages" -> Icons.Default.Chat
                                    else -> Icons.Default.Person
                                }

                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        selectedTab = tab
                                    },
                                    icon = {
                                        Icon(icon, contentDescription = tab)
                                    },
                                    label = {
                                        Text(tab)
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Cyan,
                                        selectedTextColor = Cyan,
                                        indicatorColor = Purple.copy(alpha = 0.2f),
                                        unselectedIconColor = AppWhite.copy(alpha = 0.6f),
                                        unselectedTextColor = AppWhite.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->

                    when (selectedTab) {

                        "Profile" -> ProfileScreen(
                            padding = padding,
                            frame = equippedFrame,
                            title = equippedTitle,
                            coins = coins,
                            diamonds = diamonds,
                            charmLevel = charmLevel,
                            wealthLevel = wealthLevel,
                            openStore = {
                                currentPage = "store"
                            },
                            openPage = {
                                currentPage = it
                            }
                        )

                        "Home" -> SimpleScreen(
                            "DISCO VOICE CHAT",
                            "Discover rooms, creators and activities.",
                            padding
                        )

                        "Friends" -> FriendsScreen(padding)

                        "Rooms" -> SimpleScreen(
                            "VOICE ROOMS",
                            "Discover public and official voice rooms.",
                            padding
                        )

                        "Messages" -> SimpleScreen(
                            "MESSAGES",
                            "Your conversations and message requests.",
                            padding
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- PROFILE SCREEN ---------------- */

@Composable
fun ProfileScreen(
    padding: PaddingValues,
    frame: String,
    title: String,
    coins: Int,
    diamonds: Int,
    charmLevel: Int,
    wealthLevel: Int,
    openStore: () -> Unit,
    openPage: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MY PROFILE",
                color = AppWhite,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )

            IconButton(onClick = {
                openPage("Settings")
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Cyan
                )
            }
        }

        Spacer(Modifier.height(15.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF22234F)
            )
        ) {

            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF252052),
                                Color(0xFF102E4C),
                                Color(0xFF42164D)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    AvatarPreview(frame, 115)

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "DISCO USER",
                        color = AppWhite,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "DISCO ID: 10000001",
                        color = Cyan,
                        fontSize = 13.sp
                    )

                    Text(
                        title,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        ProfileStat("Charm", "Lv.$charmLevel")
                        ProfileStat("Wealth", "Lv.$wealthLevel")
                        ProfileStat("VIP", "Inactive")
                    }

                    Spacer(Modifier.height(15.dp))

                    OutlinedButton(
                        onClick = {
                            openPage("Edit Profile")
                        }
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            BalanceCard(
                "Gold Coins",
                coins.toString(),
                Gold,
                Modifier.weight(1f)
            )

            BalanceCard(
                "Diamonds",
                diamonds.toString(),
                Cyan,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionTitle("PROFILE & SOCIAL")

        MenuGrid(
            listOf(
                "Edit Profile",
                "My ID & QR",
                "Friends",
                "Following",
                "Followers",
                "Achievements",
                "Agency",
                "My Collection"
            ),
            openPage
        )

        Spacer(Modifier.height(17.dp))

        SectionTitle("MEMBERSHIP & LEVELS")

        MenuGrid(
            listOf(
                "VIP Membership",
                "SVIP Membership",
                "Charm Level",
                "Wealth Level"
            ),
            openPage
        )

        Spacer(Modifier.height(17.dp))

        SectionTitle("COSMETICS & STORE")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    openStore()
                },
            colors = CardDefaults.cardColors(
                containerColor = CardBg
            ),
            shape = RoundedCornerShape(18.dp)
        ) {

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Storefront,
                    null,
                    tint = Gold,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "OPEN DISCO STORE",
                        color = AppWhite,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Frames • Entry Effects • Titles • Badges",
                        color = AppWhite.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Cyan
                )
            }
        }

        Spacer(Modifier.height(17.dp))

        SectionTitle("WALLET & ACTIVITIES")

        MenuGrid(
            listOf(
                "Wallet",
                "Transaction History",
                "Daily Tasks",
                "Gift Collection",
                "Settings",
                "Privacy & Security",
                "Help & Support"
            ),
            openPage
        )

        Spacer(Modifier.height(20.dp))
    }
}

/* ---------------- STORE SCREEN ---------------- */

@Composable
fun StoreScreen(
    category: String,
    filter: String,
    ownedItems: Set<String>,
    equippedFrame: String,
    equippedTitle: String,
    coins: Int,
    onCategory: (String) -> Unit,
    onFilter: (String) -> Unit,
    onBack: () -> Unit,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit
) {

    val categories = listOf(
        "Frames",
        "Entry Effects",
        "Backgrounds",
        "Name Styles",
        "Chat Bubbles",
        "Badges",
        "Titles",
        "Gift Collection"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    tint = AppWhite
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "DISCO STORE",
                    color = AppWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "Cosmetics & Collection",
                    color = AppWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.Diamond,
                null,
                tint = Gold
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Your coins", color = AppWhite.copy(alpha = 0.7f))
            Text("$coins", color = Gold, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(categories) { item ->

                FilterChip(
                    selected = category == item,
                    onClick = {
                        onCategory(item)
                    },
                    label = {
                        Text(item)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple,
                        selectedLabelColor = AppWhite,
                        labelColor = AppWhite
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {

            listOf(
                "Featured",
                "New",
                "Popular",
                "My Collection"
            ).forEach { item ->

                FilterChip(
                    selected = filter == item,
                    onClick = {
                        onFilter(item)
                    },
                    label = {
                        Text(item, fontSize = 10.sp)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            val allItems = getCosmetics(category)

            val visibleItems = if (filter == "My Collection") {
                allItems.filter { it in ownedItems }
            } else {
                allItems
            }

            items(visibleItems) { item ->

                val isOwned = item in ownedItems

                val isEquipped =
                    item == equippedFrame ||
                    item == equippedTitle

                StoreItemCard(
                    item = item,
                    category = category,
                    owned = isOwned,
                    equipped = isEquipped,
                    onBuy = {
                        onBuy(item)
                    },
                    onEquip = {
                        onEquip(item)
                    }
                )
            }
        }
    }
}

/* ---------------- COSMETIC CATALOG ---------------- */

fun getCosmetics(category: String): List<String> {

    return when (category) {

        "Frames" -> listOf(
            "Basic Blue Frame",
            "Neon Pulse Frame",
            "Purple Galaxy Frame",
            "VIP Royal Frame",
            "SVIP Crown Frame",
            "Legendary Dragon Frame",
            "Crystal Ring Frame",
            "Electric Storm Frame",
            "Diamond Halo Frame",
            "Event Champion Frame",
            "Ice Blue Frame",
            "Fire Dragon Frame",
            "Golden Royal Frame",
            "Cyber Neon Frame",
            "Galaxy Star Frame"
        )

        "Entry Effects" -> listOf(
            "Blue Portal Entry",
            "Neon Burst Entry",
            "Royal Crown Arrival",
            "Galaxy Warp Entry",
            "Dragon Flame Arrival",
            "Diamond Shower Entry",
            "VIP Spotlight",
            "SVIP Cosmic Gate",
            "Electric Storm Entry",
            "Star Explosion Entry",
            "Crystal Portal Entry",
            "Golden Light Arrival"
        )

        "Backgrounds" -> listOf(
            "Midnight Galaxy",
            "Neon City",
            "Royal Purple Hall",
            "Ocean Aurora",
            "Cyber Blue",
            "Diamond Night",
            "Starry Universe",
            "Blue Lightning",
            "Pink Galaxy",
            "Royal Black"
        )

        "Name Styles" -> listOf(
            "Neon Name",
            "Gold Royal Name",
            "Ice Glow Name",
            "Rainbow Aura Name",
            "VIP Shine Name",
            "Dragon Name",
            "Galaxy Name",
            "Crystal Name"
        )

        "Chat Bubbles" -> listOf(
            "Cyber Bubble",
            "Royal Bubble",
            "Crystal Bubble",
            "Neon Wave Bubble",
            "Galaxy Bubble",
            "VIP Bubble",
            "SVIP Bubble"
        )

        "Badges" -> listOf(
            "DISCO Member",
            "Verified Creator",
            "VIP Member",
            "SVIP Member",
            "Event Champion",
            "Agency Head",
            "Top Contributor",
            "Official Staff",
            "Voice Legend",
            "Rising Star"
        )

        "Titles" -> listOf(
            "DISCO Member",
            "Rising Star",
            "Voice Legend",
            "VIP Star",
            "SVIP Elite",
            "Event Champion",
            "Official Host",
            "Agency Head",
            "Music King",
            "Music Queen",
            "Legendary Singer",
            "Super Creator"
        )

        else -> listOf(
            "Welcome Gift",
            "Blue Rose",
            "Crystal Heart",
            "Star Gift",
            "Royal Crown",
            "Galaxy Gift",
            "Diamond Flower",
            "Golden Trophy"
        )
    }
}

/* ---------------- STORE ITEM CARD ---------------- */

@Composable
fun StoreItemCard(
    item: String,
    category: String,
    owned: Boolean,
    equipped: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        )
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CosmeticPreview(
                category,
                item,
                62
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    item,
                    color = AppWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Text(
                    if (owned) "Owned • Collection"
                    else "Available cosmetic",
                    color = AppWhite.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )

                Text(
                    when {
                        item.contains("SVIP") -> "SVIP Collection"
                        item.contains("VIP") -> "VIP Collection"
                        item.contains("Legendary") -> "Legendary Collection"
                        else -> "DISCO Collection"
                    },
                    color = Cyan,
                    fontSize = 10.sp
                )
            }

            if (owned) {

                Button(
                    onClick = onEquip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (equipped) {
                            Color(0xFF28645C)
                        } else {
                            Purple
                        }
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp
                    )
                ) {
                    Text(
                        if (equipped) "Equipped" else "Equip",
                        fontSize = 10.sp
                    )
                }

            } else {

                OutlinedButton(
                    onClick = onBuy,
                    contentPadding = PaddingValues(
                        horizontal = 10.dp
                    )
                ) {
                    Text("Get", fontSize = 11.sp)
                }
            }
        }
    }
}

/* ---------------- COSMETIC PREVIEW ---------------- */

@Composable
fun CosmeticPreview(
    category: String,
    name: String,
    size: Int
) {

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF252C59),
                        Color(0xFF49215F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        when (category) {

            "Frames" -> AvatarPreview(name, size)

            "Entry Effects" -> {

                val transition = rememberInfiniteTransition(
                    label = "entryAnimation"
                )

                val alpha by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "entryAlpha"
                )

                Icon(
                    Icons.Default.Bolt,
                    null,
                    tint = Cyan.copy(alpha = alpha),
                    modifier = Modifier.size((size * 0.6f).dp)
                )
            }

            "Backgrounds" -> {

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Purple, Cyan, Pink)
                            )
                        )
                )
            }

            "Badges" -> Icon(
                Icons.Default.Verified,
                null,
                tint = Gold,
                modifier = Modifier.size((size * 0.55f).dp)
            )

            "Titles" -> Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = Gold,
                modifier = Modifier.size((size * 0.55f).dp)
            )

            "Chat Bubbles" -> Icon(
                Icons.Default.ChatBubble,
                null,
                tint = Pink,
                modifier = Modifier.size((size * 0.55f).dp)
            )

            else -> Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = Cyan,
                modifier = Modifier.size((size * 0.55f).dp)
            )
        }
    }
}

/* ---------------- AVATAR FRAME PREVIEW ---------------- */

@Composable
fun AvatarPreview(
    style: String,
    size: Int
) {

    val frameColors = when {

        style.contains("Royal") ||
        style.contains("Crown") ->
            listOf(Gold, Color(0xFFFF783C))

        style.contains("Galaxy") ||
        style.contains("Dragon") ->
            listOf(Purple, Pink)

        style.contains("Blue") ||
        style.contains("Neon") ->
            listOf(Cyan, Purple)

        else ->
            listOf(Pink, Gold)
    }

    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = (size / 14f).dp,
                    brush = Brush.sweepGradient(frameColors),
                    shape = CircleShape
                )
                .padding((size / 9f).dp)
                .background(
                    Color(0xFF10152D),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                Icons.Default.Person,
                null,
                tint = AppWhite,
                modifier = Modifier.size((size * 0.55f).dp)
            )
        }

        if (
            style.contains("Royal") ||
            style.contains("Crown")
        ) {

            Icon(
                Icons.Default.WorkspacePremium,
                null,
                tint = Gold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size((size * 0.3f).dp)
            )
        }
    }
}

/* ---------------- VIP / SVIP ---------------- */

@Composable
fun MembershipScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, tint = AppWhite)
        }

        Icon(
            Icons.Default.WorkspacePremium,
            null,
            tint = Gold,
            modifier = Modifier.size(65.dp)
        )

        Text(
            title,
            color = Gold,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            description,
            color = AppWhite.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(20.dp))

        for (level in 1..10) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBg
                )
            ) {

                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "$title $level",
                        color = AppWhite,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Level $level",
                        color = Cyan
                    )
                }
            }
        }

        Text(
            "Membership activation will be connected to the secure Owner Panel.",
            color = AppWhite.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

/* ---------------- WALLET ---------------- */

@Composable
fun WalletScreen(
    onBack: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        BackHeader("WALLET", onBack)

        BalanceCard(
            "Gold Coins",
            "1,000",
            Gold,
            Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        BalanceCard(
            "Diamonds",
            "0",
            Cyan,
            Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        SectionTitle("WALLET OPTIONS")

        MenuGrid(
            listOf(
                "Coin Balance",
                "Diamond Balance",
                "Gift History",
                "Transaction History",
                "Rewards",
                "Wallet Help"
            )
        ) {}
    }
}

/* ---------------- LEVEL SCREEN ---------------- */

@Composable
fun LevelScreen(
    title: String,
    level: Int,
    onBack: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        BackHeader(title, onBack)

        Spacer(Modifier.height(20.dp))

        Text(
            "Current Level: $level",
            color = Gold,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        LinearProgressIndicator(
            progress = { 0.35f },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape),
            color = Cyan,
            trackColor = CardBg
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Progress to next level",
            color = AppWhite
        )

        Text(
            "Level progress and rewards will be calculated by the server.",
            color = AppWhite.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

/* ---------------- FRIENDS ---------------- */

@Composable
fun FriendsScreen(padding: PaddingValues) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp)
    ) {

        Text(
            "FRIENDS",
            color = AppWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search by name or DISCO ID")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null)
            },
            singleLine = true
        )

        Spacer(Modifier.height(15.dp))

        MenuGrid(
            listOf(
                "Friends",
                "Following",
                "Followers",
                "Friend Requests",
                "Blocked Users"
            )
        ) {}
    }
}

/* ---------------- GENERAL PAGE ---------------- */

@Composable
fun GeneralPage(
    page: String,
    onBack: () -> Unit
) {

    val options = when (page) {

        "Settings" -> listOf(
            "Account Settings",
            "Notifications",
            "Sound Settings",
            "Language",
            "Privacy",
            "Security",
            "Blocked Users",
            "Logout"
        )

        "Agency" -> listOf(
            "Create Agency",
            "Agency Information",
            "Agency Head",
            "Managers",
            "Agency Hosts",
            "Member Requests",
            "Agency Rules"
        )

        "Edit Profile" -> listOf(
            "Change Profile Photo",
            "Change Username",
            "Edit Bio",
            "Country",
            "Online Status",
            "Profile Background"
        )

        "Achievements" -> listOf(
            "Daily Achievements",
            "Voice Achievements",
            "Social Achievements",
            "Event Achievements"
        )

        "Daily Tasks" -> listOf(
            "Login Reward",
            "Visit Voice Room",
            "Follow Creator",
            "Complete Profile",
            "Collect Achievement"
        )

        "My ID & QR" -> listOf(
            "DISCO ID: 10000001",
            "Copy ID",
            "Share Profile",
            "Show QR Code"
        )

        else -> listOf(
            "Overview",
            "Details",
            "History",
            "My Collection"
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        BackHeader(page.uppercase(), onBack)

        Spacer(Modifier.height(20.dp))

        options.forEach { item ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable {},
                colors = CardDefaults.cardColors(
                    containerColor = CardBg
                )
            ) {

                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        item,
                        color = AppWhite,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Cyan
                    )
                }
            }
        }
    }
}

/* ---------------- REUSABLE COMPONENTS ---------------- */

@Composable
fun ProfileStat(label: String, value: String) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            value,
            color = AppWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Text(
            label,
            color = AppWhite.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun BalanceCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        ),
        shape = RoundedCornerShape(16.dp)
    ) {

        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                if (label.contains("Diamond")) {
                    Icons.Default.Diamond
                } else {
                    Icons.Default.MonetizationOn
                },
                null,
                tint = color
            )

            Spacer(Modifier.width(9.dp))

            Column {

                Text(
                    label,
                    color = AppWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Text(
                    value,
                    color = AppWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {

    Text(
        title,
        color = Cyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun MenuGrid(
    items: List<String>,
    onClick: (String) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items.chunked(2).forEach { pair ->

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                pair.forEach { item ->

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onClick(item)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = CardBg
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ) {

                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                item,
                                color = AppWhite,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = Cyan,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack,
                null,
                tint = AppWhite
            )
        }

        Text(
            title,
            color = AppWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
fun SimpleScreen(
    title: String,
    description: String,
    padding: PaddingValues
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            Icons.Default.GraphicEq,
            null,
            tint = Purple,
            modifier = Modifier.size(65.dp)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            title,
            color = AppWhite,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            description,
            color = AppWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
