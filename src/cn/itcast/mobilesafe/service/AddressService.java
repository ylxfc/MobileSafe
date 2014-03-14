package cn.itcast.mobilesafe.service;

import java.lang.reflect.Method;

import com.android.internal.telephony.ITelephony;

import cn.itcast.mobilesafe.R;
import cn.itcast.mobilesafe.db.dao.BlackNumberDao;
import cn.itcast.mobilesafe.engine.NumberAddressService2;
import cn.itcast.mobilesafe.ui.CallInterceptRecode;
import cn.itcast.mobilesafe.uitl.TimeTransition;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AddressService extends Service {
	private TelephonyManager manager = null;
	private MyPhoneListener listener;
	//private TextView view = null;
	private BlackNumberDao dao = null;
	private SharedPreferences sp = null;
	private WindowManager windowManager = null;
	private View view = null;
	private long firstRingTime;
	private long endRingTime;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 开启查询电话号码地址服务
	 */
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		// 注册系统电话管理服务的监听器
		listener = new MyPhoneListener();
		manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		manager.listen(listener,
				PhoneStateListener.LISTEN_CALL_STATE);
		dao = new BlackNumberDao(this);
		sp = getSharedPreferences("config", Context.MODE_PRIVATE);
	}

	private class MyPhoneListener extends PhoneStateListener {
		/**
		 * 电话状态发生改变调用的方法
		 */
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			// 第二个参数是来电的电话号码
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			/**
			 * 处于静止状态，没有呼叫
			 */
			case TelephonyManager.CALL_STATE_IDLE:
				endRingTime = System.currentTimeMillis();
				long time = endRingTime - firstRingTime;
				if(time<5000){
					System.out.println("响一声电话");
					showNotification(incomingNumber);
				}
				if(view!=null){
					windowManager.removeView(view);
					view = null;
					}
				//没有呼叫的时候在次获取系统时间，可以判断是否是骚扰电话1322毫秒
				break;
				/**
				 * 零响的状态
				 */
			case TelephonyManager.CALL_STATE_RINGING:
				//查询黑名单，查询出来，就直接挂断电话
				firstRingTime = System.currentTimeMillis();
				if(dao.find(incomingNumber)){
					System.out.println("查询出了黑名单"+incomingNumber);
					endCall();
					//注册一个内容观察者 观察call_log的uri的信息 ,第一个参数是在哪一个URI上面去注册内容观察者
					//第三个参数是，当注册到CallLog.Calls.CONTENT_URI这个URI上面时候，用哪个观察者去观察
				    getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, new Myobserver(new Handler(),incomingNumber));	
				}
               String address = NumberAddressService2.getAddress(incomingNumber);
               showLoaction(address);
               //当零响的时候获取系统时间
				break;
				/**
				 * 接通电话的状态
				 */
			case TelephonyManager.CALL_STATE_OFFHOOK:
				if(view!=null){
					windowManager.removeView(view);
					view = null;
					}
				break;
			}
		}
/**
 * 挂断电话
 */
		private void endCall() {
			try {
				Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
				IBinder binder = (IBinder)method.invoke(null, new Object[]{TELEPHONY_SERVICE});
				ITelephony telephony = ITelephony.Stub.asInterface(binder);
				telephony.endCall();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	/**
	 * notification状态栏
	 */
	public void showNotification(String incomingNumber){
		//1创建一个notification的对象
	NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//2当信息发送过来状态栏上面显示的信息,把一个要显示的notification对象创建出来
	int icon = R.drawable.main_icon_36;
	String tickerText = "发现响一声电话号码";
	long when = System.currentTimeMillis();
	Notification notification = new Notification(icon, tickerText, when);
		//3当拉下来显示的信息,配置notification的一些参数
	Context context = getApplicationContext();
	String contentTitle = "响一声号码";
	String address = NumberAddressService2.getAddress(incomingNumber);
	String contentText = incomingNumber+"("+address+")";
	//点击notification自动清空
	notification.flags = Notification.FLAG_AUTO_CANCEL;
	Intent notificationIntent = new Intent(this,CallInterceptRecode.class);
	//把拦截到的电话号码，地址，时间，发送到电话拦截记录的activity
	notificationIntent.putExtra("incomingNumber", incomingNumber);
	//获取系统拦截时间
//	long systemTime = System.currentTimeMillis();
	notificationIntent.putExtra("systemTime", TimeTransition.getTime(when+""));
	//查询出电话号码的归属地
	notificationIntent.putExtra("address", address);
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	//收到notification播放音乐
	notification.sound = Uri.parse(Environment.getExternalStorageDirectory()+"/"+"alarm.wav");
	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	// 4. 通过manger把notification 激活
	manager.notify(0, notification);
	}
/**
 * 服务停止的时候调用的方法
 */
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		manager.listen(listener, PhoneStateListener.LISTEN_NONE);
		listener = null;
	}
	/**
	 * 把View对象挂载到窗体上面
	 */
	public void showLoaction(String address) {
		WindowManager.LayoutParams params = new LayoutParams();
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.format = PixelFormat.TRANSLUCENT;
        params.type = WindowManager.LayoutParams.TYPE_TOAST;
        params.setTitle("Toast");
        //默认挂载到窗体是中间，首先获取位置，在把保存的x和Y轴设置进来
        params.gravity = Gravity.LEFT | Gravity.TOP;

        params.x =     sp.getInt("lastx", 0);
        params.y =     sp.getInt("lasty", 0);
//         view = new TextView(AddressService.this);
//        view.setText(address);
        view = View.inflate(this, R.layout.show_location, null);
        view.findViewById(R.id.iv_show_location);
        TextView tv_show_location = (TextView) view.findViewById(R.id.tv_show_location);
        tv_show_location.setText(address);
        ImageView iv_show_location = (ImageView) view.findViewById(R.id.iv_show_location);
        LinearLayout ll = (LinearLayout) view.findViewById(R.id.ll_location);
        int colour = sp.getInt("colour", 0);
        if(colour==1){
        	ll.setBackgroundResource(R.drawable.call_locate_gray);
        }else if(colour==2){
        	ll.setBackgroundResource(R.drawable.call_locate_orange);
        }else if(colour==3){
        	ll.setBackgroundResource(R.drawable.call_locate_blue);
        }else if(colour==4){
        	ll.setBackgroundResource(R.drawable.call_locate_white);
        }else if(colour==5){
        	ll.setBackgroundResource(R.drawable.call_locate_green);
        }
        
        windowManager =  (WindowManager) this.getSystemService(WINDOW_SERVICE);
       windowManager.addView(view, params);
	}
	private class Myobserver extends ContentObserver{
		private String incomingnumber;
		public Myobserver(Handler handler,String incomingnumber) {
			super(handler);
			this.incomingnumber = incomingnumber;
		}
		/**
		 * 当里面的内容发生改变就执行的方法
		 */
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			deleteCallLog(incomingnumber);
			
			//当删除了呼叫记录后 反注册内容观察者
			getContentResolver().unregisterContentObserver(this);
		}
		/**
		 * 根据电话号码删除呼叫记录
		 * @param incomingNumber 要删除呼叫记录的号码
		 */
		private void deleteCallLog(String incomingnumber2) {
			ContentResolver resolver = getContentResolver();
			Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null, "number=?", new String[]{incomingnumber2}, null);
		    if(cursor.moveToNext()){
		    	String id = cursor.getString(cursor.getColumnIndex("_id"));
		    	resolver.delete(CallLog.Calls.CONTENT_URI, "_id=?",new String[]{id});
		    }
		}
	}
}
















