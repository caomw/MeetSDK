package android.pplive.media.player;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaFormat;
import android.pplive.media.util.LogUtils;

public class FFMediaExtractor implements MediaExtractable {
	
	private final static String TAG = "FFMediaExtractor";

	private static boolean inited = false;
	public static boolean initExtrator() {
		if (inited)
			return true;
		
		inited = native_init();
		return inited;
	}
	
	public FFMediaExtractor(Object mediaplayer_this) {
        LogUtils.info("setup FFMediaExtractor");
		
		setup(mediaplayer_this);
	}

	private static native boolean native_init();
	
	@Override
	public native boolean advance();

	@Override
	public native long getCachedDuration();

	@Override
	public native int getSampleFlags();

	@Override
	public native long getSampleTime();

	@Override
	public native int getSampleTrackIndex();

	@Override
	public native int getTrackCount();

	@Override
	public MediaFormat getTrackFormat(int index) {
		MediaFormat mediaformat = new MediaFormat();
		ByteBuffer buf1 = ByteBuffer.allocate(128);
		ByteBuffer buf2 = ByteBuffer.allocate(64);

		/*getTrackFormatNative(index, mediaformat, buf1, buf2);
		if (index == 1)
			mediaformat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
		return mediaformat;*/
		
		return getTrackFormatNative(index, mediaformat, buf1, buf2) ? mediaformat : null;
		
		/*if (index == 0) {			
			The MTK H264 decoder need the parameter csd-0 and csd-1 to init the decoder(You can get some information at http://developer.android.com/reference/android/media/MediaCodec.html). csd-0 and csd-1 stands for SPS and PPS of H264.I have asked a MTK engineer and he said that we can use the code below to set these two parameters.

			byte[] sps = {0,0,0,1,103,100,0,40,-84,52,-59,1,-32,17,31,120,11,80,16,16,31
			              ,0,0,3,3,-23,0,0,-22,96,-108};
			byte[] pps = {0,0,0,1,104,-18,60,-128};
			MediaFormat mFormat = MediaFormat.createVideoFormat("video/avc", width, height);
			mFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
			mFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
			return mediaformat;
		}
		else if (index == 1) {
			byte[] bytes = {17, -112};
			mediaformat.setByteBuffer("csd-0", ByteBuffer.wrap(bytes));
			LogUtils.info(String.format("Java: getTrackFormat() #%d %s", index, mediaformat.toString()));
			return mediaformat;
		}*/
	}
	
	private native boolean getTrackFormatNative(int index, 
			MediaFormat mediaformat, ByteBuffer buf1, ByteBuffer buf2);

	@Override
	public native boolean hasCachedReachedEndOfStream();
	
	@Override
	public native int readSampleData(ByteBuffer byteBuf, int offset);

	@Override
	public native void release();

	@Override
	public native void seekTo(long timeUs, int mode);

	@Override
	public native void selectTrack(int index);

	@Override
	public native void setDataSource(String path) throws IOException;

	@Override
	public native void unselectTrack(int index);
	
	private native void setup(Object mediaplayer_this);
	
	private int mNativeContext;
}