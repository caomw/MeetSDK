package com.pplive.epg.sohu;

import javax.imageio.ImageIO;
import javax.swing.*; 

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*; 
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import com.pplive.epg.sohu.PlaylinkSohu.SOHU_FT;

public class SohuFrame extends JFrame {
	
	private SOHUVIDEO_EPG_STATE mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_IDLE;
	
	private enum SOHUVIDEO_EPG_STATE {
		SOHUVIDEO_EPG_STATE_IDLE,
		SOHUVIDEO_EPG_STATE_ERROR,
		
		SOHUVIDEO_EPG_STATE_CHANNEL,
		SOHUVIDEO_EPG_STATE_SUBCHANNEL,
		SOHUVIDEO_EPG_STATE_CLIP,
		SOHUVIDEO_EPG_STATE_CATE,
		SOHUVIDEO_EPG_STATE_TOPIC,
		SOHUVIDEO_EPG_STATE_ALBUM,
		SOHUVIDEO_EPG_STATE_EPISODE,
		SOHUVIDEO_EPG_STATE_PLAYLINK,
		
		SOHUVIDEO_EPG_STATE_FOUND_PLAYLINK,
	}
	
	private SohuUtil			mEPG;
	private List<ChannelSohu>	mChannelList;
	private List<SubChannelSohu>mSubChannelList;
	private List<CategorySohu>	mCateList;
	private List<TopicSohu>		mTopicList;
	private List<AlbumSohu>		mAlbumList;
	private List<EpisodeSohu>	mEpisodeList;
	private String 				mMoreList;
	
	int last_aid = -1;
	
	private final static int page_size = 10;
	private int album_page_index = 1;
	private int ep_page_index = 1;
	
	JButton btnOK		= new JButton("OK");
	JButton btnReset 	= new JButton("重置");
	JButton btnGo 		= new JButton("进入");
	JButton btnNext 	= new JButton("翻页");
	
	JLabel lblInfo = new JLabel("info");
	
	JList<String> listItem 				= null;
	DefaultListModel<String> listModel 	= null;
	
	JTextPane editorSearch = new JTextPane();
	JButton btnSearch = new JButton("搜索");
	
	JLabel lblImage = new JLabel();
	JLabel lblTip = new JLabel();
	
	Font f = new Font("宋体", 0, 18);
	
	public SohuFrame() {
		super();
		
		mEPG = new SohuUtil();
		
		this.setTitle("Test EPG");
		this.setBounds(400, 300, 550, 600);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				System.exit(0);
			}
		});

		this.getContentPane().setLayout(null);
		// Action
		lblInfo.setBounds(5, 40, 300, 30);
		this.getContentPane().add(lblInfo);
		
		listItem = new JList<String>();
		listItem.setFont(f);
		listItem.setBounds(20, 80, 200, 250);
		listModel = new DefaultListModel<String>();
		listItem.setModel(listModel);
		listItem.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listItem.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent event) {
				// TODO Auto-generated method stub
				if (listItem.getSelectedIndex() != -1) {
					if (event.getClickCount() == 1) {
						if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM) {
							int n = listItem.getSelectedIndex();
							AlbumSohu al = mAlbumList.get(n);

							lblTip.setText(al.getTip());
							
							BufferedImage image = null;
							try {
								String img_url = al.getImgUrl(false);
								System.out.println("Java image url: " + img_url);
								URL imageURL = new URL(img_url);
								InputStream is = imageURL.openConnection()
										.getInputStream();
								image = ImageIO.read(is);
								System.out.println("Java image is: " + image);
							} catch (Exception e) {
								e.printStackTrace();
								return;
							}

							lblImage.setIcon(new ImageIcon(image));
						}
					} else if (event.getClickCount() == 2) {
						action();
					}

				}
			}
		});
		this.getContentPane().add(listItem);
		
		lblImage.setBounds(230, 100, 256, 256);
		this.getContentPane().add(lblImage);
		
		lblTip.setFont(f);
		lblTip.setBounds(230, 320, 100, 40);
		this.getContentPane().add(lblTip);
		
		btnOK.setBounds(0, 0, 80, 30);
		this.getContentPane().add(btnOK);
		btnGo.setBounds(230, 80, 70, 40);
		btnGo.setFont(f);
		this.getContentPane().add(btnGo);
		btnReset.setBounds(300, 80, 70, 40);
		btnReset.setFont(f);
		this.getContentPane().add(btnReset);
		btnNext.setFont(f);
		btnNext.setBounds(370, 80, 80, 40);
		this.getContentPane().add(btnNext);

		btnOK.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				lblInfo.setText("You Click OK!");
			}
		});

		btnReset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				init_combobox();
			}
		});
		
		btnGo.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action();
			}
		});
		
		btnNext.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				
				if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM) {
					album_page_index++;
					morelist();
				}
				else if (mState == SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE) {
					ep_page_index++;
					selectEpisode();
				}
					
			}
		});
		
		editorSearch.setFont(f);
		editorSearch.setBounds(20, 350, 200, 40);
		editorSearch.setText("越狱");
	    this.getContentPane().add(editorSearch);
	    
	    btnSearch.setFont(f);
	    btnSearch.setBounds(230, 350, 80, 40);
	    editorSearch.setFont(f);
		this.getContentPane().add(btnSearch);
		btnSearch.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String key = editorSearch.getText();
				search(key);
			}
		});
		
		init_combobox();
	}
	
	private void search(String key) {
		if (!mEPG.search(key, album_page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mAlbumList = mEPG.getSearchItemList();
		for (int i=0;i<mAlbumList.size();i++) {
			listModel.addElement(mAlbumList.get(i).getTitle());
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void action() {
		switch (mState) {
		case SOHUVIDEO_EPG_STATE_CHANNEL:
			selectCate();
			break;
		case SOHUVIDEO_EPG_STATE_SUBCHANNEL:
		case SOHUVIDEO_EPG_STATE_CATE:
			selectAlbumNew();
			break;
		case SOHUVIDEO_EPG_STATE_CLIP:
			selectClip();
			break;
		case SOHUVIDEO_EPG_STATE_TOPIC:
			//selectTopic();
			break;
		case SOHUVIDEO_EPG_STATE_ALBUM:
			selectEpisode();
			break;
		case SOHUVIDEO_EPG_STATE_EPISODE:
			playProgram();
			break;
		default:
			break;
		}
	}
	
	private void selectClip() {
		int n = listItem.getSelectedIndex();
		System.out.println("Java: clip info: " + mAlbumList.get(n).toString());
		
		int vid = mAlbumList.get(n).getVid();
		int aid = mAlbumList.get(n).getAid();
		PlaylinkSohu link = mEPG.detail(vid, aid);
		if (link != null) {
			System.out.println("Java: link info: " + link.getTitle());
		}
		
		String url = link.getUrl(SOHU_FT.SOHU_FT_HIGH);

		System.out.println("final play link " + url);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void selectEpisode() {
		if (last_aid == -1) {
			int n = listItem.getSelectedIndex();
			last_aid = mAlbumList.get(n).getAid();
		}
		
		if (!mEPG.episode(last_aid, ep_page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			listModel.addElement(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}
	
	private void selectCate() {
		int n = listItem.getSelectedIndex();
		String id = mChannelList.get(n).mChannelId;
		
		if (!mEPG.channel_select(id)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		listModel.clear();
		
		mSubChannelList = mEPG.getSubChannelList();
		for (int i=0;i<mSubChannelList.size();i++) {
			listModel.addElement(mSubChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_SUBCHANNEL;
	}
	
	private void selectAlbumNew() {
		int n = listItem.getSelectedIndex();
		int id = mSubChannelList.get(n).mSubChannelId;
		if (!mEPG.subchannel(id, page_size, 0)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		 morelist();
		 mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void morelist() {
		mMoreList = mEPG.getMoreList();
		if (mMoreList != null && !mMoreList.isEmpty()) {
			if (!mEPG.morelist(mMoreList, page_size, (album_page_index - 1) * page_size)) {
				mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
				return;
			}
		}
		
		listModel.clear();
		
		mAlbumList = mEPG.getAlbumList();
		int c = mAlbumList.size();
		for (int i=0;i<c;i++) {
			listModel.addElement(mAlbumList.get(i).getTitle());
		}
	}
	
	/*private void selectCatePath() {
		int n = comboItem.getSelectedIndex();
		String cateUrl = mChannelList.get(n).mCateUrl;
		
		if (!mEPG.channel_sel(cateUrl)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mCateList = mEPG.getCateList();
		for (int i=0;i<mCateList.size();i++) {
			comboItem.addItem(mCateList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CATE;
	}
	
	private void selectTopic() {
		int n = comboItem.getSelectedIndex();
		int tid = mTopicList.get(n).mTid;
		
		if (!mEPG.album(tid, album_page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mAlbumList = mEPG.getAlbumList();
		for (int i=0;i<mAlbumList.size();i++) {
			comboItem.addItem(mAlbumList.get(i).mAlbumName);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ALBUM;
	}
	
	private void selectAlbum() {
		int aid;
		if (last_aid == -1) {
			int n = comboItem.getSelectedIndex();
			last_aid = aid = mAlbumList.get(n).getAid();	
		}
		else {
			aid = last_aid;
		}
		
		if (!mEPG.episode(aid, album_page_index, page_size)) {
			mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_ERROR;
			return;
		}
		
		comboItem.removeAllItems();
		
		mEpisodeList = mEPG.getEpisodeList();
		for (int i=0;i<mEpisodeList.size();i++) {
			comboItem.addItem(mEpisodeList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_EPISODE;
	}*/
	
	private void playProgram() {
		int index = listItem.getSelectedIndex();

		int vid = mEpisodeList.get(index).mVid;
		int aid = mEpisodeList.get(index).mAid;
		//PlaylinkSohu pl = mEPG.detail(vid, aid);
		PlaylinkSohu pl = mEPG.playlink_pptv(vid, 0);
		String strUrl = pl.getUrl(SOHU_FT.SOHU_FT_ORIGIN);
		if (strUrl == null || strUrl.isEmpty())
			strUrl = pl.getUrl(SOHU_FT.SOHU_FT_SUPER);
		if (strUrl == null || strUrl.isEmpty())
			strUrl = pl.getUrl(SOHU_FT.SOHU_FT_HIGH);
		if (strUrl == null || strUrl.isEmpty())
			strUrl = pl.getUrl(SOHU_FT.SOHU_FT_NORMAL);
		if (strUrl == null || strUrl.isEmpty()) {
			System.out.println("no stream available");
			return;
		}
		
		int pos = strUrl.indexOf(',');
		String url = null;
		if (pos != -1)
			url = strUrl.substring(0, pos);
		else
			url = strUrl;

		System.out.println("final play link " + url);
		
		String exe_filepath  = "D:/software/ffmpeg/ffplay.exe";
		String[] cmd = new String[] {exe_filepath, url};
		openExe(cmd);
	}
	
	private void init_combobox() {
		/*if (!mEPG.topic(album_page_index, page_size)) {
			System.out.println("failed to channel()");
			return;
		}*/
		
		album_page_index	= 1;
		ep_page_index 		= 1;
		last_aid			= -1;
		
		if (!mEPG.channel_list()) {
			System.out.println("failed to column()");
			return;
		}
		
		listModel.clear();
		
		mChannelList = mEPG.getChannelList();
		int size = mChannelList.size();
		for (int i=0;i<size;i++) {
			listModel.addElement(mChannelList.get(i).mTitle);
		}
		
		mState = SOHUVIDEO_EPG_STATE.SOHUVIDEO_EPG_STATE_CHANNEL;
	}
	
	private void openExe(String... params) {
		Runtime rn = Runtime.getRuntime();
		Process proc = null;
		try {
			proc = rn.exec(params);
			
			 StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "Error");            
             StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "Output");
             errorGobbler.start();
             outputGobbler.start();
             //proc.waitFor();
		} catch (Exception e) {
			System.out.println("Error exec!");
		}
	}
	
	class StreamGobbler extends Thread {
		InputStream is;

		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					if (type.equals("Error"))
						System.out.println("[error] " + line);
					else
						System.out.println("[info] " + line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
}