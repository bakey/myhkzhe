package net.oschina.app.ui;

import net.oschina.app.AppContext;
import net.oschina.app.AppException;
import com.hkzhe.wwtt.R;
import net.oschina.app.api.ApiClient;
import net.oschina.app.bean.Result;
import net.oschina.app.bean.User;
import net.oschina.app.common.StringUtils;
import net.oschina.app.common.UIHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ViewSwitcher;

/**
 * 鐢ㄦ埛鐧诲綍瀵硅瘽妗�
 * @author liux (http://my.oschina.net/liux)
 * @version 1.0
 * @created 2012-3-21
 */
public class LoginDialog extends Activity{
	
	private ViewSwitcher mViewSwitcher;
	private ImageButton btn_close;
	private Button btn_login;
	private AutoCompleteTextView mAccount;
	private EditText mPwd;
	private AnimationDrawable loadingAnimation;
	private View loginLoading;
	private CheckBox chb_rememberMe;
	private int curLoginType;
	private InputMethodManager imm;
	
	public final static int LOGIN_OTHER = 0x00;
	public final static int LOGIN_MAIN = 0x01;
	public final static int LOGIN_SETTING = 0x02;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_dialog);
        
        imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        
        curLoginType = getIntent().getIntExtra("LOGINTYPE", LOGIN_OTHER);
        
        mViewSwitcher = (ViewSwitcher)findViewById(R.id.logindialog_view_switcher);       
        loginLoading = (View)findViewById(R.id.login_loading);
        mAccount = (AutoCompleteTextView)findViewById(R.id.login_account);
        mPwd = (EditText)findViewById(R.id.login_password);
        chb_rememberMe = (CheckBox)findViewById(R.id.login_checkbox_rememberMe);
        
        btn_close = (ImageButton)findViewById(R.id.login_close_button);
        btn_close.setOnClickListener(UIHelper.finish(this));        
        
        btn_login = (Button)findViewById(R.id.login_btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//闅愯棌杞敭鐩�
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);  
				
				String account = mAccount.getText().toString();
				String pwd = mPwd.getText().toString();
				boolean isRememberMe = chb_rememberMe.isChecked();
				//鍒ゆ柇杈撳叆
				if(StringUtils.isEmpty(account)){
					UIHelper.ToastMessage(v.getContext(), getString(R.string.msg_login_email_null));
					return;
				}
				if(StringUtils.isEmpty(pwd)){
					UIHelper.ToastMessage(v.getContext(), getString(R.string.msg_login_pwd_null));
					return;
				}
				
		        btn_close.setVisibility(View.GONE);
		        loadingAnimation = (AnimationDrawable)loginLoading.getBackground();
		        loadingAnimation.start();
		        mViewSwitcher.showNext();
		        
		        login(account, pwd, isRememberMe);
			}
		});

        //鏄惁鏄剧ず鐧诲綍淇℃伅
        AppContext ac = (AppContext)getApplication();
        User user = ac.getLoginInfo();
        if(user==null || !user.isRememberMe()) return;
        if(!StringUtils.isEmpty(user.getAccount())){
        	mAccount.setText(user.getAccount());
        	mAccount.selectAll();
        	chb_rememberMe.setChecked(user.isRememberMe());
        }
        if(!StringUtils.isEmpty(user.getPwd())){
        	mPwd.setText(user.getPwd());
        }
    }
    
    //鐧诲綍楠岃瘉
    private void login(final String account, final String pwd, final boolean isRememberMe) {
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				if(msg.what == 1){
					User user = (User)msg.obj;
					if(user != null){
						//娓呯┖鍘熷厛cookie
						ApiClient.cleanCookie();
						//鍙戦�閫氱煡骞挎挱
						UIHelper.sendBroadCast(LoginDialog.this, user.getNotice());
						//鎻愮ず鐧婚檰鎴愬姛
						UIHelper.ToastMessage(LoginDialog.this, R.string.msg_login_success);
						if(curLoginType == LOGIN_MAIN){
							//璺宠浆--鍔犺浇鐢ㄦ埛鍔ㄦ�
							Intent intent = new Intent(LoginDialog.this, Main.class);
							intent.putExtra("LOGIN", true);
							startActivity(intent);
						}else if(curLoginType == LOGIN_SETTING){
							//璺宠浆--鐢ㄦ埛璁剧疆椤甸潰
							Intent intent = new Intent(LoginDialog.this, Setting.class);
							intent.putExtra("LOGIN", true);
							startActivity(intent);
						}
						finish();
					}
				}else if(msg.what == 0){
					mViewSwitcher.showPrevious();
					UIHelper.ToastMessage(LoginDialog.this, getString(R.string.msg_login_fail)+msg.obj);
				}else if(msg.what == -1){
					mViewSwitcher.showPrevious();
					((AppException)msg.obj).makeToast(LoginDialog.this);
				}
			}
		};
		new Thread(){
			public void run() {
				Message msg =new Message();
				try {
					AppContext ac = (AppContext)getApplication(); 
	                User user = ac.loginVerify(account, pwd);
	                user.setAccount(account);
	                user.setPwd(pwd);
	                user.setRememberMe(isRememberMe);
	                Result res = user.getValidate();
	                if(res.OK()){
	                	ac.saveLoginInfo(user);//淇濆瓨鐧诲綍淇℃伅
	                	msg.what = 1;//鎴愬姛
	                	msg.obj = user;
	                }else{
	                	ac.cleanLoginInfo();//娓呴櫎鐧诲綍淇℃伅
	                	msg.what = 0;//澶辫触
	                	msg.obj = res.getErrorMessage();
	                }
	            } catch (AppException e) {
	            	e.printStackTrace();
			    	msg.what = -1;
			    	msg.obj = e;
	            }
				handler.sendMessage(msg);
			}
		}.start();
    }
}
