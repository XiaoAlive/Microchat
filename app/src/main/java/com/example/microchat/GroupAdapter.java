package com.example.microchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 群聊列表适配器
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private Context context;
    private List<GroupInfo> groupList;

    public GroupAdapter(Context context, List<GroupInfo> groupList) {
        this.context = context;
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupInfo groupInfo = groupList.get(position);
        holder.ivGroupAvatar.setImageResource(groupInfo.getAvatarResId());
        holder.tvGroupName.setText(groupInfo.getName());
        holder.tvGroupInfo.setText(groupInfo.getMemberCount() + "人 · " + groupInfo.getType());
        
        // 设置加入按钮点击事件
        holder.btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "申请加入" + groupInfo.getName(), Toast.LENGTH_SHORT).show();
                // 这里可以添加加入群聊的逻辑
            }
        });
        
        // 设置item点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 这里可以添加查看群聊详情的逻辑
                Toast.makeText(context, "查看" + groupInfo.getName() + "详情", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGroupAvatar;
        TextView tvGroupName;
        TextView tvGroupInfo;
        Button btnJoin;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGroupAvatar = itemView.findViewById(R.id.group_avatar);
            tvGroupName = itemView.findViewById(R.id.group_name);
            tvGroupInfo = itemView.findViewById(R.id.group_info);
            btnJoin = itemView.findViewById(R.id.btn_join);
        }
    }
}