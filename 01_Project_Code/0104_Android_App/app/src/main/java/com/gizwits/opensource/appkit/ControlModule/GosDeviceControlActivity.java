package com.gizwits.opensource.appkit.ControlModule;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.opensource.appkit.CommonModule.GosDeploy;
import com.gizwits.opensource.appkit.R;

public class GosDeviceControlActivity extends GosControlModuleBaseActivity
		implements OnClickListener, OnEditorActionListener, OnSeekBarChangeListener {

	/** 设备列表传入的设备变量 */
	private GizWifiDevice mDevice;

	private Switch sw_bool_Led_WarmSta;
	private Switch sw_bool_LedOnOff;
	private TextView tv_data_Led_RVal;
	private SeekBar sb_data_Led_RVal;
	private TextView tv_data_Led_GVal;
	private SeekBar sb_data_Led_GVal;
	private TextView tv_data_Led_BVal;
	private SeekBar sb_data_Led_BVal;
	private TextView tv_data_Led_Brightness;
	private SeekBar sb_data_Led_Brightness;


	private enum handler_key {

		/** 更新界面 */
		UPDATE_UI,

		DISCONNECT,
	}

	private Runnable mRunnable = new Runnable() {
		public void run() {
			if (isDeviceCanBeControlled()) {
				progressDialog.cancel();
			} else {
				toastDeviceNoReadyAndExit();
			}
		}

	};



	/** The handler. */
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handler_key key = handler_key.values()[msg.what];
			switch (key) {
			case UPDATE_UI:
				updateUI();
				break;
			case DISCONNECT:
				toastDeviceDisconnectAndExit();
				break;
			}

		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gos_device_control);
		initDevice();
		setToolBar(true, getDeviceName());
		final Drawable add = getResources().getDrawable(R.drawable.common_setting_more);
		int color = GosDeploy.appConfig_Contrast();
		add.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		mToolbar.setOverflowIcon(add);
		initView();
		initEvent();
	}

	private void initView() {
		
		sw_bool_Led_WarmSta = (Switch) findViewById(R.id.sw_bool_Led_WarmSta);
		sw_bool_LedOnOff = (Switch) findViewById(R.id.sw_bool_LedOnOff);
		tv_data_Led_RVal = (TextView) findViewById(R.id.tv_data_Led_RVal);
		sb_data_Led_RVal = (SeekBar) findViewById(R.id.sb_data_Led_RVal);
		tv_data_Led_GVal = (TextView) findViewById(R.id.tv_data_Led_GVal);
		sb_data_Led_GVal = (SeekBar) findViewById(R.id.sb_data_Led_GVal);
		tv_data_Led_BVal = (TextView) findViewById(R.id.tv_data_Led_BVal);
		sb_data_Led_BVal = (SeekBar) findViewById(R.id.sb_data_Led_BVal);
		tv_data_Led_Brightness = (TextView) findViewById(R.id.tv_data_Led_Brightness);
		sb_data_Led_Brightness = (SeekBar) findViewById(R.id.sb_data_Led_Brightness);
	}

	private void initEvent() {

		sw_bool_Led_WarmSta.setOnClickListener(this);
		sw_bool_LedOnOff.setOnClickListener(this);
		sb_data_Led_RVal.setOnSeekBarChangeListener(this);
		sb_data_Led_GVal.setOnSeekBarChangeListener(this);
		sb_data_Led_BVal.setOnSeekBarChangeListener(this);
		sb_data_Led_Brightness.setOnSeekBarChangeListener(this);
	
	}

	private void initDevice() {
		Intent intent = getIntent();
		mDevice = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
		mDevice.setListener(gizWifiDeviceListener);
		Log.i("Apptest", mDevice.getDid());
	}

	private String getDeviceName() {
		if (TextUtils.isEmpty(mDevice.getAlias())) {
			return mDevice.getProductName();
		}
		return mDevice.getAlias();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getStatusOfDevice();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(mRunnable);
		// 退出页面，取消设备订阅
		mDevice.setSubscribe(false);
		mDevice.setListener(null);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.sw_bool_Led_WarmSta:
			sendCommand(KEY_LED_WARMSTA, sw_bool_Led_WarmSta.isChecked());
			break;
		case R.id.sw_bool_LedOnOff:
			sendCommand(KEY_LEDONOFF, sw_bool_LedOnOff.isChecked());
			break;
		default:
			break;
		}
	}

	/*
	 * ========================================================================
	 * EditText 点击键盘“完成”按钮方法
	 * ========================================================================
	 */
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

		switch (v.getId()) {
		default:
			break;
		}
		hideKeyBoard();
		return false;

	}
	
	/*
	 * ========================================================================
	 * seekbar 回调方法重写
	 * ========================================================================
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		switch (seekBar.getId()) {
		case R.id.sb_data_Led_RVal:
			tv_data_Led_RVal.setText(formatValue((progress + LED_RVAL_OFFSET) * LED_RVAL_RATIO + LED_RVAL_ADDITION, 1));
			//if (updateInd == true) {
			//	sendCommand(KEY_LED_RVAL, (seekBar.getProgress() + LED_RVAL_OFFSET) * LED_RVAL_RATIO + LED_RVAL_ADDITION);
			//}

			break;
		case R.id.sb_data_Led_GVal:
			tv_data_Led_GVal.setText(formatValue((progress + LED_GVAL_OFFSET) * LED_GVAL_RATIO + LED_GVAL_ADDITION, 1));
			//if (updateInd == true) {
			//	sendCommand(KEY_LED_GVAL, (seekBar.getProgress() + LED_GVAL_OFFSET) * LED_GVAL_RATIO + LED_GVAL_ADDITION);
			//}

			break;
		case R.id.sb_data_Led_BVal:
			tv_data_Led_BVal.setText(formatValue((progress + LED_BVAL_OFFSET) * LED_BVAL_RATIO + LED_BVAL_ADDITION, 1));
			//if (updateInd == true) {
			//	sendCommand(KEY_LED_BVAL, (seekBar.getProgress() + LED_BVAL_OFFSET) * LED_BVAL_RATIO + LED_BVAL_ADDITION);
			//}

			break;
		case R.id.sb_data_Led_Brightness:
			tv_data_Led_Brightness.setText(formatValue((progress + LED_BRIGHTNESS_OFFSET) * LED_BRIGHTNESS_RATIO + LED_BRIGHTNESS_ADDITION, 1));
			//if (updateInd == true) {
			//	sendCommand(KEY_LED_BRIGHTNESS, (seekBar.getProgress() + LED_BRIGHTNESS_OFFSET) * LED_BRIGHTNESS_RATIO + LED_BRIGHTNESS_ADDITION);
			//}

			break;
		default:
			break;
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		switch (seekBar.getId()) {
		case R.id.sb_data_Led_RVal:
			sendCommand(KEY_LED_RVAL, (seekBar.getProgress() + LED_RVAL_OFFSET ) * LED_RVAL_RATIO + LED_RVAL_ADDITION);
			break;
		case R.id.sb_data_Led_GVal:
			sendCommand(KEY_LED_GVAL, (seekBar.getProgress() + LED_GVAL_OFFSET ) * LED_GVAL_RATIO + LED_GVAL_ADDITION);
			break;
		case R.id.sb_data_Led_BVal:
			sendCommand(KEY_LED_BVAL, (seekBar.getProgress() + LED_BVAL_OFFSET ) * LED_BVAL_RATIO + LED_BVAL_ADDITION);
			break;
		case R.id.sb_data_Led_Brightness:
			sendCommand(KEY_LED_BRIGHTNESS, (seekBar.getProgress() + LED_BRIGHTNESS_OFFSET ) * LED_BRIGHTNESS_RATIO + LED_BRIGHTNESS_ADDITION);
			break;
		default:
			break;
		}

	}

	/*
	 * ========================================================================
	 * 菜单栏
	 * ========================================================================
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.device_more, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_setDeviceInfo:
			setDeviceInfo();
			break;

		case R.id.action_getHardwareInfo:
			if (mDevice.isLAN()) {
				mDevice.getHardwareInfo();
			} else {
				myToast("只允许在局域网下获取设备硬件信息！");
			}
			break;

		case R.id.action_getStatu:
			mDevice.getDeviceStatus();
			break;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Description:根据保存的的数据点的值来更新UI
	 */
	protected void updateUI() {
		sw_bool_Led_WarmSta.setChecked(data_Led_WarmSta);
		sw_bool_LedOnOff.setChecked(data_LedOnOff);
		tv_data_Led_RVal.setText(data_Led_RVal + "");
		sb_data_Led_RVal.setProgress((int) ((data_Led_RVal - LED_RVAL_ADDITION) / LED_RVAL_RATIO - LED_RVAL_OFFSET));
		tv_data_Led_GVal.setText(data_Led_GVal + "");
		sb_data_Led_GVal.setProgress((int) ((data_Led_GVal - LED_GVAL_ADDITION) / LED_GVAL_RATIO - LED_GVAL_OFFSET));
		tv_data_Led_BVal.setText(data_Led_BVal + "");
		sb_data_Led_BVal.setProgress((int) ((data_Led_BVal - LED_BVAL_ADDITION) / LED_BVAL_RATIO - LED_BVAL_OFFSET));
		tv_data_Led_Brightness.setText(data_Led_Brightness + "");
		sb_data_Led_Brightness.setProgress((int) ((data_Led_Brightness - LED_BRIGHTNESS_ADDITION) / LED_BRIGHTNESS_RATIO - LED_BRIGHTNESS_OFFSET));

	}

	private void setEditText(EditText et, Object value) {
		et.setText(value.toString());
		et.setSelection(value.toString().length());
		et.clearFocus();
	}

	/**
	 * Description:页面加载后弹出等待框，等待设备可被控制状态回调，如果一直不可被控，等待一段时间后自动退出界面
	 */
	private void getStatusOfDevice() {
		// 设备是否可控
		if (isDeviceCanBeControlled()) {
			// 可控则查询当前设备状态
			mDevice.getDeviceStatus();
		} else {
			// 显示等待栏
			progressDialog.show();
			if (mDevice.isLAN()) {
				// 小循环10s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 10000);
			} else {
				// 大循环20s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 20000);
			}
		}
	}

	/**
	 * 发送指令,下发单个数据点的命令可以用这个方法
	 * 
	 * <h3>注意</h3>
	 * <p>
	 * 下发多个数据点命令不能用这个方法多次调用，一次性多次调用这个方法会导致模组无法正确接收消息，参考方法内注释。
	 * </p>
	 * 
	 * @param key
	 *            数据点对应的标识名
	 * @param value
	 *            需要改变的值
	 */
	private void sendCommand(String key, Object value) {
		if (value == null) {
			return;
		}
		int sn = 5;
		ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<String, Object>();
		hashMap.put(key, value);
		// 同时下发多个数据点需要一次性在map中放置全部需要控制的key，value值
		// hashMap.put(key2, value2);
		// hashMap.put(key3, value3);
		mDevice.write(hashMap, sn);
		Log.i("liang", "下发命令：" + hashMap.toString());
	}

	private boolean isDeviceCanBeControlled() {
		return mDevice.getNetStatus() == GizWifiDeviceNetStatus.GizDeviceControlled;
	}

	private void toastDeviceNoReadyAndExit() {
		Toast.makeText(this, "设备无响应，请检查设备是否正常工作", Toast.LENGTH_SHORT).show();
		finish();
	}

	private void toastDeviceDisconnectAndExit() {
		Toast.makeText(GosDeviceControlActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
		finish();
	}

	/**
	 * 展示设备硬件信息
	 * 
	 * @param hardwareInfo
	 */
	private void showHardwareInfo(String hardwareInfo) {
		String hardwareInfoTitle = "设备硬件信息";
		new AlertDialog.Builder(this).setTitle(hardwareInfoTitle).setMessage(hardwareInfo)
				.setPositiveButton(R.string.besure, null).show();
	}

	/**
	 * Description:设置设备别名与备注
	 */
	private void setDeviceInfo() {

		final Dialog mDialog = new AlertDialog.Builder(this,R.style.edit_dialog_style).setView(new EditText(this)).create();
		mDialog.show();

		Window window = mDialog.getWindow();
		window.setContentView(R.layout.alert_gos_set_device_info);
		WindowManager.LayoutParams layoutParams = window.getAttributes();
		layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		window.setAttributes(layoutParams);
		final EditText etAlias;
		final EditText etRemark;
		etAlias = (EditText) window.findViewById(R.id.etAlias);
		etRemark = (EditText) window.findViewById(R.id.etRemark);

		LinearLayout llNo, llSure;
		llNo = (LinearLayout) window.findViewById(R.id.llNo);
		llSure = (LinearLayout) window.findViewById(R.id.llSure);

		if (!TextUtils.isEmpty(mDevice.getAlias())) {
			setEditText(etAlias, mDevice.getAlias());
		}
		if (!TextUtils.isEmpty(mDevice.getRemark())) {
			setEditText(etRemark, mDevice.getRemark());
		}

		llNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDialog.dismiss();
			}
		});

		llSure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(etRemark.getText().toString())
						&& TextUtils.isEmpty(etAlias.getText().toString())) {
					myToast("请输入设备别名或备注！");
					return;
				}
				mDevice.setCustomInfo(etRemark.getText().toString(), etAlias.getText().toString());
				mDialog.dismiss();
				String loadingText = (String) getText(R.string.loadingtext);
				progressDialog.setMessage(loadingText);
				progressDialog.show();
			}
		});

		mDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				hideKeyBoard();
			}
		});
	}
	
	/*
	 * 获取设备硬件信息回调
	 */
	@Override
	protected void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device,
			ConcurrentHashMap<String, String> hardwareInfo) {
		super.didGetHardwareInfo(result, device, hardwareInfo);
		StringBuffer sb = new StringBuffer();
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS != result) {
			myToast("获取设备硬件信息失败：" + result.name());
		} else {
			sb.append("Wifi Hardware Version:" + hardwareInfo.get(WIFI_HARDVER_KEY) + "\r\n");
			sb.append("Wifi Software Version:" + hardwareInfo.get(WIFI_SOFTVER_KEY) + "\r\n");
			sb.append("MCU Hardware Version:" + hardwareInfo.get(MCU_HARDVER_KEY) + "\r\n");
			sb.append("MCU Software Version:" + hardwareInfo.get(MCU_SOFTVER_KEY) + "\r\n");
			sb.append("Wifi Firmware Id:" + hardwareInfo.get(WIFI_FIRMWAREID_KEY) + "\r\n");
			sb.append("Wifi Firmware Version:" + hardwareInfo.get(WIFI_FIRMWAREVER_KEY) + "\r\n");
			sb.append("Product Key:" + "\r\n" + hardwareInfo.get(PRODUCT_KEY) + "\r\n");

			// 设备属性
			sb.append("Device ID:" + "\r\n" + mDevice.getDid() + "\r\n");
			sb.append("Device IP:" + mDevice.getIPAddress() + "\r\n");
			sb.append("Device MAC:" + mDevice.getMacAddress() + "\r\n");
		}
		showHardwareInfo(sb.toString());
	}
	
	/*
	 * 设置设备别名和备注回调
	 */
	@Override
	protected void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
		super.didSetCustomInfo(result, device);
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
			myToast("设置成功");
			progressDialog.cancel();
			finish();
		} else {
			myToast("设置失败：" + result.name());
		}
	}

	/*
	 * 设备状态改变回调，只有设备状态为可控才可以下发控制命令
	 */
	@Override
	protected void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
		super.didUpdateNetStatus(device, netStatus);
		if (netStatus == GizWifiDeviceNetStatus.GizDeviceControlled) {
			mHandler.removeCallbacks(mRunnable);
			progressDialog.cancel();
		} else {
			mHandler.sendEmptyMessage(handler_key.DISCONNECT.ordinal());
		}
	}
	
	/*
	 * 设备上报数据回调，此回调包括设备主动上报数据、下发控制命令成功后设备返回ACK
	 */
	@Override
	protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
			ConcurrentHashMap<String, Object> dataMap, int sn) {
		super.didReceiveData(result, device, dataMap, sn);
		Log.i("liang", "接收到数据");
		if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS && dataMap.get("data") != null) {
			getDataFromReceiveDataMap(dataMap);
			mHandler.sendEmptyMessage(handler_key.UPDATE_UI.ordinal());
		}
	}

}