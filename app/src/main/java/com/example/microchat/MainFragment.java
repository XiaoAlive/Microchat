package com.example.microchat;
// 2025年11月9日
import com.example.microchat.model.ListTree;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.adapter.MessagePageListAdapter;
import com.example.microchat.service.ChatService;
import com.google.android.material.tabs.TabLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.util.Log;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    // 静态的ListTree对象，用于在Activity间共享联系人数据
    private static ListTree tree = new ListTree();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    // 用作测试背景的成员变量
    private ViewPager viewPager;
    private View listViews[] = {null, null, null};
    private TabLayout tabLayout;
    private ViewGroup rootView;
    private SwipeRefreshLayout swipeRefreshLayout; // 添加下拉刷新组件引用
    
    // 组节点成员变量，用于访问联系人组
    private ContactsPageListAdapter.GroupNode groupNode1;
    private ContactsPageListAdapter.GroupNode groupNode2;
    private ContactsPageListAdapter.GroupNode groupNode3;
    private ContactsPageListAdapter.GroupNode groupNode4;
    private ContactsPageListAdapter.GroupNode groupNode5;
    
    // 适配器和recyclerView引用
    private ContactsPageListAdapter contactsAdapter;
    private RecyclerView contactsRecyclerView;
    private final List<ContactsPageListAdapter.ContactInfo> cachedContacts = new ArrayList<>();
    private int currentContactsTab = 0;
    
    // Retrofit相关变量
    private Retrofit retrofit;
    private ChatService chatService;
    
    // 定时器订阅
    private Disposable observableDisposable; //用于停止订阅的对象
    private Disposable conversationDisposable; //会话列表获取的订阅对象
    
    // 消息页面的适配器和会话列表
    private MessagePageListAdapter messageAdapter;
    
    // 广播接收器
    private BroadcastReceiver friendAddedReceiver;
    private BroadcastReceiver messageSentReceiver;
    private BroadcastReceiver newMessageReceiver;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * 获取静态的联系人树对象，用于在Activity间共享联系人数据
     * @return ListTree对象
     */
    public static ListTree getContactsTree() {
        return tree;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        
        // 初始化Retrofit
        initRetrofit();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        // 注册广播接收器
        friendAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("FRIEND_ADDED".equals(intent.getAction())) {
                    // 好友添加成功，立即刷新联系人列表
                    refreshContactsImmediately();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("FRIEND_ADDED");
        getActivity().registerReceiver(friendAddedReceiver, filter);
        
        // 注册消息发送广播接收器
        messageSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("MESSAGE_SENT".equals(intent.getAction())) {
                    // 消息发送成功，立即刷新会话列表
                    fetchConversations();
                }
            }
        };
        
        IntentFilter messageFilter = new IntentFilter("MESSAGE_SENT");
        getActivity().registerReceiver(messageSentReceiver, messageFilter);
        
        // 注册新消息接收广播
        newMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("NEW_MESSAGE_RECEIVED".equals(intent.getAction())) {
                    // 接收到新消息，立即刷新会话列表
                    fetchConversations();
                }
            }
        };
        
        IntentFilter newMessageFilter = new IntentFilter("NEW_MESSAGE_RECEIVED");
        getActivity().registerReceiver(newMessageReceiver, newMessageFilter);
        
        // 创建一个定时器Observable，间隔改为30秒，减少频繁刷新
        Observable intervalObservable = Observable.interval(30, TimeUnit.SECONDS);
        intervalObservable.retry().flatMap(v -> {
            // 向服务端发出获取联系人列表的请求
            if (MainActivity.myInfo != null) {
                return chatService.getContacts(MainActivity.myInfo.getId()).map(result -> {
                    // 转换服务端返回的数据，将真正的负载发给观察者
                    if (result != null && result.getRetCode() == 0) {
                        return result.getData();
                    } else {
                        throw new RuntimeException(result != null ? result.getErrMsg() : "未知错误");
                    }
                });
            } else {
                // 如果没有登录信息，返回空列表
                return Observable.just(new ArrayList<ContactsPageListAdapter.ContactInfo>());
            }
        }).retry().subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<ContactsPageListAdapter.ContactInfo>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        observableDisposable = d;
                    }

                    @Override
                    public void onNext(List<ContactsPageListAdapter.ContactInfo> contactInfos) {
                        // 更新联系人列表
                        updateContactsList(contactInfos);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 提示错误信息
                        String errmsg = e.getLocalizedMessage();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "获取联系人失败：" + errmsg, Toast.LENGTH_SHORT).show();
                        }
                        Log.e("qqapp1", "获取联系人错误：" + errmsg, e);
                    }

                    @Override
                    public void onComplete() {
                        Log.i("qqapp1", "get contacts completed!");
                    }
                });
    }
    
    @Override
    public void onStop() {
        super.onStop();

        // 注销广播接收器
        if (friendAddedReceiver != null) {
            getActivity().unregisterReceiver(friendAddedReceiver);
            friendAddedReceiver = null;
        }
        
        if (messageSentReceiver != null) {
            getActivity().unregisterReceiver(messageSentReceiver);
            messageSentReceiver = null;
        }
        
        if (newMessageReceiver != null) {
            getActivity().unregisterReceiver(newMessageReceiver);
            newMessageReceiver = null;
        }

        // 停止RxJava定时器
        if (observableDisposable != null && !observableDisposable.isDisposed()) {
            observableDisposable.dispose();
            observableDisposable = null;
        }
        
        // 停止会话列表订阅
        if (conversationDisposable != null && !conversationDisposable.isDisposed()) {
            conversationDisposable.dispose();
            conversationDisposable = null;
        }
    }
    
    // 立即刷新联系人列表
    private void refreshContactsImmediately() {
        if (chatService != null && MainActivity.myInfo != null) {
            chatService.getContacts(MainActivity.myInfo.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ServerResult<List<ContactsPageListAdapter.ContactInfo>>>() {
                        @Override
                        public void onSubscribe(Disposable d) {}

                        @Override
                        public void onNext(ServerResult<List<ContactsPageListAdapter.ContactInfo>> result) {
                            if (result != null && result.getRetCode() == 0) {
                                // 更新联系人列表
                                updateContactsList(result.getData());
                                Toast.makeText(getContext(), "好友列表已更新", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("MainFragment", "刷新联系人列表失败：" + e.getMessage(), e);
                        }

                        @Override
                        public void onComplete() {}
                    });
        }
    }
    
    // 初始化Retrofit
    private void initRetrofit() {
        // 使用MainActivity中设置的服务器地址，而不是硬编码的地址
        // 这样真机和模拟器都能正确连接到服务器
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            // 首先检查是否已有Retrofit实例
            retrofit = activity.getRetrofitVar();
            if (retrofit == null) {
                // 如果不存在，调用getRetrofit()创建实例（可能会弹出设置对话框）
                retrofit = activity.getRetrofit();
            }
        }
        
        if (retrofit != null) {
            chatService = retrofit.create(ChatService.class);
        }
    }
    
    // 更新联系人列表
    private void updateContactsList(List<ContactsPageListAdapter.ContactInfo> contacts) {
        android.util.Log.d("MainFragment", "更新联系人列表，数量: " + (contacts != null ? contacts.size() : 0));
        
        // 检查联系人列表是否真的发生了变化
        boolean contactsChanged = false;
        if (contacts == null && !cachedContacts.isEmpty()) {
            contactsChanged = true;
        } else if (contacts != null && contacts.size() != cachedContacts.size()) {
            contactsChanged = true;
        } else if (contacts != null) {
            // 检查联系人内容是否变化
            for (int i = 0; i < contacts.size(); i++) {
                if (i >= cachedContacts.size() || 
                    !contacts.get(i).getName().equals(cachedContacts.get(i).getName())) {
                    contactsChanged = true;
                    break;
                }
            }
        }
        
        if (!contactsChanged) {
            android.util.Log.d("MainFragment", "联系人列表无变化，跳过更新");
            return;
        }
        
        cachedContacts.clear();
        if (contacts != null) {
            cachedContacts.addAll(contacts);
        }
        
        // 重新构建树形结构
        rebuildContactsTree();
        
        if (contactsRecyclerView == null) {
            android.util.Log.d("MainFragment", "contactsRecyclerView为空");
            return;
        }
        
        // 根据当前选中的标签页显示对应的视图
        if (currentContactsTab == 0) {
            android.util.Log.d("MainFragment", "显示分组视图");
            showGroupView();
        } else if (currentContactsTab == 1) {
            android.util.Log.d("MainFragment", "显示好友视图");
            showFriendsView();
        }
    }
    
    private void rebuildContactsTree() {
        android.util.Log.d("MainFragment", "重建联系人树，联系人数量: " + cachedContacts.size());
        
        if (tree == null) {
            android.util.Log.d("MainFragment", "tree为空，创建新的ListTree");
            tree = new ListTree();
        }
        
        tree.clear();
        
        if (cachedContacts.isEmpty()) {
            android.util.Log.d("MainFragment", "联系人列表为空");
            return;
        }
        
        // 创建"我的好友"分组
        ContactsPageListAdapter.GroupInfo groupInfo =
                new ContactsPageListAdapter.GroupInfo("我的好友", cachedContacts.size());
        ContactsPageListAdapter.GroupNode root =
                new ContactsPageListAdapter.GroupNode(groupInfo, 0);
        
        // 将联系人添加到分组中
        for (ContactsPageListAdapter.ContactInfo contact : cachedContacts) {
            ContactsPageListAdapter.ContactNode contactNode =
                    new ContactsPageListAdapter.ContactNode(contact, 1);
            root.addChild(contactNode);
            android.util.Log.d("MainFragment", "添加联系人到树: " + contact.getName());
        }
        
        // 将分组节点添加到树中
        tree.addRootNode(root);
        android.util.Log.d("MainFragment", "树形结构构建完成，根节点子节点数: " + root.getChildrenCount());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //创建三个RecyclerView，分别对应QQ消息页，QQ联系人页，QQ空间页
        listViews[0]=new RecyclerView(getContext());
        listViews[1]=new RecyclerView(getContext());
        listViews[2]=new RecyclerView(getContext());

        RecyclerView v1 = new RecyclerView(getContext());
        View v2 = createContactsPage();
        RecyclerView v3 = new RecyclerView(getContext());
        //将这三个View设置到数组中
        listViews[0] = v1;
        listViews[1] = v2;
        listViews[2] = v3;

        //别忘了设置layout管理器，否则不显示条目
        v1.setLayoutManager(new LinearLayoutManager(getContext()));
        //v3.setLayoutManager(new LinearLayoutManager(getContext()));

        //为RecyclerView设置Adapter
        messageAdapter = new MessagePageListAdapter(getActivity());
        v1.setAdapter(messageAdapter);
        
        // 获取会话列表
        fetchConversations();
        //v3.setAdapter(new SpacePageListAdapter());

        // Inflate the layout for this fragment
        this.rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);
        
        //获取ViewPager实例，将Adapter设置给它
        viewPager = this.rootView.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPageAdapter());

        //获取TabLayout并配置它
        tabLayout = this.rootView.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        
        //初始化SwipeRefreshLayout并设置刷新监听器
        swipeRefreshLayout = this.rootView.findViewById(R.id.refreshLayout);
        
        // 设置刷新颜色（可选）
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, 
                                                 android.R.color.holo_green_light, 
                                                 android.R.color.holo_orange_light);
        
        // 设置刷新监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 模拟网络请求，刷新数据
                refreshData();
            }
        });

        //响应+号图标点击事件，显示遮罩和气泡菜单
        TextView popMenu = this.rootView.findViewById(R.id.textViewPopMenu);
        popMenu.setOnClickListener(new View.OnClickListener() {
            //把弹出窗口作为成员变量
            PopupWindow pop;
            View mask; // 声明但不初始化

            @Override
            public void onClick(View view) {
                //向Fragment容器(FrameLayout)中加入一个View作为上层容器和遮罩
                mask = new View(getContext());
                mask.setBackgroundColor(Color.DKGRAY);
                mask.setAlpha(0.5f);
                MainFragment.this.rootView.addView(mask,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                //响应蒙板View的点击事件
                mask.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //隐藏弹出窗口
                        if (pop != null && pop.isShowing()) {
                            pop.dismiss();
                        }
                    }
                });

                //如果弹出窗口还未创建，则创建它
                if(pop==null) {
                    //创建PopupWindow，用于承载气泡菜单
                    pop = new PopupWindow(getActivity());

                    //加载气泡图像，以作为window的背景
                    Drawable drawable = getResources().getDrawable(R.drawable.pop_bk);
                    //设置气泡图像为window的背景
                    pop.setBackgroundDrawable(drawable);

                    //加载菜单项资源，是用LinearLayout模拟的菜单
                    LinearLayout menu = (LinearLayout) LayoutInflater
                            .from(getActivity())
                            .inflate(R.layout.pop_menu_layout, null);
                    
                    // 为"加好友/群"菜单项添加点击事件
                    // 获取第二个LinearLayout（加好友/群）
                    if (menu.getChildCount() > 1) {
                        View addFriendItem = menu.getChildAt(1); // 索引为1的是加好友/群项
                        addFriendItem.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 跳转到添加联系人界面
                                Intent intent = new Intent(getActivity(), AddContactActivity.class);
                                startActivity(intent);
                                // 关闭弹出菜单
                                if (pop != null && pop.isShowing()) {
                                    pop.dismiss();
                                }
                            }
                        });
                    }

                    //设置window中要显示的View
                    pop.setContentView(menu);
                    //计算一下菜单layout的实际大小然后获取之
                    menu.measure(0, 0);

                    pop.setAnimationStyle(R.style.popoMenuAnim);
                    //设置窗口出现时获取焦点，这样在按下返回键时，窗口才会消失
                    pop.setFocusable(true);
                    pop.setOnDismissListener(() -> {
                        //去掉蒙板
                        rootView.removeView(mask);
                    });
                }
                //显示窗口
                pop.showAsDropDown(view, -pop.getContentView().getWidth() -10, -10);
            }
        });

            //头像监听器
        ImageView headImage = rootView.findViewById(R.id.headImage);
        
        // 设置主页面头像
        ContactsPageListAdapter.ContactInfo myInfo = MainActivity.myInfo;
        if (myInfo != null) {
            // 使用Glide加载头像，正确处理头像URL
            String avatarUrl = myInfo.getAvatarUrl();
            String serverHost = MainActivity.serverHostURL;
            
            // 确保服务器主机地址不为空
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = "http://10.0.2.2:8080"; // 使用默认值
            }
            
            // 构建完整的头像URL，仅当avatarUrl不为空时才拼接
            String imgURL = null;
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // 检查URL是否已经是完整路径（以http开头）
                if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                    imgURL = avatarUrl;
                } else {
                    // 如果不是完整路径，则拼接服务器地址，处理头像URL路径确保没有双斜杠
                    String cleanAvatarUrl = avatarUrl.startsWith("/") ? avatarUrl.substring(1) : avatarUrl;
                    imgURL = serverHost + (serverHost.endsWith("/") ? "" : "/") + cleanAvatarUrl;
                }
            }
            
            // 使用Glide加载头像，如果imgURL为空则自动使用占位符
            Glide.with(getContext())
                    .load(imgURL)
                    .placeholder(R.drawable.contacts_normal) // 设置占位图
                    .error(R.drawable.contacts_normal)      // 设置错误图
                    .into(headImage);
        }
        
        headImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建抽屉页面
                View drawerLayout = getActivity().getLayoutInflater().inflate(
                        R.layout.drawer_layout,rootView,false);
                //先计算一下消息页面中，左边一排图像的大小，在界面构建器中设置的是dp
                //在代码中只能用像素，所以这里要换算一下，因为不同的屏幕分辩率，dp对应的像素数是不同的
                //使用Resources.getSystem().getDisplayMetrics()替代Utils.dip2px()
                float density = Resources.getSystem().getDisplayMetrics().density;
                int messageImageWidth = (int) (60 * density + 0.5f);
                //计算抽屉页面的宽度，rootView是FrameLayout，
                //利用getWidth()即可获得它当前的宽度
                int drawerWidth = rootView.getWidth()-messageImageWidth;
                //设置抽屉页面的宽度
                drawerLayout.getLayoutParams().width = drawerWidth;
                //将抽屉页面加入FrameLayout中
                rootView.addView(drawerLayout);

                //创建蒙板View
                final View maskView = new View(getContext());
                maskView.setBackgroundColor(Color.GRAY);
                //必须将其初始透明度设为完全透明
                maskView.setAlpha(0);
                rootView.addView(maskView);
                
                //当点击蒙板View时，隐藏抽屉页面
                maskView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //动画反着来，让抽屉消失
                        //动画持续的时间
                        int duration = 400;
                        //获取原内容的根控件
                        View contentLayout = rootView.findViewById(R.id.contentLayout);
                        
                        // 添加null检查
                        if (contentLayout == null) {
                            // 如果contentLayout不存在，我们仍然可以隐藏抽屉，但没有内容平移效果
                            // 创建抽屉动画
                            ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(drawerLayout, "translationX", 0, -drawerWidth);
                            animatorDrawer.setDuration(duration);
                            
                            // 同时添加遮罩渐变效果
                            ObjectAnimator animatorMaskAlpha = ObjectAnimator.ofFloat(maskView, "alpha", maskView.getAlpha(), 0);
                            animatorMaskAlpha.setDuration(duration);
                            
                            // 创建动画集合
                            AnimatorSet animatorSet = new AnimatorSet();
                            animatorSet.playTogether(animatorDrawer, animatorMaskAlpha);
                            animatorSet.setDuration(duration);
                            
                            // 设置侦听器，主要侦听动画关闭事件
                            animatorSet.addListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    // 动画结束，将蒙板和抽屉页面删除
                                    rootView.removeView(maskView);
                                    rootView.removeView(drawerLayout);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            });
                            
                            animatorSet.start();
                            return;
                        }
                        
                        //创建动画，移动原内容，从抽屉宽度位置移回0位置
                        ObjectAnimator animatorContent = ObjectAnimator.ofFloat(
                                contentLayout,
                                "translationX",
                                drawerWidth, 0);

                        //移动蒙板的动画
                        ObjectAnimator animatorMask = ObjectAnimator.ofFloat(
                                maskView,
                                "translationX",
                                drawerWidth, 0);
                        
                        //响应此动画的更新事件，在其中改变蒙板的透明度，使其逐渐变透明
                        animatorMask.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                //计算当前进度比例
                                float progress = (animation.getCurrentPlayTime() / (float) duration);
                                maskView.setAlpha(1 - progress);
                            }
                        });

                        //创建动画，让抽屉页面向左移，回到屏幕之外
                        ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(
                                drawerLayout,
                                "translationX",
                                0, -drawerWidth);

                        //创建动画集合，同时播放三个动画
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animatorContent, animatorMask, animatorDrawer);
                        animatorSet.setDuration(duration);
                        
                        //设置侦听器，主要侦听动画关闭事件
                        animatorSet.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                //动画结束，将蒙板和抽屉页面删除
                                rootView.removeView(maskView);
                                rootView.removeView(drawerLayout);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        });
                        
                        animatorSet.start();
                    }
                });

                //动画持续的时间
                int duration=400;
                //获取原内容的根控件
                View contentLayout = rootView.findViewById(R.id.contentLayout);
                
                // 添加null检查
                if (contentLayout == null) {
                    // 如果contentLayout不存在，我们仍然可以显示抽屉，但没有内容平移效果
                    drawerLayout.setTranslationX(-drawerWidth);
                    // 创建抽屉动画
                    ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(drawerLayout, "translationX", -drawerWidth, 0);
                    animatorDrawer.setDuration(duration);
                    animatorDrawer.start();
                    
                    // 同时添加遮罩渐变效果
                    ObjectAnimator animatorMaskAlpha = ObjectAnimator.ofFloat(maskView, "alpha", 0, 0.5f);
                    animatorMaskAlpha.setDuration(duration);
                    animatorMaskAlpha.start();
                    return;
                }
                
                //把它搞到最上层，这样在移动时能一直看到它（QQ就是这个效果）
                contentLayout.bringToFront();
                //再将蒙板View搞到最上层
                maskView.bringToFront();
                //创建动画，移动原内容，从0位置移动抽屉页面宽度的距离（注意其宽度不变）
                ObjectAnimator animatorContent = ObjectAnimator.ofFloat(contentLayout,
                        "translationX",0,drawerWidth);

                //移动蒙板的动画
                ObjectAnimator animatorMask = ObjectAnimator.ofFloat(maskView,
                        "translationX",0,drawerWidth);

                //响应此动画的刷新事件，在其中改变原页面的背景色，使其逐渐变暗
                animatorMask.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    //响应动画更新的方法
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        //计算当前进度比例,最后除以2的原因是因为透明度最终只降到一半,约127
                        float progress = (animation.getCurrentPlayTime()/(float)duration)/2;
                        maskView.setAlpha(progress);
                    }
                });

                //创建动画，让抽屉页面向右移，注意它是从左移出来的，
                //所以其初始位值设置为-drawerWidth，即完全位于屏幕之外
                ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(drawerLayout, "translationX", -drawerWidth, 0);

                //创建动画集合，同时播放三个动画
                AnimatorSet animatorSet=new AnimatorSet();
                animatorSet.playTogether(animatorContent,animatorMask,animatorDrawer);
                animatorSet.setDuration(duration);
                animatorSet.start();
                
                // 设置抽屉页面的用户信息
                ContactsPageListAdapter.ContactInfo myInfo = MainActivity.myInfo;
                if (myInfo != null) {
                    // 设置用户名
                    TextView drawerUsername = drawerLayout.findViewById(R.id.textView8);
                    if (drawerUsername != null) {
                        drawerUsername.setText(myInfo.getName());
                    }
                    
                    // 设置状态信息
                    TextView drawerStatus = drawerLayout.findViewById(R.id.textView9);
                    if (drawerStatus != null) {
                        String status = myInfo.getStatus();
                        if (status != null && !status.isEmpty()) {
                            drawerStatus.setText(status);
                        }
                    }
                    
                    // 设置头像
                    ImageView drawerAvatar = drawerLayout.findViewById(R.id.imageView4);
                    if (drawerAvatar != null) {
                        // 使用Glide加载头像
                        String imgURL = MainActivity.serverHostURL + myInfo.getAvatarUrl();
                        Glide.with(getContext())
                                .load(imgURL)
                                .placeholder(R.drawable.contacts_normal)
                                .into(drawerAvatar);
                        
                        // 给抽屉页面的头像添加点击事件，跳转到我的资料页面
                        drawerAvatar.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 跳转到我的资料页面
                                Intent intent = new Intent(getContext(), ProfileActivity.class);
                                startActivity(intent);
                            }
                        });
                    }
                }
                
                // 给登出标签添加点击事件
                TextView logoutTextView = drawerLayout.findViewById(R.id.textViewLogout);
                if (logoutTextView != null) {
                    logoutTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showLogoutMenu();
                        }
                    });
                }
            }
        });

        return rootView;
    }
    
    // 模拟刷新数据的方法
    private void refreshData() {
        // 模拟网络延迟
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 模拟网络请求耗时
                    Thread.sleep(1000);  // 减少延迟时间，提升用户体验
                    
                    // 在UI线程中停止刷新动画
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 停止刷新动画
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            
                            // 根据当前页面刷新相应数据
                            if (viewPager.getCurrentItem() == 0) {
                                // 消息页面：刷新会话列表
                                fetchConversations();
                            } else if (viewPager.getCurrentItem() == 1) {
                                // 联系人页面：刷新联系人列表
                                fetchContactsFromServer();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 发生异常时也停止刷新动画
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    //创建并初始化联系人页面，返回这个页面
    private View createContactsPage(){        //创建View
        View v = getLayoutInflater().inflate(R.layout.contacts_page_layout,null);
        
        // 为顶部搜索框设置点击事件
        View topSearchView = v.findViewById(R.id.searchViewStub);
        if (topSearchView != null) {
            topSearchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), SearchActivity.class);
                    startActivity(intent);
                }
            });
        }
        
        //获取页面里的RecyclerView，为它设置Adapter
        contactsRecyclerView = v.findViewById(R.id.contactListView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 默认渲染分组视图
        showGroupView();
        
        // 初始获取联系人数据 - 立即从服务器获取
        fetchContactsFromServer();
        
        // 获取TabLayout并设置切换监听器
        TabLayout contactsTabLayout = v.findViewById(R.id.contactsTabLayout);
        if (contactsTabLayout != null) {
            contactsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    handleTabSelection(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
            // 默认选中第一个标签（分组）
            contactsTabLayout.getTabAt(0).select();
        }
        
        return v;
    }
    
    // 从服务器获取联系人数据
    private void fetchContactsFromServer() {
        if (chatService != null && MainActivity.myInfo != null) {
            chatService.getContacts(MainActivity.myInfo.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ServerResult<List<ContactsPageListAdapter.ContactInfo>>>() {
                        @Override
                        public void onSubscribe(Disposable d) {}

                        @Override
                        public void onNext(ServerResult<List<ContactsPageListAdapter.ContactInfo>> result) {
                            if (result != null && result.getRetCode() == 0) {
                                // 更新联系人列表
                                updateContactsList(result.getData());
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("MainFragment", "获取联系人失败：" + e.getMessage(), e);
                        }

                        @Override
                        public void onComplete() {}
                    });
        }
    }
    
    // 获取会话列表
    private void fetchConversations() {
        if (chatService != null && MainActivity.myInfo != null) {
            long userId = MainActivity.myInfo.getId();
            chatService.getConversations(userId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ServerResult<List<Map<String, Object>>>>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            conversationDisposable = d;
                        }

                        @Override
                        public void onNext(ServerResult<List<Map<String, Object>>> result) {
                            if (result != null && result.getRetCode() == 0) {
                                // 将Map转换为Conversation对象
                                List<Conversation> conversations = new ArrayList<>();
                                List<Map<String, Object>> data = result.getData();
                                
                                if (data != null) {
                                    for (Map<String, Object> item : data) {
                                        Conversation conversation = new Conversation();
                                        
                                        // 从Map中获取数据
                                        Object id = item.get("id");
                                        if (id != null) {
                                            try {
                                                // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                                conversation.setId(Double.valueOf(id.toString()).longValue());
                                            } catch (NumberFormatException e) {
                                                conversation.setId(0L);
                                            }
                                        }
                                        
                                        Object name = item.get("name");
                                        if (name != null) {
                                            conversation.setName(name.toString());
                                        }
                                        
                                        Object avatarUrl = item.get("avatarUrl");
                                        if (avatarUrl != null) {
                                            conversation.setAvatarUrl(avatarUrl.toString());
                                        }
                                        
                                        Object lastMessage = item.get("lastMessage");
                                        if (lastMessage != null) {
                                            conversation.setLastMessage(lastMessage.toString());
                                        }
                                        
                                        Object lastMessageTime = item.get("lastMessageTime");
                                        if (lastMessageTime != null) {
                                            try {
                                                // 先尝试转换为浮点数，再转为长整型，处理可能的"2.0"格式
                                                conversation.setLastMessageTime(Double.valueOf(lastMessageTime.toString()).longValue());
                                            } catch (NumberFormatException e) {
                                                conversation.setLastMessageTime(System.currentTimeMillis());
                                            }
                                        }
                                        
                                        Object unreadCount = item.get("unreadCount");
                                        if (unreadCount != null) {
                                            try {
                                                conversation.setUnreadCount(Integer.parseInt(unreadCount.toString()));
                                            } catch (NumberFormatException e) {
                                                conversation.setUnreadCount(0);
                                            }
                                        }
                                        
                                        conversations.add(conversation);
                                    }
                                }
                                
                                // 更新会话列表
                                if (messageAdapter != null) {
                                    messageAdapter.setConversations(conversations);
                                }
                            } else {
                                Log.e("MainFragment", "获取会话列表失败：" + result.getErrMsg());
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("MainFragment", "获取会话列表错误：" + e.getMessage(), e);
                        }
                        
                        @Override
                        public void onComplete() {
                            // 不需要特殊处理
                        }
                    });
        }
    }
    
    // 处理Tab切换逻辑
    private void handleTabSelection(int position) {
        if (contactsRecyclerView == null) {
            return;
        }
        
        switch (position) {
            case 0: // 分组标签
                // 只显示特别关心和我的好友两个组
                currentContactsTab = 0;
                showGroupView();
                break;
            case 1: // 好友标签
                // 显示好友列表，按首字母排序
                currentContactsTab = 1;
                showFriendsView();
                break;
            default: // 其他标签（群聊、频道、机器人、设备）
                // 显示"持续更新中..."
                currentContactsTab = 2;
                showUpdatingView();
                break;
        }
    }
    
    // 显示分组视图
    private void showGroupView() {
        if (contactsRecyclerView == null) {
            return;
        }
        currentContactsTab = 0;
        
        // 重新创建适配器以确保正确显示
        contactsAdapter = new ContactsPageListAdapter();
        contactsAdapter.setShowSearchBox(false);
        contactsRecyclerView.setAdapter(contactsAdapter);
        
        // 清空现有数据
        contactsAdapter.clearAllNodes();
        
        ContactsPageListAdapter.GroupInfo group1 =
                new ContactsPageListAdapter.GroupInfo("特别关心", 0);
        ContactsPageListAdapter.GroupInfo group2 =
                new ContactsPageListAdapter.GroupInfo("我的好友", cachedContacts.size());
        
        groupNode1 = contactsAdapter.addGroupNode(group1);
        groupNode2 = contactsAdapter.addGroupNode(group2);
        
        for (ContactsPageListAdapter.ContactInfo contact : cachedContacts) {
            contactsAdapter.addContactToGroup(groupNode2, contact);
        }
    }
    
    // 显示好友视图 - 按首字母排序显示
    private void showFriendsView() {
        if (contactsRecyclerView == null) {
            return;
        }
        currentContactsTab = 1;
        
        // 重新创建适配器以确保正确显示
        contactsAdapter = new ContactsPageListAdapter();
        contactsAdapter.setShowSearchBox(false);
        contactsRecyclerView.setAdapter(contactsAdapter);
        
        List<ContactsPageListAdapter.ContactNode> allContacts = new ArrayList<>();
        for (ContactsPageListAdapter.ContactInfo contact : cachedContacts) {
            allContacts.add(new ContactsPageListAdapter.ContactNode(contact, 0));
        }
        
        contactsAdapter.generateAlphabetSortedContacts(allContacts);
    }
    
    // 显示"持续更新中..."视图
    private void showUpdatingView() {
        currentContactsTab = 2;
        if (contactsRecyclerView == null) {
            return;
        }
        // 创建一个简单的适配器来显示"持续更新中..."
        RecyclerView.Adapter adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_text_item, parent, false);
                // 设置整个View的居中对齐
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = 100; // 增加顶部间距，使文本在屏幕中间
                view.setLayoutParams(params);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView textView = holder.itemView.findViewById(R.id.text_view);
                textView.setText("持续更新中...");
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(18);
                textView.setTextColor(Color.GRAY);
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        };
        
        // 直接设置临时适配器用于显示"持续更新中"，但保留原始contactsAdapter引用
        contactsRecyclerView.setAdapter(adapter);
    }

    // 为ViewPager派生一个适配器类
    class ViewPageAdapter extends PagerAdapter {
        // 构造方法
        ViewPageAdapter(){}
        
        // 注意：这里不再声明新的listViews数组，直接使用外部类的listViews数组

        @Override
        public int getCount() {
            return listViews.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        //实例化一个子View，container是子View容器，就是ViewPager，
        //position是当前的页数，从0开始计
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = listViews[position];
            //必须加入容器中
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        //为参数title中的字符串前面加上iconResId所引用图像
        public CharSequence makeTabItemTitle(String title,int iconResId) {
            Drawable image = getResources().getDrawable(iconResId);
            // 使用dp转换为像素
                int iconSize = (int)(40 * getResources().getDisplayMetrics().density);
                image.setBounds(0, 0, iconSize, iconSize);
            //Replace blank spaces with image icon
            SpannableString sb = new SpannableString(" "+title);
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BASELINE);
            sb.setSpan(imageSpan, 0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }

        //返回每一页的标题，参数是页号，从0开始
        @Override
        public CharSequence getPageTitle(int position) {
            if(position==0){
                return makeTabItemTitle("消息",R.drawable.message_normal);
            }else if(position==1){
                return makeTabItemTitle("联系人",R.drawable.contacts_normal);
            }else if(position==2){
                return makeTabItemTitle("动态",R.drawable.space_normal);
            }
            return null;
        }
    }
    
    // 显示退出登录底部菜单
    private void showLogoutMenu() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(getActivity());
        View view = getLayoutInflater().inflate(R.layout.logout_sheet_menu, null);
        sheetDialog.setContentView(view);
        sheetDialog.show();
        
        // 退出登录
        view.findViewById(R.id.sheetItemLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheetDialog.dismiss();
                showLogoutConfirmDialog(true);
            }
        });
        
        // 退出QQ
        view.findViewById(R.id.sheetItemExitQQ).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheetDialog.dismiss();
                showLogoutConfirmDialog(false);
            }
        });
        
        // 取消
        view.findViewById(R.id.sheetItemCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheetDialog.dismiss();
            }
        });
    }
    
    // 显示退出确认对话框
    private void showLogoutConfirmDialog(boolean isLogout) {
        String title = isLogout ? "退出登录" : "退出QQ";
        String message = isLogout ? "确定要退出登录吗？" : "确定要退出QQ吗？";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (isLogout) {
                            performLogout(); // 退出登录，返回登录页面
                        } else {
                            performExitQQ(); // 退出QQ，完全退出应用
                        }
                    }
                })
                .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    
    // 执行退出QQ，完全退出应用
    private void performExitQQ() {
        // 清除用户信息
        MainActivity.myInfo = null;
        
        // 停止定时器
        if (observableDisposable != null && !observableDisposable.isDisposed()) {
            observableDisposable.dispose();
            observableDisposable = null;
        }
        
        // 清除登录状态（可选，根据需求决定是否在退出QQ时清除登录状态）
        // 在这里我们不清除登录状态，这样用户再次进入应用时仍会保持登录
        
        // 完全退出应用
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    // 执行退出登录
    private void performLogout() {
        // 清除用户信息
        MainActivity.myInfo = null;
        
        // 停止定时器
        if (observableDisposable != null && !observableDisposable.isDisposed()) {
            observableDisposable.dispose();
            observableDisposable = null;
        }
        
        // 清除登录状态标记和用户信息
        SharedPreferences preferences = getActivity().getSharedPreferences("qqapp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("is_logged_in", false);
        // 清除用户信息
        editor.remove("username");
        editor.remove("status");
        editor.remove("userId");
        // 清除头像URL信息
        editor.remove("avatarUrl");
        editor.commit();
        
        // 清除可能的用户数据缓存
        // 这里可以添加清除其他用户相关数据的代码
        
        // 返回登录页面
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.setMainActivity((MainActivity) getActivity());
        fragmentTransaction.replace(R.id.fragment_container, loginFragment);
        // 设置事务动画（可选）
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        // 清空后退栈，这样用户无法通过返回键回到主页面
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // 使用commitNow()确保立即执行，避免状态不一致
        fragmentTransaction.commitNow();
    }
}

