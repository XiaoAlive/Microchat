package com.example.microchat;

import android.content.Intent;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AddContactActivity extends AppCompatActivity {

    private FrameLayout containerFriend;
    private FrameLayout containerGroup;
    private Button tabFriend;
    private Button tabGroup;
    private ImageButton btnBack;
    private EditText etSearch;
    private RecyclerView recyclerGroups;
    private LinearLayout emptyGroupTip;
    private LinearLayout emptyFriendTip;
    private GroupAdapter groupAdapter;
    private List<GroupInfo> groupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        
        initViews();
        initData();
        initListeners();
    }

    private void initViews() {
        containerFriend = findViewById(R.id.container_friend);
        containerGroup = findViewById(R.id.container_group);
        tabFriend = findViewById(R.id.tab_friend);
        tabGroup = findViewById(R.id.tab_group);
        btnBack = findViewById(R.id.btn_back);
        etSearch = findViewById(R.id.et_search);
        recyclerGroups = findViewById(R.id.recycler_groups);
        emptyGroupTip = findViewById(R.id.empty_group_tip);
        emptyFriendTip = findViewById(R.id.empty_friend_tip);
    }

    private void initData() {
        // 初始化群聊数据
        groupList = new ArrayList<>();
        // 添加一些模拟数据
        groupList.add(new GroupInfo("泰拉厨房上菜团", 879, "10+人在聊天", "二次元", R.drawable.group_avatar_1));
        groupList.add(new GroupInfo("欢迎来到ZZZ~", 894, "5人在聊天", "00后", R.drawable.group_avatar_2));
        groupList.add(new GroupInfo("曲奇泡泡群", 15, "5人在聊天", "00后", R.drawable.group_avatar_3));


        // 设置群聊列表适配器
        groupAdapter = new GroupAdapter(groupList);
        recyclerGroups.setLayoutManager(new LinearLayoutManager(this));
        recyclerGroups.setAdapter(groupAdapter);
    }

    private void initListeners() {
        // 返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());

        // Tab切换事件
        tabFriend.setOnClickListener(v -> {
            switchToFriendTab();
        });

        tabGroup.setOnClickListener(v -> {
            switchToGroupTab();
        });

        // 搜索框点击事件 - 跳转到搜索页面
        etSearch.setOnClickListener(v -> {
            Intent intent = new Intent(AddContactActivity.this, SearchActivity.class);
            startActivity(intent);
        });
    }

    private void switchToFriendTab() {
        // 切换到好友标签
        tabFriend.setTextColor(getResources().getColor(R.color.black));
        tabFriend.setCompoundDrawablesWithIntrinsicBounds(null, null, null, getResources().getDrawable(R.drawable.tab_indicator));
        tabGroup.setTextColor(getResources().getColor(R.color.gray));
        tabGroup.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        
        containerFriend.setVisibility(View.VISIBLE);
        containerGroup.setVisibility(View.GONE);
        
        // 更新搜索框提示文本
        etSearch.setHint("账号/手机号/群");
    }

    private void switchToGroupTab() {
        // 切换到群聊标签
        tabGroup.setTextColor(getResources().getColor(R.color.black));
        tabGroup.setCompoundDrawablesWithIntrinsicBounds(null, null, null, getResources().getDrawable(R.drawable.tab_indicator));
        tabFriend.setTextColor(getResources().getColor(R.color.gray));
        tabFriend.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        
        containerGroup.setVisibility(View.VISIBLE);
        containerFriend.setVisibility(View.GONE);
        
        // 更新搜索框提示文本
        etSearch.setHint("群号/群名");
    }

    // 群聊信息类
    private static class GroupInfo {
        private String name;
        private int memberCount;
        private String status;
        private String tag;
        private int avatarResId;

        public GroupInfo(String name, int memberCount, String status, String tag, int avatarResId) {
            this.name = name;
            this.memberCount = memberCount;
            this.status = status;
            this.tag = tag;
            this.avatarResId = avatarResId;
        }

        public GroupInfo(String name, String tag, int avatarResId) {
            this.name = name;
            this.tag = tag;
            this.avatarResId = avatarResId;
        }

        public GroupInfo(String name, int avatarResId) {
            this.name = name;
            this.avatarResId = avatarResId;
        }

        public String getName() {
            return name;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public String getStatus() {
            return status;
        }

        public String getTag() {
            return tag;
        }

        public int getAvatarResId() {
            return avatarResId;
        }
    }

    // 群聊列表适配器
    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
        private List<GroupInfo> dataList;

        public GroupAdapter(List<GroupInfo> dataList) {
            this.dataList = dataList;
        }

        public void setData(List<GroupInfo> dataList) {
            this.dataList = dataList;
            notifyDataSetChanged();
            // 根据数据是否为空显示或隐藏空数据提示
            emptyGroupTip.setVisibility(dataList.isEmpty() ? View.VISIBLE : View.GONE);
        }

        @Override
        public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_group, parent, false);
            return new GroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(GroupViewHolder holder, int position) {
            GroupInfo group = dataList.get(position);
            holder.avatar.setImageResource(group.getAvatarResId());
            holder.name.setText(group.getName());
            
            StringBuilder infoBuilder = new StringBuilder();
            if (group.getMemberCount() > 0) {
                infoBuilder.append("\uD83D\uDC65 "); // 用户图标
                infoBuilder.append(group.getMemberCount());
            }
            if (group.getStatus() != null) {
                if (infoBuilder.length() > 0) {
                    infoBuilder.append(" ");
                }
                infoBuilder.append(group.getStatus());
            }
            if (group.getTag() != null) {
                if (infoBuilder.length() > 0) {
                    infoBuilder.append(" ");
                }
                infoBuilder.append("\uD83C\uDFE0 "); // 标签图标
                infoBuilder.append(group.getTag());
            }
            
            holder.info.setText(infoBuilder.toString());
            
            // 设置加入按钮点击事件
            holder.joinBtn.setOnClickListener(v -> {
                // 实现加入群聊的逻辑
            });
        }

        @Override
        public int getItemCount() {
            return dataList != null ? dataList.size() : 0;
        }

        class GroupViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name;
            TextView info;
            Button joinBtn;

            public GroupViewHolder(View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.group_avatar);
                name = itemView.findViewById(R.id.group_name);
                info = itemView.findViewById(R.id.group_info);
                joinBtn = itemView.findViewById(R.id.btn_join);
            }
        }
    }
}