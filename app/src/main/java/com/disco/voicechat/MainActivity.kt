
package com.disco.voicechat

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val BG = Color(0xFF090B1B)
private val CARD = Color(0xFF171B38)
private val PURPLE = Color(0xFF985CFF)
private val CYAN = Color(0xFF38D9FF)
private val GOLD = Color(0xFFFFC857)
private val PINK = Color(0xFFFF4FA3)
private val WHITE = Color(0xFFF5F5FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiscoApp()
        }
    }
}

// CLOUDINARY UPLOAD

private suspend fun uploadProfileToCloudinary(
    context: Context,
    imageUri: Uri
): String = withContext(Dispatchers.IO) {

    val cloudName = "x0bgvsgy"
    val uploadPreset = "disco_voice_chat"
    val boundary = "----Disco${UUID.randomUUID()}"

    val connection = URL(
        "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        connection.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=$boundary"
        )

        connection.outputStream.use { output ->

            fun writeField(name: String, value: String) {
                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    "Content-Disposition: form-data; name=\"$name\"\r\n\r\n"
                        .toByteArray()
                )
                output.write(value.toByteArray())
                output.write("\r\n".toByteArray())
            }

            writeField("upload_preset", uploadPreset)

            output.write("--$boundary\r\n".toByteArray())
            output.write(
                "Content-Disposition: form-data; name=\"file\"; filename=\"profile_${UUID.randomUUID()}.jpg\"\r\n"
                    .toByteArray()
            )

            val mimeType = context.contentResolver.getType(imageUri)
                ?: "image/jpeg"

            output.write("Content-Type: $mimeType\r\n\r\n".toByteArray())

            val stream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot read selected image")

            stream.use {
                it.copyTo(output)
            }

            output.write("\r\n--$boundary--\r\n".toByteArray())
        }

        val responseCode = connection.responseCode

        val response = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use {
                it.readText()
            }
        } else {
            connection.errorStream?.bufferedReader()?.use {
                it.readText()
            } ?: "Cloudinary error: $responseCode"
        }

        if (responseCode !in 200..299) {
            throw Exception(response)
        }

        JSONObject(response).getString("secure_url")

    } finally {
        connection.disconnect()
    }
}

// MAIN APP

@Composable
fun DiscoApp() {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf(false) }
    var profileLoaded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Welcome") }
    var tab by remember { mutableStateOf("Home") }

    var name by remember { mutableStateOf("DISCO USER") }
    var email by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("Welcome to DISCO VOICE CHAT") }

    var profileImageUrl by remember { mutableStateOf("") }
    var uploadingImage by remember { mutableStateOf(false) }

    var coins by remember { mutableIntStateOf(1000) }
    var diamonds by remember { mutableIntStateOf(0) }
    var charm by remember { mutableIntStateOf(1) }
    var wealth by remember { mutableIntStateOf(1) }
    var vip by remember { mutableIntStateOf(0) }
    var svip by remember { mutableIntStateOf(0) }

    var frame by remember { mutableStateOf("Basic Blue Frame") }
    var title by remember { mutableStateOf("DISCO Member") }
    var entry by remember { mutableStateOf("Default Entry") }

    var owned by remember {
        mutableStateOf(
            setOf(
                "Basic Blue Frame",
                "DISCO Member",
                "Default Entry"
            )
        )
    }

    var selectedStore by remember { mutableStateOf("Frames") }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // GALLERY PICKER

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            scope.launch {

                uploadingImage = true

                try {

                    val uploadedUrl = uploadProfileToCloudinary(
                        context,
                        uri
                    )

                    val uid = auth.currentUser?.uid
                        ?: throw Exception("User not logged in")

                    db.collection("users")
                        .document(uid)
                        .set(
                            mapOf("photoUrl" to uploadedUrl),
                            SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            profileImageUrl = uploadedUrl
                            message = "Profile picture updated successfully"
                        }
                        .addOnFailureListener {
                            message = "Firebase save failed: ${it.localizedMessage}"
                        }

                } catch (e: Exception) {

                    message = "Image upload failed: ${e.localizedMessage}"

                } finally {

                    uploadingImage = false
                }
            }
        }
    }

    // LOAD FIREBASE PROFILE

    fun loadProfile(
        uid: String,
        onComplete: (Boolean, String) -> Unit
    ) {

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    name = doc.getString("displayName") ?: "DISCO USER"
                    email = doc.getString("email")
                        ?: auth.currentUser?.email.orEmpty()

                    bio = doc.getString("bio")
                        ?: "Welcome to DISCO VOICE CHAT"

                    profileImageUrl = doc.getString("photoUrl") ?: ""

                    coins = (doc.getLong("coins") ?: 1000L).toInt()
                    diamonds = (doc.getLong("diamonds") ?: 0L).toInt()
                    charm = (doc.getLong("charmLevel") ?: 1L).toInt()
                    wealth = (doc.getLong("wealthLevel") ?: 1L).toInt()
                    vip = (doc.getLong("vipLevel") ?: 0L).toInt()
                    svip = (doc.getLong("svipLevel") ?: 0L).toInt()

                    frame = doc.getString("frame") ?: "Basic Blue Frame"
                    title = doc.getString("title") ?: "DISCO Member"
                    entry = doc.getString("entry") ?: "Default Entry"

                    val savedOwned = doc.get("owned") as? List<*>

                    owned = savedOwned
                        ?.filterIsInstance<String>()
                        ?.toSet()
                        ?: setOf(
                            "Basic Blue Frame",
                            "DISCO Member",
                            "Default Entry"
                        )

                    onComplete(true, "")

                } else {

                    val user = auth.currentUser

                    val defaults = hashMapOf<String, Any>(
                        "uid" to uid,
                        "displayName" to (
                            user?.displayName
                                ?: user?.email?.substringBefore("@")
                                ?: "DISCO USER"
                            ),
                        "email" to (user?.email ?: ""),
                        "bio" to "Welcome to DISCO VOICE CHAT",
                        "photoUrl" to "",
                        "coins" to 1000L,
                        "diamonds" to 0L,
                        "charmLevel" to 1L,
                        "wealthLevel" to 1L,
                        "vipLevel" to 0L,
                        "svipLevel" to 0L,
                        "frame" to "Basic Blue Frame",
                        "title" to "DISCO Member",
                        "entry" to "Default Entry",
                        "owned" to listOf(
                            "Basic Blue Frame",
                            "DISCO Member",
                            "Default Entry"
                        )
                    )

                    db.collection("users")
                        .document(uid)
                        .set(defaults)
                        .addOnSuccessListener {

                            name = defaults["displayName"] as String
                            email = defaults["email"] as String
                            bio = defaults["bio"] as String

                            profileImageUrl = ""
                            coins = 1000
                            diamonds = 0
                            charm = 1
                            wealth = 1
                            vip = 0
                            svip = 0

                            frame = "Basic Blue Frame"
                            title = "DISCO Member"
                            entry = "Default Entry"

                            owned = setOf(
                                "Basic Blue Frame",
                                "DISCO Member",
                                "Default Entry"
                            )

                            onComplete(true, "")
                        }
                        .addOnFailureListener {
                            onComplete(
                                false,
                                it.localizedMessage ?: "Profile creation failed"
                            )
                        }
                }
            }
            .addOnFailureListener {
                onComplete(
                    false,
                    it.localizedMessage ?: "Could not load profile"
                )
            }
    }

    // SAVE PROFILE

    fun saveProfile() {

        val uid = auth.currentUser?.uid ?: return

        val data = hashMapOf<String, Any>(
            "displayName" to name,
            "email" to email,
            "bio" to bio,
            "photoUrl" to profileImageUrl,
            "coins" to coins,
            "diamonds" to diamonds,
            "charmLevel" to charm,
            "wealthLevel" to wealth,
            "vipLevel" to vip,
            "svipLevel" to svip,
            "frame" to frame,
            "title" to title,
            "entry" to entry,
            "owned" to owned.toList()
        )

        db.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
    }

    // RESTORE LOGIN SESSION

    LaunchedEffect(Unit) {

        val currentUser = auth.currentUser

        if (currentUser != null) {

            loadProfile(currentUser.uid) { success, error ->

                if (success) {

                    profileLoaded = true
                    loggedIn = true
                    page = "Main"

                } else {

                    message = error
                    auth.signOut()
                }
            }
        }
    }

    // AUTO SAVE

    LaunchedEffect(
        profileLoaded,
        name,
        email,
        bio,
        profileImageUrl,
        coins,
        diamonds,
        charm,
        wealth,
        vip,
        svip,
        frame,
        title,
        entry,
        owned
    ) {

        if (profileLoaded && auth.currentUser != null) {
            saveProfile()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PURPLE,
            secondary = CYAN,
            background = BG,
            surface = CARD
        )
    ) {

        Surface(
            Modifier.fillMaxSize(),
            color = BG
        ) {

            if (!loggedIn) {

                AuthScreen(

                    onLogin = { e, p, result ->

                        auth.signInWithEmailAndPassword(e.trim(), p)
                            .addOnSuccessListener {

                                loadProfile(auth.currentUser!!.uid) { ok, error ->

                                    if (ok) {

                                        email = e.trim()
                                        profileLoaded = true
                                        loggedIn = true
                                        page = "Main"

                                        result(true, "Login successful")

                                    } else {

                                        result(false, error)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                result(
                                    false,
                                    it.localizedMessage ?: "Login failed"
                                )
                            }
                    },

                    onRegister = { n, e, p, result ->

                        auth.createUserWithEmailAndPassword(e.trim(), p)
                            .addOnSuccessListener {

                                val uid = auth.currentUser!!.uid

                                val data = hashMapOf<String, Any>(
                                    "uid" to uid,
                                    "displayName" to n.trim(),
                                    "email" to e.trim(),
                                    "bio" to "Welcome to DISCO VOICE CHAT",
                                    "photoUrl" to "",
                                    "coins" to 1000L,
                                    "diamonds" to 0L,
                                    "charmLevel" to 1L,
                                    "wealthLevel" to 1L,
                                    "vipLevel" to 0L,
                                    "svipLevel" to 0L,
                                    "frame" to "Basic Blue Frame",
                                    "title" to "DISCO Member",
                                    "entry" to "Default Entry",
                                    "owned" to listOf(
                                        "Basic Blue Frame",
                                        "DISCO Member",
                                        "Default Entry"
                                    )
                                )

                                db.collection("users")
                                    .document(uid)
                                    .set(data)
                                    .addOnSuccessListener {

                                        name = n.trim()
                                        email = e.trim()
                                        bio = "Welcome to DISCO VOICE CHAT"
                                        profileImageUrl = ""

                                        coins = 1000
                                        diamonds = 0
                                        charm = 1
                                        wealth = 1
                                        vip = 0
                                        svip = 0

                                        frame = "Basic Blue Frame"
                                        title = "DISCO Member"
                                        entry = "Default Entry"

                                        owned = setOf(
                                            "Basic Blue Frame",
                                            "DISCO Member",
                                            "Default Entry"
                                        )

                                        profileLoaded = true
                                        loggedIn = true
                                        page = "Main"

                                        result(
                                            true,
                                            "Account created successfully"
                                        )
                                    }
                                    .addOnFailureListener {
                                        result(
                                            false,
                                            it.localizedMessage
                                                ?: "Profile creation failed"
                                        )
                                    }
                            }
                            .addOnFailureListener {
                                result(
                                    false,
                                    it.localizedMessage ?: "Registration failed"
                                )
                            }
                    },

                    onForgot = { e, result ->

                        auth.sendPasswordResetEmail(e.trim())
                            .addOnSuccessListener {
                                result(
                                    true,
                                    "Password reset link sent to your email"
                                )
                            }
                            .addOnFailureListener {
                                result(
                                    false,
                                    it.localizedMessage ?: "Password reset failed"
                                )
                            }
                    }
                )

            } else {

                when (page) {

                    "Main" -> MainDashboard(
                        tab = tab,
                        name = name,
                        frame = frame,
                        title = title,
                        coins = coins,
                        diamonds = diamonds,
                        charm = charm,
                        wealth = wealth,
                        vip = vip,
                        svip = svip,
                        photoUrl = profileImageUrl,
                        uploadingImage = uploadingImage,
                        onPickPhoto = {
                            imagePicker.launch("image/*")
                        },
                        onTab = { tab = it },
                        onPage = { page = it }
                    )

                    "Store" -> StorePage(
                        selectedStore,
                        owned,
                        coins,
                        frame,
                        title,
                        entry,
                        onBack = { page = "Main" },
                        onCategory = { selectedStore = it },
                        onBuy = { item, price ->

                            if (coins >= price && item !in owned) {

                                coins -= price
                                owned = owned + item
                                message = "$item purchased successfully"

                            } else if (item in owned) {

                                message = "Already owned"

                            } else {

                                message = "Not enough coins"
                            }
                        },
                        onEquip = { item ->

                            if (item in owned) {

                                when (selectedStore) {
                                    "Frames" -> frame = item
                                    "Titles" -> title = item
                                    "Entry Effects" -> entry = item
                                }

                                message = "$item equipped"
                            }
                        }
                    )

                    "Wallet" -> WalletPage(
                        coins,
                        diamonds
                    ) {
                        page = "Main"
                    }

                    "VIP Membership", "SVIP Membership" -> MembershipPage(
                        page,
                        if (page == "VIP Membership") vip else svip,
                        onActivate = {
                            if (page == "VIP Membership") {
                                vip = it
                            } else {
                                svip = it
                            }

                            message =
                                "Membership selection saved locally. Owner verification is required for activation."
                        },
                        onBack = { page = "Main" }
                    )

                    "Charm Level" -> LevelPage(
                        "Charm Level",
                        charm
                    ) {
                        page = "Main"
                    }

                    "Wealth Level" -> LevelPage(
                        "Wealth Level",
                        wealth
                    ) {
                        page = "Main"
                    }

                    "Edit Profile" -> EditProfilePage(
                        name = name,
                        bio = bio,
                        photoUrl = profileImageUrl,
                        uploadingImage = uploadingImage,
                        onPickPhoto = {
                            imagePicker.launch("image/*")
                        },
                        onSave = { n, b ->

                            name = n.trim()
                            bio = b.trim()

                            page = "Main"
                            tab = "Profile"

                            message = "Profile saved"
                        },
                        onBack = { page = "Main" }
                    )

                    else -> GeneralPage(
                        page,
                        search,
                        onSearch = { search = it },
                        onBack = { page = "Main" },
                        onAction = { message = it },
                        onLogout = {

                            auth.signOut()

                            profileLoaded = false
                            loggedIn = false
                            page = "Welcome"
                            tab = "Home"
                            profileImageUrl = ""
                            message = ""
                        }
                    )
                }

                if (message.isNotEmpty()) {

                    AlertDialog(
                        onDismissRequest = { message = "" },
                        title = {
                            Text("DISCO VOICE CHAT")
                        },
                        text = {
                            Text(message)
                        },
                        confirmButton = {
                            TextButton(onClick = { message = "" }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

// AUTHENTICATION

@Composable
fun AuthScreen(
    onLogin: (String, String, (Boolean, String) -> Unit) -> Unit,
    onRegister: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onForgot: (String, (Boolean, String) -> Unit) -> Unit
) {

    var mode by remember { mutableStateOf("Welcome") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF25134B),
                    BG,
                    Color(0xFF102D49)
                )
            )
        )
    ) {

        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                Modifier.size(105.dp).background(
                    Brush.linearGradient(listOf(PURPLE, CYAN)),
                    CircleShape
                ),
                Alignment.Center
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    null,
                    tint = WHITE,
                    modifier = Modifier.size(65.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            Text(
                "DISCO",
                fontSize = 35.sp,
                fontWeight = FontWeight.Black,
                color = WHITE
            )

            Text(
                "VOICE CHAT",
                fontSize = 20.sp,
                color = CYAN,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Connect • Talk • Enjoy",
                color = WHITE.copy(alpha = .7f)
            )

            Spacer(Modifier.height(30.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CARD
                )
            ) {

                Column(Modifier.padding(22.dp)) {

                    Text(
                        when (mode) {
                            "Login" -> "WELCOME BACK"
                            "Register" -> "CREATE ACCOUNT"
                            "Forgot" -> "RESET PASSWORD"
                            else -> "WELCOME TO DISCO"
                        },
                        color = WHITE,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(18.dp))

                    if (mode == "Welcome") {

                        Text(
                            "Your voice. Your friends. Your community.",
                            color = WHITE,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                mode = "Login"
                                error = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("LOGIN")
                        }

                        OutlinedButton(
                            onClick = {
                                mode = "Register"
                                error = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CREATE NEW ACCOUNT")
                        }

                    } else {

                        if (mode == "Register") {

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, null)
                                },
                                singleLine = true
                            )

                            Spacer(Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, null)
                            },
                            singleLine = true
                        )

                        if (mode != "Forgot") {

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pass,
                                onValueChange = { pass = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Password (minimum 6 characters)")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            showPass = !showPass
                                        }
                                    ) {
                                        Icon(
                                            if (showPass) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                            null
                                        )
                                    }
                                },
                                visualTransformation =
                                    if (showPass) VisualTransformation.None
                                    else PasswordVisualTransformation(),
                                singleLine = true
                            )
                        }

                        if (error.isNotEmpty()) {

                            Text(
                                error,
                                color = if (error.startsWith("Success"))
                                    CYAN else Color.Red,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(Modifier.height(15.dp))

                        Button(
                            enabled = !loading,
                            onClick = {

                                error = ""

                                if (!email.contains("@")) {

                                    error = "Enter a valid email address"

                                } else if (mode == "Forgot") {

                                    loading = true

                                    onForgot(email) { ok, msg ->
                                        loading = false
                                        error = if (ok) "Success: $msg" else msg
                                    }

                                } else if (pass.length < 6) {

                                    error = "Password must be at least 6 characters"

                                } else if (
                                    mode == "Register" && name.isBlank()
                                ) {

                                    error = "Enter your full name"

                                } else {

                                    loading = true

                                    if (mode == "Login") {

                                        onLogin(email, pass) { ok, msg ->
                                            loading = false
                                            error = if (ok) "" else msg
                                        }

                                    } else {

                                        onRegister(name, email, pass) { ok, msg ->
                                            loading = false
                                            error = if (ok) "" else msg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {

                            if (loading) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = WHITE,
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text(
                                    when (mode) {
                                        "Register" -> "REGISTER"
                                        "Forgot" -> "SEND RESET LINK"
                                        else -> "LOGIN"
                                    }
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                error = ""
                                mode = if (mode == "Login") "Forgot" else "Login"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (mode == "Login")
                                    "Forgot Password?"
                                else
                                    "Back to Login",
                                color = CYAN
                            )
                        }

                        if (mode == "Login") {

                            TextButton(
                                onClick = {
                                    mode = "Register"
                                    error = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Create New Account")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "DISCO VOICE CHAT • OFFICIAL APP",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

// MAIN DASHBOARD

@Composable
fun MainDashboard(
    tab: String,
    name: String,
    frame: String,
    title: String,
    coins: Int,
    diamonds: Int,
    charm: Int,
    wealth: Int,
    vip: Int,
    svip: Int,
    photoUrl: String,
    uploadingImage: Boolean,
    onPickPhoto: () -> Unit,
    onTab: (String) -> Unit,
    onPage: (String) -> Unit
) {

    Scaffold(
        containerColor = BG,
        bottomBar = {

            NavigationBar(containerColor = CARD) {

                listOf(
                    "Home",
                    "Friends",
                    "Rooms",
                    "Messages",
                    "Profile"
                ).forEach {

                    val icon = when (it) {
                        "Home" -> Icons.Default.Home
                        "Friends" -> Icons.Default.People
                        "Rooms" -> Icons.Default.Mic
                        "Messages" -> Icons.Default.Chat
                        else -> Icons.Default.Person
                    }

                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { onTab(it) },
                        icon = { Icon(icon, null) },
                        label = { Text(it) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CYAN,
                            selectedTextColor = CYAN,
                            indicatorColor = PURPLE.copy(alpha = .2f)
                        )
                    )
                }
            }
        }
    ) { padding ->

        when (tab) {

            "Profile" -> ProfilePage(
                padding,
                name,
                frame,
                title,
                coins,
                diamonds,
                charm,
                wealth,
                vip,
                svip,
                photoUrl,
                uploadingImage,
                onPickPhoto,
                onPage
            )

            "Home" -> HomePage(padding, name, onPage)

            "Friends" -> SocialPage(
                padding,
                "Friends",
                onPage
            )

            "Rooms" -> SocialPage(
                padding,
                "Voice Rooms",
                onPage
            )

            "Messages" -> SocialPage(
                padding,
                "Messages",
                onPage
            )
        }
    }
}

// HOME

@Composable
fun HomePage(
    padding: PaddingValues,
    name: String,
    onPage: (String) -> Unit
) {

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Text(
            "DISCO VOICE CHAT",
            color = WHITE,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text("Welcome, $name", color = CYAN)

        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CARD),
            shape = RoundedCornerShape(22.dp)
        ) {

            Column(Modifier.padding(20.dp)) {

                Icon(
                    Icons.Default.GraphicEq,
                    null,
                    tint = PURPLE,
                    modifier = Modifier.size(45.dp)
                )

                Text(
                    "Discover Voice Community",
                    color = WHITE,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Meet people and explore voice rooms.",
                    color = Color.LightGray
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Section("QUICK ACCESS")

        MenuGrid(
            listOf(
                "Voice Rooms",
                "Friends",
                "Messages",
                "Wallet",
                "Store",
                "VIP Membership",
                "Daily Tasks",
                "Notifications"
            ),
            onPage
        )

        Spacer(Modifier.height(20.dp))

        Section("DISCOVER")

        listOf(
            "Popular Rooms",
            "Official Rooms",
            "New Creators",
            "Events",
            "Rankings"
        ).forEach {
            MenuRow(it) { onPage(it) }
        }
    }
}

// PROFILE

@Composable
fun ProfilePage(
    padding: PaddingValues,
    name: String,
    frame: String,
    title: String,
    coins: Int,
    diamonds: Int,
    charm: Int,
    wealth: Int,
    vip: Int,
    svip: Int,
    photoUrl: String,
    uploadingImage: Boolean,
    onPickPhoto: () -> Unit,
    onPage: (String) -> Unit
) {

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Section("MY PROFILE")

            IconButton(onClick = { onPage("Settings") }) {
                Icon(
                    Icons.Default.Settings,
                    null,
                    tint = CYAN
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF25234F)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {

            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF28235C),
                                Color(0xFF12334D),
                                Color(0xFF421B55)
                            )
                        )
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ProfileAvatar(
                    photoUrl,
                    frame,
                    115,
                    onPickPhoto
                )

                Spacer(Modifier.height(8.dp))

                if (uploadingImage) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(25.dp),
                        color = CYAN
                    )

                    Text(
                        "Uploading picture...",
                        color = CYAN,
                        fontSize = 12.sp
                    )

                } else {

                    TextButton(onClick = onPickPhoto) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            tint = CYAN
                        )
                        Text(" Change Profile Picture")
                    }
                }

                Text(
                    name,
                    color = WHITE,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "DISCO ID: ${"10000001"}",
                    color = CYAN
                )

                Text(
                    title,
                    color = GOLD,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Stat("Charm", "Lv.$charm")
                    Stat("Wealth", "Lv.$wealth")
                    Stat(
                        "VIP",
                        if (vip > 0) "Lv.$vip" else "Inactive"
                    )
                }

                Spacer(Modifier.height(10.dp))

                Stat(
                    "SVIP",
                    if (svip > 0) "Lv.$svip" else "Inactive"
                )

                OutlinedButton(
                    onClick = { onPage("Edit Profile") }
                ) {
                    Icon(Icons.Default.Edit, null)
                    Text(" Edit Profile")
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            Balance(
                "Gold Coins",
                coins,
                GOLD,
                Modifier.weight(1f)
            ) {
                onPage("Wallet")
            }

            Balance(
                "Diamonds",
                diamonds,
                CYAN,
                Modifier.weight(1f)
            ) {
                onPage("Wallet")
            }
        }

        Spacer(Modifier.height(20.dp))

        Section("PROFILE & SOCIAL")

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
            onPage
        )

        Spacer(Modifier.height(18.dp))

        Section("MEMBERSHIP & LEVELS")

        MenuGrid(
            listOf(
                "VIP Membership",
                "SVIP Membership",
                "Charm Level",
                "Wealth Level"
            ),
            onPage
        )

        Spacer(Modifier.height(18.dp))

        Section("COSMETICS STORE")

        MenuRow("Open DISCO Store") {
            onPage("Store")
        }

        MenuGrid(
            listOf(
                "Frames",
                "Entry Effects",
                "Backgrounds",
                "Badges",
                "Titles",
                "Chat Bubbles"
            ),
            onPage
        )

        Spacer(Modifier.height(18.dp))

        Section("WALLET & ACTIVITIES")

        MenuGrid(
            listOf(
                "Wallet",
                "Transaction History",
                "Daily Tasks",
                "Gift Collection",
                "Notifications",
                "Settings",
                "Privacy & Security",
                "Help & Support"
            ),
            onPage
        )
    }
}

// PROFILE AVATAR

@Composable
fun ProfileAvatar(
    photoUrl: String,
    style: String,
    size: Int,
    onClick: () -> Unit
) {

    val colors = when {
        style.contains("Royal") || style.contains("Crown") ->
            listOf(GOLD, Color(0xFFFF783C))

        style.contains("Galaxy") || style.contains("Dragon") ->
            listOf(PURPLE, PINK)

        else -> listOf(CYAN, PURPLE)
    }

    Box(
        Modifier.size(size.dp)
            .border(
                5.dp,
                Brush.sweepGradient(colors),
                CircleShape
            )
            .padding(7.dp)
            .clip(CircleShape)
            .background(CARD)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {

        if (photoUrl.isNotBlank()) {

            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

        } else {

            Icon(
                Icons.Default.Person,
                null,
                tint = WHITE,
                modifier = Modifier.size((size * .55f).dp)
            )
        }
    }
}

// STORE

@Composable
fun StorePage(
    category: String,
    owned: Set<String>,
    coins: Int,
    frame: String,
    title: String,
    entry: String,
    onBack: () -> Unit,
    onCategory: (String) -> Unit,
    onBuy: (String, Int) -> Unit,
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

    val cosmeticItems = when (category) {

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
            "Event Champion Frame"
        )

        "Entry Effects" -> listOf(
            "Default Entry",
            "Blue Portal Entry",
            "Neon Burst Entry",
            "Royal Crown Arrival",
            "Galaxy Warp Entry",
            "Dragon Flame Arrival",
            "Diamond Shower Entry",
            "VIP Spotlight",
            "SVIP Cosmic Gate"
        )

        "Backgrounds" -> listOf(
            "Midnight Galaxy",
            "Neon City",
            "Royal Purple Hall",
            "Ocean Aurora",
            "Cyber Blue",
            "Diamond Night"
        )

        "Name Styles" -> listOf(
            "Neon Name",
            "Gold Royal Name",
            "Rainbow Name",
            "Galaxy Name"
        )

        "Chat Bubbles" -> listOf(
            "Neon Bubble",
            "Royal Bubble",
            "Galaxy Bubble",
            "Diamond Bubble"
        )

        "Badges" -> listOf(
            "Verified Badge",
            "VIP Badge",
            "SVIP Badge",
            "Champion Badge",
            "Official Badge"
        )

        "Titles" -> listOf(
            "DISCO Member",
            "VIP Member",
            "Royal Member",
            "Legend",
            "Dragon King",
            "Super Star",
            "Official Artist",
            "Champion"
        )

        else -> listOf(
            "Rose Gift",
            "Heart Gift",
            "Star Gift",
            "Crown Gift",
            "Dragon Gift",
            "Galaxy Gift"
        )
    }

    Column(
        Modifier.fillMaxSize().padding(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    null,
                    tint = WHITE
                )
            }

            Text(
                "DISCO STORE",
                color = WHITE,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
        }

        Text("Your Coins: $coins", color = GOLD)

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {

            items(categories) {

                FilterChip(
                    selected = category == it,
                    onClick = { onCategory(it) },
                    label = { Text(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(cosmeticItems) { item ->

                val price = if (
                    item.contains("Dragon") ||
                    item.contains("SVIP") ||
                    item.contains("Galaxy")
                ) 500 else 150

                val equipped =
                    item == frame ||
                    item == title ||
                    item == entry

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CARD
                    )
                ) {

                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        CosmeticIcon(category)

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {

                            Text(
                                item,
                                color = WHITE,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                if (item in owned) "Owned"
                                else "$price Gold Coins",
                                color = GOLD,
                                fontSize = 12.sp
                            )
                        }

                        if (item in owned) {

                            Button(onClick = { onEquip(item) }) {
                                Text(
                                    if (equipped) "Equipped"
                                    else "Equip"
                                )
                            }

                        } else {

                            Button(onClick = {
                                onBuy(item, price)
                            }) {
                                Text("BUY")
                            }
                        }
                    }
                }
            }
        }
    }
}

// WALLET

@Composable
fun WalletPage(
    coins: Int,
    diamonds: Int,
    onBack: () -> Unit
) {

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Header("MY WALLET", onBack)

        Spacer(Modifier.height(20.dp))

        Balance(
            "Gold Coins",
            coins,
            GOLD,
            Modifier.fillMaxWidth()
        ) {}

        Spacer(Modifier.height(12.dp))

        Balance(
            "Diamonds",
            diamonds,
            CYAN,
            Modifier.fillMaxWidth()
        ) {}

        Spacer(Modifier.height(20.dp))

        Section("WALLET OPTIONS")

        listOf(
            "Recharge Coins",
            "Diamond Exchange",
            "Gift Income",
            "Transaction History",
            "Official Coin Seller",
            "Withdrawal",
            "Wallet Support"
        ).forEach {

            MenuRow(it) {}
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Wallet transactions and payments require secure server verification.",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}

// MEMBERSHIP

@Composable
fun MembershipPage(
    type: String,
    active: Int,
    onActivate: (Int) -> Unit,
    onBack: () -> Unit
) {

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Header(type.uppercase(), onBack)

        Spacer(Modifier.height(12.dp))

        for (level in 1..10) {

            Card(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CARD
                )
            ) {

                Row(
                    Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(Modifier.weight(1f)) {

                        Text(
                            "$type Level $level",
                            color = GOLD,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Membership benefits",
                            color = Color.LightGray
                        )
                    }

                    if (active == level) {

                        Text("ACTIVE", color = CYAN)

                    } else {

                        OutlinedButton(
                            onClick = { onActivate(level) }
                        ) {
                            Text("Select")
                        }
                    }
                }
            }
        }

        Text(
            "Real membership activation and payments require Owner Panel verification.",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}

// LEVELS

@Composable
fun LevelPage(
    type: String,
    level: Int,
    onBack: () -> Unit
) {

    Column(
        Modifier.fillMaxSize().padding(20.dp)
    ) {

        Header(type, onBack)

        Spacer(Modifier.height(30.dp))

        Text(
            "Current Level: $level",
            color = GOLD,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        LinearProgressIndicator(
            progress = { .35f },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Level progress and rewards are awaiting server integration.",
            color = Color.LightGray
        )
    }
}

// EDIT PROFILE

@Composable
fun EditProfilePage(
    name: String,
    bio: String,
    photoUrl: String,
    uploadingImage: Boolean,
    onPickPhoto: () -> Unit,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {

    var newName by remember(name) {
        mutableStateOf(name)
    }

    var newBio by remember(bio) {
        mutableStateOf(bio)
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Header("EDIT PROFILE", onBack)

        Spacer(Modifier.height(20.dp))

        ProfileAvatar(
            photoUrl,
            "Basic Blue Frame",
            110,
            onPickPhoto
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onPickPhoto,
            enabled = !uploadingImage
        ) {

            Icon(Icons.Default.PhotoLibrary, null)

            Text(
                if (uploadingImage) "Uploading..."
                else "Choose Photo From Gallery"
            )
        }

        if (uploadingImage) {

            CircularProgressIndicator(
                color = CYAN,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = newBio,
            onValueChange = { newBio = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bio") },
            minLines = 3
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = {
                if (newName.isNotBlank()) {
                    onSave(newName, newBio)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = newName.isNotBlank()
        ) {
            Text("SAVE PROFILE")
        }
    }
}

// GENERAL PAGES

@Composable
fun GeneralPage(
    page: String,
    search: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
    onLogout: () -> Unit
) {

    val options = when (page) {

        "Settings" -> listOf(
            "Account Settings",
            "Notifications",
            "Sound Settings",
            "Language",
            "Privacy",
            "Security",
            "Logout"
        )

        "Agency" -> listOf(
            "Create Agency",
            "Agency Information",
            "Agency Head",
            "Managers",
            "Agency Hosts",
            "Member Requests"
        )

        "My ID & QR" -> listOf(
            "DISCO ID: 10000001",
            "Copy ID",
            "Share Profile",
            "Show QR Code"
        )

        "Daily Tasks" -> listOf(
            "Login Reward",
            "Visit Voice Room",
            "Follow Creator",
            "Complete Profile"
        )

        "Achievements" -> listOf(
            "Daily Achievements",
            "Voice Achievements",
            "Social Achievements",
            "Event Achievements"
        )

        "Voice Rooms" -> listOf(
            "Public Rooms",
            "Official Rooms",
            "Create Room",
            "Join Room"
        )

        "Messages" -> listOf(
            "Inbox",
            "Message Requests"
        )

        "Notifications" -> listOf(
            "System Notifications",
            "Friend Notifications",
            "Gift Notifications"
        )

        "Following", "Followers", "Friends" -> listOf(
            "Search Users",
            "User List",
            "Requests"
        )

        else -> listOf(
            "Overview",
            "Details",
            "History",
            "My Collection"
        )
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Header(page.uppercase(), onBack)

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search $page") },
            leadingIcon = {
                Icon(Icons.Default.Search, null)
            },
            singleLine = true
        )

        Spacer(Modifier.height(15.dp))

        options.filter {
            it.contains(search, ignoreCase = true)
        }.forEach { item ->

            MenuRow(item) {

                if (item == "Logout") {
                    onLogout()
                } else {
                    onAction("$item requires its connected service")
                }
            }
        }
    }
}

// SOCIAL SCREENS

@Composable
fun SocialPage(
    padding: PaddingValues,
    type: String,
    onPage: (String) -> Unit
) {

    var searchText by remember {
        mutableStateOf("")
    }

    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .padding(18.dp)
    ) {

        Text(
            type.uppercase(),
            color = WHITE,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
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
            when (type) {

                "Friends" -> listOf(
                    "Friends",
                    "Following",
                    "Followers",
                    "Friend Requests"
                )

                "Voice Rooms" -> listOf(
                    "Public Rooms",
                    "Official Rooms",
                    "Create Room",
                    "Join Room"
                )

                else -> listOf(
                    "Inbox",
                    "Message Requests",
                    "Notifications"
                )
            },
            onPage
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Connect the online backend to load real users and live data.",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}

// REUSABLE COMPONENTS

@Composable
fun Section(text: String) {

    Text(
        text,
        color = CYAN,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun Stat(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            value,
            color = WHITE,
            fontWeight = FontWeight.Bold
        )

        Text(
            label,
            color = Color.LightGray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun Avatar(
    style: String,
    size: Int
) {

    val colors = when {

        style.contains("Royal") || style.contains("Crown") ->
            listOf(GOLD, Color(0xFFFF783C))

        style.contains("Galaxy") || style.contains("Dragon") ->
            listOf(PURPLE, PINK)

        else -> listOf(CYAN, PURPLE)
    }

    Box(
        Modifier.size(size.dp)
            .border(
                5.dp,
                Brush.sweepGradient(colors),
                CircleShape
            )
            .padding(7.dp)
            .background(CARD, CircleShape),
        Alignment.Center
    ) {

        Icon(
            Icons.Default.Person,
            null,
            tint = WHITE,
            modifier = Modifier.size((size * .55f).dp)
        )
    }
}

@Composable
fun CosmeticIcon(category: String) {

    val icon = when (category) {

        "Frames" -> Icons.Default.Circle
        "Entry Effects" -> Icons.Default.Bolt
        "Badges" -> Icons.Default.Verified
        "Titles" -> Icons.Default.EmojiEvents
        "Backgrounds" -> Icons.Default.Landscape
        "Chat Bubbles" -> Icons.Default.ChatBubble
        else -> Icons.Default.AutoAwesome
    }

    Icon(
        icon,
        null,
        tint = GOLD,
        modifier = Modifier.size(35.dp)
    )
}

@Composable
fun Balance(
    label: String,
    amount: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier.clickable {
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = CARD
        ),
        shape = RoundedCornerShape(17.dp)
    ) {

        Column(Modifier.padding(15.dp)) {

            Icon(
                if (label.contains("Diamond"))
                    Icons.Default.Diamond
                else
                    Icons.Default.MonetizationOn,
                null,
                tint = color
            )

            Text(
                label,
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Text(
                amount.toString(),
                color = WHITE,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        }
    }
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
                        Modifier.weight(1f)
                            .clickable {
                                onClick(item)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = CARD
                        )
                    ) {

                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                item,
                                color = WHITE,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = CYAN
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
fun MenuRow(
    title: String,
    onClick: () -> Unit
) {

    Card(
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = CARD
        ),
        shape = RoundedCornerShape(13.dp)
    ) {

        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                title,
                color = WHITE,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = CYAN
            )
        }
    }
}

@Composable
fun Header(
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
                tint = WHITE
            )
        }

        Text(
            title,
            color = WHITE,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black
        )
    }
}
