package com.xiaozhi.robot.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.UUID;

public class BleManager {
    public static final String TAG = BleManager.class.getName();
    private Context mContext;
    private static BleManager manager;
    //蓝牙设备管理类
    private BluetoothManager mBluetoothManager;
    //蓝牙设配器
    private BluetoothAdapter mAdapter;
    private BluetoothLeAdvertiser mAdvertiser;
    //蓝牙Server
    private BluetoothGattServer mBluetoothGattServer;
    //蓝牙服务
    private BluetoothGattService mService;
    //蓝牙特征
    private BluetoothGattCharacteristic charWifiNameAndPassword;
    //服务UUID
    public static final String SERVICE_WIFI = "00001111-0000-1000-8000-00805f9b34fb";
    //特征UUID
    public static final String CHARACTERISTIC_SET_WIFI_NAME_AND_PASSWORD = "00008888-0000-1000-8000-00805f9b34fb";
    //描述UUID
    public static final String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static BleManager getInstance(Context context) {
        if (manager == null) {
            manager = new BleManager(context);
        }
        return manager;
    }

    public BleManager(Context context) {
        mContext = context;
        //1.获取管理类
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        //判断设备是否支持蓝牙
        if (mBluetoothManager == null) {
            return;
        }
        //2.获取蓝牙适配器
        mAdapter = mBluetoothManager.getAdapter();
        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }
    }

    /**
     * 开启蓝牙并开启ble广播
     */
    public synchronized void openBle() {
        //如果是调用closeBle来关闭蓝牙的，会将bluetoothAdapter,bluetoothReceiver置为null，需要重新赋值
        if (mAdapter == null) {
            mAdapter = mBluetoothManager.getAdapter();
        }
        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }
        if (Build.VERSION.SDK_INT >= 21 && mAdapter.isMultipleAdvertisementSupported()) {
            if (initService()) {
                //10.启动ble广播
                startAdvertise();
            } else {
                Log.d(TAG, "开启ble失败");
            }
        } else {
            Log.d(TAG, "开启ble失败");
            if (!mAdapter.isMultipleAdvertisementSupported()) {
                Log.d(TAG, "您的设备不支持蓝牙从模式");
            }
        }

    }

    /**
     * 开启ble广播
     */
    private void startAdvertise() {
        mAdvertiser = mAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build();
        ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(SERVICE_WIFI));
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(parcelUuid)
                .build();
        mAdvertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "开启ble广播成功");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.d(TAG, "开启ble广播失败");
            }
        });

    }

    private boolean initService() {
        //3.获取服务
        mService = new BluetoothGattService(UUID.fromString(SERVICE_WIFI), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //4.获取一个特征
        charWifiNameAndPassword = new BluetoothGattCharacteristic(UUID.fromString(CHARACTERISTIC_SET_WIFI_NAME_AND_PASSWORD), BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        //5.获取一个描述
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(CHARACTERISTIC_CONFIG), BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue("WIFI ACCOUNT".getBytes(Charset.forName("UTF-8")));
        //6.将描述加入到特征中
        charWifiNameAndPassword.addDescriptor(descriptor);
        //7.将特征加入到服务中
        mService.addCharacteristic(charWifiNameAndPassword);
        //8.获取周边
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                //连接状态发生变化回调
                Log.d(TAG, "连接状态发生改变:" + newState);
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
                //当周边添加到服务成功时的回调
                Log.d(TAG, "服务添加成功");
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                //当远程设备请求读取本地特征时回调
                //必须调用BluetoothGattServer.sendResponse
                Log.d(TAG, "远程设备读取本地特征");
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                //当远程设备请求写入本地特征时回调
                //通常我们讲的BLE通信，其实就说对characteristic的读写或者订阅
                //必须调用BluetoothGattServer.sendResponse
                try {
                    Log.d(TAG, "远程设备写入本地特征:" + new String(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                //当远程设备请求读取本地描述时回调
                //必须调用BluetoothGattServer.sendResponse
                Log.d(TAG, "远程设备读取本地描述");
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                //当远程设备请求写入本地描述时回调
                //必须调用BluetoothGattServer.sendResponse
                Log.d(TAG, "远程设备写入本地描述:" + value);
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                super.onExecuteWrite(device, requestId, execute);
                //执行本地设备所有挂起的写操作
                //必须调用BluetoothGattServer.sendResponse
                Log.d(TAG, "执行所有挂起的写操作");
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
                //当通知发送到远程设备时的回调
                Log.d(TAG, "通知发送成功");
            }

            /**
             * MTU(Maxximum Transmission Unit)最大传输单元:指在一个协议数据单元中(PDU,Protocol Data Unit)有效的最大传输Byte
             * AndroidMTU一般为23，发送长包需要更改MTU(5.1(API21)开始支持MTU修改)或者分包发送
             * core spec里面定义了ATT的默认MTU为23bytes，除去ATT的opcode一个字节以及ATT的handle2个字节后，剩余20个bytes留给GATT
             * MTU是不可以协商的，只是通知对方，双方在知道对方的极限后会选择一个较小的值作为以后的MTU
             */
            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
                //MTU更改时的回调
                Log.d(TAG, "MTU发生更改：" + mtu);
            }

            /**
             * PHY(Physical)：物理接口收发器，实现OSI模型的物理层
             * 当调用BluetoothGattServer.setPreferredPhy时，或者远程设备更改了PHY时回调
             * 低功耗蓝牙5.0协议中，定义了两种调制方案。这两种方案都采用了GFSK调制。区别在于symbol rate不同，一种1 Msym/s，另一种2Msym/s。
             * 其中1 Msym/s是符合低功耗蓝牙5.0协议的设备所必须支持的。
             * 在1 Msym/s调制下，低功耗蓝牙5.0协议定义了两种PHY:(1)LE 1MPHY ,即信息数据不变吗，信息数据的传输速率就为1Mb/s
             *                                                 (2)LE Coded PHY，即信息数据编码方式，信息数据的传输速率为125kb/s或者500kb/s
             * 在2 Msym/s调制下，低功耗蓝牙5.0协议仅定义了一种PHY：LE 2MPHY,即信息数据不编码，信息数据的传输速率就为2Mb/s
             */
            @Override
            public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(device, txPhy, rxPhy, status);
                //当调用了BluetoothGattServer.setPreferredPhy时，或者远程设备更改了PHY时回调
                Log.d(TAG, "onPhyUpdate");
            }

            @Override
            public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                super.onPhyRead(device, txPhy, rxPhy, status);
                //当调用了BluetoothGattServer.readPhy时回调
                Log.d(TAG, "onPhyRead");
            }
        });
        //9.将服务加入到周边
        return mBluetoothGattServer.addService(mService);

    }

}
