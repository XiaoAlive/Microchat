package com.example.microchat.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.microchat.ChatActivity;
import com.example.microchat.MainActivity;
import com.example.microchat.R;
import com.example.microchat.model.ListTree;
import com.bumptech.glide.Glide;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ContactsPageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //树形节点基类
    //节点类型常量
    public static final int NODE_TYPE_GROUP = 0; //组节点类型
    public static final int NODE_TYPE_CONTACT = 1; //联系人节点类型
    public static final int NODE_TYPE_SEARCH = 2; //搜索节点类型
    public static final int NODE_TYPE_HEADER = 3; //首字母标题类型
    
    public static abstract class TreeNode {
        protected Object data;
        protected int level = 0;
        protected boolean expanded = false;
        protected List<TreeNode> children = new ArrayList<>();
        protected TreeNode parent;
        
        public abstract int getType();

        public TreeNode(Object data, int level) {
            this.data = data;
            this.level = level;
        }

        public abstract int getLayoutId();
        public abstract boolean isExpandable();

        public void addChild(TreeNode child) {
            child.parent = this;
            children.add(child);
        }

        public List<TreeNode> getChildren() {
            return children;
        }

        public Object getData() {
            return data;
        }

        public int getLevel() {
            return level;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public int getChildrenCount() {
            return children.size();
        }
    }

    //组节点
    public static class GroupNode extends TreeNode {
        public GroupNode(GroupInfo data, int level) {
            super(data, level);
        }

        @Override
        public int getLayoutId() {
            return R.layout.contacts_group_item;
        }

        @Override
        public boolean isExpandable() {
            return true;
        }
        
        @Override
        public int getType() {
            return NODE_TYPE_GROUP;
        }

        public GroupInfo getGroupInfo() {
            return (GroupInfo) data;
        }
    }

    //联系人节点
    public static class ContactNode extends TreeNode {
        public ContactNode(ContactInfo data, int level) {
            super(data, level);
        }

        @Override
        public int getLayoutId() {
            return R.layout.contacts_contact_item;
        }

        @Override
        public boolean isExpandable() {
            return false;
        }
        
        @Override
        public int getType() {
            return NODE_TYPE_CONTACT;
        }

        public ContactInfo getContactInfo() {
            return (ContactInfo) data;
        }
    }

    //组数据
    public static class GroupInfo {
        private String title; //组标题
        private int onlineCount; //此组内在线的人数

        public GroupInfo(String title, int onlineCount) {
            this.title = title;
            this.onlineCount = onlineCount;
        }

        public String getTitle() {
            return title;
        }

        public int getOnlineCount() {
            return onlineCount;
        }
    }

    //联系人数据
    //实现Serializable接口是为了在Activity间传递
    public static class ContactInfo implements Serializable {
        //头像在服务器的路径
        private int id;
        @com.google.gson.annotations.SerializedName("name")
        private String name; //名字，从服务器的name字段映射
        private String status; //状态
        private String avatarUrl; //头像URL，如果为空则使用默认头像
        private String phone; //电话号码
        private String account; //10位数字账号

        // 无参构造函数，Gson反序列化需要
        public ContactInfo() {
        }

        public ContactInfo(int id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.avatarUrl = null; // 初始化为空，表示使用默认头像
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getAvatarUrl() {
            // 如果avatarUrl为空，返回默认头像路径
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                return "/image/head/" + id + ".png"; // 默认头像路径
            }
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        // 检查是否使用默认头像
        public boolean isUsingDefaultAvatar() {
            return avatarUrl == null || avatarUrl.isEmpty();
        }
    }

    private List<TreeNode> mVisibleNodes = new ArrayList<>();
    private List<TreeNode> mAllNodes = new ArrayList<>();
    private boolean showSearchBox = false;
    private OnSearchClickListener searchClickListener;
    private TreeNode searchNode = new TreeNode(null, 0) {
        @Override
        public int getLayoutId() {
            return R.layout.common_search_view;
        }
        
        @Override
        public boolean isExpandable() {
            return false;
        }
        
        @Override
        public int getType() {
            return NODE_TYPE_SEARCH;
        }
    };

    public ContactsPageListAdapter() {
    }

    //清空所有节点
    public void clearAllNodes() {
        mAllNodes.clear();
        mVisibleNodes.clear();
        notifyDataSetChanged();
    }

    //添加根节点（组节点）
    public GroupNode addGroupNode(GroupInfo groupInfo) {
        GroupNode groupNode = new GroupNode(groupInfo, 0);
        mAllNodes.add(groupNode);
        mVisibleNodes.add(groupNode);
        notifyDataSetChanged();
        return groupNode;
    }

    //在组节点下添加联系人
    public void addContactToGroup(GroupNode groupNode, ContactInfo contactInfo) {
        ContactNode contactNode = new ContactNode(contactInfo, groupNode.getLevel() + 1);
        groupNode.addChild(contactNode);
        mAllNodes.add(contactNode);
        
        //如果组是展开的，将联系人添加到可见列表
        if (groupNode.isExpanded()) {
            int groupIndex = mVisibleNodes.indexOf(groupNode);
            if (groupIndex != -1) {
                mVisibleNodes.add(groupIndex + 1, contactNode);
                notifyItemInserted(groupIndex + 1);
            }
        }
    }
    
    //清空指定组中的所有联系人
    public void clearContactsInGroup(GroupNode groupNode) {
        // 记录组在可见列表中的位置
        int groupIndex = mVisibleNodes.indexOf(groupNode);
        boolean wasExpanded = groupNode.isExpanded();
        
        // 移除组下的所有子节点
        List<TreeNode> childrenToRemove = new ArrayList<>(groupNode.getChildren());
        for (TreeNode child : childrenToRemove) {
            groupNode.getChildren().remove(child);
            mAllNodes.remove(child);
        }
        
        // 如果组是展开的，移除可见列表中的联系人
        if (wasExpanded && groupIndex != -1) {
            int childCount = removeAllChildren(groupNode, groupIndex + 1);
            if (childCount > 0) {
                notifyItemRangeRemoved(toAdapterPosition(groupIndex + 1), childCount);
            }
        }
        
        // 更新组节点的状态（重置为折叠）
        groupNode.setExpanded(false);
        if (groupIndex != -1) {
            notifyItemChanged(toAdapterPosition(groupIndex));
        }
    }

    //切换组节点的展开/折叠状态
    public void toggleGroup(int position) {
        toggleGroup(position, null);
    }
    
    //切换组节点的展开/折叠状态（带箭头视图参数）
    public void toggleGroup(int position, ImageView arrowView) {
        if (position < 0 || position >= mVisibleNodes.size()) {
            return;
        }
        TreeNode node = mVisibleNodes.get(position);
        if (node instanceof GroupNode && node.isExpandable()) {
            GroupNode groupNode = (GroupNode) node;
            boolean expanded = !groupNode.isExpanded();
            groupNode.setExpanded(expanded);
            
            // 更新箭头方向
            if (arrowView != null) {
                updateArrowIcon(arrowView, expanded);
            }
            
            int startPosition = position + 1;
            int childCount = 0;
            
            if (expanded) {
                //展开组，添加所有子节点到可见列表
                for (TreeNode child : groupNode.getChildren()) {
                    mVisibleNodes.add(startPosition + childCount, child);
                    childCount++;
                }
                notifyItemRangeInserted(toAdapterPosition(startPosition), childCount);
            } else {
                //折叠组，移除所有子节点
                childCount = removeAllChildren(groupNode, startPosition);
                notifyItemRangeRemoved(toAdapterPosition(startPosition), childCount);
            }
            
            //通知组节点自身状态改变
            notifyItemChanged(toAdapterPosition(position));
        }
    }
    
    // 添加首字母标题节点
    public void addHeaderNode(String headerText) {
        HeaderNode headerNode = new HeaderNode(headerText);
        mAllNodes.add(headerNode);
        mVisibleNodes.add(headerNode);
        notifyDataSetChanged();
    }
    
    // 首字母标题节点类
    public static class HeaderNode extends TreeNode {
        private String headerText;
        
        public HeaderNode(String headerText) {
            super(headerText, 0);
            this.headerText = headerText;
        }
        
        public String getHeaderText() {
            return headerText;
        }
        
        @Override
        public int getLayoutId() {
            return 0; // 不再使用
        }
        
        @Override
        public boolean isExpandable() {
            return false;
        }
        
        @Override
        public int getType() {
            return NODE_TYPE_HEADER;
        }
    }

    //递归移除所有子节点
    private int removeAllChildren(TreeNode parent, int startPosition) {
        int count = 0;
        for (TreeNode child : new ArrayList<>(mVisibleNodes)) {
            if (mVisibleNodes.indexOf(child) >= startPosition && isChildOf(child, parent)) {
                mVisibleNodes.remove(child);
                count++;
            }
        }
        return count;
    }

    //检查节点是否是指定父节点的后代
    private boolean isChildOf(TreeNode node, TreeNode parent) {
        TreeNode current = node;
        while (current != null) {
            if (current.parent == parent) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    //获取可见节点数
    @Override
    public int getItemCount() {
        return showSearchBox ? mVisibleNodes.size() + 1 : mVisibleNodes.size();
    }

    //获取节点类型（布局ID）
    //设置是否显示搜索框
    public void setShowSearchBox(boolean showSearchBox) {
        if (this.showSearchBox != showSearchBox) {
            this.showSearchBox = showSearchBox;
            notifyDataSetChanged();
        }
    }

    //设置搜索框点击监听器
    public void setOnSearchClickListener(OnSearchClickListener listener) {
        this.searchClickListener = listener;
    }

    //搜索框点击监听器接口
    public interface OnSearchClickListener {
        void onSearchClick();
    }

    @Override
    public int getItemViewType(int position) {
        // 如果显示搜索框且是第一个位置，返回搜索框布局
        if (showSearchBox && position == 0) {
            return R.layout.common_search_view;
        }
        // 否则返回对应节点的类型
        int nodePosition = showSearchBox ? position - 1 : position;
        return mVisibleNodes.get(nodePosition).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == R.layout.common_search_view) {
            // 搜索框视图
            View view = inflater.inflate(viewType, parent, false);
            return new SearchViewHolder(view);
        } else if (viewType == NODE_TYPE_GROUP) {
            // 组节点视图
            View view = inflater.inflate(R.layout.contacts_group_item, parent, false);
            return new GroupViewHolder(view);
        } else if (viewType == NODE_TYPE_CONTACT) {
            // 联系人节点视图
            View view = inflater.inflate(R.layout.contacts_contact_item, parent, false);
            return new ContactViewHolder(view);
        } else if (viewType == NODE_TYPE_HEADER) {
            // 首字母标题视图
            View view = inflater.inflate(R.layout.contacts_header_item, parent, false);
            return new HeaderViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // 处理搜索框
        if (holder instanceof SearchViewHolder) {
            SearchViewHolder searchHolder = (SearchViewHolder) holder;
            searchHolder.searchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (searchClickListener != null) {
                        searchClickListener.onSearchClick();
                    }
                }
            });
            return;
        }
        
        // 处理普通节点
        int nodePosition = showSearchBox ? position - 1 : position;
        TreeNode node = mVisibleNodes.get(nodePosition);
        
        //设置缩进
        View itemView = holder.itemView;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.leftMargin = node.getLevel() * 30; //每个级别缩进30dp
        itemView.setLayoutParams(params);
        
        //绑定数据
        if (holder instanceof GroupViewHolder && node instanceof GroupNode) {
            GroupNode groupNode = (GroupNode) node;
            GroupViewHolder groupHolder = (GroupViewHolder) holder;
            
            groupHolder.textViewTitle.setText(groupNode.getGroupInfo().getTitle());
            
            //设置展开/折叠图标（始终展示箭头）
            groupHolder.imageViewExpand.setVisibility(View.VISIBLE);
            updateArrowIcon(groupHolder.imageViewExpand, groupNode.isExpanded());
            
            View.OnClickListener toggleListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int adapterPos = groupHolder.getAdapterPosition();
                    if (adapterPos == RecyclerView.NO_POSITION) {
                        return;
                    }
                    int nodePos = showSearchBox ? adapterPos - 1 : adapterPos;
                    if (nodePos < 0) {
                        return;
                    }
                    toggleGroup(nodePos, groupHolder.imageViewExpand);
                }
            };
            groupHolder.itemView.setOnClickListener(toggleListener);
            groupHolder.imageViewExpand.setOnClickListener(toggleListener);
        } else if (holder instanceof ContactViewHolder && node instanceof ContactNode) {
            ContactNode contactNode = (ContactNode) node;
            ContactViewHolder contactHolder = (ContactViewHolder) holder;
            
            ContactInfo info = contactNode.getContactInfo();
            
            // 使用Glide下载网络图片并设置到图像控件中
            String imgURL = MainActivity.serverHostURL + info.getAvatarUrl();
            Glide.with(contactHolder.itemView.getContext()).load(imgURL).placeholder(R.drawable.contacts_focus).into(contactHolder.imageViewHead);
            
            contactHolder.textViewTitle.setText(info.getName());
            contactHolder.textViewDetail.setText(info.getStatus());
        } else if (holder instanceof HeaderViewHolder && node instanceof HeaderNode) {
            HeaderNode headerNode = (HeaderNode) node;
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.textViewHeader.setText(headerNode.getHeaderText());
        }
    }

    //组ViewHolder
    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;    //显示标题的控件
        ImageView imageViewExpand; //展开/折叠图标

        public GroupViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            imageViewExpand = itemView.findViewById(R.id.imageViewExpand); // 获取箭头视图
        }
    }
    
    //首字母标题ViewHolder
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHeader; //首字母标题

        public HeaderViewHolder(View itemView) {
            super(itemView);
            textViewHeader = itemView.findViewById(R.id.header_text);
        }
    }
    
    // 按首字母排序联系人并生成首字母分组视图
    public void generateAlphabetSortedContacts(List<ContactNode> allContacts) {
        // 清空当前节点列表
        mAllNodes.clear();
        mVisibleNodes.clear();
        
        // 使用TreeMap自动按键排序
        TreeMap<String, List<ContactNode>> sortedContacts = new TreeMap<>(new java.util.Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if ("#".equals(o1) && "#".equals(o2)) {
                    return 0;
                }
                if ("#".equals(o1)) {
                    return 1;
                }
                if ("#".equals(o2)) {
                    return -1;
                }
                return o1.compareTo(o2);
            }
        });
        
        // 遍历所有联系人，按首字母分组
        for (ContactNode contact : allContacts) {
            if (contact.getContactInfo() != null && contact.getContactInfo().getName() != null) {
                String name = contact.getContactInfo().getName();
                if (!name.isEmpty()) {
                    // 获取联系人名字的首字母（这里简化处理）
                    String firstLetter = name.substring(0, 1).toUpperCase();
                    
                    // 如果首字母不是A-Z的字母，使用#代替
                    if (!firstLetter.matches("[A-Z]") && !firstLetter.matches("[a-z]")) {
                        firstLetter = "#";
                    } else {
                        firstLetter = firstLetter.toUpperCase();
                    }
                    
                    // 将联系人添加到对应的首字母分组中
                    List<ContactNode> contactsInGroup = sortedContacts.get(firstLetter);
                    if (contactsInGroup == null) {
                        contactsInGroup = new ArrayList<>();
                        sortedContacts.put(firstLetter, contactsInGroup);
                    }
                    contactsInGroup.add(contact);
                }
            }
        }
        
        // 遍历排序后的联系人，生成视图
        for (Map.Entry<String, List<ContactNode>> entry : sortedContacts.entrySet()) {
            String letter = entry.getKey();
            List<ContactNode> contacts = entry.getValue();
            java.util.Collections.sort(contacts, new java.util.Comparator<ContactNode>() {
                @Override
                public int compare(ContactNode o1, ContactNode o2) {
                    String name1 = o1.getContactInfo().getName();
                    String name2 = o2.getContactInfo().getName();
                    if (name1 == null) {
                        return -1;
                    }
                    if (name2 == null) {
                        return 1;
                    }
                    return name1.compareToIgnoreCase(name2);
                }
            });
            
            // 添加首字母标题
            HeaderNode headerNode = new HeaderNode(letter);
            mAllNodes.add(headerNode);
            mVisibleNodes.add(headerNode);
            
            // 添加该分组下的所有联系人
            mAllNodes.addAll(contacts);
            mVisibleNodes.addAll(contacts);
        }
        
        // 通知数据变化
        notifyDataSetChanged();
    }

    private void updateArrowIcon(ImageView arrowView, boolean expanded) {
        if (arrowView == null) {
            return;
        }
        arrowView.setImageResource(expanded ? R.drawable.ic_arrow_down_small : R.drawable.ic_arrow_right_small);
    }

    private int toAdapterPosition(int nodePosition) {
        if (nodePosition < 0) {
            return nodePosition;
        }
        return showSearchBox ? nodePosition + 1 : nodePosition;
    }

    //联系人ViewHolder
    class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewHead;   //显示好友头像的控件
        TextView textViewTitle;    //显示好友名字的控件
        TextView textViewDetail;   //显示好友状态的控件

        public ContactViewHolder(final View itemView) {
            super(itemView);
            imageViewHead = itemView.findViewById(R.id.imageViewHead);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDetail = itemView.findViewById(R.id.textViewDetail);

            //当点击这一行时，开始聊天
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //进入聊天页面
                    Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                    
                    //获取当前位置，考虑搜索框的偏移
                    int position = getAdapterPosition();
                    int nodePosition = showSearchBox ? position - 1 : position;
                    
                    //从可见节点列表中获取对应的联系人节点
                    TreeNode node = mVisibleNodes.get(nodePosition);
                    if (node instanceof ContactNode) {
                        ContactNode contactNode = (ContactNode) node;
                        ContactInfo info = contactNode.getContactInfo();
                        //将对方的名字作为参数传过去
                        intent.putExtra("contact_name", info.getName());
                        itemView.getContext().startActivity(intent);
                    }
                }
            });
        }
    }
    
    //搜索框ViewHolder
    class SearchViewHolder extends RecyclerView.ViewHolder {
        View searchView;
        
        public SearchViewHolder(View itemView) {
            super(itemView);
            searchView = itemView.findViewById(R.id.common_search_view);
        }
    }
}