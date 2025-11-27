package com.example.microchat;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.example.microchat.adapter.ContactsPageListAdapter;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 设置Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("我的资料");
        setSupportActionBar(toolbar);
        // 设置显示返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取用户信息
        ContactsPageListAdapter.ContactInfo myInfo = MainActivity.myInfo;

        // 显示头像
        ImageView profileAvatar = findViewById(R.id.profileAvatar);
        if (myInfo != null) {
            // 使用Glide加载头像
            String imgURL = MainActivity.serverHostURL + myInfo.getAvatarUrl();
            Glide.with(this)
                    .load(imgURL)
                    .placeholder(R.drawable.contacts_normal)
                    .into(profileAvatar);

            // 添加调试日志
            android.util.Log.d("ProfileActivity", "User info - Name: " + myInfo.getName() + 
                              ", Phone: " + myInfo.getPhone() + 
                              ", Account: " + myInfo.getAccount() +
                              ", Avatar: " + myInfo.getAvatarUrl());
            
            // 显示用户名
            TextView profileUsername = findViewById(R.id.profileUsername);
            profileUsername.setText(myInfo.getName());
            
            // 显示电话号码
            TextView profilePhone = findViewById(R.id.profilePhone);
            profilePhone.setText("电话: " + myInfo.getPhone());
            
            // 显示账号
            TextView profileAccount = findViewById(R.id.profileAccount);
            profileAccount.setText("账号: " + myInfo.getAccount());
        } else {
            // 如果没有用户信息，显示默认值
            TextView profileUsername = findViewById(R.id.profileUsername);
            profileUsername.setText("未登录");
            
            TextView profilePhone = findViewById(R.id.profilePhone);
            profilePhone.setText("");
            
            TextView profileAccount = findViewById(R.id.profileAccount);
            profileAccount.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理返回按钮点击事件
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

