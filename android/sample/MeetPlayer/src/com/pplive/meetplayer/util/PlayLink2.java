package com.pplive.meetplayer.util;

public class PlayLink2 { 
	private String mTitle;
	private String mId;
	private String mDescription;
	
	private String mMark;
	private String mDirector;
	private String mAct;
	private String mYear;
	private String mArea;
	private String mResolution;
	private int mWidth, mHeight;
	private int mDurationSec;

	private PlayLink2() {
		
	}
	
	public PlayLink2(String title, String id, String desc) {
		this(title, id, desc, 
				"", "", "", 
				"", "",
				"", 0);
	}

	public PlayLink2(String title, String id, String desc, 
			String mark, String director, String act, 
			String year, String area,
			String resolution, int duration_sec) {
		mTitle 			= title;
		mId				= id;
		mDescription	= desc;
		
		mMark			= mark;
		mDirector		= director;
		mAct			= act;
		
		mYear			= year;
		mArea			= area;
		
		mResolution		= resolution;
		mDurationSec	= duration_sec;
		
		setResolution();
	}
	
	public String getId() {
		return mId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getDescription() {
		return mDescription;
	}
	
	public String getResolution() {
		return mResolution;
	}
	
	private void setResolution() {
		if (mResolution == null || mResolution.isEmpty())
			return;
		
    	int pos;
    	pos = mResolution.indexOf('|');
    	if (pos == -1) {
    		mWidth = mHeight = 0;
    	}
    	else {
    		mWidth = Integer.valueOf(mResolution.substring(0, pos));
    		mHeight = Integer.valueOf(mResolution.substring(pos + 1, mResolution.length()));
    	}
	}
	
	public int getWidth() {
    	return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public int getDuration() {
		return mDurationSec;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("标题: ");
		sb.append(mTitle);
		sb.append(", id: ");
		sb.append(mId);
		sb.append(", 描述: ");
		sb.append(mDescription);
		sb.append(", mark: ");
		sb.append(mMark);
		sb.append(", 导演: ");
		sb.append(mDirector);
		sb.append(", 主演: ");
		sb.append(mAct);
		sb.append(", 年份: ");
		sb.append(mYear);
		sb.append(", 地区: ");
		sb.append(mArea);
		sb.append(", 分辨率: ");
		sb.append(String.format("%dx%d", mWidth, mHeight));
		sb.append(", 时长: ");
		sb.append(mDurationSec);
		sb.append("秒");
		
		return sb.toString();
	}
}
