package com.pplive.meetplayer.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.pplive.common.sohu.AlbumSohu;
import com.pplive.common.sohu.EpisodeSohu;
import com.pplive.common.sohu.PlaylinkSohu;
import com.pplive.common.sohu.SohuUtil;
import com.pplive.common.sohu.PlaylinkSohu.SOHU_FT;
import com.pplive.meetplayer.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


public class SohuEpisodeActivity extends Activity {
	private final static String TAG = "SohuEpisodeActivity";
	
	private GridView gridView = null;  
    private MySohuEpAdapter adapter = null;
    
    private final static int MSG_EPISODE_DONE		= 1;
    private final static int MSG_PLAYLINK_DONE	= 2;
    private final static int MSG_MORELIST_DONE	= 3;
    
    private final static int TASK_EPISODE			= 1;
    private final static int TASK_PLAYLINK		= 2;
    private final static int TASK_MORELIST		= 3;
    
    private final static int SET_DATA_LIST		= 1;
    private final static int SET_DATA_SEARCH		= 2;
    
    private final static int page_size = 10;
    private int album_page_index = 1;
    private int ep_page_index = 1;
    private int search_page_index = 1;
    
    private List<Map<String, Object>> data2;
    
    private SohuUtil mEPG;
    private List<AlbumSohu> mAlbumList;
    private List<EpisodeSohu> mEpisodeList;
    private String mMoreList;
    private PlaylinkSohu mPlaylink;
    private int sub_channel_id = -1;
    private int selected_aid = -1;
    private int selected_index = -1;
    private String search_key;
    
    boolean noMoreData = false;
    boolean loadingMore = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "Java: onCreate()");
		
		setContentView(R.layout.activity_sohu_episode);  
		
		Intent intent = getIntent();
		sub_channel_id = intent.getIntExtra("sub_channel_id", -1);
		if (intent.hasExtra("search_key"))
			search_key = intent.getStringExtra("search_key");
		if (sub_channel_id == -1 && search_key == null) {
			Toast.makeText(this, "intent param is wrong", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		gridView = (GridView) findViewById(R.id.grid_view);
		
		DisplayMetrics dm = new DisplayMetrics(); 
		getWindowManager().getDefaultDisplay().getMetrics(dm); 
		int screen_width	= dm.widthPixels; 
		int numColumns = screen_width / 256;
		gridView.setNumColumns(numColumns);
		
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				//reset page_index
				ep_page_index = 1;
				
				Map<String, Object> item = adapter.getItem(position);
				selected_aid = (Integer)item.get("aid");
				new SohuEpgTask().execute(TASK_EPISODE, selected_aid);
			}
			
		});
		
		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
		            int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				Log.i(TAG, String.format("Java: onScroll first %d, visible %d, total %d", 
						firstVisibleItem, visibleItemCount, totalItemCount));
				
				int lastInScreen = firstVisibleItem + visibleItemCount;
		        if (totalItemCount > 0 && lastInScreen == totalItemCount && !noMoreData) {
		        	if (!loadingMore) {
		        		loadingMore = true;
		                new SohuEpgTask().execute(TASK_MORELIST);
		        	}
		        }
			}
		});
		
	    mEPG = new SohuUtil();
	    
	    if (search_key != null)
	    	new SetDataTask().execute(SET_DATA_SEARCH);
	    else
	    	new SetDataTask().execute(SET_DATA_LIST);
	}
	
	private Handler mhandler = new Handler(){  
  
        @Override  
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case MSG_EPISODE_DONE:
            	if (mEpisodeList.size() == 1) {
	            	int aid = mEpisodeList.get(0).mAid;
					int vid = mEpisodeList.get(0).mVid;
					selected_index = 0;
					
					new SohuEpgTask().execute(TASK_PLAYLINK, aid, vid);
					return;
            	}
            	
            	popupSelectEpisodeDlg();
            	break;
            case MSG_PLAYLINK_DONE:
            	
            	SOHU_FT ft = SOHU_FT.SOHU_FT_ORIGIN;
            	String strUrl = mPlaylink.getUrl(ft);
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_SUPER;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_HIGH;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			ft = SOHU_FT.SOHU_FT_NORMAL;
        			strUrl = mPlaylink.getUrl(ft);
        		}
        		if (strUrl == null || strUrl.isEmpty()) {
        			Toast.makeText(SohuEpisodeActivity.this, "no stream available", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
        		Intent intent = new Intent(SohuEpisodeActivity.this, PlaySohuActivity.class);
        		intent.putExtra("url_list", strUrl);
        		intent.putExtra("duration_list", mPlaylink.getDuration(ft));
        		intent.putExtra("title", mPlaylink.getTitle());
        		intent.putExtra("index", (ep_page_index - 1) * page_size + selected_index);
        		intent.putExtra("aid", selected_aid);
        		startActivity(intent);
            	break;
            case MSG_MORELIST_DONE:
            	List<Map<String, Object>> listData = adapter.getData();
            	
    			int c = mAlbumList.size();
    			for (int i=0;i<c;i++) {
    				HashMap<String, Object> episode = new HashMap<String, Object>();
    				AlbumSohu al = mAlbumList.get(i);
    				
    				episode.put("title", al.getTitle());
    				episode.put("img_url", al.getImgUrl(true));
    				episode.put("tip", al.getTip());
    				episode.put("aid", al.getAid());
    				listData.add(episode);
    			}
            	
            	adapter.notifyDataSetChanged();
            	break;
            default:
            	break;
            }
        }
	};
	
	private void popupSelectEpisodeDlg() {
		int size = mEpisodeList.size();
		if (size == 0) {
			Toast.makeText(this, "episode list is empty!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> title_list = new ArrayList<String>();
		
		for (int i=0;i<size;i++) {
			title_list.add(mEpisodeList.get(i).mTitle);
		}
		
		final String[] str_title_list = (String[])title_list.toArray(new String[size]);
		
		Dialog choose_episode_dlg = new AlertDialog.Builder(SohuEpisodeActivity.this)
		.setTitle("Select episode")
		.setItems(str_title_list, 
			new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				int aid = mEpisodeList.get(whichButton).mAid;
				int vid = mEpisodeList.get(whichButton).mVid;
				selected_index = whichButton;
				
				new SohuEpgTask().execute(TASK_PLAYLINK, aid, vid);
				dialog.dismiss();
			}
		})
		.setPositiveButton("More...", 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton){
						ep_page_index++;
						new SohuEpgTask().execute(TASK_EPISODE, selected_aid);
						dialog.dismiss();
					}
				})
		.setNegativeButton("Cancel",
			new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton){
			}})
		.create();
		choose_episode_dlg.show();
	}
	
	private class SohuEpgTask extends AsyncTask<Integer, Integer, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "failed to get episode");
				Toast.makeText(SohuEpisodeActivity.this, "failed to get episode", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			Log.i(TAG, "Java: SohuEpgTask action " + action);
			
			if (action == TASK_EPISODE) {
				int aid = params[1];
				if (!mEPG.episode(aid, ep_page_index, page_size)) {
					Log.e(TAG, "Java: failed to call episode()");
					return false;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				mhandler.sendEmptyMessage(MSG_EPISODE_DONE);
			}
			else if (action == TASK_PLAYLINK){
				int aid = params[1];
				int vid = params[2];
				//mPlaylink = mEPG.detail(vid, aid);
				mPlaylink = mEPG.playlink_pptv(vid, 0);
				if (mPlaylink == null) {
					Log.e(TAG, "Java: failed to call playlink_pptv() vid" + vid);
					return false;
				}
				
				mhandler.sendEmptyMessage(MSG_PLAYLINK_DONE);	
			}
			else if (action == TASK_MORELIST) {
				if (mMoreList == null || mMoreList.isEmpty()) {
					Log.e(TAG, "Java morelist is null");
					loadingMore = false;
					return false;
				}
				
				album_page_index++;
				if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
					Log.e(TAG, "Java: failed to call morelist() morelist " + mMoreList);
					noMoreData = true;
					return false;
				}
				
				mEpisodeList = mEPG.getEpisodeList();
				loadingMore = false;
				mhandler.sendEmptyMessage(MSG_MORELIST_DONE);
			}

			return true;
		}
		
	}
	
	private class SetDataTask extends AsyncTask<Integer, Integer, Boolean> {
		
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			if (!result) {
				Log.e(TAG, "Java: failed to get sub channel");
				return;
			}
			
			adapter = new MySohuEpAdapter(SohuEpisodeActivity.this, data2);
		    gridView.setAdapter(adapter);  
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			int action = params[0];
			
			if (action == SET_DATA_SEARCH) {
				if (!mEPG.search(search_key, search_page_index, page_size))
					return false;
				
				mAlbumList = mEPG.getSearchItemList();
			}
			else {
				if (!mEPG.subchannel(sub_channel_id, page_size, 1)) {
					Log.e(TAG, "Java: failed to call subchannel()");
					return false;
				}
				
				mMoreList = mEPG.getMoreList();
				if (mMoreList != null && !mMoreList.isEmpty()) {
					if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
						Log.e(TAG, "Java: failed to call morelist()");
						return false;
					}
				}
				else {
					Log.w(TAG, "Java: morelist param is empty");
				}
				
				mAlbumList = mEPG.getAlbumList();
			}
						  
			data2 = new ArrayList<Map<String, Object>>();
			int c = mAlbumList.size();
			Log.i(TAG, "Java album size: " + c);
			for (int i=0;i<c;i++) {
				HashMap<String, Object> episode = new HashMap<String, Object>();
				AlbumSohu al = mAlbumList.get(i);
				
				episode.put("title", al.getTitle());
				episode.put("img_url", al.getImgUrl(true));
				episode.put("tip", al.getTip());
				episode.put("aid", al.getAid());
				data2.add(episode);
			}
			
			return true;
		}
	}  
}