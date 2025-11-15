package com.example.microchat.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import androidx.cardview.widget.CardView;
import com.example.microchat.R;
import android.widget.TextView;

/**
 * 通用搜索框组件
 * 用于在不同页面中提供统一的搜索入口
 */
public class CommonSearchView {
    private View rootView;
    private CardView searchContainer;
    private TextView searchHint;
    private Context context;

    /**
     * 构造函数
     * @param context 上下文
     */
    public CommonSearchView(Context context) {
        this.context = context;
        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        // 加载布局
        rootView = LayoutInflater.from(context).inflate(R.layout.common_search_view, null);
        
        // 绑定控件
        searchContainer = rootView.findViewById(R.id.common_search_view);
        searchHint = rootView.findViewById(R.id.search_hint);
    }

    /**
     * 设置搜索框点击监听器
     * @param listener 点击监听器
     */
    public void setOnSearchClickListener(View.OnClickListener listener) {
        if (searchContainer != null) {
            searchContainer.setOnClickListener(listener);
        }
    }

    /**
     * 设置搜索提示文本
     * @param hint 提示文本
     */
    public void setSearchHint(String hint) {
        if (searchHint != null) {
            searchHint.setText(hint);
        }
    }

    /**
     * 获取根视图
     * @return 根视图
     */
    public View getView() {
        return rootView;
    }
}