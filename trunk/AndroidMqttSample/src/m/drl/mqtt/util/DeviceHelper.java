package m.drl.mqtt.util;

import java.util.List;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

public class DeviceHelper {
	private String mImei;
	private String mSIM;
	String mMobileVersion;
	List<NeighboringCellInfo> mCellinfos;
	String mNetwrokIso;
	Context mContext;
	String mNetType;
	/**
	 * 
	 * */
	public static String UA = Build.MODEL;
	public DeviceHelper(Context context) {
		mContext = context;
		findData();
	}
	/**
	 * 
	 * 设置手机立刻震动
	 * */
	public static void Vibrate(Context context,long milliseconds)
	{
		Vibrator vib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
		vib.vibrate(milliseconds);
	}
	TelephonyManager mTm = null;
	private void findData() {
		 mTm = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		setImei(mTm.getDeviceId());
		mMobileVersion = mTm.getDeviceSoftwareVersion();
		mCellinfos = mTm.getNeighboringCellInfo();
		mNetwrokIso = mTm.getNetworkCountryIso();
		setSIM(mTm.getSimSerialNumber());
		//
		try{
			ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);    
			NetworkInfo info = cm.getActiveNetworkInfo();  
			// WIFI/MOBILE 
			mNetType = info.getTypeName(); 
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public void updataDeviceInfo() {
		findData();
	}

	private static DeviceHelper INSTANCE = null;

	public static synchronized DeviceHelper getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new DeviceHelper(context);
		}
		return INSTANCE;
	}

	/**
	 * 获得android设备-唯一标识，android2.2 之前无法稳定运行.
	 * */
	public static String getDeviceId(Context mCm) {
		return Secure.getString(mCm.getContentResolver(), Secure.ANDROID_ID);
	}
	public  String getDeviceId()
	{
		return Secure.getString(mContext.getContentResolver(),  Secure.ANDROID_ID);
	}

	public String getImei() {
		return mImei;
	}

	public void setImei(String mImei) {
		this.mImei = mImei;
	}

	public String getSIM() {
		return mSIM;
	}

	public void setSIM(String mSIM) {
		this.mSIM = mSIM;
	}
	public String getUA()
	{
		return UA;
	}
	
	
	public String getSimState()
	{
		switch (mTm.getSimState()) {
		case android.telephony.TelephonyManager.SIM_STATE_UNKNOWN:
			return "未知SIM状�?_"+android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;
		case android.telephony.TelephonyManager.SIM_STATE_ABSENT:
			return "没插SIM卡_"+android.telephony.TelephonyManager.SIM_STATE_ABSENT;
		case android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED:
			return "锁定SIM状�?_�?��用户的PIN码解锁_"+ android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED;
		case android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED:
			return "锁定SIM状�?_�?��用户的PUK码解锁_"+android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED;
		case android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			return "锁定SIM状�?_�?��网络的PIN码解锁_"+android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED;
		case android.telephony.TelephonyManager.SIM_STATE_READY:
			return "就绪SIM状�?_"+android.telephony.TelephonyManager.SIM_STATE_READY;
		default:
			return "未知SIM状�?_"
					+ android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;

		}
	}
	public String getPhoneType()
	{
		switch (mTm.getPhoneType()) {
		case android.telephony.TelephonyManager.PHONE_TYPE_NONE:
			return "PhoneType: 无信号_"+android.telephony.TelephonyManager.PHONE_TYPE_NONE;
		case android.telephony.TelephonyManager.PHONE_TYPE_GSM:
			return "PhoneType: GSM信号_"+android.telephony.TelephonyManager.PHONE_TYPE_GSM;
		case android.telephony.TelephonyManager.PHONE_TYPE_CDMA:
			return "PhoneType: CDMA信号_"+android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
		default:
			return "PhoneType: 无信号_"+android.telephony.TelephonyManager.PHONE_TYPE_NONE;
		}
	}

	/**
	 * 服务商名称：例如：中国移动�?联�? �??
	 * SIM卡的状�?必须�?	 * SIM_STATE_READY(使用getSimState()判断). �??
	 */
	public String getSimOpertorName()
	{
		if(mTm.getSimState()==android.telephony.TelephonyManager.SIM_STATE_READY){
			StringBuffer sb = new StringBuffer();
//			sb.append("SimOperatorName: ").append(mTm.getSimOperatorName());
//			sb.append("\n");
			sb.append("SimOperator: ").append(mTm.getSimOperator());
			sb.append("\n");
			sb.append("Phone:").append(mTm.getLine1Number());
			return sb.toString();
		}else {
			StringBuffer sb = new StringBuffer();
			sb.append("SimOperatorName: ").append("未知");
			sb.append("\n");
			sb.append("SimOperator: ").append("未知");
			return sb.toString();
		}
	}
	
	public String getPhoneSettings()
	{
		StringBuffer buf = new StringBuffer();
		String  str=Secure.getString(mContext.getContentResolver(), Secure.BLUETOOTH_ON);
		buf.append("蓝牙:");
		if(str.equals("0")){
			buf.append("禁用");
		}else {
			buf.append("�?��");
		}
		//
		buf.append("\n");
		str=Secure.getString(mContext.getContentResolver(), Secure.WIFI_ON);
		buf.append("WIFI:");
		buf.append(str);
		
		buf.append("\n");
		str=Secure.getString(mContext.getContentResolver(), Secure.INSTALL_NON_MARKET_APPS);
		buf.append("APP位置来源:");
		buf.append(str);
		buf.append("\n");
		return buf.toString();
	}
	
	
}
