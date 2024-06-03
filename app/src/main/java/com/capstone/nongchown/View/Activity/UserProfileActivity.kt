package com.capstone.nongchown.View.Activity


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.nongchown.Adapter.PairedDeviceAdapter
import com.capstone.nongchown.Model.Enum.BluetoothState
import com.capstone.nongchown.Model.Enum.ConnectResult
import com.capstone.nongchown.Model.ForegroundService
import com.capstone.nongchown.Model.ForegroundService.Companion.isServiceRunning
import com.capstone.nongchown.Model.UserInfo
import com.capstone.nongchown.R
import com.capstone.nongchown.Utils.moveActivity
import com.capstone.nongchown.Utils.showToast
import com.capstone.nongchown.ViewModel.BluetoothViewModel
import com.capstone.nongchown.ViewModel.UserProfileViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val bluetoothViewModel by viewModels<BluetoothViewModel>()
    private lateinit var pairedDeviceAdapter: PairedDeviceAdapter
    private lateinit var recyclerView: RecyclerView

    private val userprofileViewModel = UserProfileViewModel()

    private lateinit var pageScroll: ScrollView
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var name: String
    private lateinit var email: String
    private lateinit var age: String
    private lateinit var gender: String
    private var isEditable = true
    private val emergencyContactList = mutableListOf<String>()

    private val emergencyAddButton: Button by lazy {
        findViewById(R.id.emergency_contact_addButton)
    }
    private val saveButton: Button by lazy {
        findViewById(R.id.user_profile_saveButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        pageScroll = findViewById(R.id.user_profile_scroll)
        pageScroll.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            pageScroll.getWindowVisibleDisplayFrame(rect)
            val screenHeight = pageScroll.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // 키보드가 나타나면 ScrollView의 bottom margin을 키보드 높이만큼 조정
            val params = pageScroll.layoutParams as LinearLayout.LayoutParams
            if (keypadHeight > screenHeight * 0.15) { // 키보드가 올라왔을 때
                params.bottomMargin = (screenHeight * 0.15).toInt()
                val location = IntArray(2)
                currentFocus?.getLocationInWindow(location)

                // EditText가 키보드 위에 오도록 스크롤
                pageScroll.smoothScrollTo(0, location[1] - rect.top)
            } else { // 키보드가 내려갔을 때
                params.bottomMargin = 0
            }
            pageScroll.layoutParams = params
        }

        val userName = findViewById<EditText>(R.id.user_name)
        userName.addTextChangedListener {
            Log.d("[로그]", "name changed")
            saveButton.isEnabled = true
        }

        val userEmail = findViewById<EditText>(R.id.user_email)
        userEmail.addTextChangedListener {
            Log.d("[로그]", "email changed")
            saveButton.isEnabled = true
        }

        val userAge = findViewById<EditText>(R.id.user_age)
        userAge.addTextChangedListener {
            Log.d("[로그]", "age changed")
            saveButton.isEnabled = true
        }

        val userGender = findViewById<Spinner>(R.id.gender)

        val emergencyContacts = findViewById<LinearLayout>(R.id.emergency_contact_list)

        emergencyAddButton.setOnClickListener {
            addEmergencyContact(emergencyContacts, "")
        }

        saveButton.setOnClickListener {
            try {
                if (isEditable) {
                    saveButton.text = "수정하기"
                    Log.d("[로그]", "저장 버튼 클릭")
//                    빈 비상연락처 입력란 제거
                    for (i in emergencyContacts.childCount - 1 downTo 0) {
                        val eContact = emergencyContacts.getChildAt(i)
                        if (eContact is EditText && eContact.text.isEmpty()) {
                            emergencyContacts.removeView(eContact)
                        }
                    }
//                    로컬에 저장된 비상연락망 데이터를 지우고 입력란에 있는 데이터로 재설정
                    emergencyContactList.clear()
                    emergencyContacts.children.forEach { emergencyContact ->
                        if (emergencyContact is EditText) {
                            emergencyContactList.add(emergencyContact.text.toString())
                        }
                    }

                    val userInfo = UserProfileViewModel().userProfileSave(
                        UserInfo(
                            userName.text.toString(),
                            userEmail.text.toString(),
                            userAge.text.toString(),
                            userGender.selectedItem.toString(),
                            emergencyContactList
                        )
                    )
                    name = userInfo.name
                    email = userInfo.email
                    age = userInfo.age
                    gender = userInfo.gender
                    emergencyContactList.clear()
                    Log.d("[로그]", "emergencyContactList.clear(): $emergencyContactList")
                    emergencyContactList.addAll(userInfo.emergencyContactList)

                    userName.setText(userInfo.name)
                    userEmail.setText(userInfo.email)
                    userAge.setText(userInfo.age)
                    userGender.setSelection((if (userInfo.gender == "남") 0 else 1))
                    emergencyContacts.removeViews(0, emergencyContacts.childCount - 1)
                    for (i: Int in 0..<userInfo.emergencyContactList.size) {
                        addEmergencyContact(emergencyContacts, userInfo.emergencyContactList[i])
                    }

                    val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("ID", email)
                    editor.apply()

                    isEditable = false
                    userName.isFocusable = isEditable
                    userEmail.isFocusable = isEditable
                    userAge.isFocusable = isEditable
                    emergencyAddButton.isEnabled = isEditable
                } else {
                    saveButton.text = "저장하기"
                    isEditable = true
                    userName.isFocusable = isEditable
                    userName.isFocusableInTouchMode = isEditable
                    userEmail.isFocusable = isEditable
                    userEmail.isFocusableInTouchMode = isEditable
                    userAge.isFocusable = isEditable
                    userAge.isFocusableInTouchMode = isEditable
                    emergencyAddButton.isEnabled = isEditable

                    saveButton.isEnabled = false

                }

            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "입력 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        //        앱 시작 시 데이터베이스로부터 사용자 데이터를 받아온다.(있다고 가정)
        initUserInfo(
            userName, userEmail, userAge, userGender, emergencyContacts
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /** sideBar */
        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        clickAndOpenSideBar()
        sideBarInnerAction()

    }

    private fun initUserInfo(
        userName: EditText,
        userEmail: EditText,
        userAge: EditText,
        userGender: Spinner,
        emergencyContacts: LinearLayout
    ) {

        Log.d("[로그]", "initializing")

        val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
        val userID = sharedPreferences.getString("ID", "")
        email = userID.toString()
        if (email == "") {
            saveButton.text = "저장하기"
            return
        }


        lifecycleScope.launch {
            val userInfo = userprofileViewModel.loadStoredData(email)

            userName.setText(userInfo.name)
            userEmail.setText(userInfo.email)
            userAge.setText(userInfo.age)
            userGender.setSelection((if (userInfo.gender == "남") 0 else 1))
            for (i: Int in 0..<userInfo.emergencyContactList.size) {
                addEmergencyContact(emergencyContacts, userInfo.emergencyContactList[i])
            }
            isEditable = false
            userName.isFocusable = isEditable
            userEmail.isFocusable = isEditable
            userAge.isFocusable = isEditable
            saveButton.text = "수정하기"

        }
        Log.d("[로그]", "initializing complete")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addEmergencyContact(emergencyContacts: LinearLayout, emergencyContact: String) {
        val inflater = LayoutInflater.from(this)
        val eContact =
            inflater.inflate(R.layout.emergency_contact_item, emergencyContacts, false) as EditText

        eContact.addTextChangedListener {
            Log.d("[로그]", "emergencyContact changed")
            saveButton.isEnabled = true

            eContact.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP && isEditable) {
                    eContact.isFocusable = true
                    eContact.isFocusableInTouchMode = true
                    val clearDrawable = eContact.compoundDrawablesRelative[2]
                    if (clearDrawable != null && event.rawX >= (eContact.right - clearDrawable.bounds.width())) {
                        eContact.setText("")
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
        eContact.setText(emergencyContact)
        emergencyContacts.addView(eContact, emergencyContacts.childCount - 1)
    }


    /** sideBar */
    override fun onNavigationItemSelected(item: MenuItem): Boolean { // X
        return false
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK -> {
                    Log.d("[로그]", "블루투스 활성화")
                }

                RESULT_CANCELED -> {
                    Log.d("[로그]", "사용자 블루투스 활성화 거부")
                }
            }

        }

    private fun clickAndOpenSideBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        toolbar.setNavigationOnClickListener {
            checkBluetoothEnabledState { // 밑의 동작 람다식으로 넣음
                drawerLayout.openDrawer(GravityCompat.START)
                bluetoothViewModel.getPairedDevices()
            }
        }
    }

    private fun sideBarInnerAction() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val navHeader = navigationView.getHeaderView(0)

        /** 내부 동작 */
        addNewDevices(navHeader)
        pairedDevices(navHeader)
        connectDevice()
        disconnectDevice(navHeader)
    }

    private fun addNewDevices(navHeader: View) {
        val btnDeviceDiscovery = navHeader.findViewById<Button>(R.id.btndevicediscovery)
        btnDeviceDiscovery.setOnClickListener {
            checkBluetoothEnabledState {
                drawerLayout.closeDrawer(GravityCompat.START)
                moveActivity(DeviceDiscoveryActivity::class.java)
            }
        }
    }

    private fun pairedDevices(navHeader: View) {
        recyclerView = navHeader.findViewById(R.id.paireddevice)
        pairedDeviceAdapter = PairedDeviceAdapter(emptyList())

        recyclerView.apply {
            adapter = pairedDeviceAdapter
            layoutManager = LinearLayoutManager(this@UserProfileActivity)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bluetoothViewModel.pairedDevices.collect { devices ->
                    pairedDeviceAdapter.updateDevices(devices)
                }
            }
        }
    }

    private fun connectDevice() {
        pairedDeviceAdapter.itemClick = object : PairedDeviceAdapter.ItemClick {

            override fun onClick(view: View, position: Int) {
                checkBluetoothEnabledState {
                    lifecycleScope.launch {
                        if (isServiceRunning()) {
                            suspendCoroutine<Unit> { continuation ->
                                val filter = IntentFilter("SERVICE_STOPPED")
                                val serviceStoppedReceiver = object : BroadcastReceiver() {
                                    override fun onReceive(context: Context?, intent: Intent?) {
                                        if (intent?.action == "SERVICE_STOPPED") {
                                            Log.d("[로그]", "SERVICE_STOPPED 수신")
                                            Log.d("[로그]", "종료 후 서비스 상태 : ${isServiceRunning()}")
                                            context?.unregisterReceiver(this)
                                            continuation.resume(Unit)
                                        }
                                    }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    registerReceiver(
                                        serviceStoppedReceiver,
                                        filter,
                                        RECEIVER_EXPORTED
                                    )
                                } else {
                                    registerReceiver(serviceStoppedReceiver, filter)
                                }

                                stopForegroundService()
                            }

                            if (!isServiceRunning()) {
                                attemptConnectToDevice(position)
                            }

                        } else {
                            Log.d("[로그]", "페어링 기기 눌렀을 때 - 서비스 상태 : ${isServiceRunning()}")
                            attemptConnectToDevice(position)
                        }
                    }
                }
            }
        }
    }

    private fun disconnectDevice(navHeader: View) {
        val disconnectView = navHeader.findViewById<View>(R.id.disconnect)
        disconnectView.setOnClickListener {

            if (isServiceRunning()) {
                lifecycleScope.launch {
                    suspendCoroutine<Unit> { continuation ->
                        val filter = IntentFilter("SERVICE_STOPPED")

                        val serviceStoppedReceiver = object : BroadcastReceiver() {
                            override fun onReceive(context: Context?, intent: Intent?) {
                                if (intent?.action == "SERVICE_STOPPED") {
                                    Log.d("[로그]", "SERVICE_STOPPED 수신")
                                    Log.d("[로그]", "모든 연결을 해제합니다.")
                                    context?.unregisterReceiver(this)
                                    continuation.resume(Unit)
                                }
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            registerReceiver(serviceStoppedReceiver, filter, RECEIVER_EXPORTED)
                        } else {
                            registerReceiver(serviceStoppedReceiver, filter)
                        }

                        stopForegroundService()
                    }

                    delay(1000)
                    showToast("모든 연결을 해제합니다.")
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }

        }
    }

    /** 블루투스 활성화 상태 템플릿 */
    fun checkBluetoothEnabledState(enabledAction: () -> Unit) {
        when (bluetoothViewModel.checkBluetoothState()) {
            BluetoothState.ENABLED -> {
                enabledAction()
            }

            BluetoothState.DISABLED -> {
                Log.d("[로그]", "블루투스 활성화 되어있지 않습니다.")
                val bluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startForResult.launch(bluetoothIntent)
            }

            else -> showToast("블루투스를 지원하지 않는 장비입니다.")
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
//        setServiceState(true)
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(this@UserProfileActivity, ForegroundService::class.java)
        stopService(serviceIntent)
//        setServiceState(false)
    }

    suspend fun attemptConnectToDevice(position: Int) {
        val device = pairedDeviceAdapter.getDeviceAtPosition(position).bluetoothDevice
        val flag = bluetoothViewModel.connectToDevice(device)
        delay(700)
        handleConnectionResult(flag)
    }

    private fun handleConnectionResult(flag: ConnectResult) {
        when (flag) {
            ConnectResult.CONNECT -> {
                showToast("연결되었습니다.")
                drawerLayout.closeDrawer(GravityCompat.START)
                startForegroundService()
            }

            ConnectResult.DISCONNECT -> {
                showToast("연결 실패했습니다.")
            }

            else -> {}
        }
    }

}



