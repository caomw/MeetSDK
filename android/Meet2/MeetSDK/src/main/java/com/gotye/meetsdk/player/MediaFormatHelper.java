package com.gotye.meetsdk.player;

import java.lang.reflect.Constructor;
import java.util.Map;

import android.media.MediaFormat;
import com.gotye.meetsdk.util.LogUtils;

class MediaFormatHelper {
	
	static MediaFormat createMediaFormatFromMap(Map<String, Object> map) {
		MediaFormat format = null;
		
		try {
			Class<?> clazz = Class.forName("android.media.MediaFormat");
			Class<?>[] params = new Class[]{Map.class};
			Constructor<?> constructor = clazz.getDeclaredConstructor(params);
			constructor.setAccessible(true);
			
			format = (MediaFormat) constructor.newInstance(new Object[]{map});
		} catch (Exception e) {
			e.printStackTrace();
		    LogUtils.error("Exception", e);
		}
		
		return format;
	}
}
