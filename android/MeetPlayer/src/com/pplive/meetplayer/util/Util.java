package com.pplive.meetplayer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.pplive.media.MeetSDK;
import android.pplive.media.player.MediaInfo;
import android.pplive.media.player.TrackInfo;
import android.util.Log;

import com.pplive.meetplayer.ui.PPTVEpisodeActivity;
import com.pplive.sdk.MediaSDK;

public class Util {
	private final static String TAG = "Util";
	
	private final static String PREF_NAME = "settings";
	
	public static boolean startP2PEngine(Context context) {
		Log.d("Util", "startP2PEngine()");
		if (context == null) {
			return false;
		}

		String gid = "12";
		String pid = "161";
		String auth = "08ae1acd062ea3ab65924e07717d5994";

		String libPath = "/data/data/com.pplive.meetplayer/lib";
		MediaSDK.libPath = libPath;
		MediaSDK.logPath = "/data/data/com.pplive.meetplayer/cache";
		MediaSDK.logOn = false;
		MediaSDK.setConfig("", "HttpManager", "addr", "127.0.0.1:9106+");
		MediaSDK.setConfig("", "RtspManager", "addr", "127.0.0.1:5156+");
		
		Properties props = System.getProperties();
        String osArch = props.getProperty("os.arch");
        if (osArch != null && osArch.contains("aarch64"))
        	MediaSDK.libName = "ppbox_jni-armandroid-r4-gcc44-mt-1.1.0";
		
		long ret = -1;

		try {
			ret = MediaSDK.startP2PEngine(gid, pid, auth);
		} catch (Throwable e) {
			Log.e(TAG, e.toString());
		}

		Log.i(TAG, "Java: startP2PEngine result " + ret);
		return (ret != -1);// 端口占用&& ret != 9);
	}
	
	public static boolean initMeetSDK(Context ctx) {
		// upload util will upload /data/data/pacake_name/Cache/xxx
		// so must NOT change path
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PlayerPolicy.xml";
		File file = new File(path);
		if (file.exists()) {
			try{
				FileInputStream fin = new FileInputStream(file);
				Log.i(TAG, "Java: PlayerPolicy file size: " + fin.available());
				
				byte[] buf = new byte[fin.available()];
				int readed = fin.read(buf);
				Log.i(TAG, "Java: PlayerPolicy read size: " + readed);
				String xml = new String(buf);
				Log.i(TAG, "Java: PlayerPolicy xml: " + xml);
				MeetSDK.setPlayerPolicy(new String(buf));
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, "file not found");
			}
			catch(Exception e){   
				e.printStackTrace();
				Log.e(TAG, "an error occured while load policy", e);
			}
		}
		
		MeetSDK.setLogPath(
				ctx.getCacheDir().getAbsolutePath() + "/meetplayer.log", 
				ctx.getCacheDir().getParentFile().getAbsolutePath() + "/");
		// /data/data/com.svox.pico/
		return MeetSDK.initSDK(ctx, "");
	}
	
	public static boolean writeSettings(Context ctx, String key, String value) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(key, value);
    	return editor.commit();
	}
	
	public static String readSettings(Context ctx, String key) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	return settings.getString(key, "");
	}
	
	public static boolean writeSettingsInt(Context ctx, String key, int value) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(key, value);
    	return editor.commit();
	}
	
	public static int readSettingsInt(Context ctx, String key) {
		SharedPreferences settings = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // create it if NOT exist
    	return settings.getInt(key, 0);
	}
	
	public static void add_sohuvideo_history(Context ctx, String title, int vid, long aid, int site) {
		String key = "SohuPlayHistory";
		String regularEx = ",";
		final int save_max_count = 20;
		String value = readSettings(ctx, key);
		
		List<String> playHistoryList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, regularEx, false);
        while (st.hasMoreElements()) {
        	String token = st.nextToken();
        	playHistoryList.add(token);
        }
        
        String new_video = String.format("%s|%d|%d|%d", title, vid, aid, site);
        
        int count = playHistoryList.size();
        StringBuffer sb = new StringBuffer();
        int start = count - save_max_count + 1;
        if (start < 0)
        	start = 0;
        
        boolean isNewVideo = true;
        for (int i = start; i<count ; i++) {
        	String item = playHistoryList.get(i);
        	if (new_video.contains(item) && isNewVideo)
        		isNewVideo = false;
        	
        	sb.append(item);
        	sb.append(regularEx);
        }
        
        if (isNewVideo)
        	sb.append(new_video);
        else
        	Log.i(TAG, String.format("Java %s already in history list", new_video));
        
		writeSettings(ctx, key, sb.toString());
	}
	
	public static void add_pptvvideo_history(Context ctx, String title, int playlink, int ft) {
		String key = "PPTVPlayHistory";
		String regularEx = ",";
		final int save_max_count = 20;
		String value = readSettings(ctx, key);
		
		List<String> playHistoryList = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, regularEx, false);
        while (st.hasMoreElements()) {
        	String token = st.nextToken();
        	playHistoryList.add(token);
        }
        
        String new_video = String.format("%s|%d|%d", title, playlink, ft);
        
        int count = playHistoryList.size();
        StringBuffer sb = new StringBuffer();
        int start = count - save_max_count + 1;
        if (start < 0)
        	start = 0;
        
        boolean isNewVideo = true;
        for (int i = start; i<count ; i++) {
        	String item = playHistoryList.get(i);
        	if (new_video.contains(item) && isNewVideo)
        		isNewVideo = false;
        	
        	sb.append(item);
        	sb.append(regularEx);
        }
        
        if (isNewVideo)
        	sb.append(new_video);
        else
        	Log.i(TAG, String.format("Java %s already in history list", new_video));
        
		writeSettings(ctx, key, sb.toString());
	}
	
	public static String read_file(String path, String encode) {
		if (path == null || encode == null)
			return null;
		
		File file = new File(path);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		
		InputStreamReader inputStreamReader = null;  
	    try {  
	        inputStreamReader = new InputStreamReader(fis, encode);  
	    } catch (UnsupportedEncodingException e) {  
	        e.printStackTrace();  
	    }  
	    
	    BufferedReader reader = new BufferedReader(inputStreamReader);  
	    StringBuffer sb = new StringBuffer("");  
	    String line;  
	    try {  
	        while ((line = reader.readLine()) != null) {  
	            sb.append(line);  
	            sb.append("\n");  
	        }  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    }  
	    
	    try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    return sb.toString();  
	}
	
	public static boolean IsHaveInternet(final Context context) { 
        try { 
            ConnectivityManager manger = (ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE); 
 
            NetworkInfo info = manger.getActiveNetworkInfo(); 
            return (info != null && info.isConnected()); 
        } catch (Exception e) { 
        	e.printStackTrace();
            return false; 
        } 
    } 
	
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetworkInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return wifiNetworkInfo.isConnected();
	}
	
	public static boolean isLANConnected(Context context){
	        ConnectivityManager connectivityManager =(ConnectivityManager) context
	        		.getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo lanNetworkInfo = connectivityManager
	        		.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
	        return lanNetworkInfo.isConnected();
	}
	
	public static String getWifiIpAddr(Context context) {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return intToIp(ipAddress);
	}
	
	public static String getIpAddr(Context context) {
		try {
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			while (en.hasMoreElements()) {
				NetworkInterface intf = en.nextElement();
				if (intf.getName().toLowerCase().equals("eth0")
						|| intf.getName().toLowerCase().equals("wlan0")) {
					for (Enumeration<InetAddress> enumIpAddr = intf
							.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							String ipaddress = inetAddress.getHostAddress().toString();
							if (!ipaddress.contains("::")) {// ipV6的地址
								return ipaddress;
							}
						}
					}
				} else {
					continue;
				}
			}
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void copyFile(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			if (oldfile.exists()) { // 文件存在时
				InputStream inStream = new FileInputStream(oldPath); // 读入原文件
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread; // 字节数 文件大小
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
				fs.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "copy single file error");
			e.printStackTrace();
		}

	}
	
	public static String getMediaInfoDescription(String path, MediaInfo info) {
    	StringBuffer sbInfo = new StringBuffer();

    	sbInfo.append("文件名 ");
    	sbInfo.append(path);
    	
    	sbInfo.append("\n分辨率 ");
    	sbInfo.append(info.getWidth());
    	sbInfo.append(" x ");
    	sbInfo.append(info.getHeight());
    	sbInfo.append("\n时长 ");
    	sbInfo.append(msecToString(info.getDuration()));
    	sbInfo.append("\n帧率 ");
    	sbInfo.append(String.format("%.2f", info.getFrameRate()));
    	sbInfo.append("\n码率 ");
    	sbInfo.append(info.getBitrate() / 1024);
    	sbInfo.append(" kbps");
    	if (info.getVideoCodecName() != null) {
    		sbInfo.append("\n视频编码 ");
    		sbInfo.append(info.getVideoCodecName());
    	}
    	String vProfile = info.getVideoCodecProfile();
    	if (vProfile != null) {
    		sbInfo.append("(profile ");
    		sbInfo.append(vProfile);
    		sbInfo.append(")");
    	}
    	
    	sbInfo.append("\n音轨数 ");
    	int audioNum = info.getAudioChannels();
    	sbInfo.append(audioNum);
    	
    	if (audioNum > 0) {
    		sbInfo.append("\n音轨编码 ");
    		List<TrackInfo> audioList = info.getAudioChannelsInfo();
    		if (audioList != null) {
	    		for (int i=0;i<audioNum;i++) {
	    			if (i > 0)
	    				sbInfo.append(" | ");
	    			
	    			sbInfo.append(audioList.get(i).getCodecName());
	    			String profile = audioList.get(i).getCodecProfile();
	    			if (profile != null) {
	    	    		sbInfo.append("(profile ");
	    	    		sbInfo.append(profile);
	    	    		sbInfo.append(")");
	    	    	}
	    		}
    		}
    	}
    	
    	sbInfo.append("\n内嵌字幕数 ");
    	int subtitleNum = info.getSubtitleChannels();
    	sbInfo.append(subtitleNum);

    	if (subtitleNum > 0) {
    		sbInfo.append("\n字幕编码 ");
    		List<TrackInfo> subtitleList = info.getSubtitleChannelsInfo();
    		if (subtitleList != null) {
	    		for (int i=0;i<subtitleNum;i++) {
	    			if (i > 0)
	    				sbInfo.append(" | ");
	    			
	    			sbInfo.append(subtitleList.get(i).getCodecName());
	    		}
    		}
    	}
    	
    	Map<String, String> metadata = info.getMetaData();
    	if (metadata != null) {
    		sbInfo.append("\n容器元数据\n");
    		Iterator<Map.Entry<String, String>> iter = metadata.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
				String key = entry.getKey();
				String val = entry.getValue();
				sbInfo.append(key);
				sbInfo.append(":   ");
				sbInfo.append(val);
				sbInfo.append("\n");
			}
    	}

		return sbInfo.toString();
	}
	
	public void copyFolder(String oldPath, String newPath) {

		try {
			(new File(newPath)).mkdirs(); // 如果文件夹不存在 则建立新文件夹
			File a = new File(oldPath);
			String[] file = a.list();
			File temp = null;
			for (int i = 0; i < file.length; i++) {
				if (oldPath.endsWith(File.separator)) {
					temp = new File(oldPath + file[i]);
				} else {
					temp = new File(oldPath + File.separator + file[i]);
				}

				if (temp.isFile()) {
					FileInputStream input = new FileInputStream(temp);
					FileOutputStream output = new FileOutputStream(newPath
							+ "/" + (temp.getName()).toString());
					byte[] b = new byte[1024 * 5];
					int len;
					while ((len = input.read(b)) != -1) {
						output.write(b, 0, len);
					}
					output.flush();
					output.close();
					input.close();
				}
				if (temp.isDirectory()) {// 如果是子文件夹
					copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "copy folder error");
			e.printStackTrace();

		}
	}
	
	private static String msecToString(long msec) {
		long msec_, sec, minute, hour, tmp;
		msec_ = msec % 1000;
		sec = msec / 1000;

		// sec = 3710
		tmp = sec % 3600; // 110
		hour = sec / 3600; // 1
		sec = tmp % 60; // 50
		minute = tmp / 60; // 1
		return String.format("%02d:%02d:%02d:%03d", hour, minute, sec, msec_);
	}
	
	private static String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}
}
