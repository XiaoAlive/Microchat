package com.example.microchat.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.microchat.ChatActivity;
import com.example.microchat.Conversation;
import com.example.microchat.MainActivity;
import com.example.microchat.R;
import java.util.ArrayList;
import java.util.List;

public class MessagePageListAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //用于获取
    private Activity activity;
    private List<Conversation> conversationList = new ArrayList<>();
    
    // 常量，用于标识不同的View类型
    private static final int VIEW_TYPE_SEARCH = 0;
    private static final int VIEW_TYPE_CONVERSATION = 1;

    //创建一个带参数的构造方法，通过参数可以把Activity传过来
    public MessagePageListAdapter(Activity activity){
        this.activity = activity;
    }
    
    /**
     * 设置会话列表数据
     * @param conversations 会话列表
     */
    public void setConversations(List<Conversation> conversations) {
        this.conversationList.clear();
        if (conversations != null) {
            this.conversationList.addAll(conversations);
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        //从layout资源加载行View
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = null;
        
        if (viewType == VIEW_TYPE_SEARCH) {
            view = inflater.inflate(R.layout.common_search_view, parent, false);
            return new SearchViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.message_list_item_normal, parent, false);
            return new ConversationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SearchViewHolder) {
            // 搜索框的处理
            SearchViewHolder searchHolder = (SearchViewHolder) holder;
            View searchViewStub = searchHolder.itemView.findViewById(R.id.common_search_view);
            if (searchViewStub != null) {
                searchViewStub.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 跳转到搜索页面
                        Intent intent = new Intent(activity, com.example.microchat.SearchActivity.class);
                        activity.startActivity(intent);
                    }
                });
            }
        } else if (holder instanceof ConversationViewHolder) {
            // 会话项的处理
            final int conversationIndex = position - 1; // 减1是因为第一项是搜索框
            if (conversationIndex >= 0 && conversationIndex < conversationList.size()) {
                final Conversation conversation = conversationList.get(conversationIndex);
                ConversationViewHolder convHolder = (ConversationViewHolder) holder;
                
                // 设置用户名
                convHolder.nameTextView.setText(conversation.getName());
                
                // 设置最后一条消息
                convHolder.messageTextView.setText(conversation.getLastMessage());
                
                // 设置时间
                convHolder.timeTextView.setText(conversation.getFormattedTime());
                
                // 设置未读消息数
                String unreadCount = conversation.getUnreadCountDisplay();
                if (unreadCount.isEmpty()) {
                    // 如果没有未读消息，隐藏徽章
                    convHolder.badgeCardView.setVisibility(View.GONE);
                } else {
                    // 如果有未读消息，显示徽章并设置数量
                    convHolder.badgeCardView.setVisibility(View.VISIBLE);
                    convHolder.badgeTextView.setText(unreadCount);
                    // 设置徽章背景色为红色，更醒目
                    convHolder.badgeCardView.setCardBackgroundColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                }
                
                // 加载头像
                String avatarUrl = conversation.getAvatarUrl();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    // 检查URL是否已经是完整路径（以http开头）
                    String fullAvatarUrl;
                    if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                        fullAvatarUrl = avatarUrl;
                    } else {
                        // 如果不是完整路径，则拼接服务器地址
                        String serverHost = MainActivity.serverHostURL;
                        if (serverHost == null || serverHost.isEmpty()) {
                            serverHost = "http://10.0.2.2:8080"; // 使用默认值
                        }
                        
                        // 处理头像URL路径确保没有双斜杠
                        String cleanAvatarUrl = avatarUrl.startsWith("/") ? avatarUrl.substring(1) : avatarUrl;
                        fullAvatarUrl = serverHost + (serverHost.endsWith("/") ? "" : "/") + cleanAvatarUrl;
                    }
                    
                    Glide.with(activity)
                            .load(fullAvatarUrl)
                            .placeholder(R.drawable.message_normal)
                            .error(R.drawable.message_normal)
                            .into(convHolder.avatarImageView);
                } else {
                    // 没有头像，使用默认头像
                    convHolder.avatarImageView.setImageResource(R.drawable.message_normal);
                }
                
                // 设置点击事件，跳转到聊天界面
                convHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 创建Intent，跳转到聊天界面
                        Intent intent = new Intent(activity, ChatActivity.class);
                        intent.putExtra("contact_name", conversation.getName());
                        intent.putExtra("contact_id", conversation.getId());
                        activity.startActivity(intent);
                        
                        // 如果有未读消息，点击后清零
                        if (conversation.getUnreadCount() > 0) {
                            conversation.setUnreadCount(0);
                            // 通知适配器更新该行
                            notifyItemChanged(position);
                        }
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        // 搜索框 + 会话列表项数
        return 1 + conversationList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            // 只有最顶端这行是搜索框
            return VIEW_TYPE_SEARCH;
        } else {
            // 其余都是会话项
            return VIEW_TYPE_CONVERSATION;
        }
    }

    // 搜索框的ViewHolder
    static class SearchViewHolder extends RecyclerView.ViewHolder {
        public SearchViewHolder(View itemView) {
            super(itemView);
        }
    }
    
    // 会话项的ViewHolder
    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView nameTextView;
        TextView messageTextView;
        TextView timeTextView;
        androidx.cardview.widget.CardView badgeCardView;
        TextView badgeTextView;
        
        public ConversationViewHolder(View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.imageView);
            nameTextView = itemView.findViewById(R.id.textViewTitle);
            messageTextView = itemView.findViewById(R.id.textViewDetial);
            timeTextView = itemView.findViewById(R.id.textViewTime);
            badgeCardView = itemView.findViewById(R.id.cardViewBadge);
            badgeTextView = itemView.findViewById(R.id.textViewBadge);
        }
    }
}
