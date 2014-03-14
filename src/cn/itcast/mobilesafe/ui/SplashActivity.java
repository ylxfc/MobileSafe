package cn.itcast.mobilesafe.ui;

import java.io.File;

import cn.itcast.mobilesafe.R;
import cn.itcast.mobilesafe.domain.UpdataInfo;
import cn.itcast.mobilesafe.engine.DownLoadFileTask;
import cn.itcast.mobilesafe.engine.UpdataInfoService;
import cn.itcast.mobilesafe.service.MonitorService;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity {
	private LinearLayout ll = null;
	private TextView tv_splash = null;
	private UpdataInfo info = null;
	private ProgressDialog pd = null;
	private String version = null;
	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			isNeedUpdate();
			super.handleMessage(msg);
		}
		
	};
		
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.kn_app_start);
		//开始就开启检测服务
		Intent intent= new Intent(this,MonitorService.class); 
		startService(intent);
		version = getVession();
		ll = (LinearLayout) findViewById(R.id.ll_splash);
		tv_splash = (TextView) findViewById(R.id.tv_splash);
		pd = new ProgressDialog(this);
		pd.setTitle("正在下载.....");
		//设置样式水平前进的样式
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		tv_splash.setText(version);
		new Thread(){

			@Override
			public void run() {
				try {
					sleep(2000);
					
					handler.sendEmptyMessage(0);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				super.run();
			}
			
		}.start();
		
		
		
		AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
		aa.setDuration(2000);
		ll.startAnimation(aa);
		//完成窗体的全屏显示
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
	}
	private void isNeedUpdate() {
		UpdataInfoService service = new UpdataInfoService(this);
		try {
		  info = service.getUpdataInfo(R.string.updataURL);
			/**
			 * 版本号相同
			 */
			if(info.getVersion().equals(version)){
				System.out.println("版本相同直接进入主界面");
				 mainUI();
			}else{
				showUpdataDialog();
			}
		} catch (Exception e) {
			 mainUI();
			e.printStackTrace();
		}
	}
    /**
     * show出对话框
     */
	private void showUpdataDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("版本升级");
		builder.setMessage(info.getDescription());
		builder.setCancelable(false);
		builder.setPositiveButton("是", new OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				System.out.println("升级");
				//判断SD卡状态是否可用
				
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					//下载是耗时的工作，需要放入到子线程里面去执行
					DownLoadFileThreadTask download = new DownLoadFileThreadTask(info.getApkurl(),"/sdcard/new.apk");
				    pd.show();
				    new Thread(download).start();
				    mainUI();
				}else{
					
				}
				
			}
			
		});
		builder.setNegativeButton("否", new OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				System.out.println("不升级，进入主界面");
				 mainUI();
			}
			
		});
		builder.create().show();
	}
	/**
	 *  安装apk
	 *  
	 */
	private void installAPK(File file){
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		finish();
		startActivity(intent);
	}
/**
 * 下载APK
 */
	private class DownLoadFileThreadTask implements Runnable{
		String apkurl;
		String fileurl;
		public DownLoadFileThreadTask(String apkurl, String fileurl) {
			
			this.apkurl = apkurl;
			this.fileurl = fileurl;
		}
		public void run() {
			DownLoadFileTask down = new DownLoadFileTask();
			try {
				File file = down.getFile(apkurl, fileurl, pd);
				System.out.println("下载完成，进行安装");
				pd.dismiss();
				installAPK(file);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "下载文件失败", 0).show();
				pd.dismiss();
				mainUI();
			}
		}
	}
	/**
	 * 获得版本号
	 */
	private String getVession() {

		try {
			PackageManager pm = getPackageManager();
			// 第一个参数获得包的名字
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
/**
 * 进入主界面
 */
	private void mainUI(){
		Intent intetn = new Intent(this,MainActivity.class);
		startActivity(intetn);
		finish();
	};
}
