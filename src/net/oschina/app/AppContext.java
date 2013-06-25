package net.oschina.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Properties;
import java.util.UUID;

import com.hkzhe.app.R;

import net.oschina.app.api.ApiClient;
import net.oschina.app.bean.ActiveList;
import net.oschina.app.bean.Blog;
import net.oschina.app.bean.BlogCommentList;
import net.oschina.app.bean.BlogList;
import net.oschina.app.bean.CategoryList;
import net.oschina.app.bean.CommentList;
import net.oschina.app.bean.FavoriteList;
import net.oschina.app.bean.FriendList;
import net.oschina.app.bean.MessageList;
import net.oschina.app.bean.MyInformation;
import net.oschina.app.bean.News;
import net.oschina.app.bean.NewsList;
import net.oschina.app.bean.Notice;
import net.oschina.app.bean.Post;
import net.oschina.app.bean.PostList;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.SearchList;
import net.oschina.app.bean.Software;
import net.oschina.app.bean.SoftwareCatalogList;
import net.oschina.app.bean.SoftwareList;
import net.oschina.app.bean.Tweet;
import net.oschina.app.bean.TweetList;
import net.oschina.app.bean.User;
import net.oschina.app.bean.UserInformation;
import net.oschina.app.common.CyptoUtils;
import net.oschina.app.common.FileUtils;
import net.oschina.app.common.ImageUtils;
import net.oschina.app.common.MethodsCompat;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
//import android.webkit.CacheManager;
import android.util.Log;

/**
 * 閸忋劌鐪惔鏃傛暏缁嬪绨猾浼欑窗閻劋绨穱婵嗙摠閸滃矁鐨熼悽銊ュ弿鐏烇拷绨查悽銊╁帳缂冾喖寮风拋鍧楁６缂冩垹绮堕弫鐗堝祦
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class AppContext extends Application {
	
	public static final int NETTYPE_WIFI = 0x01;
	public static final int NETTYPE_CMWAP = 0x02;
	public static final int NETTYPE_CMNET = 0x03;
	
	public static final int PAGE_SIZE = 20;//姒涙顓婚崚鍡涖�婢堆冪毈
	private static final int CACHE_TIME = 10*60000;//缂傛挸鐡ㄦ径杈ㄦ櫏閺冨爼妫�
	
	private boolean login = false;	//閻ц缍嶉悩鑸碉拷
	private int loginUid = 0;	//閻ц缍嶉悽銊﹀煕閻ㄥ埇d
	private Hashtable<String, Object> memCacheRegion = new Hashtable<String, Object>();
	
	private Handler unLoginHandler = new Handler(){
		public void handleMessage(Message msg) {
			if(msg.what == 1){
				UIHelper.ToastMessage(AppContext.this, getString(R.string.msg_login_error));
				UIHelper.showLoginDialog(AppContext.this);
			}
		}		
	};

	/**
	 * 濡拷绁寸純鎴犵捕閺勵垰鎯侀崣顖滄暏
	 * @return
	 */
	public boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}

	/**
	 * 閼惧嘲褰囪ぐ鎾冲缂冩垹绮剁猾璇茬�
	 * @return 0閿涙碍鐥呴張澶岀秹缂侊拷  1閿涙瓙IFI缂冩垹绮�  2閿涙瓙AP缂冩垹绮�   3閿涙瓊ET缂冩垹绮�
	 */
	public int getNetworkType() {
		int netType = 0;
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null) {
			return netType;
		}		
		int nType = networkInfo.getType();
		if (nType == ConnectivityManager.TYPE_MOBILE) {
			String extraInfo = networkInfo.getExtraInfo();
			if(!StringUtils.isEmpty(extraInfo)){
				if (extraInfo.toLowerCase().equals("cmnet")) {
					netType = NETTYPE_CMNET;
				} else {
					netType = NETTYPE_CMWAP;
				}
			}
		} else if (nType == ConnectivityManager.TYPE_WIFI) {
			netType = NETTYPE_WIFI;
		}
		return netType;
	}
	
	/**
	 * 閸掋倖鏌囪ぐ鎾冲閻楀牊婀伴弰顖氭儊閸忕厧顔愰惄顔界垼閻楀牊婀伴惃鍕煙濞夛拷
	 * @param VersionCode
	 * @return
	 */
	public static boolean isMethodsCompat(int VersionCode) {
		int currentVersion = android.os.Build.VERSION.SDK_INT;
		return currentVersion >= VersionCode;
	}
	
	/**
	 * 閼惧嘲褰嘇pp鐎瑰顥栭崠鍛繆閹拷
	 * @return
	 */
	public PackageInfo getPackageInfo() {
		PackageInfo info = null;
		try { 
			info = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {    
			e.printStackTrace(System.err);
		} 
		if(info == null) info = new PackageInfo();
		return info;
	}
	
	/**
	 * 閼惧嘲褰嘇pp閸烆垯绔撮弽鍥槕
	 * @return
	 */
	public String getAppId() {
		String uniqueID = getProperty(AppConfig.CONF_APP_UNIQUEID);
		if(StringUtils.isEmpty(uniqueID)){
			uniqueID = UUID.randomUUID().toString();
			setProperty(AppConfig.CONF_APP_UNIQUEID, uniqueID);
		}
		return uniqueID;
	}
	
	/**
	 * 閻劍鍩涢弰顖氭儊閻ц缍�
	 * @return
	 */
	public boolean isLogin() {
		return login;
	}
	
	/**
	 * 閼惧嘲褰囬惂璇茬秿閻劍鍩沬d
	 * @return
	 */
	public int getLoginUid() {
		return this.loginUid;
	}
	
	/**
	 * 閻劍鍩涘▔銊╂敘
	 */
	public void Logout() {
		ApiClient.cleanCookie();
		this.cleanCookie();
		this.login = false;
		this.loginUid = 0;
	}
	
	/**
	 * 閺堫亞娅ヨぐ鏇熷灗娣囶喗鏁肩�鍡欑垳閸氬海娈戞径鍕倞
	 */
	public Handler getUnLoginHandler() {
		return this.unLoginHandler;
	}
	
	/**
	 * 閸掓繂顬婇崠鏍暏閹撮娅ヨぐ鏇氫繆閹拷
	 */
	public void initLoginInfo() {
		User loginUser = getLoginInfo();
		if(loginUser!=null && loginUser.getUid()>0 && loginUser.isRememberMe()){
			this.loginUid = loginUser.getUid();
			this.login = true;
		}else{
			this.Logout();
		}
	}
	
	/**
	 * 閻劍鍩涢惂璇茬秿妤犲矁鐦�
	 * @param account
	 * @param pwd
	 * @return
	 * @throws AppException
	 */
	public User loginVerify(String account, String pwd) throws AppException {
		return ApiClient.login(this, account, pwd);
	}
	
	/**
	 * 閹存垹娈戞稉顏冩眽鐠у嫭鏋�
	 * @param isRefresh 閺勵垰鎯佹稉璇插З閸掗攱鏌�
	 * @return
	 * @throws AppException
	 */
	public MyInformation getMyInformation(boolean isRefresh) throws AppException {
		MyInformation myinfo = null;
		String key = "myinfo_"+loginUid;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				myinfo = ApiClient.myInformation(this, loginUid);
				if(myinfo != null && myinfo.getName().length() > 0){
					Notice notice = myinfo.getNotice();
					myinfo.setNotice(null);
					saveObject(myinfo, key);
					myinfo.setNotice(notice);
				}
			}catch(AppException e){
				myinfo = (MyInformation)readObject(key);
				if(myinfo == null)
					throw e;
			}
		} else {
			myinfo = (MyInformation)readObject(key);
			if(myinfo == null)
				myinfo = new MyInformation();
		}
		return myinfo;
	}
	
	/**
	 * 閼惧嘲褰囬悽銊﹀煕娣団剝浼呮稉顏冩眽娑撴捇銆夐敍鍫濆瘶閸氼偉顕氶悽銊﹀煕閻ㄥ嫬濮╅幀浣蜂繆閹垯浜掗崣濠侀嚋娴滆桨淇婇幁顖ょ礆
	 * @param uid 閼奉亜绻侀惃鍓坕d
	 * @param hisuid 鐞氼偅鐓￠惇瀣暏閹撮娈憉id
	 * @param hisname 鐞氼偅鐓￠惇瀣暏閹撮娈戦悽銊﹀煕閸氾拷
	 * @param pageIndex 妞ょ敻娼扮槐銏犵穿
	 * @return
	 * @throws AppException
	 */
	public UserInformation getInformation(int uid, int hisuid, String hisname, int pageIndex, boolean isRefresh) throws AppException {
		String _hisname = ""; 
		if(!StringUtils.isEmpty(hisname)){
			_hisname = hisname;
		}
		UserInformation userinfo = null;
		String key = "userinfo_"+uid+"_"+hisuid+"_"+hisname+"_"+pageIndex+"_"+PAGE_SIZE; 
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {			
			try{
				userinfo = ApiClient.information(this, uid, hisuid, _hisname, pageIndex, PAGE_SIZE);
				if(userinfo != null && pageIndex == 0){
					Notice notice = userinfo.getNotice();
					userinfo.setNotice(null);
					saveObject(userinfo, key);
					userinfo.setNotice(notice);
				}
			}catch(AppException e){
				userinfo = (UserInformation)readObject(key);
				if(userinfo == null)
					throw e;
			}
		} else {
			userinfo = (UserInformation)readObject(key);
			if(userinfo == null)
				userinfo = new UserInformation();
		}
		return userinfo;
	}
	
	/**
	 * 閺囧瓨鏌婇悽銊﹀煕娑斿妫块崗宕囬兇閿涘牆濮為崗铏暈閵嗕礁褰囧☉鍫濆彠濞夘煉绱�
	 * @param uid 閼奉亜绻侀惃鍓坕d
	 * @param hisuid 鐎佃鏌熼悽銊﹀煕閻ㄥ増id
	 * @param newrelation 0:閸欐牗绉风�閫涚铂閻ㄥ嫬鍙у▔锟�:閸忚櫕鏁炴禒锟�	 * @return
	 * @throws AppException
	 */
	public Result updateRelation(int uid, int hisuid, int newrelation) throws AppException {
		return ApiClient.updateRelation(this, uid, hisuid, newrelation);
	}
	
	/**
	 * 濞撳懐鈹栭柅姘辩叀濞戝牊浼�
	 * @param uid
	 * @param type 1:@閹存垹娈戞穱鈩冧紖 2:閺堫亣顕板☉鍫熶紖 3:鐠囧嫯顔戞稉顏呮殶 4:閺傛壆鐭囨稉婵呴嚋閺侊拷
	 * @return
	 * @throws AppException
	 */
	public Result noticeClear(int uid, int type) throws AppException {
		return ApiClient.noticeClear(this, uid, type);
	}
	
	/**
	 * 閼惧嘲褰囬悽銊﹀煕闁氨鐓℃穱鈩冧紖
	 * @param uid
	 * @return
	 * @throws AppException
	 */
	public Notice getUserNotice(int uid) throws AppException {
		return ApiClient.getUserNotice(this, uid);
	}
	
	/**
	 * 閻劍鍩涢弨鎯版閸掓銆�
	 * @param type 0:閸忋劑鍎撮弨鎯版 1:鏉烆垯娆�2:鐠囨繈顣�3:閸楁艾顓�4:閺備即妞�5:娴狅絿鐖�
	 * @param pageIndex 妞ょ敻娼扮槐銏犵穿 0鐞涖劎銇氱粭顑跨妞わ拷
	 * @return
	 * @throws AppException
	 */
	public FavoriteList getFavoriteList(int type, int pageIndex, boolean isRefresh) throws AppException {
		FavoriteList list = null;
		String key = "favoritelist_"+loginUid+"_"+type+"_"+pageIndex+"_"+PAGE_SIZE; 
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getFavoriteList(this, loginUid, type, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (FavoriteList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (FavoriteList)readObject(key);
			if(list == null)
				list = new FavoriteList();
		}
		return list;
	}
	
	/**
	 * 閻劍鍩涚划澶夌閵嗕礁鍙у▔銊ゆ眽閸掓銆�
	 * @param relation 0:閺勫墽銇氶懛顏勭箒閻ㄥ嫮鐭囨稉锟�:閺勫墽銇氶懛顏勭箒閻ㄥ嫬鍙у▔銊拷
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public FriendList getFriendList(int relation, int pageIndex, boolean isRefresh) throws AppException {
		FriendList list = null;
		String key = "friendlist_"+loginUid+"_"+relation+"_"+pageIndex+"_"+PAGE_SIZE; 
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getFriendList(this, loginUid, relation, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (FriendList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (FriendList)readObject(key);
			if(list == null)
				list = new FriendList();
		}
		return list;
	}
	
	/**
	 * 鏂伴椈鍒楄〃
	 * @param catalog
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 * @throws ApiException
	 */
	public NewsList getNewsList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
		NewsList list = null;
		String key = "newslist_"+catalog+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getNewsList(this, catalog, pageIndex, PAGE_SIZE);

				if(list != null && pageIndex == 0){					
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (NewsList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (NewsList)readObject(key);
			if(list == null)
				list = new NewsList();
		}
		return list;
	}
	
	/**
	 * 閺備即妞堢拠锔藉剰
	 * @param news_id
	 * @return
	 * @throws ApiException
	 */
	public News getNews(int news_id, boolean isRefresh) throws AppException {		
		News news = null;
		String key = "news_"+news_id;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				news = ApiClient.getNewsDetail(this, news_id);
				if(news != null){
					Notice notice = news.getNotice();
					news.setNotice(null);
					saveObject(news, key);
					news.setNotice(notice);
				}
			}catch(AppException e){
				news = (News)readObject(key);
				if(news == null)
					throw e;
			}
		} else {
			news = (News)readObject(key);
			if(news == null)
				news = new News();
		}
		return news;		
	}
	
	/**
	 * 閻劍鍩涢崡姘吂閸掓銆�
	 * @param authoruid
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public BlogList getUserBlogList(int authoruid, String authorname, int pageIndex, boolean isRefresh) throws AppException {
		BlogList list = null;
		String key = "userbloglist_"+authoruid+"_"+loginUid+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getUserBlogList(this, authoruid, authorname, loginUid, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (BlogList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (BlogList)readObject(key);
			if(list == null)
				list = new BlogList();
		}
		return list;
	}
	public CategoryList getCategoryList(String type, int pageIndex, boolean isRefresh) throws AppException {
		CategoryList list = null;
		String key = "catelist_"+type+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getCategoryList(this, type, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					//Notice notice = list.getNotice();
					//list.setNotice(null);
					Log.d("bakey" , "get cate list size = " + list.getCatelist().size() );
					saveObject(list, key);
					//list.setNotice(notice);
				}
			}catch(AppException e){
				list = (CategoryList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (CategoryList)readObject(key);
			if(list == null)
				list = new CategoryList();
		}
		return list;
	}
	
	/**
	 * 鍗氬鍒楄〃
	 * @param type 鎺ㄨ崘锛歳ecommend 鏈�柊锛歭atest
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public BlogList getBlogList(String type, int pageIndex, boolean isRefresh) throws AppException {
		BlogList list = null;
		String key = "bloglist_"+type+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getBlogList(this, type, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (BlogList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (BlogList)readObject(key);
			if(list == null)
				list = new BlogList();
		}
		return list;
	}
	
	/**
	 * 閸楁艾顓圭拠锔藉剰
	 * @param blog_id
	 * @return
	 * @throws AppException
	 */
	public Blog getBlog(int blog_id, boolean isRefresh) throws AppException {
		Blog blog = null;
		String key = "blog_"+blog_id;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				blog = ApiClient.getBlogDetail(this, blog_id);
				if(blog != null){
					Notice notice = blog.getNotice();
					blog.setNotice(null);
					saveObject(blog, key);
					blog.setNotice(notice);
				}
			}catch(AppException e){
				blog = (Blog)readObject(key);
				if(blog == null)
					throw e;
			}
		} else {
			blog = (Blog)readObject(key);
			if(blog == null)
				blog = new Blog();
		}
		return blog;
	}
	
	/**
	 * 鏉烆垯娆㈤崚妤勩�
	 * @param searchTag 鏉烆垯娆㈤崚鍡欒  閹恒劏宕�recommend 閺堬拷鏌�time 閻戭參妫�view 閸ユ垝楠�list_cn
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public SoftwareList getSoftwareList(String searchTag, int pageIndex, boolean isRefresh) throws AppException {
		SoftwareList list = null;
		String key = "softwarelist_"+searchTag+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getSoftwareList(this, searchTag, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (SoftwareList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (SoftwareList)readObject(key);
			if(list == null)
				list = new SoftwareList();
		}
		return list;
	}
	
	/**
	 * 鏉烆垯娆㈤崚鍡欒閻ㄥ嫯钂嬫禒璺哄灙鐞涳拷
	 * @param searchTag 娴犲窏oftwarecatalog_list閼惧嘲褰囬惃鍓嘺g
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public SoftwareList getSoftwareTagList(int searchTag, int pageIndex, boolean isRefresh) throws AppException {
		SoftwareList list = null;
		String key = "softwaretaglist_"+searchTag+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getSoftwareTagList(this, searchTag, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (SoftwareList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (SoftwareList)readObject(key);
			if(list == null)
				list = new SoftwareList();
		}
		return list;
	}
	
	/**
	 * 鏉烆垯娆㈤崚鍡欒閸掓銆�
	 * @param tag 缁楊兛绔寸痪锟�  缁楊兛绨╃痪锟絫ag
	 * @return
	 * @throws AppException
	 */
	public SoftwareCatalogList getSoftwareCatalogList(int tag) throws AppException {
		SoftwareCatalogList list = null;
		String key = "softwarecataloglist_"+tag;
		if(isNetworkConnected() && isCacheDataFailure(key)) {
			try{
				list = ApiClient.getSoftwareCatalogList(this, tag);
				if(list != null){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (SoftwareCatalogList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (SoftwareCatalogList)readObject(key);
			if(list == null)
				list = new SoftwareCatalogList();
		}
		return list;
	}
	
	/**
	 * 鏉烆垯娆㈢拠锔藉剰
	 * @param soft_id
	 * @return
	 * @throws AppException
	 */
	public Software getSoftware(String ident, boolean isRefresh) throws AppException {
		Software soft = null;
		String key = "software_"+ident;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				soft = ApiClient.getSoftwareDetail(this, ident);
				if(soft != null){
					Notice notice = soft.getNotice();
					soft.setNotice(null);
					saveObject(soft, key);
					soft.setNotice(notice);
				}
			}catch(AppException e){
				soft = (Software)readObject(key);
				if(soft == null)
					throw e;
			}
		} else {
			soft = (Software)readObject(key);
			if(soft == null)
				soft = new Software();
		}
		return soft;
	}
	
	/**
	 * 鐢牕鐡欓崚妤勩�
	 * @param catalog
	 * @param pageIndex
	 * @return
	 * @throws ApiException
	 */
	public PostList getPostList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
		PostList list = null;
		String key = "postlist_"+catalog+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {		
			try{
				list = ApiClient.getPostList(this, catalog, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (PostList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (PostList)readObject(key);
			if(list == null)
				list = new PostList();
		}
		return list;
	}
	
	/**
	 * 鐠囪褰囩敮鏍х摍鐠囷附鍎�
	 * @param post_id
	 * @return
	 * @throws ApiException
	 */
	public Post getPost(int post_id, boolean isRefresh) throws AppException {		
		Post post = null;
		String key = "post_"+post_id;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {	
			try{
				post = ApiClient.getPostDetail(this, post_id);
				if(post != null){
					Notice notice = post.getNotice();
					post.setNotice(null);
					saveObject(post, key);
					post.setNotice(notice);
				}
			}catch(AppException e){
				post = (Post)readObject(key);
				if(post == null)
					throw e;
			}
		} else {
			post = (Post)readObject(key);
			if(post == null)
				post = new Post();
		}
		return post;		
	}
	
	/**
	 * 閸斻劌鑴婇崚妤勩�
	 * @param catalog -1 閻戭參妫敍锟�閺堬拷鏌婇敍灞姐亣娴滐拷 閺屾劗鏁ら幋椋庢畱閸斻劌鑴�uid)
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public TweetList getTweetList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
		TweetList list = null;
		String key = "tweetlist_"+catalog+"_"+pageIndex+"_"+PAGE_SIZE;		
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getTweetList(this, catalog, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (TweetList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (TweetList)readObject(key);
			if(list == null)
				list = new TweetList();
		}
		return list;
	}
	
	/**
	 * 閼惧嘲褰囬崝銊ヨ剨鐠囷附鍎�
	 * @param tweet_id
	 * @return
	 * @throws AppException
	 */
	public Tweet getTweet(int tweet_id, boolean isRefresh) throws AppException {
		Tweet tweet = null;
		String key = "tweet_"+tweet_id;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				tweet = ApiClient.getTweetDetail(this, tweet_id);
				if(tweet != null){
					Notice notice = tweet.getNotice();
					tweet.setNotice(null);
					saveObject(tweet, key);
					tweet.setNotice(notice);
				}
			}catch(AppException e){
				tweet = (Tweet)readObject(key);
				if(tweet == null)
					throw e;
			}
		} else {
			tweet = (Tweet)readObject(key);
			if(tweet == null)
				tweet = new Tweet();
		}
		return tweet;
	}
	
	/**
	 * 鐠囧嫯顔戦崚妤勩�
	 * @param catalog 1閺堬拷鏌婇崝銊︼拷 2@閹达拷3鐠囧嫯顔�4閹存垼鍤滃锟�	 * @param id
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public ActiveList getActiveList(int catalog, int pageIndex, boolean isRefresh) throws AppException {
		ActiveList list = null;
		String key = "activelist_"+loginUid+"_"+catalog+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getActiveList(this, loginUid, catalog, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (ActiveList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (ActiveList)readObject(key);
			if(list == null)
				list = new ActiveList();
		}
		return list;
	}
	
	/**
	 * 閻ｆ瑨鈻堥崚妤勩�
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public MessageList getMessageList(int pageIndex, boolean isRefresh) throws AppException {
		MessageList list = null;
		String key = "messagelist_"+loginUid+"_"+pageIndex+"_"+PAGE_SIZE;
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getMessageList(this, loginUid, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (MessageList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (MessageList)readObject(key);
			if(list == null)
				list = new MessageList();
		}
		return list;
	}
	
	/**
	 * 閸楁艾顓圭拠鍕啈閸掓銆�
	 * @param id 閸楁艾顓笽d
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public BlogCommentList getBlogCommentList(int id, int pageIndex, boolean isRefresh) throws AppException {
		BlogCommentList list = null;
		String key = "blogcommentlist_"+id+"_"+pageIndex+"_"+PAGE_SIZE;		
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getBlogCommentList(this, id, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (BlogCommentList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (BlogCommentList)readObject(key);
			if(list == null)
				list = new BlogCommentList();
		}
		return list;
	}
	
	/**
	 * 鐠囧嫯顔戦崚妤勩�
	 * @param catalog 1閺備即妞�2鐢牕鐡�3閸斻劌鑴�4閸斻劍锟�
	 * @param id 閺屾劖娼弬浼存閿涘苯绗樼�鎰剁礉閸斻劌鑴婇惃鍒琩 閹存牞锟介弻鎰蒋閻ｆ瑨鈻堥惃鍒iendid
	 * @param pageIndex
	 * @return
	 * @throws AppException
	 */
	public CommentList getCommentList(int catalog, int id, int pageIndex, boolean isRefresh) throws AppException {
		CommentList list = null;
		String key = "commentlist_"+catalog+"_"+id+"_"+pageIndex+"_"+PAGE_SIZE;		
		if(isNetworkConnected() && (isCacheDataFailure(key) || isRefresh)) {
			try{
				list = ApiClient.getCommentList(this, catalog, id, pageIndex, PAGE_SIZE);
				if(list != null && pageIndex == 0){
					Notice notice = list.getNotice();
					list.setNotice(null);
					saveObject(list, key);
					list.setNotice(notice);
				}
			}catch(AppException e){
				list = (CommentList)readObject(key);
				if(list == null)
					throw e;
			}
		} else {
			list = (CommentList)readObject(key);
			if(list == null)
				list = new CommentList();
		}
		return list;
	}
	
	/**
	 * 閼惧嘲褰囬幖婊呭偍閸掓銆�
	 * @param catalog 閸忋劑鍎�all 閺備即妞�news  闂傤喚鐡�post 鏉烆垯娆�software 閸楁艾顓�blog 娴狅絿鐖�code
	 * @param content 閹兼粎鍌ㄩ惃鍕敶鐎癸拷
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 * @throws AppException
	 */
	public SearchList getSearchList(String catalog, String content, int pageIndex, int pageSize) throws AppException {
		return ApiClient.getSearchList(this, catalog, content, pageIndex, pageSize);
	}
	
	/**
	 * 閸欐垵绗樼�锟�	 * @param post 閿涘澆id閵嗕辜itle閵嗕恭atalog閵嗕恭ontent閵嗕巩sNoticeMe閿涳拷
	 * @return
	 * @throws AppException
	 */
	public Result pubPost(Post post) throws AppException {
		return ApiClient.pubPost(this, post);
	}
	
	/**
	 * 閸欐垵濮╁锟�	 * @param Tweet-uid & msg & image
	 * @return
	 * @throws AppException
	 */
	public Result pubTweet(Tweet tweet) throws AppException {
		return ApiClient.pubTweet(this, tweet);
	}
	
	/**
	 * 閸掔娀娅庨崝銊ヨ剨
	 * @param uid
	 * @param tweetid
	 * @return
	 * @throws AppException
	 */
	public Result delTweet(int uid, int tweetid) throws AppException {
		return ApiClient.delTweet(this, uid, tweetid);
	}
	
	/**
	 * 閸欐垿锟介悾娆掆枅
	 * @param uid 閻ц缍嶉悽銊﹀煕uid
	 * @param receiver 閹恒儱褰堥懓鍛畱閻劍鍩沬d
	 * @param content 濞戝牊浼呴崘鍛啇閿涘本鏁為幇蹇庣瑝閼冲�绉存潻锟�0娑擃亜鐡х粭锟�	 * @return
	 * @throws AppException
	 */
	public Result pubMessage(int uid, int receiver, String content) throws AppException {
		return ApiClient.pubMessage(this, uid, receiver, content);
	}
	
	/**
	 * 鏉烆剙褰傞悾娆掆枅
	 * @param uid 閻ц缍嶉悽銊﹀煕uid
	 * @param receiver 閹恒儱褰堥懓鍛畱閻劍鍩涢崥锟�	 * @param content 濞戝牊浼呴崘鍛啇閿涘本鏁為幇蹇庣瑝閼冲�绉存潻锟�0娑擃亜鐡х粭锟�	 * @return
	 * @throws AppException
	 */
	public Result forwardMessage(int uid, String receiver, String content) throws AppException {
		return ApiClient.forwardMessage(this, uid, receiver, content);
	}
	
	/**
	 * 閸掔娀娅庨悾娆掆枅
	 * @param uid 閻ц缍嶉悽銊﹀煕uid
	 * @param friendid 閻ｆ瑨鈻堥懓鍗沝
	 * @return
	 * @throws AppException
	 */
	public Result delMessage(int uid, int friendid) throws AppException {
		return ApiClient.delMessage(this, uid, friendid);
	}
	
	/**
	 * 閸欐垼銆冪拠鍕啈
	 * @param catalog 1閺備即妞� 2鐢牕鐡� 3閸斻劌鑴� 4閸斻劍锟�
	 * @param id 閺屾劖娼弬浼存閿涘苯绗樼�鎰剁礉閸斻劌鑴婇惃鍒琩
	 * @param uid 閻劍鍩泆id
	 * @param content 閸欐垼銆冪拠鍕啈閻ㄥ嫬鍞寸�锟�	 * @param isPostToMyZone 閺勵垰鎯佹潪顒�絺閸掔増鍨滈惃鍕敄闂傦拷 0娑撳秷娴嗛崣锟�1鏉烆剙褰�
	 * @return
	 * @throws AppException
	 */
	public Result pubComment(int catalog, int id, int uid, String content, int isPostToMyZone) throws AppException {
		return ApiClient.pubComment(this, catalog, id, uid, content, isPostToMyZone);
	}
	
	/**
	 * 
	 * @param id 鐞涖劎銇氱悮顐ョ槑鐠佽櫣娈戦弻鎰蒋閺備即妞堥敍灞界瑯鐎涙劧绱濋崝銊ヨ剨閻ㄥ埇d 閹存牞锟介弻鎰蒋濞戝牊浼呴惃锟絝riendid 
	 * @param catalog 鐞涖劎銇氱拠銉ㄧ槑鐠佺儤澧嶇仦鐐扮矆娑斿牏琚崹瀣剁窗1閺備即妞� 2鐢牕鐡� 3閸斻劌鑴� 4閸斻劍锟�
	 * @param replyid 鐞涖劎銇氱悮顐㈡礀婢跺秶娈戦崡鏇氶嚋鐠囧嫯顔慽d
	 * @param authorid 鐞涖劎銇氱拠銉ㄧ槑鐠佽櫣娈戦崢鐔奉瀶娴ｆ粏锟絠d
	 * @param uid 閻劍鍩泆id 娑擄拷鍩囬柈鑺ユЦ瑜版挸澧犻惂璇茬秿閻劍鍩泆id
	 * @param content 閸欐垼銆冪拠鍕啈閻ㄥ嫬鍞寸�锟�	 * @return
	 * @throws AppException
	 */
	public Result replyComment(int id, int catalog, int replyid, int authorid, int uid, String content) throws AppException {
		return ApiClient.replyComment(this, id, catalog, replyid, authorid, uid, content);
	}
	
	/**
	 * 閸掔娀娅庣拠鍕啈
	 * @param id 鐞涖劎銇氱悮顐ョ槑鐠佸搫顕惔鏃傛畱閺屾劖娼弬浼存,鐢牕鐡�閸斻劌鑴婇惃鍒琩 閹存牞锟介弻鎰蒋濞戝牊浼呴惃锟絝riendid
	 * @param catalog 鐞涖劎銇氱拠銉ㄧ槑鐠佺儤澧嶇仦鐐扮矆娑斿牏琚崹瀣剁窗1閺備即妞� 2鐢牕鐡� 3閸斻劌鑴� 4閸斻劍锟�閻ｆ瑨鈻�
	 * @param replyid 鐞涖劎銇氱悮顐㈡礀婢跺秶娈戦崡鏇氶嚋鐠囧嫯顔慽d
	 * @param authorid 鐞涖劎銇氱拠銉ㄧ槑鐠佽櫣娈戦崢鐔奉瀶娴ｆ粏锟絠d
	 * @return
	 * @throws AppException
	 */
	public Result delComment(int id, int catalog, int replyid, int authorid) throws AppException {
		return ApiClient.delComment(this, id, catalog, replyid, authorid);
	}
	
	/**
	 * 閸欐垼銆冮崡姘吂鐠囧嫯顔�
	 * @param blog 閸楁艾顓筰d
	 * @param uid 閻у妾伴悽銊﹀煕閻ㄥ増id
	 * @param content 鐠囧嫯顔戦崘鍛啇
	 * @return
	 * @throws AppException
	 */
	public Result pubBlogComment(int blog, int uid, String content) throws AppException {
		return ApiClient.pubBlogComment(this, blog, uid, content);
	}
	
	/**
	 * 閸欐垼銆冮崡姘吂鐠囧嫯顔�
	 * @param blog 閸楁艾顓筰d
	 * @param uid 閻у妾伴悽銊﹀煕閻ㄥ増id
	 * @param content 鐠囧嫯顔戦崘鍛啇
	 * @param reply_id 鐠囧嫯顔慽d
	 * @param objuid 鐞氼偉鐦庣拋铏规畱鐠囧嫯顔戦崣鎴ｃ�閼板懐娈憉id
	 * @return
	 * @throws AppException
	 */
	public Result replyBlogComment(int blog, int uid, String content, int reply_id, int objuid) throws AppException {
		return ApiClient.replyBlogComment(this, blog, uid, content, reply_id, objuid);
	}
	
	/**
	 * 閸掔娀娅庨崡姘吂鐠囧嫯顔�
	 * @param uid 閻ц缍嶉悽銊﹀煕閻ㄥ増id
	 * @param blogid 閸楁艾顓筰d
	 * @param replyid 鐠囧嫯顔慽d
	 * @param authorid 鐠囧嫯顔戦崣鎴ｃ�閼板懐娈憉id
	 * @param owneruid 閸楁艾顓规担婊嗭拷uid
	 * @return
	 * @throws AppException
	 */
	public Result delBlogComment(int uid, int blogid, int replyid, int authorid, int owneruid) throws AppException {
		return ApiClient.delBlogComment(this, uid, blogid, replyid, authorid, owneruid);
	}
	
	/**
	 * 閸掔娀娅庨崡姘吂
	 * @param uid 閻ц缍嶉悽銊﹀煕閻ㄥ増id
	 * @param authoruid 閸楁艾顓规担婊嗭拷uid
	 * @param id 閸楁艾顓筰d
	 * @return
	 * @throws AppException
	 */
	public Result delBlog(int uid, int authoruid, int id) throws AppException { 	
		return ApiClient.delBlog(this, uid, authoruid, id);
	}
	
	/**
	 * 閻劍鍩涘ǎ璇插閺�儼妫�
	 * @param uid 閻劍鍩沀ID
	 * @param objid 濮ｆ柨顪嗛弰顖涙煀闂傜睂D 閹存牞锟介梻顔剧摕ID 閹存牞锟介崝銊ヨ剨ID
	 * @param type 1:鏉烆垯娆�2:鐠囨繈顣�3:閸楁艾顓�4:閺備即妞�5:娴狅絿鐖�
	 * @return
	 * @throws AppException
	 */
	public Result addFavorite(int uid, int objid, int type) throws AppException {
		return ApiClient.addFavorite(this, uid, objid, type);
	}
	
	/**
	 * 閻劍鍩涢崚鐘绘珟閺�儼妫�
	 * @param uid 閻劍鍩沀ID
	 * @param objid 濮ｆ柨顪嗛弰顖涙煀闂傜睂D 閹存牞锟介梻顔剧摕ID 閹存牞锟介崝銊ヨ剨ID
	 * @param type 1:鏉烆垯娆�2:鐠囨繈顣�3:閸楁艾顓�4:閺備即妞�5:娴狅絿鐖�
	 * @return
	 * @throws AppException
	 */
	public Result delFavorite(int uid, int objid, int type) throws AppException { 	
		return ApiClient.delFavorite(this, uid, objid, type);
	}
	
	/**
	 * 娣囨繂鐡ㄩ惂璇茬秿娣団剝浼�
	 * @param username
	 * @param pwd
	 */
	public void saveLoginInfo(final User user) {
		this.loginUid = user.getUid();
		this.login = true;
		setProperties(new Properties(){{
			setProperty("user.uid", String.valueOf(user.getUid()));
			setProperty("user.name", user.getName());
			setProperty("user.face", FileUtils.getFileName(user.getFace()));//閻劍鍩涙径鏉戝剼-閺傚洣娆㈤崥锟�			setProperty("user.account", user.getAccount());
			setProperty("user.pwd", CyptoUtils.encode("oschinaApp",user.getPwd()));
			setProperty("user.location", user.getLocation());
			setProperty("user.followers", String.valueOf(user.getFollowers()));
			setProperty("user.fans", String.valueOf(user.getFans()));
			setProperty("user.score", String.valueOf(user.getScore()));
			setProperty("user.isRememberMe", String.valueOf(user.isRememberMe()));//閺勵垰鎯佺拋棰佺秶閹存垹娈戞穱鈩冧紖
		}});		
	}
	
	/**
	 * 濞撳懘娅庨惂璇茬秿娣団剝浼�
	 */
	public void cleanLoginInfo() {
		this.loginUid = 0;
		this.login = false;
		removeProperty("user.uid","user.name","user.face","user.account","user.pwd",
				"user.location","user.followers","user.fans","user.score","user.isRememberMe");
	}
	
	/**
	 * 閼惧嘲褰囬惂璇茬秿娣団剝浼�
	 * @return
	 */
	public User getLoginInfo() {		
		User lu = new User();		
		lu.setUid(StringUtils.toInt(getProperty("user.uid"), 0));
		lu.setName(getProperty("user.name"));
		lu.setFace(getProperty("user.face"));
		lu.setAccount(getProperty("user.account"));
		lu.setPwd(CyptoUtils.decode("oschinaApp",getProperty("user.pwd")));
		lu.setLocation(getProperty("user.location"));
		lu.setFollowers(StringUtils.toInt(getProperty("user.followers"), 0));
		lu.setFans(StringUtils.toInt(getProperty("user.fans"), 0));
		lu.setScore(StringUtils.toInt(getProperty("user.score"), 0));
		lu.setRememberMe(StringUtils.toBool(getProperty("user.isRememberMe")));
		return lu;
	}
	
	/**
	 * 娣囨繂鐡ㄩ悽銊﹀煕婢舵潙鍎�
	 * @param fileName
	 * @param bitmap
	 */
	public void saveUserFace(String fileName,Bitmap bitmap) {
		try {
			ImageUtils.saveImage(this, fileName, bitmap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 閼惧嘲褰囬悽銊﹀煕婢舵潙鍎�
	 * @param key
	 * @return
	 * @throws AppException
	 */
	public Bitmap getUserFace(String key) throws AppException {
		FileInputStream fis = null;
		try{
			fis = openFileInput(key);
			return BitmapFactory.decodeStream(fis);
		}catch(Exception e){
			throw AppException.run(e);
		}finally{
			try {
				fis.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 閺勵垰鎯侀崝鐘烘祰閺勫墽銇氶弬鍥╃彿閸ュ墽澧�
	 * @return
	 */
	public boolean isLoadImage()
	{
		String perf_loadimage = getProperty(AppConfig.CONF_LOAD_IMAGE);
		//姒涙顓婚弰顖氬鏉炵晫娈�
		if(StringUtils.isEmpty(perf_loadimage))
			return true;
		else
			return StringUtils.toBool(perf_loadimage);
	}
	
	/**
	 * 鐠佸墽鐤嗛弰顖氭儊閸旂姾娴囬弬鍥╃彿閸ュ墽澧�
	 * @param b
	 */
	public void setConfigLoadimage(boolean b)
	{
		setProperty(AppConfig.CONF_LOAD_IMAGE, String.valueOf(b));
	}
	
	/**
	 * 閺勵垰鎯佸锕�礁濠婃垵濮�
	 * @return
	 */
	public boolean isScroll()
	{
		String perf_scroll = getProperty(AppConfig.CONF_SCROLL);
		//姒涙顓婚弰顖氬彠闂傤厼涔忛崣铏拨閸旓拷
		if(StringUtils.isEmpty(perf_scroll))
			return false;
		else
			return StringUtils.toBool(perf_scroll);
	}
	
	/**
	 * 鐠佸墽鐤嗛弰顖氭儊瀹革箑褰稿鎴濆З
	 * @param b
	 */
	public void setConfigScroll(boolean b)
	{
		setProperty(AppConfig.CONF_SCROLL, String.valueOf(b));
	}
	
	/**
	 * 閺勵垰鎯丠ttps閻ц缍�
	 * @return
	 */
	public boolean isHttpsLogin()
	{
		String perf_httpslogin = getProperty(AppConfig.CONF_HTTPS_LOGIN);
		//姒涙顓婚弰鐥焧tp
		if(StringUtils.isEmpty(perf_httpslogin))
			return false;
		else
			return StringUtils.toBool(perf_httpslogin);
	}
	
	/**
	 * 鐠佸墽鐤嗛弰顖涙Ц閸氼泹ttps閻ц缍�
	 * @param b
	 */
	public void setConfigHttpsLogin(boolean b)
	{
		setProperty(AppConfig.CONF_HTTPS_LOGIN, String.valueOf(b));
	}
	
	/**
	 * 濞撳懘娅庢穱婵嗙摠閻ㄥ嫮绱︾�锟�	 */
	public void cleanCookie()
	{
		removeProperty(AppConfig.CONF_COOKIE);
	}
	

	public boolean isCacheDataFailure(String cachefile)
	{
		/*boolean failure = false;
		File data = getFileStreamPath(cachefile);
		if(data.exists() && (System.currentTimeMillis() - data.lastModified()) > CACHE_TIME)
			failure = true;
		else if(!data.exists())
			failure = true;
		return failure;*/
		return true;
	}
	
	/**
	 * 濞撳懘娅巃pp缂傛挸鐡�
	 */
	public void clearAppCache()
	{
		//濞撳懘娅巜ebview缂傛挸鐡�
		/*File file = CacheManager.getCacheFileBaseDir();  
		if (file != null && file.exists() && file.isDirectory()) {  
		    for (File item : file.listFiles()) {  
		    	item.delete();  
		    }  
		    file.delete();  
		} */ 		  
		deleteDatabase("webview.db");  
		deleteDatabase("webview.db-shm");  
		deleteDatabase("webview.db-wal");  
		deleteDatabase("webviewCache.db");  
		deleteDatabase("webviewCache.db-shm");  
		deleteDatabase("webviewCache.db-wal");  
		//濞撳懘娅庨弫鐗堝祦缂傛挸鐡�
		clearCacheFolder(getFilesDir(),System.currentTimeMillis());
		clearCacheFolder(getCacheDir(),System.currentTimeMillis());
		//2.2閻楀牊婀伴幍宥嗘箒鐏忓棗绨查悽銊х处鐎涙娴嗙粔璇插煂sd閸楋紕娈戦崝鐔诲厴
		if(isMethodsCompat(android.os.Build.VERSION_CODES.FROYO)){
			clearCacheFolder(MethodsCompat.getExternalCacheDir(this),System.currentTimeMillis());
		}
		//濞撳懘娅庣紓鏍帆閸ｃ劋绻氱�妯兼畱娑撳瓨妞傞崘鍛啇
		Properties props = getProperties();
		for(Object key : props.keySet()) {
			String _key = key.toString();
			if(_key.startsWith("temp"))
				removeProperty(_key);
		}
	}	
	
	// clear the cache before time numDays     
	private int clearCacheFolder(File dir, long numDays) {          
	    int deletedFiles = 0;         
	    if (dir!= null && dir.isDirectory()) {             
	        try {                
	            for (File child:dir.listFiles()) {    
	                if (child.isDirectory()) {              
	                    deletedFiles += clearCacheFolder(child, numDays);          
	                }  
	                if (child.lastModified() < numDays) {     
	                    if (child.delete()) {                   
	                        deletedFiles++;           
	                    }    
	                }    
	            }             
	        } catch(Exception e) {       
	            e.printStackTrace();    
	        }     
	    }       
	    return deletedFiles;     
	}
	
	/**
	 * 鐏忓棗顕挒鈥茬箽鐎涙ê鍩岄崘鍛摠缂傛挸鐡ㄦ稉锟�	 * @param key
	 * @param value
	 */
	public void setMemCache(String key, Object value) {
		memCacheRegion.put(key, value);
	}
	
	/**
	 * 娴犲骸鍞寸�妯肩处鐎涙ü鑵戦懢宄板絿鐎电钖�
	 * @param key
	 * @return
	 */
	public Object getMemCache(String key){
		return memCacheRegion.get(key);
	}
	
	/**
	 * 娣囨繂鐡ㄧ壕浣烘磸缂傛挸鐡�
	 * @param key
	 * @param value
	 * @throws IOException
	 */
	public void setDiskCache(String key, String value) throws IOException {
		FileOutputStream fos = null;
		try{
			fos = openFileOutput("cache_"+key+".data", Context.MODE_PRIVATE);
			fos.write(value.getBytes());
			fos.flush();
		}finally{
			try {
				fos.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 閼惧嘲褰囩壕浣烘磸缂傛挸鐡ㄩ弫鐗堝祦
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public String getDiskCache(String key) throws IOException {
		FileInputStream fis = null;
		try{
			fis = openFileInput("cache_"+key+".data");
			byte[] datas = new byte[fis.available()];
			fis.read(datas);
			return new String(datas);
		}finally{
			try {
				fis.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 淇濆瓨瀵硅薄
	 * @param ser
	 * @param file
	 * @throws IOException
	 */
	public boolean saveObject(Serializable ser, String file) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try{
			fos = openFileOutput(file, MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(ser);
			oos.flush();
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally{
			try {
				oos.close();
			} catch (Exception e) {}
			try {
				fos.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 鐠囪褰囩�纭呰杽
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public Serializable readObject(String file){
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try{
			fis = openFileInput(file);
			ois = new ObjectInputStream(fis);
			return (Serializable)ois.readObject();
		}catch(FileNotFoundException e){
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				ois.close();
			} catch (Exception e) {}
			try {
				fis.close();
			} catch (Exception e) {}
		}
		return null;
	}

	public void setProperties(Properties ps){
		AppConfig.getAppConfig(this).set(ps);
	}

	public Properties getProperties(){
		return AppConfig.getAppConfig(this).get();
	}
	
	public void setProperty(String key,String value){
		AppConfig.getAppConfig(this).set(key, value);
	}
	
	public String getProperty(String key){
		return AppConfig.getAppConfig(this).get(key);
	}
	public void removeProperty(String...key){
		AppConfig.getAppConfig(this).remove(key);
	}	
}
