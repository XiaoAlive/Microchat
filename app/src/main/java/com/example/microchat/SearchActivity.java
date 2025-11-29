package com.example.microchat;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.model.ListTree;
import com.example.microchat.service.ChatService;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageButton btnClear;
    private TextView tvCancel;
    private TextView tvNoUser;
    private RecyclerView resultListView;
    
    private Retrofit retrofit;
    private ChatService chatService;
    private SearchResultAdapter adapter;
    private List<ContactsPageListAdapter.ContactInfo> searchResultList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 初始化Retrofit
        initRetrofit();
        
        // 初始化视图
        initViews();
        
        // 设置监听器
        initListeners();
        
        // 处理传入的搜索关键词
        handleIntent();
    }

    private void initRetrofit() {
        // 从SharedPreferences获取服务器地址
        String serverAddress = getSharedPreferences("app_config", 0)
                .getString("server_addr", "http://10.0.2.2:8080");
        
        retrofit = new Retrofit.Builder()
                .baseUrl(serverAddress)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        
        chatService = retrofit.create(ChatService.class);
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnClear = findViewById(R.id.btn_clear);
        tvCancel = findViewById(R.id.tvCancel);
        tvNoUser = findViewById(R.id.tv_no_user);
        resultListView = findViewById(R.id.resultListView);
        
        // 初始化RecyclerView
        adapter = new SearchResultAdapter();
        resultListView.setLayoutManager(new LinearLayoutManager(this));
        resultListView.setAdapter(adapter);
    }

    private void initListeners() {
        // 取消按钮点击事件
        tvCancel.setOnClickListener(v -> finish());
        
        // 清除按钮点击事件
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            clearSearchResults();
        });
        
        // 搜索框文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 显示/隐藏清除按钮
                if (s.length() > 0) {
                    btnClear.setVisibility(View.VISIBLE);
                } else {
                    btnClear.setVisibility(View.GONE);
                    clearSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 搜索按钮点击事件
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                searchUser(keyword);
            }
            return true;
        });
    }
    
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("search_keyword")) {
            String keyword = intent.getStringExtra("search_keyword");
            etSearch.setText(keyword);
            etSearch.setSelection(keyword.length());
            searchUser(keyword);
        }
    }
    
    private void searchUser(String keyword) {
        // 显示加载状态
        showLoading();
        
        // 添加调试信息
        String serverAddress = getSharedPreferences("app_config", 0)
                .getString("server_addr", "http://10.0.2.2:8080");
        android.util.Log.d("SearchActivity", "搜索关键词: " + keyword);
        android.util.Log.d("SearchActivity", "服务器地址: " + serverAddress);
        
        // 调用服务器搜索接口
        chatService.searchUser(keyword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ServerResult<ContactsPageListAdapter.ContactInfo>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(ServerResult<ContactsPageListAdapter.ContactInfo> result) {
                        android.util.Log.d("SearchActivity", "服务器响应 - retCode: " + result.getRetCode() + ", errMsg: " + result.getErrMsg());
                        
                        if (result != null && result.getRetCode() == 0) {
                            // 搜索成功
                            if (result.getData() != null) {
                                // 显示用户信息
                                android.util.Log.d("SearchActivity", "找到用户: " + result.getData().getName());
                                showUserResult(result.getData());
                            } else {
                                // 用户不存在
                                android.util.Log.d("SearchActivity", "用户不存在");
                                showNoUser();
                            }
                        } else {
                            // 搜索失败
                            android.util.Log.d("SearchActivity", "搜索失败，retCode: " + result.getRetCode());
                            showNoUser();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 网络错误或服务器错误
                        android.util.Log.e("SearchActivity", "搜索错误: " + e.getMessage(), e);
                        showNoUser();
                        Toast.makeText(SearchActivity.this, "搜索失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {}
                });
    }
    
    private void showLoading() {
        tvNoUser.setVisibility(View.GONE);
        resultListView.setVisibility(View.GONE);
    }
    
    private void showUserResult(ContactsPageListAdapter.ContactInfo userInfo) {
        searchResultList.clear();
        searchResultList.add(userInfo);
        adapter.notifyDataSetChanged();
        
        tvNoUser.setVisibility(View.GONE);
        resultListView.setVisibility(View.VISIBLE);
    }
    
    private void showNoUser() {
        searchResultList.clear();
        adapter.notifyDataSetChanged();
        
        tvNoUser.setVisibility(View.VISIBLE);
        resultListView.setVisibility(View.GONE);
    }
    
    private void clearSearchResults() {
        searchResultList.clear();
        adapter.notifyDataSetChanged();
        
        tvNoUser.setVisibility(View.GONE);
        resultListView.setVisibility(View.GONE);
    }

    // 搜索结果适配器
    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.search_result_user_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ContactsPageListAdapter.ContactInfo userInfo = searchResultList.get(position);
            
            // 设置用户名
            holder.tvUsername.setText(userInfo.getName());
            
            // 设置账号（手机号搜索时显示账号）
            holder.tvAccount.setText("账号：" + userInfo.getAccount());
            
            // 加载头像
            String avatarUrl = userInfo.getAvatarUrl();
            String serverHost = getSharedPreferences("app_config", 0)
                    .getString("server_addr", "http://10.0.2.2:8080");
            
            android.util.Log.d("SearchActivity", "头像URL: " + avatarUrl);
            android.util.Log.d("SearchActivity", "服务器主机: " + serverHost);
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String imgURL;
                if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                    imgURL = avatarUrl;
                } else {
                    // 处理头像URL路径，确保没有双斜杠
                    String cleanAvatarUrl = avatarUrl.startsWith("/") ? avatarUrl.substring(1) : avatarUrl;
                    imgURL = serverHost + (serverHost.endsWith("/") ? "" : "/") + cleanAvatarUrl;
                }
                
                android.util.Log.d("SearchActivity", "完整头像URL: " + imgURL);
                
                Glide.with(SearchActivity.this)
                        .load(imgURL)
                        .placeholder(R.drawable.contacts_normal)
                        .error(R.drawable.contacts_normal)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.contacts_normal);
            }
            
            // 判断是否为当前用户自己
            ContactsPageListAdapter.ContactInfo myInfo = MainActivity.myInfo;
            boolean isCurrentUser = myInfo != null && 
                    (userInfo.getAccount().equals(myInfo.getAccount()) || 
                     userInfo.getPhone().equals(myInfo.getPhone()));
            
            // 检查是否已经是好友（需要从服务器获取好友列表）
            boolean isAlreadyFriend = isCurrentUser || checkIfFriend(userInfo);
            
            // 设置按钮状态
            if (isAlreadyFriend || isCurrentUser) {
                holder.btnAdd.setText("已添加");
                holder.btnAdd.setBackgroundResource(R.drawable.button_already_added_background);
                holder.btnAdd.setTextColor(getResources().getColor(R.color.gray));
                holder.btnAdd.setEnabled(false);
            } else {
                holder.btnAdd.setText("添加");
                holder.btnAdd.setBackgroundResource(R.drawable.button_add_background);
                holder.btnAdd.setTextColor(getResources().getColor(R.color.white));
                holder.btnAdd.setEnabled(true);
            }
            
            // 添加按钮点击事件
            holder.btnAdd.setOnClickListener(v -> {
                if (!isAlreadyFriend && !isCurrentUser) {
                    addFriend(userInfo);
                }
            });
        }

        @Override
        public int getItemCount() {
            return searchResultList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvUsername;
            TextView tvAccount;
            Button btnAdd;

            public ViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvAccount = itemView.findViewById(R.id.tv_account);
                btnAdd = itemView.findViewById(R.id.btn_add);
            }
        }
    }
    
    // 检查是否已经是好友
    private boolean checkIfFriend(ContactsPageListAdapter.ContactInfo userInfo) {
        // 这里需要从服务器获取当前用户的好友列表，然后检查是否包含该用户
        // 由于目前联系人列表已经缓存，可以直接检查缓存
        if (MainActivity.myInfo != null) {
            // 获取联系人列表
            List<ContactsPageListAdapter.ContactInfo> contacts = getCachedContacts();
            for (ContactsPageListAdapter.ContactInfo contact : contacts) {
                if (contact.getId() == userInfo.getId()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // 获取缓存的好友列表
    private List<ContactsPageListAdapter.ContactInfo> getCachedContacts() {
        List<ContactsPageListAdapter.ContactInfo> contacts = new ArrayList<>();
        if (MainFragment.getContactsTree() != null) {
            try {
                // 遍历树形结构获取所有联系人
                ListTree.EnumPos pos = MainFragment.getContactsTree().startEnumNode();
                while (pos != null) {
                    ListTree.TreeNode treeNode = MainFragment.getContactsTree().getNodeByEnumPos(pos);
                    if (treeNode != null && treeNode.getData() instanceof ContactsPageListAdapter.ContactInfo) {
                        contacts.add((ContactsPageListAdapter.ContactInfo) treeNode.getData());
                    }
                    pos = MainFragment.getContactsTree().enumNext(pos);
                }
            } catch (Exception e) {
                android.util.Log.e("SearchActivity", "获取缓存联系人失败: " + e.getMessage());
            }
        }
        return contacts;
    }
    
    // 添加好友方法
    private void addFriend(ContactsPageListAdapter.ContactInfo userInfo) {
        // 获取当前用户信息
        ContactsPageListAdapter.ContactInfo myInfo = MainActivity.myInfo;
        if (myInfo == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("userId", myInfo.getId());
        params.put("friendId", userInfo.getId());
        params.put("remark", userInfo.getName());
        
        // 调用添加好友API
        chatService.addFriend(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ServerResult<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(ServerResult<String> result) {
                        if (result != null && result.getRetCode() == 0) {
                            // 添加成功
                            Toast.makeText(SearchActivity.this, "添加好友成功", Toast.LENGTH_SHORT).show();
                            
                            // 发送广播通知联系人页面更新
                            Intent intent = new Intent("FRIEND_ADDED");
                            sendBroadcast(intent);
                            
                            // 刷新当前页面
                            adapter.notifyDataSetChanged();
                        } else {
                            // 添加失败
                            String errorMsg = result != null ? result.getErrMsg() : "添加失败";
                            Toast.makeText(SearchActivity.this, "添加失败：" + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 网络错误
                        Toast.makeText(SearchActivity.this, "网络错误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {}
                });
    }
}