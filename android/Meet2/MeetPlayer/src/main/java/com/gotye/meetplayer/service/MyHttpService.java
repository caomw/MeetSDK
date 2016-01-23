package com.gotye.meetplayer.service;

import java.util.Random;

import com.gotye.common.util.MyNanoHTTPD;

import fi.iki.elonen.NanoHTTPD;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyHttpService extends Service {
	private final static String TAG = "HttpService";
	private static int HTTP_PORT = 8011;
	
	private NanoHTTPD nanoHttpd;
	
	public static int getPort() {
		return HTTP_PORT;
	}
	
	@Override  
    public void onCreate() {  
        Log.i(TAG, "Java: HttpService onCreate");  
        super.onCreate();
        
        Random rand =new Random();
		HTTP_PORT = 8800 + rand.nextInt(100);
		Log.i(TAG, "Java: http port: " + HTTP_PORT);
        
        nanoHttpd = new MyNanoHTTPD(this, HTTP_PORT, null);
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.i(TAG, String.format("Java: HttpService onStartCommand() %d %d", flags, startId)); 
		
		try {  
			if (nanoHttpd.isAlive())
				nanoHttpd.stop();
			
            nanoHttpd.start();  
            Log.i(TAG, "Java: MyNanoHTTPD started");
        } catch(Exception e) {  
            e.printStackTrace();
            Log.e(TAG, "Java: MyNanoHTTPD failed to start");
        }  
		
		return Service.START_STICKY;
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

    	Log.i(TAG, "Java: HttpService onDestroy()");
    	
    	if (nanoHttpd.isAlive())
			nanoHttpd.stop();
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
	
