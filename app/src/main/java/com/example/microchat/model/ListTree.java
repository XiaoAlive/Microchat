package com.example.microchat.model;

import com.example.microchat.adapter.ContactsPageListAdapter.GroupNode;
import com.example.microchat.adapter.ContactsPageListAdapter.ContactNode;
import com.example.microchat.adapter.ContactsPageListAdapter.GroupInfo;
import com.example.microchat.adapter.ContactsPageListAdapter.ContactInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人树形结构管理类
 * 用于存储和管理联系人数据，支持在Activity间共享
 */
public class ListTree {
    // 为了兼容SearchActivity中的引用，添加TreeNode内部类
    public static class TreeNode {
        private com.example.microchat.adapter.ContactsPageListAdapter.TreeNode actualNode;
        
        public TreeNode(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode actualNode) {
            this.actualNode = actualNode;
        }
        
        public Object getData() {
            return actualNode.getData();
        }
        
        public TreeNode getParent() {
            com.example.microchat.adapter.ContactsPageListAdapter.TreeNode parentNode = getParentOfActualNode(actualNode);
            return parentNode != null ? new TreeNode(parentNode) : null;
        }
        
        // 由于ContactsPageListAdapter.TreeNode的parent是protected的，我们需要通过反射获取
        // 这里简化实现，直接返回null，实际使用中需要处理
        private com.example.microchat.adapter.ContactsPageListAdapter.TreeNode getParentOfActualNode(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node) {
            try {
                java.lang.reflect.Field parentField = com.example.microchat.adapter.ContactsPageListAdapter.TreeNode.class.getDeclaredField("parent");
                parentField.setAccessible(true);
                return (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode) parentField.get(node);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
    private List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> rootNodes;
    
    // 枚举位置内部类，用于遍历树节点
    public static class EnumPos {
        private com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node;
        private int index;
        
        public EnumPos(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node, int index) {
            this.node = node;
            this.index = index;
        }
    }
    
    public ListTree() {
        rootNodes = new ArrayList<>();
    }
    
    /**
     * 添加根节点
     * @param node 要添加的根节点
     */
    public void addRootNode(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node) {
        rootNodes.add(node);
    }
    
    /**
     * 获取所有根节点
     * @return 根节点列表
     */
    public List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> getRootNodes() {
        return rootNodes;
    }
    
    /**
     * 清除所有节点
     */
    public void clear() {
        rootNodes.clear();
    }
    
    /**
     * 获取节点总数
     * @return 节点总数
     */
    public int getNodeCount() {
        int count = 0;
        for (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node : rootNodes) {
            count += countNodes(node);
        }
        return count;
    }
    
    /**
     * 递归计算节点总数
     * @param node 起始节点
     * @return 包含子节点的总数
     */
    private int countNodes(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node) {
        int count = 1; // 当前节点
        // 检查节点是否为GroupNode类型
        if (node.getClass().getName().endsWith("GroupNode")) {
            try {
                // 使用反射获取children字段
                java.lang.reflect.Field childrenField = node.getClass().getDeclaredField("children");
                childrenField.setAccessible(true);
                java.util.List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> children = 
                    (java.util.List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode>) childrenField.get(node);
                for (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode child : children) {
                    count += countNodes(child);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }
    
    /**
     * 开始枚举节点
     * @return 第一个节点的位置，如果没有节点则返回null
     */
    public EnumPos startEnumNode() {
        if (rootNodes.isEmpty()) {
            return null;
        }
        return new EnumPos(rootNodes.get(0), 0);
    }
    
    /**
     * 根据枚举位置获取节点
     * @param pos 枚举位置
     * @return 对应的节点
     */
    public TreeNode getNodeByEnumPos(EnumPos pos) {
        return new TreeNode(pos.node);
    }
    
    /**
     * 获取下一个节点的枚举位置
     * @param currentPos 当前节点的枚举位置
     * @return 下一个节点的位置，如果没有下一个节点则返回null
     */
    public EnumPos enumNext(EnumPos currentPos) {
        com.example.microchat.adapter.ContactsPageListAdapter.TreeNode currentNode = currentPos.node;
        int currentIndex = currentPos.index;
        
        // 先尝试遍历当前节点的子节点
        if (!currentNode.getChildren().isEmpty()) {
            return new EnumPos(currentNode.getChildren().get(0), 0);
        }
        
        // 然后尝试查找兄弟节点或父节点的兄弟节点
        com.example.microchat.adapter.ContactsPageListAdapter.TreeNode parent = getParentNode(currentNode);
        
        if (parent == null) {
            // 如果是根节点，查找下一个根节点
            for (int i = currentIndex + 1; i < rootNodes.size(); i++) {
                return new EnumPos(rootNodes.get(i), i);
            }
        } else {
            // 查找兄弟节点
            List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> siblings = parent.getChildren();
            for (int i = siblings.indexOf(currentNode) + 1; i < siblings.size(); i++) {
                return new EnumPos(siblings.get(i), i);
            }
            
            // 递归查找父节点的兄弟节点
            EnumPos parentNextPos = enumNextParentSibling(parent, rootNodes);
            if (parentNextPos != null) {
                return parentNextPos;
            }
        }
        
        return null; // 没有下一个节点
    }
    
    /**
     * 获取节点的父节点
     * @param node 节点
     * @return 父节点，如果是根节点则返回null
     */
    private com.example.microchat.adapter.ContactsPageListAdapter.TreeNode getParentNode(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node) {
        // 使用反射获取protected的parent属性
        try {
            java.lang.reflect.Field parentField = com.example.microchat.adapter.ContactsPageListAdapter.TreeNode.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            return (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode) parentField.get(node);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果反射失败，尝试通过递归查找
            for (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode root : rootNodes) {
                com.example.microchat.adapter.ContactsPageListAdapter.TreeNode parent = findParent(root, node);
                if (parent != null) {
                    return parent;
                }
            }
            return null;
        }
    }
    
    /**
     * 递归查找节点的父节点
     * @param parent 当前父节点候选
     * @param child 要查找父节点的子节点
     * @return 如果找到则返回父节点，否则返回null
     */
    private com.example.microchat.adapter.ContactsPageListAdapter.TreeNode findParent(
            com.example.microchat.adapter.ContactsPageListAdapter.TreeNode parent,
            com.example.microchat.adapter.ContactsPageListAdapter.TreeNode child) {
        // 如果当前节点就是子节点的父节点
        if (parent.getClass().getName().endsWith("GroupNode")) {
            try {
                java.lang.reflect.Field childrenField = parent.getClass().getDeclaredField("children");
                childrenField.setAccessible(true);
                java.util.List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> children = 
                    (java.util.List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode>) childrenField.get(parent);
                
                if (children.contains(child)) {
                    return parent;
                }
                
                // 递归查找子节点的子节点
                for (com.example.microchat.adapter.ContactsPageListAdapter.TreeNode subChild : children) {
                    com.example.microchat.adapter.ContactsPageListAdapter.TreeNode found = findParent(subChild, child);
                    if (found != null) {
                        return found;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * 递归查找父节点的兄弟节点
     */
    private EnumPos enumNextParentSibling(com.example.microchat.adapter.ContactsPageListAdapter.TreeNode node, List<com.example.microchat.adapter.ContactsPageListAdapter.TreeNode> siblings) {
        for (int i = siblings.indexOf(node) + 1; i < siblings.size(); i++) {
            return new EnumPos(siblings.get(i), i);
        }
        
        com.example.microchat.adapter.ContactsPageListAdapter.TreeNode parent = getParentNode(node);
        if (parent != null) {
            // 查找祖父节点的子节点中，parent的兄弟节点
            com.example.microchat.adapter.ContactsPageListAdapter.TreeNode grandparent = getParentNode(parent);
            if (grandparent != null) {
                return enumNextParentSibling(parent, grandparent.getChildren());
            } else {
                // parent是根节点
                return enumNextParentSibling(parent, rootNodes);
            }
        }
        
        return null;
    }
}