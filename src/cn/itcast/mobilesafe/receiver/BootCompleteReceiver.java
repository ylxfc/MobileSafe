package cn.itcast.mobilesafe.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;

public class BootCompleteReceiver extends BroadcastReceiver {
    private SharedPreferences sp = null;
	@Override
	public void onReceive(Context context, Intent intent) {
		sp = context.getSharedPreferences("config", context.MODE_PRIVATE);
        boolean isProtected = sp.getBoolean("isProtected", false);
        //判断是否开启保护
        System.out.println("xxxx发送报警短信");
        if(isProtected){
        	//开启后判断SIM卡是否发生了改变
        	String sim = sp.getString("sim", "");
        	TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        	String currentsim = manager.getSimSerialNumber();
        	if(!currentsim.equals(sim)){
        		//sim卡串号不同发送报警短信
        		System.out.println("发送报警短信");
        		SmsManager sms = SmsManager.getDefault();
        		String safetyTel = sp.getString("tel", "");
        		sms.sendTextMessage(safetyTel, null, "sim ka fa sheng le gai bian", null, null);
        	}
        }
	}

}

































