package com.capstone.nongchown.Repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class BluetoothRepositoryImpl @Inject constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothRepository {

    //    private var deviceScanReceiver: BroadcastReceiver? = null // null 초기화 : 필요한 시점까지 객체의 생성을 늦춘다.
    private val _discoveredDeviceList = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _pairedDeviceList = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    @SuppressLint("MissingPermission")
    override fun startDiscovery(): MutableStateFlow<List<BluetoothDevice>> {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //블루투스 상태변화 액션
        filter.addAction(BluetoothDevice.ACTION_FOUND) //기기 검색됨
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //기기 검색 시작
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //기기 검색 종료
        context.registerReceiver(deviceScanReceiver, filter)
        bluetoothAdapter?.startDiscovery()
        return _discoveredDeviceList
    }

    @SuppressLint("MissingPermission")
    override fun getPairedDevice(): StateFlow<List<BluetoothDevice>> {

        if (bluetoothAdapter != null) {
            // Ensure Bluetooth is enabled
            if (!bluetoothAdapter.isEnabled) {
                // You might want to prompt the user to enable Bluetooth
            }
            // Get the list of paired devices
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            _pairedDeviceList.value = pairedDevices.toList()
        } else {
            // Bluetooth is not supported on this device
        }
        return _pairedDeviceList
    }

    override fun connectToDevice() {
        Log.d("[connectToDevice]", "CONNECT TO DEVICE")
    }

    override fun stopDiscovery() {
        Log.d("[stopDiscovery]", "DISCOVERY STOP")
        Log.d("[unregisterReceiver]", "UNREGISTER RECEIVER")
        context.unregisterReceiver(deviceScanReceiver)
    }

    @SuppressLint("MissingPermission")
    override fun cancelDiscovery() {
        Log.d("[cancelDiscovery]", "DISCOVERY CANCEL")
        bluetoothAdapter?.cancelDiscovery()
    }


    @Suppress("DEPRECATION", "MissingPermission")
    private val deviceScanReceiver = object : BroadcastReceiver() {
        val tempDeviceList = mutableListOf<BluetoothDevice>()
        override fun onReceive(context: Context?, intent: Intent?) {

            var action = ""
            if (intent != null) {
                action = intent.action.toString() //입력된 action
            }

            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    Log.d("[CHANGED]", "STATE CHANGED")
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("[STARTED]", "DISCOVERY STARTED")
                    tempDeviceList.clear()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("[FINISHED]", "DISCOVERY FINISHED")
                    _discoveredDeviceList.value = tempDeviceList
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        // 로그 찍기 용
                        if (it.name != null) {
                            Log.d("[FOUND]", "Name: ${device.name}, Address: ${device.address}")
                        }

                        // 중복 방지
                        if (it.name != null && !tempDeviceList.contains(it)) {
                            tempDeviceList.add(it)
                        }
                    }
                }
            }

        }
    }

}