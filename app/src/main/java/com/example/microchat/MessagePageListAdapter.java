package com.example.microchat;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

public class MessagePageListAdapter extends
        RecyclerView.Adapter<MessagePageListAdapter.MyViewHolder> {

    //用于获取
    private Activity activity;

    //创建一个带参数的构造方法，通过参数可以把Activity传过来
    public MessagePageListAdapter(Activity activity){
        this.activity = activity;
    }

    @Override
    public MessagePageListAdapter.MyViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        //从layout资源加载行View
        LayoutInflater inflater = activity.getLayoutInflater();
        View view=null;
        if(viewType == R.layout.message_list_item_search) {
            view = inflater.inflate(R.layout.message_list_item_search,
                    parent, false);
        }else{
            view = inflater.inflate(R.layout.message_list_item_normal,
                    parent, false);
        }

        MyViewHolder viewHolder=new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(
            MessagePageListAdapter.MyViewHolder holder,
            int position) {
    }

    @Override
    public int getItemCount() {
        return 10;
    }

    @Override
    public int getItemViewType(int position) {
        if(0==position){
            //只有最顶端这行是搜索
            return R.layout.message_list_item_search;
        }
        //其余各合都一样的控件
        return R.layout.message_list_item_normal;
    }

    //将ViewHolder声明为Adapter的内部类，反正外面也用不到
    class MyViewHolder extends RecyclerView.ViewHolder{
        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
