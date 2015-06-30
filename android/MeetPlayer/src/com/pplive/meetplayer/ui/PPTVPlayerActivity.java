package com.pplive.meetplayer.ui;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.pplive.common.pptv.EPGUtil;
import com.pplive.common.pptv.PlayLink2;
import com.pplive.common.pptv.PlayLinkUtil;
import com.pplive.meetplayer.util.Util;
import com.pplive.sdk.MediaSDK;

public class PPTVPlayerActivity extends VideoPlayerActivity {

	private final static String TAG = "PPTVPlayerActivity";
	
	private final static int TASK_DETAIL			= 1;
	private final static int TASK_NEXT_EP			= 2;
	private final static int TASK_ITEM_FT			= 4;
	
	private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_PLAY_CDN_FT		= 3;
	private final static int MSG_FAIL_TO_DETAIL	= 12;
    private final static int MSG_FAIL_TO_GET_FT	= 13;
	
	private EPGUtil mEPG;
	private List<PlayLink2> mEpisodeList;
	private String episode_title;
	private int mEpisodeIndex;
	private int mVid;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Java: onCreate()");
		
		Intent intent 	= getIntent();
		mVid 			= intent.getIntExtra("vid", -1);
		mEpisodeIndex	= intent.getIntExtra("index", -1);
		mEPG = new EPGUtil();
	}
	
	@Override
	protected void onComplete() {
		mVideoView.stopPlayback();
		
		mEpisodeIndex ++;
		
		if (mEpisodeList == null) {
			if (mVid == -1) {
				Toast.makeText(this, "mVid is invalid", Toast.LENGTH_SHORT).show();
			}
			else {
				new EpisodeTask().execute(TASK_DETAIL, mVid);
			}
		}
		else {
			mHandler.sendEmptyMessage(MSG_EPISODE_DONE);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		Log.d(TAG, "keyCode: " + keyCode);
		int incr;
		
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (mController.isShowing()) {
				return super.onKeyDown(keyCode, event);
			}
			
			if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode)
				incr = -1;
			else
				incr = 1;
			
			mEpisodeIndex += incr;
			
			if (mEpisodeList == null) {
				if (mVid == -1) {
					Toast.makeText(this, "mVid is invalid", Toast.LENGTH_SHORT).show();
				}
				else {
					new EpisodeTask().execute(TASK_DETAIL, mVid);
				}
			}
			else {
				mHandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private Handler mHandler = new Handler(){  
		  
        @Override  
        public void handleMessage(Message msg) {  
            switch(msg.what) {
            case MSG_EPISODE_DONE:
            	if (mEpisodeList.size() < 2) {
            		finish();
            		return;
            	}
            	
            	if (mEpisodeIndex < 0) {
            		mEpisodeIndex = mEpisodeList.size() - 1;
            		Toast.makeText(PPTVPlayerActivity.this, 
            				"switch to ep tail, list size: " + mEpisodeList.size(), 
            				Toast.LENGTH_SHORT).show();
            	}
            	else if (mEpisodeIndex > mEpisodeList.size() - 1) {
            		mEpisodeIndex = 0;
            		Toast.makeText(PPTVPlayerActivity.this, 
            				"switch to ep head, list size: " + mEpisodeList.size(), 
            				Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	PlayLink2 pl = mEpisodeList.get(mEpisodeIndex);
            	String playlink = pl.getId();
            	short http_port = MediaSDK.getPort("http");
            	String url = PlayLinkUtil.getPlayUrl(
            			Integer.valueOf(playlink), http_port, mFt, 3, null);
            	mUri = Uri.parse(url);
            	
            	String info = String.format("ready to play video %s, playlink: %s, ft: %d", 
            			pl.getTitle(), playlink, mFt);
        		Log.i(TAG, info);
        		Toast.makeText(PPTVPlayerActivity.this, info, Toast.LENGTH_SHORT).show();
        		
        		Util.add_pptvvideo_history(PPTVPlayerActivity.this, 
        				pl.getTitle(), Integer.valueOf(playlink), mFt);
        		
        		mTitle = pl.getTitle();
        		
            	setupPlayer();
            	break;
			default:
				Log.w(TAG, "Java: unknown msg.what " + msg.what);
				break;
			}			 
        }
	}; 
	
	private class EpisodeTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "failed to get episode");
				Toast.makeText(PPTVPlayerActivity.this, 
						"failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			long action = params[0];
			Log.i(TAG, "Java: PPTVEpgTask action " + action);
			
			if (action == TASK_DETAIL) {
				if (params.length < 2) {
					Log.e(TAG, "Java: failed to call detail()");
					mHandler.sendEmptyMessage(MSG_FAIL_TO_DETAIL);
					return false;
				}
				
				int vid = params[1];
				if (!mEPG.detail(String.valueOf(vid))) {
					Log.e(TAG, "Java: failed to call detail()");
					mHandler.sendEmptyMessage(MSG_FAIL_TO_DETAIL);
					return false;
				}
			
				mEpisodeList = mEPG.getLink();
				mHandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_ITEM_FT) {
        		Log.i(TAG, "Java: EPGTask start to getCDNUrl");
        		
        		int vid = params[1];
        		int []ft_list = mEPG.getAvailableFT(String.valueOf(vid));
        		if (ft_list == null || ft_list.length == 0) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		int ft = -1;
        		for (int i=ft_list.length - 1;i>=0;i--) {
        			if (ft_list[i] >= 0 && ft_list[i] < 4) {
        				ft = ft_list[i];
        				break;
        			}
        		}
        		
        		if (ft == -1) {
        			mHandler.sendEmptyMessage(MSG_FAIL_TO_GET_FT);
            		return false;
        		}
        		
        		Util.add_pptvvideo_history(PPTVPlayerActivity.this, episode_title, vid, ft);
        		
        		Message msg = mHandler.obtainMessage(MSG_PLAY_CDN_FT, ft, ft);
    	        msg.sendToTarget();
        	}
			else {
				Log.e(TAG, "Java: invalid action type: " + action);
				return false;
			}

			return true;// all done!
		}
		
	}
}