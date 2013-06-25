package net.oschina.app.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.oschina.app.AppConfig;
import net.oschina.app.AppContext;
import net.oschina.app.AppException;
import net.oschina.app.AppManager;
import com.hkzhe.app.R;
import net.oschina.app.adapter.ListViewCommentAdapter;
import net.oschina.app.bean.Blog;
import net.oschina.app.bean.BlogCommentList;
import net.oschina.app.bean.Comment;
import net.oschina.app.bean.FavoriteList;
import net.oschina.app.bean.Notice;
import net.oschina.app.bean.Result;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;
import net.oschina.app.widget.BadgeView;
import net.oschina.app.widget.PullToRefreshListView;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

/**
 * ��������
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class BlogDetail extends Activity {
	
	private FrameLayout mHeader;
	private LinearLayout mFooter;
	private ImageView mBack;
	private ImageView mFavorite;
	private ImageView mRefresh;
	private TextView mHeadTitle;
	private ProgressBar mProgressbar;
	private ScrollView mScrollView;
    private ViewSwitcher mViewSwitcher;
	
	private BadgeView bv_comment;
	private ImageView mDetail;
	private ImageView mCommentList;
	private ImageView mShare;
    
	private ImageView mDocTYpe;
	private TextView mTitle;
	private TextView mAuthor;
	private TextView mPubDate;
	private TextView mCommentCount;
	
	private WebView mWebView;
    private Handler mHandler;
    private Blog blogDetail;
    private int blogId;
    
	private final static int VIEWSWITCH_TYPE_DETAIL = 0x001;
	private final static int VIEWSWITCH_TYPE_COMMENTS = 0x002;
	
	private final static int DATA_LOAD_ING = 0x001;
	private final static int DATA_LOAD_COMPLETE = 0x002;
	private final static int DATA_LOAD_FAIL = 0x003;
	
	private PullToRefreshListView mLvComment;
	private ListViewCommentAdapter lvCommentAdapter;
	private List<Comment> lvCommentData = new ArrayList<Comment>();
	private View lvComment_footer;
	private TextView lvComment_foot_more;
	private ProgressBar lvComment_foot_progress;
    private Handler mCommentHandler;
    private int lvSumData;
    
    private int curId;
	private int curCatalog;	//�������۷���
	private int curLvDataState;
	private int curLvPosition;//��ǰlistviewѡ�е�itemλ��
	
	private ViewSwitcher mFootViewSwitcher;
	private ImageView mFootEditebox;
	private EditText mFootEditer;
	private Button mFootPubcomment;	
	private ProgressDialog mProgress;
	private InputMethodManager imm;
	private String tempCommentKey = AppConfig.TEMP_COMMENT;
	
	private int _id;
	private int _uid;
	private String _content;
	
	private GestureDetector gd;
	private boolean isFullScreen;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blog_detail);
        
        AppManager.getAppManager().addActivity(this);
        
        this.initView();        
        this.initData();
        
    	//����������ͼ&����
    	this.initCommentView();
    	this.initCommentData();
        
    	//ע��˫��ȫ���¼�
    	this.regOnDoubleEvent();
    }
    
    //��ʼ����ͼ�ؼ�
    private void initView()
    {
    	blogId = getIntent().getIntExtra("blog_id", 0);
    	
    	if(blogId > 0) tempCommentKey = AppConfig.TEMP_COMMENT + "_" + CommentPub.CATALOG_BLOG + "_" + blogId;
    	
    	mHeader = (FrameLayout)findViewById(R.id.blog_detail_header);
    	mFooter = (LinearLayout)findViewById(R.id.blog_detail_footer);
    	mBack = (ImageView)findViewById(R.id.blog_detail_back);
    	mRefresh = (ImageView)findViewById(R.id.blog_detail_refresh);
    	mProgressbar = (ProgressBar)findViewById(R.id.blog_detail_head_progress);
    	mHeadTitle = (TextView)findViewById(R.id.blog_detail_head_title);
    	mViewSwitcher = (ViewSwitcher)findViewById(R.id.blog_detail_viewswitcher);
    	mScrollView = (ScrollView)findViewById(R.id.blog_detail_scrollview);
    	
    	mDetail = (ImageView)findViewById(R.id.blog_detail_footbar_detail);
    	mCommentList = (ImageView)findViewById(R.id.blog_detail_footbar_commentlist);
    	mShare = (ImageView)findViewById(R.id.blog_detail_footbar_share);
    	mFavorite = (ImageView)findViewById(R.id.blog_detail_footbar_favorite);
    	
    	mDocTYpe = (ImageView)findViewById(R.id.blog_detail_documentType);
    	mTitle = (TextView)findViewById(R.id.blog_detail_title);
    	mAuthor = (TextView)findViewById(R.id.blog_detail_author);
    	mPubDate = (TextView)findViewById(R.id.blog_detail_date);
    	mCommentCount = (TextView)findViewById(R.id.blog_detail_commentcount);
    	
    	mDetail.setEnabled(false);
    	
    	mWebView = (WebView)findViewById(R.id.blog_detail_webview);
    	mWebView.getSettings().setJavaScriptEnabled(false);
    	mWebView.getSettings().setSupportZoom(true);
    	mWebView.getSettings().setBuiltInZoomControls(true);
    	mWebView.getSettings().setDefaultFontSize(15);
    	
    	mBack.setOnClickListener(UIHelper.finish(this));
    	mFavorite.setOnClickListener(favoriteClickListener);
    	mRefresh.setOnClickListener(refreshClickListener);
    	mAuthor.setOnClickListener(authorClickListener);
    	mShare.setOnClickListener(shareClickListener);
    	mDetail.setOnClickListener(detailClickListener);
    	mCommentList.setOnClickListener(commentlistClickListener);
    	
    	bv_comment = new BadgeView(this, mCommentList);
    	bv_comment.setBackgroundResource(R.drawable.widget_count_bg2);
    	bv_comment.setIncludeFontPadding(false);
    	bv_comment.setGravity(Gravity.CENTER);
    	bv_comment.setTextSize(8f);
    	bv_comment.setTextColor(Color.WHITE);
    	
    	imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    	
    	mFootViewSwitcher = (ViewSwitcher)findViewById(R.id.blog_detail_foot_viewswitcher);
    	mFootPubcomment = (Button)findViewById(R.id.blog_detail_foot_pubcomment);
    	mFootPubcomment.setOnClickListener(commentpubClickListener);
    	mFootEditebox = (ImageView)findViewById(R.id.blog_detail_footbar_editebox);
    	mFootEditebox.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFootViewSwitcher.showNext();
				mFootEditer.setVisibility(View.VISIBLE);
				mFootEditer.requestFocus();
				mFootEditer.requestFocusFromTouch();
			}
		});
    	mFootEditer = (EditText)findViewById(R.id.blog_detail_foot_editer);
    	mFootEditer.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus){  
					imm.showSoftInput(v, 0);  
		        }  
		        else{  
		            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
		        }  
			}
		}); 
    	mFootEditer.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if(mFootViewSwitcher.getDisplayedChild()==1){
						mFootViewSwitcher.setDisplayedChild(0);
						mFootEditer.clearFocus();
						mFootEditer.setVisibility(View.GONE);
					}
					return true;
				}
				return false;
			}
		});
    	//�༭�������ı�����
    	mFootEditer.addTextChangedListener(UIHelper.getTextWatcher(this, tempCommentKey));
    	
    	//��ʾ��ʱ�༭����
    	UIHelper.showTempEditContent(this, mFootEditer, tempCommentKey);
    }
    
    //��ʼ���ؼ�����
	private void initData()
	{	
		mHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{				
				if(msg.what == 1)
				{					
					headButtonSwitch(DATA_LOAD_COMPLETE);
					
					int docType = blogDetail.getDocumentType(); 
					if(docType == Blog.DOC_TYPE_ORIGINAL){
						mDocTYpe.setImageResource(R.drawable.widget_original_icon);
					}else if(docType == Blog.DOC_TYPE_REPASTE){
						mDocTYpe.setImageResource(R.drawable.widget_repaste_icon);
					}
					
					mTitle.setText(blogDetail.getTitle());
					mAuthor.setText(blogDetail.getAuthor());
					mPubDate.setText(StringUtils.friendly_time(blogDetail.getPubDate()));
					mCommentCount.setText(String.valueOf(blogDetail.getCommentCount()));
					
					//�Ƿ��ղ�
					if(blogDetail.getFavorite() == 1)
						mFavorite.setImageResource(R.drawable.widget_bar_favorite2);
					else
						mFavorite.setImageResource(R.drawable.widget_bar_favorite);
					
					//��ʾ������
					if(blogDetail.getCommentCount() > 0){
						bv_comment.setText(blogDetail.getCommentCount()+"");
						bv_comment.show();
					}else{
						bv_comment.setText("");
						bv_comment.hide();
					}
					
					String body = blogDetail.getBody() + "<div style=\"margin-bottom: 80px\" />";
					//��ȡ�û����ã��Ƿ��������ͼƬ--Ĭ����wifi��ʼ�ռ���ͼƬ
					boolean isLoadImage;
					AppContext ac = (AppContext)getApplication();
					if(AppContext.NETTYPE_WIFI == ac.getNetworkType()){
						isLoadImage = true;
					}else{
						isLoadImage = ac.isLoadImage();
					}
					if(isLoadImage){
						body = body.replaceAll("(<img[^>]*?)\\s+width\\s*=\\s*\\S+","$1");
						body = body.replaceAll("(<img[^>]*?)\\s+height\\s*=\\s*\\S+","$1");
					}else{
						body = body.replaceAll("<\\s*img\\s+([^>]*)\\s*>","");
					}
					if(!body.trim().startsWith("<style>")){
						String html = UIHelper.WEB_STYLE;
						body = html + body;
					}

					mWebView.loadDataWithBaseURL(null, body, "text/html", "utf-8",null);
					mWebView.setWebViewClient(UIHelper.getWebViewClient());
					
					//����֪ͨ�㲥
					if(msg.obj != null){
						UIHelper.sendBroadCast(BlogDetail.this, (Notice)msg.obj);
					}
				}
				else if(msg.what == 0)
				{
					headButtonSwitch(DATA_LOAD_FAIL);
					
					UIHelper.ToastMessage(BlogDetail.this, R.string.msg_load_is_null);
				}
				else if(msg.what == -1 && msg.obj != null)
				{
					headButtonSwitch(DATA_LOAD_FAIL);
					
					((AppException)msg.obj).makeToast(BlogDetail.this);
				}
			}
		};
		
		initData(blogId, false);
	}
	
    private void initData(final int blog_id, final boolean isRefresh)
    {	
    	headButtonSwitch(DATA_LOAD_ING);
    	
		new Thread(){
			public void run() {
                Message msg = new Message();
				try {
					blogDetail = ((AppContext)getApplication()).getBlog(blog_id, isRefresh);
	                msg.what = (blogDetail!=null && blogDetail.getId()>0) ? 1 : 0;
	                msg.obj = (blogDetail!=null) ? blogDetail.getNotice() : null;
	            } catch (AppException e) {
	                e.printStackTrace();
	            	msg.what = -1;
	            	msg.obj = e;
	            }
                mHandler.sendMessage(msg);
			}
		}.start();
    }
    
    /**
     * �ײ����л�
     * @param type
     */
    private void viewSwitch(int type) {
    	switch (type) {
		case VIEWSWITCH_TYPE_DETAIL:
			mDetail.setEnabled(false);
			mCommentList.setEnabled(true);
			mHeadTitle.setText(R.string.blog_detail_head_title);
			mViewSwitcher.setDisplayedChild(0);			
			break;
		case VIEWSWITCH_TYPE_COMMENTS:
			mDetail.setEnabled(true);
			mCommentList.setEnabled(false);
			mHeadTitle.setText(R.string.comment_list_head_title);
			mViewSwitcher.setDisplayedChild(1);
			break;
    	}
    }
    
    /**
     * ͷ����ťչʾ
     * @param type
     */
    private void headButtonSwitch(int type) {
    	switch (type) {
		case DATA_LOAD_ING:
			mScrollView.setVisibility(View.GONE);
			mProgressbar.setVisibility(View.VISIBLE);
			mRefresh.setVisibility(View.GONE);
			break;
		case DATA_LOAD_COMPLETE:
			mScrollView.setVisibility(View.VISIBLE);
			mProgressbar.setVisibility(View.GONE);
			mRefresh.setVisibility(View.VISIBLE);
			break;
		case DATA_LOAD_FAIL:
			mScrollView.setVisibility(View.GONE);
			mProgressbar.setVisibility(View.GONE);
			mRefresh.setVisibility(View.VISIBLE);
			break;
		}
    }
	
    private View.OnClickListener refreshClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			initData(blogId, true);
			loadLvCommentData(curId,0,mCommentHandler,UIHelper.LISTVIEW_ACTION_REFRESH);
		}
	};
    
	private View.OnClickListener authorClickListener = new View.OnClickListener() {
		public void onClick(View v) {				
			UIHelper.showUserCenter(v.getContext(), blogDetail.getAuthorId(), blogDetail.getAuthor());
		}
	};
	
	private View.OnClickListener shareClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			if(blogDetail == null){
				UIHelper.ToastMessage(v.getContext(), R.string.msg_read_detail_fail);
				return;
			}
			//������
			UIHelper.showShareDialog(BlogDetail.this, blogDetail.getTitle(), blogDetail.getUrl());
		}
	};
	
	private View.OnClickListener detailClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			if(blogId == 0){
				return;
			}
			//�л�������
			viewSwitch(VIEWSWITCH_TYPE_DETAIL);
		}
	};	
	
	private View.OnClickListener commentlistClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			if(blogId == 0){
				return;
			}
			//�л�������
			viewSwitch(VIEWSWITCH_TYPE_COMMENTS);
		}
	};
	
	private View.OnClickListener favoriteClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			if(blogId == 0 || blogDetail == null){
				return;
			}
			
			final AppContext ac = (AppContext)getApplication();
			if(!ac.isLogin()){
				UIHelper.showLoginDialog(BlogDetail.this);
				return;
			}
			final int uid = ac.getLoginUid();
						
			final Handler handler = new Handler(){
				public void handleMessage(Message msg) {
					if(msg.what == 1){
						Result res = (Result)msg.obj;
						if(res.OK()){
							if(blogDetail.getFavorite() == 1){
								blogDetail.setFavorite(0);
								mFavorite.setImageResource(R.drawable.widget_bar_favorite);
							}else{
								blogDetail.setFavorite(1);
								mFavorite.setImageResource(R.drawable.widget_bar_favorite2);
							}	
						}
						UIHelper.ToastMessage(BlogDetail.this, res.getErrorMessage());
					}else{
						((AppException)msg.obj).makeToast(BlogDetail.this);
					}
				}        			
    		};
    		new Thread(){
				public void run() {
					Message msg = new Message();
					Result res = null;
					try {
						if(blogDetail.getFavorite() == 1){
							res = ac.delFavorite(uid, blogId, FavoriteList.TYPE_BLOG);
						}else{
							res = ac.addFavorite(uid, blogId, FavoriteList.TYPE_BLOG);
						}
						msg.what = 1;
						msg.obj = res;
		            } catch (AppException e) {
		            	e.printStackTrace();
		            	msg.what = -1;
		            	msg.obj = e;
		            }
	                handler.sendMessage(msg);
				}        			
    		}.start();	
		}
	};
	
    //��ʼ����ͼ�ؼ�
    private void initCommentView()
    {    	
    	lvComment_footer = getLayoutInflater().inflate(R.layout.listview_footer, null);
    	lvComment_foot_more = (TextView)lvComment_footer.findViewById(R.id.listview_foot_more);
        lvComment_foot_progress = (ProgressBar)lvComment_footer.findViewById(R.id.listview_foot_progress);

    	lvCommentAdapter = new ListViewCommentAdapter(this, lvCommentData, R.layout.comment_listitem); 
    	mLvComment = (PullToRefreshListView)findViewById(R.id.comment_list_listview);
    	
        mLvComment.addFooterView(lvComment_footer);//���ӵײ���ͼ  ������setAdapterǰ
        mLvComment.setAdapter(lvCommentAdapter); 
        mLvComment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		//���ͷ�����ײ�����Ч
        		if(position == 0 || view == lvComment_footer) return;
        		
        		Comment com = null;
        		//�ж��Ƿ���TextView
        		if(view instanceof TextView){
        			com = (Comment)view.getTag();
        		}else{
            		ImageView img = (ImageView)view.findViewById(R.id.comment_listitem_userface);
            		com = (Comment)img.getTag();
        		} 
        		if(com == null) return;
        		
        		//��ת--�ظ����۽���
        		UIHelper.showCommentReply(BlogDetail.this,curId, curCatalog, com.getId(), com.getAuthorId(), com.getAuthor(), com.getContent());
        	}
		});
        mLvComment.setOnScrollListener(new AbsListView.OnScrollListener() {
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mLvComment.onScrollStateChanged(view, scrollState);
				
				//����Ϊ��--���ü������������
				if(lvCommentData.size() == 0) return;
				
				//�ж��Ƿ�������ײ�
				boolean scrollEnd = false;
				try {
					if(view.getPositionForView(lvComment_footer) == view.getLastVisiblePosition())
						scrollEnd = true;
				} catch (Exception e) {
					scrollEnd = false;
				}
				
				if(scrollEnd && curLvDataState==UIHelper.LISTVIEW_DATA_MORE)
				{
					lvComment_foot_more.setText(R.string.load_ing);
					lvComment_foot_progress.setVisibility(View.VISIBLE);
					//��ǰpageIndex
					int pageIndex = lvSumData/20;
					loadLvCommentData(curId, pageIndex, mCommentHandler, UIHelper.LISTVIEW_ACTION_SCROLL);
				}
			}
			public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {
				mLvComment.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
			}
		});
        mLvComment.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				//���ͷ�����ײ�����Ч
        		if(position == 0 || view == lvComment_footer) return false;				
				
        		Comment _com = null;
        		//�ж��Ƿ���TextView
        		if(view instanceof TextView){
        			_com = (Comment)view.getTag();
        		}else{
            		ImageView img = (ImageView)view.findViewById(R.id.comment_listitem_userface);
            		_com = (Comment)img.getTag();
        		} 
        		if(_com == null) return false;
        		
        		final Comment com = _com;
        		
        		curLvPosition = lvCommentData.indexOf(com);
        		
        		final AppContext ac = (AppContext)getApplication();
				//����--�ظ� & ɾ��        		
        		final int uid = ac.getLoginUid();
        		//�жϵ�ǰ��¼�û��Ƿ��ǲ��� ���� �������Ƿ��ǵ�ǰ��¼�û������ģ�true--��ɾ������  false--û��ɾ������
        		if(uid == com.getAuthorId() || (blogDetail != null && uid == blogDetail.getAuthorId()))
        		{
	        		final Handler handler = new Handler(){
						public void handleMessage(Message msg) {
							if(msg.what == 1){
								Result res = (Result)msg.obj;
								if(res.OK()){
									lvSumData--;
									bv_comment.setText(lvSumData+"");
						    		bv_comment.show();
									lvCommentData.remove(com);
									lvCommentAdapter.notifyDataSetChanged();
								}
								UIHelper.ToastMessage(BlogDetail.this, res.getErrorMessage());
							}else{
								((AppException)msg.obj).makeToast(BlogDetail.this);
							}
						}        			
	        		};
	        		final Thread thread = new Thread(){
						public void run() {
							Message msg = new Message();
							try {
								Result res = ac.delBlogComment(uid, blogId, com.getId(), com.getAuthorId(), blogDetail.getAuthorId());
								msg.what = 1;
								msg.obj = res;
				            } catch (AppException e) {
				            	e.printStackTrace();
				            	msg.what = -1;
				            	msg.obj = e;
				            }
			                handler.sendMessage(msg);
						}        			
	        		};
	        		UIHelper.showCommentOptionDialog(BlogDetail.this, curId, curCatalog, com, thread);
        		}
        		else
        		{
        			UIHelper.showCommentOptionDialog(BlogDetail.this, curId, curCatalog, com, null);
        		}
				return true;
			}        	
		});
        mLvComment.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener() {
			public void onRefresh() {
				loadLvCommentData(curId, 0, mCommentHandler, UIHelper.LISTVIEW_ACTION_REFRESH);
            }
        });
    }
    
    //��ʼ����������
	private void initCommentData()
	{
		curId = blogId;
		curCatalog = CommentPub.CATALOG_BLOG;
		
    	mCommentHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				if(msg.what >= 0){						
					BlogCommentList list = (BlogCommentList)msg.obj;
					Notice notice = list.getNotice();
					//����listview����
					switch (msg.arg1) {
					case UIHelper.LISTVIEW_ACTION_INIT:
					case UIHelper.LISTVIEW_ACTION_REFRESH:
						lvSumData = msg.what;
						lvCommentData.clear();//�����ԭ������
						lvCommentData.addAll(list.getCommentlist());
						break;
					case UIHelper.LISTVIEW_ACTION_SCROLL:
						lvSumData += msg.what;
						if(lvCommentData.size() > 0){
							for(Comment com1 : list.getCommentlist()){
								boolean b = false;
								for(Comment com2 : lvCommentData){
									if(com1.getId() == com2.getId() && com1.getAuthorId() == com2.getAuthorId()){
										b = true;
										break;
									}
								}
								if(!b) lvCommentData.add(com1);
							}
						}else{
							lvCommentData.addAll(list.getCommentlist());
						}
						break;
					}	
					
					//����������
					if(blogDetail != null && lvCommentData.size() > blogDetail.getCommentCount()){
						blogDetail.setCommentCount(lvCommentData.size());
						bv_comment.setText(lvCommentData.size()+"");
						bv_comment.show();
					}
					
					if(msg.what < 20){
						curLvDataState = UIHelper.LISTVIEW_DATA_FULL;
						lvCommentAdapter.notifyDataSetChanged();
						lvComment_foot_more.setText(R.string.load_full);
					}else if(msg.what == 20){					
						curLvDataState = UIHelper.LISTVIEW_DATA_MORE;
						lvCommentAdapter.notifyDataSetChanged();
						lvComment_foot_more.setText(R.string.load_more);
					}
					//����֪ͨ�㲥
					if(notice != null){
						UIHelper.sendBroadCast(BlogDetail.this, notice);
					}
				}
				else if(msg.what == -1){
					//���쳣--��ʾ���س��� & ����������Ϣ
					curLvDataState = UIHelper.LISTVIEW_DATA_MORE;
					lvComment_foot_more.setText(R.string.load_error);
					((AppException)msg.obj).makeToast(BlogDetail.this);
				}
				if(lvCommentData.size()==0){
					curLvDataState = UIHelper.LISTVIEW_DATA_EMPTY;
					lvComment_foot_more.setText(R.string.load_empty);
				}
				lvComment_foot_progress.setVisibility(View.GONE);
				if(msg.arg1 == UIHelper.LISTVIEW_ACTION_REFRESH){
					mLvComment.onRefreshComplete(getString(R.string.pull_to_refresh_update) + new Date().toLocaleString());
					mLvComment.setSelection(0);
				}
			}
		};
		this.loadLvCommentData(curId,0,mCommentHandler,UIHelper.LISTVIEW_ACTION_INIT);
    }
    /**
     * �̼߳�����������
     * @param id ��ǰ����id
     * @param pageIndex ��ǰҳ��
     * @param handler ������
     * @param action ������ʶ
     */
	private void loadLvCommentData(final int id,final int pageIndex,final Handler handler,final int action){  
		new Thread(){
			public void run() {
				Message msg = new Message();
				boolean isRefresh = false;
				if(action == UIHelper.LISTVIEW_ACTION_REFRESH || action == UIHelper.LISTVIEW_ACTION_SCROLL)
					isRefresh = true;
				try {
					BlogCommentList commentlist = ((AppContext)getApplication()).getBlogCommentList(id, pageIndex, isRefresh);
					msg.what = commentlist.getPageSize();
					msg.obj = commentlist;
	            } catch (AppException e) {
	            	e.printStackTrace();
	            	msg.what = -1;
	            	msg.obj = e;
	            }
				msg.arg1 = action;//��֪handler��ǰaction
                handler.sendMessage(msg);
			}
		}.start();
	} 
	
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{ 	
		if (resultCode != RESULT_OK) return;   
    	if (data == null) return;
    	
    	viewSwitch(VIEWSWITCH_TYPE_COMMENTS);//���������б�
    	
        if (requestCode == UIHelper.REQUEST_CODE_FOR_RESULT) 
        { 
        	Comment comm = (Comment)data.getSerializableExtra("COMMENT_SERIALIZABLE");
        	lvCommentData.add(0,comm);
        	lvCommentAdapter.notifyDataSetChanged();
        	mLvComment.setSelection(0);        	
    		//��ʾ������
            int count = blogDetail.getCommentCount() + 1;
            blogDetail.setCommentCount(count);
    		bv_comment.setText(count+"");
    		bv_comment.show();
        }
        else if (requestCode == UIHelper.REQUEST_CODE_FOR_REPLY)
        {
        	Comment comm = (Comment)data.getSerializableExtra("COMMENT_SERIALIZABLE");
        	lvCommentData.set(curLvPosition, comm);
        	lvCommentAdapter.notifyDataSetChanged();
        }
	}
	
	private View.OnClickListener commentpubClickListener = new View.OnClickListener() {
		public void onClick(View v) {	
			_id = curId;
			
			if(curId == 0){
				return;
			}
			
			_content = mFootEditer.getText().toString();
			if(StringUtils.isEmpty(_content)){
				UIHelper.ToastMessage(v.getContext(), "��������������");
				return;
			}
			
			final AppContext ac = (AppContext)getApplication();
			if(!ac.isLogin()){
				UIHelper.showLoginDialog(BlogDetail.this);
				return;
			}
				
			_uid = ac.getLoginUid();
			
			mProgress = ProgressDialog.show(v.getContext(), null, "�����С�����",true,true); 			
			
			final Handler handler = new Handler(){
				public void handleMessage(Message msg) {
					
					if(mProgress!=null)mProgress.dismiss();
					
					if(msg.what == 1){
						Result res = (Result)msg.obj;
						UIHelper.ToastMessage(BlogDetail.this, res.getErrorMessage());
						if(res.OK()){
							//����֪ͨ�㲥
							if(res.getNotice() != null){
								UIHelper.sendBroadCast(BlogDetail.this, res.getNotice());
							}
							//�ָ���ʼ�ײ���
							mFootViewSwitcher.setDisplayedChild(0);
							mFootEditer.clearFocus();
							mFootEditer.setText("");
							mFootEditer.setVisibility(View.GONE);
							//���������б�
					    	viewSwitch(VIEWSWITCH_TYPE_COMMENTS);
					    	//���������б�
					    	lvCommentData.add(0,res.getComment());
					    	lvCommentAdapter.notifyDataSetChanged();
					    	mLvComment.setSelection(0);        	
							//��ʾ������
					        int count = blogDetail.getCommentCount() + 1;
					        blogDetail.setCommentCount(count);
							bv_comment.setText(count+"");
							bv_comment.show();
							//���֮ǰ����ı༭����
							ac.removeProperty(tempCommentKey);
						}
					}
					else {
						((AppException)msg.obj).makeToast(BlogDetail.this);
					}
				}
			};
			new Thread(){
				public void run() {
					Message msg = new Message();
					Result res = new Result();
					try {
						//��������
						res = ac.pubBlogComment(_id, _uid, _content);
						msg.what = 1;
						msg.obj = res;
		            } catch (AppException e) {
		            	e.printStackTrace();
						msg.what = -1;
						msg.obj = e;
		            }
					handler.sendMessage(msg);
				}
			}.start();
		}
	};
	
	/**
	 * ע��˫��ȫ���¼�
	 */
	private void regOnDoubleEvent(){
		gd = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				isFullScreen = !isFullScreen;
				if (!isFullScreen) {   
                    WindowManager.LayoutParams params = getWindow().getAttributes();   
                    params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);   
                    getWindow().setAttributes(params);   
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);  
                    mHeader.setVisibility(View.VISIBLE);
                    mFooter.setVisibility(View.VISIBLE);
                } else {    
                    WindowManager.LayoutParams params = getWindow().getAttributes();   
                    params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;   
                    getWindow().setAttributes(params);   
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);   
                    mHeader.setVisibility(View.GONE);
                    mFooter.setVisibility(View.GONE);
                }
				return true;
			}
		});
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		gd.onTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}
}