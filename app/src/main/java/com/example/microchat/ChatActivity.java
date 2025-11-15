package com.example.microchat;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    //存放一条消息之数据的类
    public static class ChatMessage{
        String contactName;//联系人的名字
        Date time;//日期
        String content;//消息的内容
        boolean isMe;//这个消息是不是我发出的?

        //构造方法
        public ChatMessage(String contactName, Date time, String content, boolean isMe) {
            this.contactName = contactName;
            this.time = time;
            this.content = content;
            this.isMe = isMe;
        }
    }

    //存放所有的聊天消息
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置Layout
        setContentView(R.layout.activity_chat);
        //设置动作栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //获取启动此Activity时传过来的数据
        //在启动聊天界面时，通过此方式把对方的名字传过来
        String contactName=getIntent().getStringExtra("contact_name");
        if(contactName!=null){
            toolbar.setTitle(contactName);
        }

        setSupportActionBar(toolbar);
        //设置显示动作栏上的返回图标
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //获取Recycler控件并设置适配器
        RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ChatMessagesAdapter());

        //响应按钮的点击，发出消息
        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //从EditText控件取得消息
                EditText editText = findViewById(R.id.editMessage);
                String msg = editText.getText().toString().trim();
                
                //检查消息是否为空
                if (msg.isEmpty()) {
                    return;
                }
                
                //清空输入框，这样用户可以继续输入
                editText.setText("");
                
                //添加自己发送的消息
                ChatMessage myMessage = new ChatMessage("我", new Date(), msg, true);
                chatMessages.add(myMessage);
                
                //通知RecyclerView刷新
                recyclerView.getAdapter().notifyItemInserted(chatMessages.size() - 1);
                
                //让RecyclerView向下滚动，以显示最新的消息
                recyclerView.scrollToPosition(chatMessages.size() - 1);
                
                //模拟对方回复
                //使用postDelayed给一个小延迟，让用户体验更真实
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //添加对方的回复
                        ChatMessage replyMessage = new ChatMessage("对方", new Date(), "你是谁?你妈贵姓?", false);
                        chatMessages.add(replyMessage);
                        
                        //通知RecyclerView刷新
                        recyclerView.getAdapter().notifyItemInserted(chatMessages.size() - 1);
                        
                        //滚动到底部
                        recyclerView.scrollToPosition(chatMessages.size() - 1);
                    }
                }, 500); //500毫秒延迟
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //当点击动作栏上的返回图标时执行
            //关闭自己，返回来时的页面
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //为RecyclerView提供数据的适配器
    public class ChatMessagesAdapter extends
            RecyclerView.Adapter<ChatMessagesAdapter.MyViewHolder> {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //参数viewType即行的Layout资源Id，由getItemViewType()的返回值决定的
            View itemView = getLayoutInflater().inflate(viewType,parent,false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            ChatMessage message = chatMessages.get(position);
            holder.textView.setText(message.content);
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        //有两种行layout，所以Override此方法
        @Override
        public int getItemViewType(int position) {
            ChatMessage message = chatMessages.get(position);
            if(message.isMe) {
                //如果是我的，靠右显示
                return R.layout.chat_message_right_item;
            }else{
                //对方的，靠左显示
                return R.layout.chat_message_left_item;
            }
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            private TextView textView;
            private ImageView imageView;

            public MyViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}