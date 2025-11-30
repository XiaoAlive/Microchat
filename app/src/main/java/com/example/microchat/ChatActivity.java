package com.example.microchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.microchat.service.ChatService;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.util.Log;

public class ChatActivity extends AppCompatActivity {
    //用于网络通讯
    private Retrofit retrofit;
    private ChatService chatService;
    private Disposable uploadDisposable;
    private Disposable downloadDisposable;
    private Disposable getMessagesDisposable;
    private Disposable markAsReadDisposable;
    
    //存放所有的聊天消息
    private List<Message> chatMessages = new ArrayList<>();
    
    // 当前聊天的联系人信息
    private long contactId;
    private String contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置Layout
        setContentView(R.layout.activity_chat);
        //设置动作栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //获取启动此Activity时传过来的数据
        //在启动聊天界面时，通过此方式把对方的名字和ID传过来
        contactName = getIntent().getStringExtra("contact_name");
        contactId = getIntent().getLongExtra("contact_id", 0);
        if(contactName!=null){
            toolbar.setTitle(contactName);
        }

        setSupportActionBar(toolbar);
        //设置显示动作栏上的返回图标
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //初始化Retrofit
        retrofit = getRetrofit();
        chatService = retrofit.create(ChatService.class);

        //获取Recycler控件并设置适配器
        RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ChatMessagesAdapter());
        
        // 获取历史消息
        loadHistoryMessages();
        
        // 标记消息为已读
        markMessagesAsRead();
        
        //每隔2秒向服务端获取一下新的聊天消息
        downloadDisposable = Observable.interval(2, TimeUnit.SECONDS).flatMap(v -> {

            //创建获取聊天消息的Observable
            long currentUserId = MainActivity.myInfo.getId();
            return chatService.getMessages(currentUserId, contactId)
                    .map(result -> {
                        //判断服务端是否正确返回
                        if (result.getRetCode() == 0) {
                            //服务端无错误，返回消息列表
                            return result.getData();
                        } else {
                            //服务端出错了，抛出异常，在Observer中捕获之
                            throw new RuntimeException(result.getErrMsg());
                        }
                    });

        }).retry()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Map<String, Object>>>() {//onNext()
                    @Override
                    public void accept(List<Map<String, Object>> messageMaps) throws Exception {
                        // 将Map转换为Message对象并添加到列表中
                        for (Map<String, Object> item : messageMaps) {
                            Message newMessage = new Message();
                            
                            // 从Map中获取数据
                            Object id = item.get("id");
                            if (id != null) {
                                try {
                                    // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                    newMessage.setId(Double.valueOf(id.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    newMessage.setId(0L);
                                }
                            }
                            
                            Object senderId = item.get("senderId");
                            if (senderId != null) {
                                try {
                                    newMessage.setSenderId(Double.valueOf(senderId.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    newMessage.setSenderId(0L);
                                }
                            }
                            
                            Object receiverId = item.get("receiverId");
                            if (receiverId != null) {
                                try {
                                    newMessage.setReceiverId(Double.valueOf(receiverId.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    newMessage.setReceiverId(0L);
                                }
                            }
                            
                            Object content = item.get("content");
                            if (content != null) {
                                newMessage.setContent(content.toString());
                            }
                            
                            Object contactName = item.get("contactName");
                            if (contactName != null) {
                                newMessage.setContactName(contactName.toString());
                            }
                            
                            Object time = item.get("time");
                            if (time != null) {
                                try {
                                    // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                    newMessage.setTime(Double.valueOf(time.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    newMessage.setTime(System.currentTimeMillis());
                                }
                            }
                            
                            boolean isDuplicate = false;
                            // 检查消息是否已存在于列表中
                            for (Message existingMessage : chatMessages) {
                                // 防止空指针异常，检查ID是否为null
                                if (existingMessage.getId() != null && newMessage.getId() != null && 
                                    existingMessage.getId().equals(newMessage.getId())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            
                            // 如果消息不存在于列表中，则添加
                            if (!isDuplicate) {
                                chatMessages.add(newMessage);
                                // 在view中显示出来。通知RecyclerView，更新一行
                                recyclerView.getAdapter().notifyItemInserted(chatMessages.size() - 1);
                                // 让RecyclerView向下滚动，以显示最新的消息
                                recyclerView.scrollToPosition(chatMessages.size() - 1);
                                
                                // 如果是接收到的消息，标记为已读
                                // 使用senderId而不是contactName来判断
                                if (newMessage.getSenderId() != null && 
                                    !newMessage.getSenderId().equals(MainActivity.myInfo.getId())) {
                                    markMessagesAsRead();
                                    
                                    // 发送广播通知MainFragment刷新会话列表
                                    Intent intent = new Intent("NEW_MESSAGE_RECEIVED");
                                    sendBroadcast(intent);
                                }
                            }
                        }
                    }
                }, new Consumer<Throwable>() {//onError()
                    @Override
                    public void accept(Throwable e) throws Exception {
                        //反正要重试，什么也不做了
                        Log.e("chatactivity", e.getLocalizedMessage());
                    }
                }, new Action() { //onComplete()
                    @Override
                    public void run() throws Exception {

                    }
                });

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
                
                //创建消息对象，准备上传
                long currentUserId = MainActivity.myInfo.getId();
                Message chatMessage = new Message();
                chatMessage.setSenderId(currentUserId);
                chatMessage.setReceiverId(contactId);
                chatMessage.setContent(msg);
                chatMessage.setContactName(MainActivity.myInfo.getName());
                chatMessage.setTime(new Date().getTime());
                
                //上传到服务端
                chatService.uploadMessage(chatMessage)
                .retry()
                .map(result -> {
                    //判断服务端是否正确返回
                    if (result.getRetCode() == 0) {
                        //服务端无错误，返回服务器生成的消息ID
                        Object data = result.getData();
                        if (data instanceof Map) {
                            Map<String, Object> msgData = (Map<String, Object>) data;
                            Object msgId = msgData.get("id");
                            if (msgId != null) {
                                try {
                                    // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                    chatMessage.setId(Double.valueOf(msgId.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    chatMessage.setId(0L);
                                }
                            }
                            // 设置contactName，确保消息显示正确
                            Object contactName = msgData.get("contactName");
                            if (contactName != null) {
                                chatMessage.setContactName(contactName.toString());
                            }
                            // 设置时间戳
                            Object time = msgData.get("time");
                            if (time != null) {
                                try {
                                    chatMessage.setTime(Double.valueOf(time.toString()).longValue());
                                } catch (NumberFormatException e) {
                                    chatMessage.setTime(System.currentTimeMillis());
                                }
                            }
                        }
                        return 0;
                    } else {
                        //服务端出错了，抛出异常，在Observer中捕获之
                        throw new RuntimeException(result.getErrMsg());
                    }
                }).subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Object>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                uploadDisposable = d;
                            }
                            
                            @Override
                            public void onNext(Object data) {
                                //消息发送成功，刷新会话列表
                                // 发送广播通知MainFragment刷新会话列表
                                Intent intent = new Intent("MESSAGE_SENT");
                                intent.putExtra("contact_id", contactId);
                                intent.putExtra("contact_name", contactName);
                                sendBroadcast(intent);
                            }
                            
                            @Override
                            public void onError(Throwable e) {
                                //对应onError()，向用户提示错误
                                String errmsg = e.getLocalizedMessage();
                                Snackbar.make(view, "消息发送失败：" + errmsg, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                            
                            @Override
                            public void onComplete() {
                                
                            }
                        });
                
                //添加到集合中，从而能在RecyclerView中显示
                chatMessages.add(chatMessage);
                //在view中显示出来。通知RecyclerView，更新一行
                recyclerView.getAdapter().notifyItemInserted(chatMessages.size() - 1);
                //让RecyclerView向下滚动，以显示最新的消息
                recyclerView.scrollToPosition(chatMessages.size() - 1);
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
            Message message = chatMessages.get(position);
            // 防止空指针异常
            if (message != null && message.getContent() != null) {
                holder.textView.setText(message.getContent());
            } else {
                holder.textView.setText("");
            }
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        //有两种行layout，所以Override此方法
        @Override
        public int getItemViewType(int position) {
            Message message = chatMessages.get(position);
            // 使用senderId判断消息归属，而不是使用contactName
            // 如果是当前用户发送的消息，靠右显示；否则靠左显示
            if (message != null && message.getSenderId() != null && 
                message.getSenderId().equals(MainActivity.myInfo.getId())) {
                //如果是我的，靠右显示
                return R.layout.chat_message_right_item;
            } else {
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
    
    //获取Retrofit实例
    private Retrofit getRetrofit() {
        // 使用MainActivity中设置的服务器地址，确保与应用其他部分一致
        // 这样真机和模拟器都能正确连接到服务器
        if (MainActivity.serverHostURL.isEmpty()) {
            // 如果MainActivity中没有设置服务器地址，尝试从MainActivity获取
            if (getParent() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getParent();
                Retrofit existingRetrofit = mainActivity.getRetrofitVar();
                if (existingRetrofit != null) {
                    return existingRetrofit;
                }
            }
            // 如果都不行，返回默认的Retrofit（这种情况应该不会发生，因为登录前需要设置服务器地址）
            return new Retrofit.Builder()
                    .baseUrl(MainActivity.serverHostURL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        } else {
            return new Retrofit.Builder()
                    .baseUrl(MainActivity.serverHostURL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }
    }
    
    //显示服务器地址设置对话框
    public void showServerAddressSetDlg() {
        final EditText etUrl = new EditText(this);
        // 使用MainActivity中存储的服务器地址
        etUrl.setText(MainActivity.serverHostURL);
        new AlertDialog.Builder(this)
                .setTitle("设置服务器地址")
                .setView(etUrl)
                .setPositiveButton("确定", (dialog, which) -> {
                    String url = etUrl.getText().toString();
                    if (!url.startsWith("http://")) {
                        url = "http://" + url;
                    }
                    // 使用与MainActivity相同的SharedPreferences
                    SharedPreferences preferences = getSharedPreferences("qqapp", MODE_PRIVATE);
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("server_addr", url);
                    edit.commit();
                    // 更新MainActivity中的静态变量
                    MainActivity.serverHostURL = url;
                    
                    Toast.makeText(ChatActivity.this, "服务器地址已更新", Toast.LENGTH_SHORT).show();
                    //重新初始化Retrofit
                    retrofit = getRetrofit();
                    chatService = retrofit.create(ChatService.class);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 加载历史消息
     */
    private void loadHistoryMessages() {
        long currentUserId = MainActivity.myInfo.getId();
        chatService.getMessages(currentUserId, contactId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ServerResult<List<Map<String, Object>>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        getMessagesDisposable = d;
                    }
                    
                    @Override
                    public void onNext(ServerResult<List<Map<String, Object>>> result) {
                        if (result.getRetCode() == 0) {
                            List<Map<String, Object>> messageMaps = result.getData();
                            if (messageMaps != null && !messageMaps.isEmpty()) {
                                chatMessages.clear();
                                
                                // 将Map转换为Message对象
                                for (Map<String, Object> item : messageMaps) {
                                    Message message = new Message();
                                    
                                    // 从Map中获取数据
                                    Object id = item.get("id");
                                    if (id != null) {
                                        try {
                                            // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                            message.setId(Double.valueOf(id.toString()).longValue());
                                        } catch (NumberFormatException e) {
                                            message.setId(0L);
                                        }
                                    }
                                    
                                    Object senderId = item.get("senderId");
                                    if (senderId != null) {
                                        try {
                                            message.setSenderId(Double.valueOf(senderId.toString()).longValue());
                                        } catch (NumberFormatException e) {
                                            message.setSenderId(0L);
                                        }
                                    }
                                    
                                    Object receiverId = item.get("receiverId");
                                    if (receiverId != null) {
                                        try {
                                            message.setReceiverId(Double.valueOf(receiverId.toString()).longValue());
                                        } catch (NumberFormatException e) {
                                            message.setReceiverId(0L);
                                        }
                                    }
                                    
                                    Object content = item.get("content");
                                    if (content != null) {
                                        message.setContent(content.toString());
                                    }
                                    
                                    Object contactName = item.get("contactName");
                                    if (contactName != null) {
                                        message.setContactName(contactName.toString());
                                    }
                                    
                                    Object time = item.get("time");
                                    if (time != null) {
                                        try {
                                            // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                            message.setTime(Double.valueOf(time.toString()).longValue());
                                        } catch (NumberFormatException e) {
                                            message.setTime(System.currentTimeMillis());
                                        }
                                    }
                                    
                                    chatMessages.add(message);
                                }
                                
                                // 通知适配器更新
                                RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
                                recyclerView.getAdapter().notifyDataSetChanged();
                                // 滚动到最新消息
                                recyclerView.scrollToPosition(chatMessages.size() - 1);
                            }
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("ChatActivity", "加载历史消息失败: " + throwable.getMessage(), throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        // 不需要特殊处理
                    }
                });
    }
    
    /**
     * 标记消息为已读
     */
    private void markMessagesAsRead() {
        long currentUserId = MainActivity.myInfo.getId();
        
        // 准备请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("senderId", contactId);
        params.put("receiverId", currentUserId);
        
        chatService.markMessagesAsRead(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ServerResult<String>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        markAsReadDisposable = d;
                    }
                    
                    @Override
                    public void onNext(ServerResult<String> result) {
                        if (result.getRetCode() == 0) {
                            Log.d("ChatActivity", "标记消息为已读: " + result.getData());
                        } else {
                            Log.e("ChatActivity", "标记消息为已读失败: " + result.getErrMsg());
                        }
                    }
                    
                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("ChatActivity", "标记消息为已读错误: " + throwable.getMessage(), throwable);
                    }
                    
                    @Override
                    public void onComplete() {
                        // 不需要特殊处理
                    }
                });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if(uploadDisposable!=null && !uploadDisposable.isDisposed()) {
            uploadDisposable.dispose();
            uploadDisposable = null;
        }

        if(downloadDisposable!=null && !downloadDisposable.isDisposed()) {
            downloadDisposable.dispose();
            downloadDisposable = null;
        }
        
        if(getMessagesDisposable!=null && !getMessagesDisposable.isDisposed()) {
            getMessagesDisposable.dispose();
            getMessagesDisposable = null;
        }
        
        if(markAsReadDisposable!=null && !markAsReadDisposable.isDisposed()) {
            markAsReadDisposable.dispose();
            markAsReadDisposable = null;
        }
    }
}