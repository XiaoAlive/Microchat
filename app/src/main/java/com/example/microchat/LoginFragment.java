package com.example.microchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.service.ChatService;
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

    private FragmentListener fragmentListener;
    private Retrofit retrofit;
    private MainActivity mainActivity;
    private PopupWindow popupDialog; // 添加popupDialog成员变量


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

        // 注册点击事件
        v.findViewById(R.id.textViewRegister).setOnClickListener(view -> {
            // 启动注册Activity
            Intent intent = new Intent(getContext(),RegisterActivity.class);
            startActivity(intent);
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
                retrofit = fragmentListener.getRetrofit();
                startTimer();
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
                                doLogin();
                            }
                        });
                    }
                    this.cancel();
                }
            }
        },0,1000);
    }

    public void doLogin() {
        ChatService service = retrofit.create(ChatService.class);
        String username = editTextQQNum.getText().toString();
        // 创建请求参数Map
        Map<String, String> loginParam = new HashMap<>();
        String password = editTextPassword.getText().toString();
        loginParam.put("username", username);
        loginParam.put("password", password);
        Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                service.requestLogin(loginParam);
        observable.map(result -> {
            //判断服务端是否正确返回
            if(result.getRetCode()==0) {
                //服务端无错误，处理返回的数据
                return result.getData();
            }else{
                //服务端出错了，抛出异常，在Observer中捕获之
                throw new RuntimeException(result.getErrMsg());
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
                        //保存下我的信息
                        MainActivity.myInfo = contactInfo;

                        //无错误时执行,登录成功，进入主页面
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        MainFragment fragment = new MainFragment();
                        //替换掉FrameLayout中现有的Fragment
                        fragmentTransaction.replace(R.id.fragment_container, fragment);
                        //将这次切换放入后退栈中，这样可以在点后退键时自动返回上一个页面
                        fragmentTransaction.addToBackStack("login");
                        fragmentTransaction.commit();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e.getMessage().contains("Failed to connect")) {
                            SharedPreferences preferences = getContext().getApplicationContext().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
                            String addr = preferences.getString("server_addr","");
                            preferences.edit().clear().commit();
                            retrofit = null;
                            showMsg("404 not found! "+addr);
                            mainActivity.setRetrofitVar(null);
                            mainActivity.getRetrofit();
                            startTimer();
                        } else {
                            showMsg(e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {
                    }
                });
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
}