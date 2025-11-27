package com.example.microchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Typeface;
import android.view.*;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.database.AppDatabase;
import com.example.microchat.database.UserDao;
import com.example.microchat.database.UserEntity;
import com.example.microchat.service.ChatService;
import com.example.microchat.service.FragmentListener;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // 新增成员变量2025年11月8日00:06:44
    private ConstraintLayout layoutContext;// 正常内容部分，是一个ConstraintLayout
    private LinearLayout layoutHistory;// 历史菜单部分，是一个LinearLayout
    private EditText editTextQQNum;// 用户名输入框
    private EditText editTextPassword;// 密码输入框
    private EditText editTextVerificationCode;// 验证码输入框
    private TextView tvAccountLogin;// 账号登录标签
    private TextView tvPhoneLogin;// 手机号登录标签
    private TextView tvGetVerificationCode;// 获取验证码按钮
    private TextView tvPhoneFormatError;// 手机号格式错误提示
    private TextView tvErrorMessage;// 通用错误提示

    private FragmentListener fragmentListener;
    private Retrofit retrofit;
    private MainActivity mainActivity;
    private PopupWindow popupDialog; // 添加popupDialog成员变量
    
    // 登录模式：true=账号登录，false=手机号登录
    private boolean isAccountLoginMode = true;
    
    // 当前验证码
    private String currentVerificationCode = "";


    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListener) {
            fragmentListener = (FragmentListener) context;
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        fragmentListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        // 初始化成员变量
        layoutContext = v.findViewById(R.id.layoutContext);
        layoutHistory = v.findViewById(R.id.layoutHistory);
        editTextQQNum = v.findViewById(R.id.editTextQQNum);
        editTextPassword = v.findViewById(R.id.editTextPassword);
        editTextVerificationCode = v.findViewById(R.id.editTextVerificationCode);
        tvAccountLogin = v.findViewById(R.id.tvAccountLogin);
        tvPhoneLogin = v.findViewById(R.id.tvPhoneLogin);
        tvGetVerificationCode = v.findViewById(R.id.tvGetVerificationCode);
        tvPhoneFormatError = v.findViewById(R.id.tvPhoneFormatError);
        tvErrorMessage = v.findViewById(R.id.tvErrorMessage);

        // 注册点击事件
        v.findViewById(R.id.textViewRegister).setOnClickListener(view -> {
            // 启动注册Activity
            Intent intent = new Intent(getContext(),RegisterActivity.class);
            startActivity(intent);
        });
        
        // 数据库管理按钮点击事件
        v.findViewById(R.id.textViewDBManage).setOnClickListener(view -> {
            // 显示数据库管理对话框
            showDatabaseManagementDialog();
        });
        
        // 账号登录点击事件
        tvAccountLogin.setOnClickListener(view -> {
            if (!isAccountLoginMode) {
                switchToAccountLoginMode();
            }
        });
        
        // 手机号登录点击事件
        tvPhoneLogin.setOnClickListener(view -> {
            if (isAccountLoginMode) {
                switchToPhoneLoginMode();
            }
        });
        

        
        // 获取验证码点击事件
        tvGetVerificationCode.setOnClickListener(view -> {
            String phoneNumber = editTextQQNum.getText().toString().trim();
            
            // 隐藏所有错误提示
            hideAllErrorMessages();
            
            if (phoneNumber.isEmpty()) {
                // 未输入手机号
                showErrorMessage("请输入手机号");
            } else if (!isValidPhoneNumber(phoneNumber)) {
                // 手机号格式不正确
                showErrorMessage("手机号格式有误");
            } else {
                // 手机号格式正确，生成验证码
                generateAndShowVerificationCode();
            }
        });

        // 响应下拉箭头的点击事件，弹出登录历史记录菜单
        v.findViewById(R.id.textViewHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutContext.setVisibility(View.INVISIBLE);
                layoutHistory.setVisibility(View.VISIBLE);

                // 创建两条历史记录菜单项，添加到layoutHistory中
                for(int i=0;i<3;i++) {
                    View layoutItem = getActivity().getLayoutInflater().inflate(R.layout.login_history_item, null);
                    //响应菜单项的点击，把它里面的信息填到输入框中。
                    layoutItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editTextQQNum.setText("1234567890");
                            layoutContext.setVisibility(View.VISIBLE);
                            layoutHistory.setVisibility(View.INVISIBLE);
                        }
                    });
                    layoutHistory.addView(layoutItem);
                }

                // 使用动画显示历史记录
                AnimationSet set = (AnimationSet) AnimationUtils.loadAnimation(
                        getContext(), R.anim.login_history_anim);
                layoutHistory.startAnimation(set);
            }
        });

        // 当点击菜单项之外的区域时，把历史菜单隐藏
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(layoutHistory.getVisibility()==View.VISIBLE){
                    layoutContext.setVisibility(View.VISIBLE);
                    layoutHistory.setVisibility(View.INVISIBLE);
                }
            }
        });

        //响应登录按钮的点击事件
        View buttonLogin = v.findViewById(R.id.buttonLogin);
        // 使用RxView.click()防止按钮重复点击
        io.reactivex.functions.Consumer<View> clickConsumer = new io.reactivex.functions.Consumer<View>() {
            @Override
            public void accept(View view) throws Exception {
                if (isAccountLoginMode) {
                    // 账号登录模式
                    performAccountLogin();
                } else {
                    // 手机号登录模式
                    performPhoneLogin();
                }
            }
        };
        io.reactivex.subjects.PublishSubject<View> publishSubject = io.reactivex.subjects.PublishSubject.create();
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishSubject.onNext(v);
            }
        });
        publishSubject
                .throttleFirst(1, java.util.concurrent.TimeUnit.SECONDS)
                .subscribe(clickConsumer);

        return v;
    }

    // 添加登录请求
    public void showMsg(String msg) {
        Toast toast = Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // 账号登录
    private void performAccountLogin() {
        String account = editTextQQNum.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        
        // 隐藏所有错误提示
        hideAllErrorMessages();
        
        // 验证输入
        if (account.isEmpty()) {
            showErrorMessage("请输入账号");
            return;
        }
        if (password.isEmpty()) {
            showErrorMessage("请输入密码");
            return;
        }
        
        // 获取Retrofit实例
        retrofit = fragmentListener.getRetrofit();
        if (retrofit != null) {
            doAccountLogin();
        } else {
            startTimer();
        }
    }
    
    // 手机号登录
    private void performPhoneLogin() {
        String phoneNumber = editTextQQNum.getText().toString().trim();
        String verificationCode = editTextVerificationCode.getText().toString().trim();
        
        // 隐藏所有错误提示
        hideAllErrorMessages();
        
        // 验证输入
        if (phoneNumber.isEmpty()) {
            showErrorMessage("请输入手机号");
            return;
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            showErrorMessage("手机号格式有误");
            return;
        }
        if (verificationCode.isEmpty()) {
            showErrorMessage("请输入验证码");
            return;
        }
        
        if (!verificationCode.equals(currentVerificationCode)) {
            showErrorMessage("验证码有误");
            return;
        }
        
        // 验证成功，获取服务器端的用户信息
        // 这里简化处理，实际项目中应该通过手机号向服务器请求用户信息
        retrofit = fragmentListener.getRetrofit();
        if (retrofit != null) {
            // 获取ChatService实例
            ChatService service = retrofit.create(ChatService.class);
            
            // 首先尝试从SharedPreferences中查找已保存的账号信息
            SharedPreferences preferences = getContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
            String savedAccount = preferences.getString("account", "");
            String savedUsername = preferences.getString("username", "");
            int savedUserId = preferences.getInt("userId", 0);
            String savedPhone = preferences.getString("phone", "");
            String savedStatus = preferences.getString("status", "在线");
            String savedAvatarUrl = preferences.getString("avatarUrl", "");
            
            // 如果当前手机号与保存的手机号相同，则使用已保存的信息
            if (phoneNumber.equals(savedPhone) && !savedAccount.isEmpty()) {
                // 使用已保存的信息创建ContactInfo对象
                ContactsPageListAdapter.ContactInfo contactInfo = new ContactsPageListAdapter.ContactInfo();
                contactInfo.setId(savedUserId);
                contactInfo.setName(savedUsername);
                contactInfo.setStatus(savedStatus);
                contactInfo.setPhone(savedPhone);
                contactInfo.setAccount(savedAccount);
                if (!savedAvatarUrl.isEmpty()) {
                    contactInfo.setAvatarUrl(savedAvatarUrl);
                }
                
                // 保存用户信息
                MainActivity.myInfo = contactInfo;
                
                // 保存登录状态到SharedPreferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("is_logged_in", true);
                editor.commit();
                
                Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                
                // 跳转到主页面
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                MainFragment fragment = new MainFragment();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
                
                return;
            }
            
            // 如果没有保存的信息或手机号不匹配，则调用服务器API获取用户信息
            showProgressBar();
            
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable = 
                    service.getUserByPhone(phoneNumber);
            
            observable.map(result -> {
                // 判断服务端是否正确返回
                if(result.getRetCode() == 0) {
                    // 服务端无错误，处理返回的数据
                    return result.getData();
                } else {
                    // 服务端出错了，抛出异常，在Observer中捕获之
                    throw new RuntimeException(result.getErrMsg());
                }
            }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                    @Override
                    public void onSubscribe(Disposable d) {
                        //准备好进度条
                    }

                    @Override
                    public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                        // 保存用户信息
                        MainActivity.myInfo = contactInfo;
                        
                        // 保存登录状态到SharedPreferences
                        SharedPreferences loginPreferences = getContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = loginPreferences.edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putString("username", contactInfo.getName());
                        editor.putString("status", contactInfo.getStatus());
                        editor.putInt("userId", contactInfo.getId());
                        editor.putString("phone", contactInfo.getPhone());
                        editor.putString("account", contactInfo.getAccount());
                        editor.putString("avatarUrl", contactInfo.getAvatarUrl());
                        editor.commit();
                        
                        Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                        
                        // 跳转到主页面
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        MainFragment fragment = new MainFragment();
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                        fragmentTransaction.commit();
                        
                        hideProgressBar();
                    }

                    @Override
                    public void onError(Throwable e) {
                        hideProgressBar();
                        if (e.getMessage().contains("用户不存在")) {
                            showErrorMessage("该手机号未注册");
                        } else {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        } else {
            startTimer();
        }
    }

    public void startTimer() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // 获取retrofit实例，但在UI线程中执行doLogin方法
                retrofit = mainActivity.getRetrofitVar();
                if (retrofit != null) {
                    // 使用Handler在主线程中执行doLogin
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                doAccountLogin();
                            }
                        });
                    }
                    this.cancel();
                }
            }
        },0,1000);
    }

    public void doAccountLogin() {
        // 添加日志记录
        android.util.Log.d("LoginFragment", "Starting account login process");
        
        ChatService service = retrofit.create(ChatService.class);
        String account = editTextQQNum.getText().toString();
        // 创建请求参数Map
        Map<String, String> loginParam = new HashMap<>();
        String password = editTextPassword.getText().toString();
        // 使用账号而不是用户名进行登录
        loginParam.put("account", account);
        loginParam.put("password", password);
        
        // 添加日志
        android.util.Log.d("LoginFragment", "Login params - account: " + account + ", password: " + password);
        Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                service.requestLogin(loginParam);
        observable.map(result -> {
            // 添加详细的调试日志
            android.util.Log.d("LoginFragment", "Server response - retCode: " + result.getRetCode() + ", errMsg: " + result.getErrMsg());
            
            //判断服务端是否正确返回
            if(result.getRetCode()==0) {
                //服务端无错误，处理返回的数据
                ContactsPageListAdapter.ContactInfo contactInfo = result.getData();
                if (contactInfo != null && contactInfo.getId() > 0) {
                    return contactInfo;
                } else {
                    // 返回的数据无效，可能是账号不存在
                    throw new RuntimeException("账号不存在");
                }
            }else{
                //服务端出错了，抛出异常，在Observer中捕获之
                String errorMsg = result.getErrMsg();
                android.util.Log.d("LoginFragment", "Processing error message: " + errorMsg);
                
                // 根据常见的错误类型进行分类
                if (errorMsg == null || errorMsg.isEmpty()) {
                    throw new RuntimeException("登录失败，请重试");
                } else if (errorMsg.contains("账号") && errorMsg.contains("密码")) {
                    throw new RuntimeException("账号或密码有误");
                } else if (errorMsg.contains("密码")) {
                    throw new RuntimeException("密码错误");
                } else if (errorMsg.contains("用户") || errorMsg.contains("账号") || errorMsg.contains("不存在")) {
                    throw new RuntimeException("账号不存在");
                } else {
                    throw new RuntimeException(errorMsg);
                }
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideProgressBar();
                            }
                        });
                    }
                })
                .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                    @Override
                    public void onSubscribe(Disposable d) {
                        //准备好进度条
                        showProgressBar();
                    }

                    @Override
                public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                    // 添加日志记录
                    android.util.Log.d("LoginFragment", "Login successful, processing contact info");
                    
                    //保存下我的信息
                    MainActivity.myInfo = contactInfo;
                      
                    // 保存登录状态和用户信息到SharedPreferences
                    SharedPreferences preferences = getContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("is_logged_in", true);
                    // 持久化用户信息
                    editor.putString("username", contactInfo.getName());
                    editor.putString("status", contactInfo.getStatus());
                    editor.putInt("userId", contactInfo.getId());
                    // 保存头像URL信息 - 确保头像URL不为空时才保存
                    String avatarUrl = contactInfo.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        editor.putString("avatarUrl", avatarUrl);
                        android.util.Log.d("LoginFragment", "Avatar URL saved: " + avatarUrl);
                    } else {
                        android.util.Log.d("LoginFragment", "Avatar URL is null or empty, not saved");
                    }
                    // 保存电话号码和账号
                    editor.putString("phone", contactInfo.getPhone());
                    editor.putString("account", contactInfo.getAccount());
                    editor.commit();

                        //无错误时执行,登录成功，进入主页面
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        MainFragment fragment = new MainFragment();
                        //替换掉FrameLayout中现有的Fragment
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                        // 将这次切换放入后退栈中，但由于我们有持久化登录状态，这里可以不放入后退栈
                        // 这样从主页面退出后再次进入时会直接到主页面
                        fragmentTransaction.commit();
                    }

                    @Override
                    public void onError(Throwable e) {
                        android.util.Log.e("LoginFragment", "Login error: " + e.getMessage());
                        
                        // 记录详细的错误信息
                        if (e.getCause() != null) {
                            android.util.Log.e("LoginFragment", "Cause: " + e.getCause().getMessage());
                        }
                        
                        String errorMessage = e.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "未知错误";
                        }
                        
                        android.util.Log.d("LoginFragment", "Error message to process: " + errorMessage);
                        
                        if (errorMessage.contains("Failed to connect") || errorMessage.contains("404")) {
                            SharedPreferences preferences = getContext().getApplicationContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                            String addr = preferences.getString("server_addr","");
                            preferences.edit().clear().commit();
                            retrofit = null;
                            showMsg("404 not found! "+addr);
                            mainActivity.setRetrofitVar(null);
                            mainActivity.getRetrofit();
                            startTimer();
                        } else if (errorMessage.contains("账号或密码有误") || errorMessage.contains("密码错误")) {
                            showErrorMessage("账号或密码有误");
                        } else if (errorMessage.contains("账号不存在") || errorMessage.contains("用户不存在") || errorMessage.contains("用户未找到")) {
                            showErrorMessage("账号不存在");
                        } else {
                            // 显示原始错误消息
                            showErrorMessage(errorMessage);
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    // 切换到账号登录模式
    private void switchToAccountLoginMode() {
        isAccountLoginMode = true;
        
        // 更新标题栏样式 - 选中为深蓝色，未选中为浅蓝色
        tvAccountLogin.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvAccountLogin.setTypeface(null, Typeface.BOLD);
        tvPhoneLogin.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        tvPhoneLogin.setTypeface(null, Typeface.NORMAL);
        
        // 隐藏所有错误提示
        hideAllErrorMessages();
        
        // 更新输入框
        editTextQQNum.setHint("账号");
        editTextQQNum.setInputType(InputType.TYPE_CLASS_TEXT);
        editTextPassword.setVisibility(View.VISIBLE);
        editTextVerificationCode.setVisibility(View.GONE);
        tvGetVerificationCode.setVisibility(View.GONE);
    }
    
    // 切换到手机号登录模式
    private void switchToPhoneLoginMode() {
        isAccountLoginMode = false;
        
        // 更新标题栏样式 - 选中为深蓝色，未选中为浅蓝色
        tvAccountLogin.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        tvAccountLogin.setTypeface(null, Typeface.NORMAL);
        tvPhoneLogin.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvPhoneLogin.setTypeface(null, Typeface.BOLD);
        
        // 隐藏所有错误提示
        hideAllErrorMessages();
        
        // 更新输入框
        editTextQQNum.setHint("手机号");
        editTextQQNum.setInputType(InputType.TYPE_CLASS_PHONE);
        editTextPassword.setVisibility(View.GONE);
        editTextVerificationCode.setVisibility(View.VISIBLE);
        tvGetVerificationCode.setVisibility(View.VISIBLE);
    }
    
    // 验证手机号格式
    private boolean isValidPhoneNumber(String phoneNumber) {
        // 简单的手机号验证，实际项目中可能需要更严格的验证
        return phoneNumber.length() == 11 && phoneNumber.startsWith("1");
    }
    
    // 生成并显示验证码
    private void generateAndShowVerificationCode() {
        String phoneNumber = editTextQQNum.getText().toString().trim();
        
        // 获取Retrofit实例
        retrofit = fragmentListener.getRetrofit();
        if (retrofit != null) {
            // 获取ChatService实例
            ChatService service = retrofit.create(ChatService.class);
            
            // 检查手机号是否已注册
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable = 
                    service.getUserByPhone(phoneNumber);
            
            observable.map(result -> {
                // 判断服务端是否正确返回
                if(result.getRetCode() == 0) {
                    // 服务端无错误，检查返回的数据是否为空
                    ContactsPageListAdapter.ContactInfo userInfo = result.getData();
                    if (userInfo != null && userInfo.getId() > 0) {
                        // 用户信息存在，手机号已注册
                        return true;
                    } else {
                        // 用户信息不存在，手机号未注册
                        return false;
                    }
                } else {
                    // 服务端出错了，抛出异常，在Observer中捕获之
                    throw new RuntimeException(result.getErrMsg());
                }
            }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>(){
                    @Override
                    public void onSubscribe(Disposable d) {
                        // 准备好进度条
                        showProgressBar();
                    }

                    @Override
                    public void onNext(Boolean isRegistered) {
                        hideProgressBar();
                        if (isRegistered) {
                            // 手机号已注册，生成验证码
                            currentVerificationCode = String.format("%06d", (int)(Math.random() * 1000000));
                            
                            // 显示验证码提示框
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("验证码")
                                   .setMessage("您的验证码是：" + currentVerificationCode)
                                   .setPositiveButton("确定", null)
                                   .setCancelable(false)
                                   .show();
                        } else {
                            // 手机号未注册
                            showErrorMessage("该手机号未注册");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        hideProgressBar();
                        if (e.getMessage().contains("用户不存在")) {
                            showErrorMessage("该手机号未注册");
                        } else {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        } else {
            startTimer();
        }
    }
    
    // 显示错误消息
    private void showErrorMessage(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }
    
    // 隐藏所有错误提示
    private void hideAllErrorMessages() {
        tvErrorMessage.setVisibility(View.GONE);
        tvPhoneFormatError.setVisibility(View.GONE);
    }
    
    // 显示进度条
    private void showProgressBar(){
        //显示一个PopWindow，在这个Window中显示进度条
        //进度条
        ProgressBar progressBar = new ProgressBar(getContext());
        //设置进度条窗口覆盖整个父控件的范围，这样可以防止用户多次
        //点击按钮
        popupDialog = new PopupWindow(progressBar,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        //将当前主窗口变成40%半透明，以实现背景变暗效果
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = 0.4f;
        getActivity().getWindow().setAttributes(lp);
        //显示进度条窗口
        popupDialog.showAtLocation(layoutContext, Gravity.CENTER, 0, 0);
    }

    // 隐藏进度条
    private void hideProgressBar(){
        // 先检查popupDialog是否为null，避免空指针异常
        if(popupDialog != null) {
            popupDialog.dismiss();
            popupDialog = null; // 释放引用
        }
        // 恢复窗口透明度
        if(getActivity() != null) {
            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
            lp.alpha = 1f;
            getActivity().getWindow().setAttributes(lp);
        }
    }
    
    // 显示数据库管理对话框
    private void showDatabaseManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("数据管理")
               .setMessage("请选择要执行的操作：\n\n✅ 本地数据：仅影响当前设备\n✅ 服务器数据：影响所有用户")
               .setPositiveButton("管理本地数据", (dialog, which) -> {
                   showLocalDataManagement();
               })
               .setNegativeButton("管理服务器数据", (dialog, which) -> {
                   showServerDataManagement();
               })
               .setNeutralButton("取消", null)
               .show();
    }
    
    // 显示本地数据管理对话框
    private void showLocalDataManagement() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("本地数据管理")
               .setMessage("当前操作仅影响本地数据：\n\n✅ 删除本地数据库中的用户数据\n✅ 清除本地登录状态\n❌ 不影响服务器端用户数据")
               .setPositiveButton("删除本地用户数据", (dialog, which) -> {
                   deleteAllUsers();
               })
               .setNegativeButton("查看本地用户数据", (dialog, which) -> {
                   showAllUsers();
               })
               .setNeutralButton("返回", null)
               .show();
    }
    
    // 显示服务器数据管理对话框
    private void showServerDataManagement() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("服务器数据管理")
               .setMessage("⚠️ 重要：此操作将删除服务器端所有用户数据！\n\n✅ 影响所有登录设备\n✅ 彻底清除用户数据\n✅ 手机号将无法登录\n\n确定要管理服务器数据吗？")
               .setPositiveButton("删除服务器数据", (dialog, which) -> {
                   deleteServerUsers();
               })
               .setNegativeButton("查看服务器数据", (dialog, which) -> {
                   showServerUsers();
               })
               .setNeutralButton("返回", null)
               .show();
    }
    
    // 删除所有用户数据
    private void deleteAllUsers() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("确认删除")
               .setMessage("⚠️ 重要提示：\n\n• 此操作仅删除本地数据库中的用户数据\n• 服务器端用户数据仍然存在\n• 被删除手机号仍可获取验证码并登录\n• 如需完全删除请联系管理员\n\n确定要继续删除本地数据吗？")
               .setPositiveButton("删除本地数据", (dialog, which) -> {
                   // 执行删除操作
                   AppDatabase db = AppDatabase.getInstance(getContext());
                   UserDao userDao = db.userDao();
                   
                   userDao.deleteAllUsers()
                       .subscribeOn(Schedulers.io())
                       .observeOn(AndroidSchedulers.mainThread())
                       .subscribe(() -> {
                           Toast.makeText(getContext(), "本地用户数据已删除\n服务器数据不受影响", Toast.LENGTH_LONG).show();
                           
                           // 同时清除SharedPreferences中的登录状态
                           SharedPreferences preferences = getContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                           SharedPreferences.Editor editor = preferences.edit();
                           editor.putBoolean("is_logged_in", false);
                           editor.remove("username");
                           editor.remove("status");
                           editor.remove("userId");
                           editor.remove("phone");
                           editor.remove("account");
                           editor.remove("avatarUrl");
                           editor.apply();
                           
                           // 清除MainActivity中的用户信息
                           MainActivity.myInfo = null;
                       }, throwable -> {
                           Toast.makeText(getContext(), "删除失败：" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                       });
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    // 显示所有用户数据
    private void showAllUsers() {
        AppDatabase db = AppDatabase.getInstance(getContext());
        UserDao userDao = db.userDao();
        
        userDao.getAllUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(users -> {
                if (users.length == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("本地用户数据")
                           .setMessage("📊 数据来源：本地数据库\n\n当前本地数据库中没有用户数据\n\n⚠️ 注意：\n• 服务器端可能仍有用户数据\n• 手机号仍可获取验证码并登录\n• 如需删除服务器数据请联系管理员")
                           .setPositiveButton("确定", null)
                           .show();
                    return;
                }
                
                StringBuilder userInfo = new StringBuilder("📊 数据来源：本地数据库\n\n");
                userInfo.append("共找到 ").append(users.length).append(" 条本地用户数据\n\n");
                
                for (int i = 0; i < users.length; i++) {
                    UserEntity user = users[i];
                    userInfo.append("用户 ").append(i + 1).append(":\n");
                    userInfo.append("  ID: ").append(user.getId()).append("\n");
                    userInfo.append("  姓名: ").append(user.getName()).append("\n");
                    userInfo.append("  手机号: ").append(user.getPhone()).append("\n");
                    userInfo.append("  账号: ").append(user.getAccount()).append("\n");
                    userInfo.append("  头像: ").append(user.getAvatarUrl()).append("\n\n");
                }
                
                userInfo.append("\n⚠️ 注意：\n• 此数据仅来自本地数据库\n• 服务器端可能还有更多用户数据\n• 删除本地数据不影响服务器");
                
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("本地用户数据")
                       .setMessage(userInfo.toString())
                       .setPositiveButton("确定", null)
                       .show();
            }, throwable -> {
                Toast.makeText(getContext(), "获取本地用户数据失败：" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    // 删除服务器端用户数据
    private void deleteServerUsers() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("确认删除服务器数据")
               .setMessage("🚨 危险操作！\n\n此操作将：\n✅ 删除服务器端所有用户数据\n✅ 所有手机号将无法登录\n✅ 影响所有使用该服务器的设备\n❌ 此操作不可恢复！\n\n确定要删除服务器端所有用户数据吗？")
               .setPositiveButton("确定删除", (dialog, which) -> {
                   // 执行服务器端删除操作
                   retrofit = fragmentListener.getRetrofit();
                   if (retrofit != null) {
                       ChatService service = retrofit.create(ChatService.class);
                       
                       service.deleteAllUsers()
                           .subscribeOn(Schedulers.io())
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(result -> {
                               if (result.getRetCode() == 200) {
                                   Toast.makeText(getContext(), "服务器端所有用户数据已删除", Toast.LENGTH_LONG).show();
                                   
                                   // 同时删除本地数据，保持同步
                                   deleteAllUsersSilently();
                               } else {
                                   Toast.makeText(getContext(), "删除失败：" + result.getErrMsg(), Toast.LENGTH_SHORT).show();
                               }
                           }, throwable -> {
                               Toast.makeText(getContext(), "删除失败：" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                           });
                   } else {
                       Toast.makeText(getContext(), "服务器连接失败", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    // 查看服务器端用户数据
    private void showServerUsers() {
        retrofit = fragmentListener.getRetrofit();
        if (retrofit != null) {
            // 这里可以调用服务器端获取用户列表的API
            // 由于目前服务器端没有提供获取所有用户的API，暂时显示提示信息
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("服务器数据")
                   .setMessage("📊 数据来源：服务器端\n\n当前服务器端用户数据需要通过以下方式查看：\n\n1. 直接查看服务器文件：\n   MimiServer/data/users.json\n\n2. 通过用户管理界面查看\n\n3. 联系管理员获取用户列表")
                   .setPositiveButton("确定", null)
                   .show();
        } else {
            Toast.makeText(getContext(), "服务器连接失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 静默删除本地数据（不显示提示）
    private void deleteAllUsersSilently() {
        AppDatabase db = AppDatabase.getInstance(getContext());
        UserDao userDao = db.userDao();
        
        userDao.deleteAllUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(() -> {
                // 同时清除SharedPreferences中的登录状态
                SharedPreferences preferences = getContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("is_logged_in", false);
                editor.remove("username");
                editor.remove("status");
                editor.remove("userId");
                editor.remove("phone");
                editor.remove("account");
                editor.remove("avatarUrl");
                editor.apply();
                
                // 清除MainActivity中的用户信息
                MainActivity.myInfo = null;
            }, throwable -> {
                // 静默失败，不显示提示
            });
    }
}