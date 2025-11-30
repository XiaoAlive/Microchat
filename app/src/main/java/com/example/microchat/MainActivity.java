package com.example.microchat;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.database.AppDatabase;
import com.example.microchat.service.FragmentListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements FragmentListener {
    //保存我自己的信息
    public static ContactsPageListAdapter.ContactInfo myInfo;
    private Retrofit retrofit;
    public static String serverHostURL = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 首先从SharedPreferences获取serverHostURL
        SharedPreferences preferences = getSharedPreferences("qqapp", MODE_PRIVATE);
        serverHostURL = preferences.getString("server_addr", "http://10.0.2.2:8080"); // 使用默认值防止为空
        
        // 强制创建数据库文件，确保Database Inspector可以查看
        try {
            AppDatabase db = AppDatabase.getInstance(this);
            Log.d("MainActivity", "AppDatabase instance created");
            
            // 直接打开数据库连接强制创建文件
            db.getOpenHelper().getWritableDatabase();
            Log.d("MainActivity", "Database file created successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to create database: " + e.getMessage());
        }
        
        // 检查登录状态
        boolean isLoggedIn = preferences.getBoolean("is_logged_in", false);
        
        // 如果已登录，从SharedPreferences恢复用户信息
        if (isLoggedIn) {
            // 恢复用户信息
            String username = preferences.getString("username", "");
            String status = preferences.getString("status", "");
            long userId = preferences.getLong("userId", 0);
            String avatarUrl = preferences.getString("avatarUrl", "");
            String phone = preferences.getString("phone", "");
            String account = preferences.getString("account", "");
            
            // 创建ContactInfo对象并设置到MainActivity.myInfo
            myInfo = new ContactsPageListAdapter.ContactInfo();
            myInfo.setId(userId);
            myInfo.setName(username);
            myInfo.setStatus(status);
            // 恢复头像URL信息 - 确保头像URL不为空时才设置
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                myInfo.setAvatarUrl(avatarUrl);
            }
            // 恢复电话号码和账号
            myInfo.setPhone(phone);
            myInfo.setAccount(account);
        }
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        if (isLoggedIn) {
            // 如果已登录，直接显示MainFragment
            MainFragment mainFragment = new MainFragment();
            fragmentTransaction.add(R.id.fragment_container, mainFragment);
        } else {
            // 未登录，显示LoginFragment
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.setMainActivity(this); // 设置MainActivity引用
            fragmentTransaction.add(R.id.fragment_container, loginFragment);
        }
        
        fragmentTransaction.commit();
    }
    
    @Override
    public void onBackPressed() {
        // 获取当前显示的Fragment
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        
        // 获取返回栈中的Fragment数量
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        
        if (currentFragment instanceof MainFragment && backStackEntryCount == 0) {
            // 如果当前是MainFragment且返回栈为空，使用系统默认行为（正常退出应用）
            // 这样登录状态会被正确保存
            super.onBackPressed();
        } else {
            // 对于其他情况，使用默认的返回行为
            super.onBackPressed();
        }
    }

    @Override
    public Retrofit getRetrofit() {
        //从本地读取server host name，
        SharedPreferences preferences=getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
        serverHostURL = preferences.getString("server_addr", "");
        if (serverHostURL.isEmpty()){
            //弹出输入对话框，让用户设置server地址
            showServerAddressSetDlg();
        } else {
            //创建Retrofit对象
            retrofit = new Retrofit.Builder()
                    .baseUrl(serverHostURL)
                    //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                    //所以必须设置Call适配器将Observable与Call结合起来
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    //Json数据自动转换
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public Retrofit getRetrofitVar() {
        return this.retrofit;
    }
    public void setRetrofitVar(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    // 提示错误
    public void showMsg(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void showServerAddressSetDlg() {
        //弹出输入对话框，让用户设置server地址
        EditText editText = new EditText(this);
        editText.setHint("地址格式为：http://{IP地址}:{端口号}");
        // 为不同环境提供默认值提示
        String defaultHint = "注意：\n" +
                           "- 在模拟器上: http://10.0.2.2:8080\n" +
                           "- 在真实设备上: 使用服务器的实际IP地址\n" +
                           "  (确保手机和服务器在同一网络)";
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入服务器地址")
                 .setMessage(defaultHint)
                 .setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    serverHostURL = editText.getText().toString();
                    //将服务端地址保存到本地
                    SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("server_addr",serverHostURL);
                    edit.commit();
                    //创建Retrofit对象
                    try {
                        retrofit = new Retrofit.Builder()
                                .baseUrl(serverHostURL)
                                //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                                //所以必须设置Call适配器将Observable与Call结合起来
                                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                //Json数据自动转换
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
                    } catch (Exception e) {
                        showMsg("请输入合法地址！");
                        retrofit = null;
                        preferences.edit().clear().commit();
                        getRetrofit();
                    }
                }).show();
    }
}