package com.example.microchat;

import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.microchat.adapter.ContactsPageListAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements FragmentListener{
    //保存我自己的信息
    public static ContactsPageListAdapter.ContactInfo myInfo;
    private Retrofit retrofit;
    public static String serverHostURL = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //将LoginFragment加入，作为首页
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LoginFragment fragment = new LoginFragment();
        fragment.setMainActivity(this); // 设置MainActivity引用
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
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
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入服务器地址").setView(editText);
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