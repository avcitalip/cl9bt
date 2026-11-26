package com.cl9tr.obd2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cl9tr.obd2.model.AdapterTier
import com.cl9tr.obd2.model.ConnectionPhase
import com.cl9tr.obd2.model.VehicleProfile
import com.cl9tr.obd2.ui.screens.*
import com.cl9tr.obd2.ui.theme.*
import com.cl9tr.obd2.viewmodel.ObdViewModel

class MainActivity : ComponentActivity() {

    private var onPermissionResultCallback: (() -> Unit)? = null
    private var locationManager: LocationManager? = null
    private var gpsListener: LocationListener? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionResultCallback?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permLauncher.launch(perms)

        setContent {
            Cl9ObdTheme {
                val vm: ObdViewModel = viewModel()
                LaunchedEffect(Unit) {
                    vm.attachContext(this@MainActivity)
                    onPermissionResultCallback = {
                        vm.refreshPairedDevices()
                        startGpsAltitudeTracking(vm)
                    }
                    vm.refreshPairedDevices()
                    startGpsAltitudeTracking(vm)
                }
                Cl9AppRoot(vm)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsAltitudeTracking(vm: ObdViewModel) {
        try {
            if (locationManager == null) {
                locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            }
            if (gpsListener == null) {
                gpsListener = LocationListener { loc: Location ->
                    if (loc.hasAltitude()) vm.onGpsAltitudeUpdate(loc.altitude)
                }
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    25f,
                    gpsListener!!
                )
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            gpsListener?.let { locationManager?.removeUpdates(it) }
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cl9AppRoot(vm: ObdViewModel) {
    val ctx = LocalContext.current
    var currentRoute by remember { mutableStateOf("home") }
    var reorderMode by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<VehicleProfile?>(null) }
    var showProfileEditor by remember { mutableStateOf(false) }

    val conn by vm.connectionState.collectAsState()
    val devices by vm.pairedDevices.collectAsState()
    val liveValues by vm.liveValues.collectAsState()
    val pinnedPids by vm.pinnedPids.collectAsState()
    val dtcs by vm.dtcs.collectAsState()
    val freeze by vm.freezeFrame.collectAsState()
    val readiness by vm.readiness.collectAsState()
    val ecu by vm.ecuIdentity.collectAsState()
    val isRec by vm.isRecordingCsv.collectAsState()
    val csvRows by vm.csvRows.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val activeProfile by vm.activeProfile.collectAsState()
    val needsInitialProfile by vm.needsInitialProfile.collectAsState()
    val banner by vm.homeBanner.collectAsState()
    val perf by vm.perfMetrics.collectAsState()
    val fuel by vm.fuelMetrics.collectAsState()
    val simScenario by vm.simScenario.collectAsState()
    val trackedKm by vm.trackedKm.collectAsState()
    val trackedHours by vm.trackedHours.collectAsState()
    val menuOrder by vm.menuOrder.collectAsState()
    val customNames by vm.customNames.collectAsState()

    val isConnected = conn.phase == ConnectionPhase.CONNECTED || conn.phase == ConnectionPhase.DEMO_MODE
    val signalColor = when {
        !isConnected -> Color(0xFF64748B)
        conn.adapterTier == AdapterTier.TIER_1_PIC18F25K80 -> AccentGreen
        conn.adapterTier == AdapterTier.TIER_2_CLONE_V15 -> AccentAmber
        else -> AccentRed
    }

    val screenTitle = when (currentRoute) {
        "home" -> "CL9"
        "dashboard" -> "Canlı Gösterge Paneli"
        "performance" -> "Hesaplanan Motor Performansı"
        "fuel" -> "Yakıt Tüketimi Yorumlaması"
        "sensors" -> "Tüm Sensörler"
        "dtc" -> "Arıza Kodları"
        "maintenance" -> "Muhtemel Arızalar ve Bakım"
        "connect" -> "Adaptör ve Protokol Analizi"
        "logger" -> "Sürüş Veri Kaydı"
        "advice" -> "Test ve Bilgilendirme"
        else -> "CL9"
    }

    if (needsInitialProfile || showProfileEditor) {
        ProfileSetupDialog(
            initial = editingProfile,
            canClose = !needsInitialProfile,
            onDismiss = { showProfileEditor = false; editingProfile = null },
            onSave = { p ->
                vm.saveProfile(p)
                showProfileEditor = false
                editingProfile = null
            },
            onStartSimulation = {
                showProfileEditor = false
                editingProfile = null
                vm.startDemoMode(AdapterTier.TIER_1_PIC18F25K80)
            }
        )
    }

    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            profiles = profiles.ifEmpty { listOf(activeProfile) },
            activeId = activeProfile.id,
            onDismiss = { showProfileSwitcher = false },
            onSelect = { id -> vm.selectProfile(id); showProfileSwitcher = false },
            onEdit = { p -> editingProfile = p; showProfileSwitcher = false; showProfileEditor = true },
            onDelete = { id -> vm.deleteProfile(id) },
            onAddNew = { editingProfile = null; showProfileSwitcher = false; showProfileEditor = true }
        )
    }

    Scaffold(
        containerColor = BgObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgShell,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (currentRoute != "home") {
                        IconButton(onClick = { currentRoute = "home"; reorderMode = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
                        }
                    } else {
                        IconButton(onClick = { reorderMode = !reorderMode }) {
                            Icon(
                                Icons.Default.FormatListNumbered,
                                contentDescription = "Sırala",
                                tint = if (reorderMode) AccentCyan else TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileSwitcher = true }) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Profil", tint = AccentCyan)
                    }
                    IconButton(onClick = { vm.refreshPairedDevices(); currentRoute = "connect"; reorderMode = false }) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = "OBD Sinyal", tint = signalColor)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BgShell, tonalElevation = 0.dp) {
                val tabs = listOf(
                    Triple("home", "Ana Ekran", Icons.Default.Home),
                    Triple("dashboard", "Panel", Icons.Default.Speed),
                    Triple("sensors", "Sensörler", Icons.Default.ShowChart),
                    Triple("dtc", "Arıza", Icons.Default.Warning)
                )
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { currentRoute = route; reorderMode = false },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
                        label = { Text(label, fontSize = 10.5.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCyan,
                            selectedTextColor = AccentCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = AccentCyan.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
            when (currentRoute) {
                "home" -> HomeScreen(
                    conn = conn,
                    profile = activeProfile,
                    banner = banner,
                    dtcCount = dtcs.size,
                    liveRpm = liveValues["010C"]?.let { "${it.formattedValue} d/dk" } ?: "-- d/dk",
                    menuOrder = menuOrder,
                    reorderMode = reorderMode,
                    onMoveItem = vm::moveMenuItem,
                    onNavigate = {
                        if (it == "connect") vm.refreshPairedDevices()
                        if (it == "advice") { vm.readFreezeFrame(); vm.readEmissionsAndMode06(); vm.readEcuIdentity() }
                        currentRoute = it
                        reorderMode = false
                    },
                    onOpenProfileSwitcher = { showProfileSwitcher = true },
                    onToggleSim = {
                        if (isConnected) vm.disconnect()
                        else vm.startDemoMode(AdapterTier.TIER_1_PIC18F25K80)
                    },
                    onTestMeteoAlert = {
                        vm.triggerMeteorologicalHazard("AFAD ve Meteoroloji Uyarısı: Bölgenizde ani buzlanma ve gizli don tehlikesi mevcuttur. Hızınızı düşürün ve ani fren yapmayın.")
                    }
                )
                "dashboard" -> DashboardScreen(
                    conn = conn,
                    liveValues = liveValues,
                    pinnedPids = pinnedPids,
                    customNames = customNames,
                    simScenario = simScenario,
                    onSelectScenario = vm::setSimScenario,
                    onStartSim = { vm.startDemoMode(AdapterTier.TIER_1_PIC18F25K80) }
                )
                "performance" -> PerformanceScreen(
                    metrics = perf,
                    trackedKm = trackedKm,
                    trackedHours = trackedHours,
                    onStartSim = { vm.startDemoMode(AdapterTier.TIER_1_PIC18F25K80) }
                )
                "fuel" -> FuelAnalysisScreen(
                    fuel = fuel,
                    onStartSim = { vm.startDemoMode(AdapterTier.TIER_1_PIC18F25K80) }
                )
                "sensors" -> AllSensorsScreen(liveValues, pinnedPids, vm::togglePinnedPid)
                "dtc" -> DtcScreen(dtcs, vm::readDtcs, vm::clearDtcs)
                "maintenance" -> MaintenanceScreen(trackedKm, trackedHours, activeProfile, vm::resetDriveData)
                "connect" -> AdapterConnectScreen(
                    conn = conn,
                    pairedDevices = devices,
                    onRefreshDevices = vm::refreshPairedDevices,
                    onSelectDevice = vm::connectToDevice,
                    onStartDemo = vm::startDemoMode,
                    onDisconnect = vm::disconnect
                )
                "logger" -> CsvLoggerScreen(
                    isRecording = isRec,
                    rows = csvRows,
                    onToggle = vm::toggleCsvRecording,
                    onClear = vm::clearCsvLogs,
                    onShare = { vm.saveAndShareCsv(ctx) }
                )
                "advice" -> TestAndInfoScreen(
                    conn = conn,
                    profile = activeProfile,
                    readiness = readiness,
                    freeze = freeze,
                    ecu = ecu,
                    trackedKm = trackedKm,
                    trackedHours = trackedHours
                )
            }
        }
    }
}
