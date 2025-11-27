package com.example.microchat;

import android.app.AlertDialog;
import android.content.Context;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.util.Log;

public class ChatActivity extends AppCompatActivity {
    //用于网络通讯
    private Retrofit retrofit;
    private ChatService chatService;
    private Disposable uploadDisposable;
    private Disposable downloadDisposable;
    
    //存放所有的聊天消息
    private List<Message> chatMessages = new ArrayList<>();

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

        //初始化Retrofit
        retrofit = getRetrofit();
        chatService = retrofit.create(ChatService.class);

        //获取Recycler控件并设置适配器
        RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ChatMessagesAdapter());

        //每隔2秒向服务端获取一下新的聊天消息
        Observable.interval(2, TimeUnit.SECONDS).flatMap(v -> {

            //创建获取聊天消息的Observable
            //参数是下一坨Message的起始Index
            return chatService.getMessagesFromIndex(chatMessages.size())
                    .map(result -> {
                        //判断服务端是否正确返回
                        if (result.getRetCode() == 0) {
                            //服务端无错误，随便返回点东西吧，反正也不用处理
                            return result.getData();
                        } else {
                            //服务端出错了，抛出异常，在Observer中捕获之
                            throw new RuntimeException(result.getErrMsg());
                        }
                    });

        }).retry()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Message>>() {//onNext()
                    @Override
                    public void accept(List<Message> messages) throws Exception {
                        //将消息显示在RecyclerView中
                        int previousSize = chatMessages.size();
                        chatMessages.addAll(messages);
                        //在view中显示出来。通知RecyclerView，更新一行
                        recyclerView.getAdapter().notifyItemRangeInserted(
                                previousSize, messages.size());
                        //让RecyclerView向下滚动，以显示最新的消息
                        if (messages.size() > 0) {
                            recyclerView.scrollToPosition(chatMessages.size() - 1);
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
                }, new Consumer<Disposable>() { //onSubcribe()
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        //保存下downloadDisposable以取消订阅
                        downloadDisposable = disposable;
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
                Message chatMessage = new Message(MainActivity.myInfo.getName(), new Date().getTime(), msg);
                
                //上传到服务端
                Observable<ServerResult> observable = chatService.uploadMessage(chatMessage);
                observable.retry().map(result -> {
                    //判断服务端是否正确返回
                    if (result.getRetCode() == 0) {
                        //服务端无错误，随便返回点东西吧，反正也不用处理
                        return 0;
                    } else {
                        //服务端出错了，抛出异常，在Observer中捕获之
                        throw new RuntimeException(result.getErrMsg());
                    }
                }).subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Object>() {//onNext()
                            @Override
                            public void accept(Object data) throws Exception {
                                //对应onNext()，但是什么也不需要做
                            }
                        }, new Consumer<Throwable>() {//onError()
                            @Override
                            public void accept(Throwable e) throws Exception {
                                //对应onError()，向用户提示错误
                                String errmsg = e.getLocalizedMessage();
                                Snackbar.make(view, "大王祸事了：" + errmsg, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }, new Action() { //onComplete()
                            @Override
                            public void run() throws Exception {
                                
                            }
                        }, new Consumer<Disposable>() { //onSubcribe()
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                //保存下disposable以取消订阅
                                uploadDisposable = disposable;
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
            holder.textView.setText(message.getContent());
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        //有两种行layout，所以Override此方法
        @Override
        public int getItemViewType(int position) {
            Message message = chatMessages.get(position);
            if(message.getContactName().equals(MainActivity.myInfo.getName())) {
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
    
    //获取Retrofit实例
    private Retrofit getRetrofit() {
        String url = getServerAddress();
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
    
    //获取服务器地址
    private String getServerAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_info", Context.MODE_PRIVATE);
        String url = sharedPreferences.getString("server_address", "http://10.0.2.2:8081");
        return url;
    }
    
    //显示服务器地址设置对话框
    public void showServerAddressSetDlg() {
        final EditText etUrl = new EditText(this);
        etUrl.setText(getServerAddress());
        new AlertDialog.Builder(this)
                .setTitle("设置服务器地址")
                .setView(etUrl)
                .setPositiveButton("确定", (dialog, which) -> {
                    String url = etUrl.getText().toString();
                    if (!url.startsWith("http://")) {
                        url = "http://" + url;
                    }
                    SharedPreferences.Editor editor = getSharedPreferences("user_info", Context.MODE_PRIVATE).edit();
                    editor.putString("server_address", url);
                    editor.apply();
                    Toast.makeText(ChatActivity.this, "服务器地址已更新", Toast.LENGTH_SHORT).show();
                    //重新初始化Retrofit
                    retrofit = getRetrofit();
                    chatService = retrofit.create(ChatService.class);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(uploadDisposable!=null) {
            uploadDisposable.dispose();
            uploadDisposable = null;
        }

        if(downloadDisposable!=null) {
            downloadDisposable.dispose();
            downloadDisposable = null;
        }
    }
}