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
import kotlin.math.abs

@AndroidEntryPoint
class UserProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var pairedDeviceAdapter: PairedDeviceAdapter
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private val userprofileViewModel: UserProfileViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var pageScroll: ScrollView
    private lateinit var saveButton: Button

    private lateinit var nameView: EditText
    private lateinit var emailView: EditText
    private lateinit var ageView: EditText
    private lateinit var genderView: Spinner
    private lateinit var emergencyContactLayout: LinearLayout

    private var isEditable = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        pageScroll = findViewById(R.id.user_profile_scroll)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        clickAndOpenSideBar(toolbar)

        nameView = findViewById(R.id.user_name)
        emailView = findViewById(R.id.user_email)
        ageView = findViewById(R.id.user_age)
        genderView = findViewById(R.id.gender)
        emergencyContactLayout = findViewById(R.id.emergency_contact_list)

        nameView.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) pageScroll.scrollToView(v) }
        emailView.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) pageScroll.scrollToView(v) }
        ageView.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) pageScroll.scrollToView(v) }

        val emergencyAddButton = findViewById<Button>(R.id.emergency_contact_addButton)
        emergencyAddButton.setOnClickListener { addEmergencyContact("") }

        saveButton = findViewById(R.id.user_profile_saveButton)
        saveButton.setOnClickListener {
            try {
                if (isEditable) {
                    saveButton.text = "수정하기"
                    Log.d("[로그]", "저장 버튼 클릭")
//                    빈 비상연락처 입력란 제거
                    val emergencyContactList = mutableListOf<String>()
                    for (i in emergencyContactLayout.childCount - 1 downTo 0) {
                        val eContact = emergencyContactLayout.getChildAt(i) as EditText
                        if (eContact.text.isEmpty()) {
                            emergencyContactLayout.removeView(eContact)
                        } else {
                            emergencyContactList.add(eContact.text.toString())
                        }
                    }

                    val userInfo = UserProfileViewModel().userProfileSave(
                        UserInfo(
                            nameView.text.toString(),
                            emailView.text.toString(),
                            ageView.text.toString(),
                            genderView.selectedItem.toString(),
                            emergencyContactList
                        )
                    )

                    nameView.setText(userInfo.name)
                    emailView.setText(userInfo.email)
                    ageView.setText(userInfo.age)
                    genderView.setSelection((if (userInfo.gender == "남") 0 else 1))
                    emergencyContactLayout.removeAllViews()
                    for (i: Int in 0..<userInfo.emergencyContactList.size) {
                        addEmergencyContact(userInfo.emergencyContactList[i])
                    }

                    val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("ID", userInfo.email)
                    editor.apply()

                    isEditable = false
                    setFocusable(nameView, emailView, ageView)
                    emergencyAddButton.isEnabled = isEditable
                } else {
                    saveButton.text = "저장하기"
                    isEditable = true
                    setFocusable(nameView, emailView, ageView)
                    emergencyAddButton.isEnabled = isEditable
                }

            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "입력 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        //        앱 시작 시 데이터베이스로부터 사용자 데이터를 받아온다.(있다고 가정)
        initUserInfo()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun calculateRectOnScreen(view: View): Rect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(
            location[0],
            location[1],
            location[0] + view.measuredWidth,
            location[1] + view.measuredHeight
        )
    }

    private fun ScrollView.computeDistanceToView(view: View): Int {
        return abs(calculateRectOnScreen(this).top - (this.scrollY + calculateRectOnScreen(view).top))
    }

    private fun ScrollView.scrollToView(view: View) {
        val y = computeDistanceToView(view)
//        감으로 키보드 끝과 View 사이의 거리를 덜 스크롤
        this.scrollTo(0, y - 650)
    }

    private fun setFocusable(nameView: EditText, emailView: EditText, ageView: EditText) {
        nameView.isFocusable = isEditable
        nameView.isFocusableInTouchMode = isEditable

        emailView.isFocusable = isEditable
        emailView.isFocusableInTouchMode = isEditable

        ageView.isFocusable = isEditable
        ageView.isFocusableInTouchMode = isEditable
    }

    private fun initUserInfo() {

        Log.d("[로그]", "initializing")

        val sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
        val userID = sharedPreferences.getString("ID", "")
        if (userID.toString() == "") {
            isEditable = false
            setFocusable(nameView, emailView, ageView)
            saveButton.text = "등록하기"
            return
        }

        lifecycleScope.launch {
            val userInfo = userprofileViewModel.loadStoredData(userID.toString())

            nameView.setText(userInfo.name)
            emailView.setText(userInfo.email)
            ageView.setText(userInfo.age)
            genderView.setSelection((if (userInfo.gender == "남") 0 else 1))
            for (i: Int in 0..<userInfo.emergencyContactList.size) {
                addEmergencyContact(userInfo.emergencyContactList[i])
            }

            isEditable = false
            setFocusable(nameView, emailView, ageView)
            saveButton.text = "수정하기"
        }
        Log.d("[로그]", "initializing complete")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addEmergencyContact(contact: String) {
        val inflater = LayoutInflater.from(this)
        val emergencyContact =
            inflater.inflate(
                R.layout.emergency_contact_item,
                emergencyContactLayout,
                false
            ) as EditText

        emergencyContact.addTextChangedListener {
//            View 우측의 'x' 아이콘 클릭 시 내부 text
            emergencyContact.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP && isEditable) {
                    val eContact = v as EditText

                    eContact.isFocusable = true
                    eContact.isFocusableInTouchMode = true


                    if (event.rawX >= (v.right - eContact.compoundDrawablesRelative[2].bounds.width())) {
                        eContact.setText("")
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
        emergencyContact.setText(contact)
        emergencyContactLayout.addView(emergencyContact, emergencyContactLayout.childCount)
        emergencyContact.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) pageScroll.scrollToView(v)
        }
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

    private fun clickAndOpenSideBar(toolbar: Toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout)
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
//        drawer 동작 선언
        sideBarInnerAction()
    }

    //  drawer 내부의 요소들을 제어할 수 있는 함수
    private fun sideBarInnerAction() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val navHeader = navigationView.getHeaderView(0)

        val btnDeviceDiscovery = navHeader.findViewById<Button>(R.id.btndevicediscovery)
        val recyclerView = navHeader.findViewById<RecyclerView>(R.id.paireddevice)
        val disconnectView = navHeader.findViewById<View>(R.id.disconnect)

        /** 내부 동작 */
        addNewDevices(btnDeviceDiscovery)
        pairedDevices(recyclerView)
        connectDevice()
        disconnectDevice(disconnectView)
    }

    private fun addNewDevices(btnDeviceDiscovery: Button) {
        btnDeviceDiscovery.setOnClickListener {
            checkBluetoothEnabledState {
                drawerLayout.closeDrawer(GravityCompat.START)
                moveActivity(DeviceDiscoveryActivity::class.java)
            }
        }
    }

    private fun pairedDevices(recyclerView: RecyclerView) {
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
                            suspendCoroutine { continuation ->
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

    private fun disconnectDevice(disconnectView: View) {
        disconnectView.setOnClickListener {
            if (isServiceRunning()) {
                lifecycleScope.launch {
                    suspendCoroutine { continuation ->
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
