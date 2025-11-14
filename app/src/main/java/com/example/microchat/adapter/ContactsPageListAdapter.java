package com.example.microchat.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.microchat.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsPageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //树形节点基类
    public static abstract class TreeNode {
        protected Object data;
        protected int level = 0;
        protected boolean expanded = false;
        protected List<TreeNode> children = new ArrayList<>();
        protected TreeNode parent;

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
    public static class ContactInfo {
        private Bitmap avatar; //头像
        private String name; //名字
        private String status; //状态

        public ContactInfo(Bitmap avatar, String name, String status) {
            this.avatar = avatar;
            this.name = name;
            this.status = status;
        }

        public Bitmap getAvatar() {
            return avatar;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
    }

    private List<TreeNode> mVisibleNodes = new ArrayList<>();
    private List<TreeNode> mAllNodes = new ArrayList<>();

    public ContactsPageListAdapter() {
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

    //切换组节点的展开/折叠状态
    public void toggleGroup(int position) {
        TreeNode node = mVisibleNodes.get(position);
        if (node instanceof GroupNode && node.isExpandable()) {
            GroupNode groupNode = (GroupNode) node;
            boolean expanded = !groupNode.isExpanded();
            groupNode.setExpanded(expanded);
            
            int startPosition = position + 1;
            int childCount = 0;
            
            if (expanded) {
                //展开组，添加所有子节点到可见列表
                for (TreeNode child : groupNode.getChildren()) {
                    mVisibleNodes.add(startPosition + childCount, child);
                    childCount++;
                }
                notifyItemRangeInserted(startPosition, childCount);
            } else {
                //折叠组，移除所有子节点
                childCount = removeAllChildren(groupNode, startPosition);
                notifyItemRangeRemoved(startPosition, childCount);
            }
            
            //通知组节点自身状态改变
            notifyItemChanged(position);
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
        return mVisibleNodes.size();
    }

    //获取节点类型（布局ID）
    @Override
    public int getItemViewType(int position) {
        return mVisibleNodes.get(position).getLayoutId();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == R.layout.contacts_group_item) {
            View view = inflater.inflate(viewType, parent, false);
            return new GroupViewHolder(view);
        } else if (viewType == R.layout.contacts_contact_item) {
            View view = inflater.inflate(viewType, parent, false);
            return new ContactViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TreeNode node = mVisibleNodes.get(position);
        
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
            groupHolder.textViewCount.setText(groupNode.getGroupInfo().getOnlineCount() + "/" + groupNode.getChildrenCount());
            
            //设置展开/折叠图标
            if (groupNode.isExpandable() && groupNode.getChildrenCount() > 0) {
                groupHolder.imageViewExpand.setVisibility(View.VISIBLE);
                if (groupNode.isExpanded()) {
                    groupHolder.imageViewExpand.setImageResource(android.R.drawable.arrow_up_float);
                } else {
                    groupHolder.imageViewExpand.setImageResource(android.R.drawable.arrow_down_float);
                }
                
                //设置点击事件
                groupHolder.imageViewExpand.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleGroup(position);
                    }
                });
            } else {
                groupHolder.imageViewExpand.setVisibility(View.GONE);
            }
        } else if (holder instanceof ContactViewHolder && node instanceof ContactNode) {
            ContactNode contactNode = (ContactNode) node;
            ContactViewHolder contactHolder = (ContactViewHolder) holder;
            
            ContactInfo info = contactNode.getContactInfo();
            if (info.getAvatar() != null) {
                contactHolder.imageViewHead.setImageBitmap(info.getAvatar());
            } else {
                contactHolder.imageViewHead.setImageResource(R.drawable.ic_launcher_background); //默认头像
            }
            contactHolder.textViewTitle.setText(info.getName());
            contactHolder.textViewDetail.setText(info.getStatus());
        }
    }

    //组ViewHolder
    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;    //显示标题的控件
        TextView textViewCount;    //显示好友数/在线数的控件
        ImageView imageViewExpand; //展开/折叠图标

        public GroupViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewCount = itemView.findViewById(R.id.textViewCount);
            //添加展开/折叠图标
            imageViewExpand = new ImageView(itemView.getContext());
            if (itemView instanceof ViewGroup) {
                ((ViewGroup) itemView).addView(imageViewExpand);
            }
        }
    }

    //联系人ViewHolder
    class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewHead;   //显示好友头像的控件
        TextView textViewTitle;    //显示好友名字的控件
        TextView textViewDetail;   //显示好友状态的控件

        public ContactViewHolder(View itemView) {
            super(itemView);
            imageViewHead = itemView.findViewById(R.id.imageViewHead);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDetail = itemView.findViewById(R.id.textViewDetail);
        }
    }
}