package com.example.microchat;
// 2025年11月9日
import com.example.microchat.model.ListTree;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.adapter.MessagePageListAdapter;
import com.google.android.material.tabs.TabLayout;


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
        v1.setAdapter(new MessagePageListAdapter(getActivity()));
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
                    Thread.sleep(2000);
                    
                    // 在UI线程中停止刷新动画
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 停止刷新动画
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            
                            // 如果当前是消息页面，可以刷新消息列表
                            if (viewPager.getCurrentItem() == 0) {
                                RecyclerView recyclerView = (RecyclerView) listViews[0];
                                if (recyclerView != null && recyclerView.getAdapter() != null) {
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                }
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
    private View createContactsPage(){
        //创建View
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
        
        //创建适配器
        ContactsPageListAdapter adapter = new ContactsPageListAdapter();
        
        //向适配器中添加组节点
        //创建组们
        ContactsPageListAdapter.GroupInfo group1=new ContactsPageListAdapter.GroupInfo("特别关心",0);
        ContactsPageListAdapter.GroupInfo group2=new ContactsPageListAdapter.GroupInfo("我的好友",1);
        ContactsPageListAdapter.GroupInfo group3=new ContactsPageListAdapter.GroupInfo("朋友",0);
        ContactsPageListAdapter.GroupInfo group4=new ContactsPageListAdapter.GroupInfo("家人",0);
        ContactsPageListAdapter.GroupInfo group5=new ContactsPageListAdapter.GroupInfo("同学",0);

        //添加组节点到适配器
        ContactsPageListAdapter.GroupNode groupNode1 = adapter.addGroupNode(group1);
        ContactsPageListAdapter.GroupNode groupNode2 = adapter.addGroupNode(group2);
        ContactsPageListAdapter.GroupNode groupNode3 = adapter.addGroupNode(group3);
        ContactsPageListAdapter.GroupNode groupNode4 = adapter.addGroupNode(group4);
        ContactsPageListAdapter.GroupNode groupNode5 = adapter.addGroupNode(group5);

        //第二层，联系人信息
        //头像
        Bitmap bitmap= BitmapFactory.decodeResource(getResources(), R.drawable.contacts_normal);
        //联系人1
        ContactsPageListAdapter.ContactInfo contact1 = new ContactsPageListAdapter.ContactInfo(
                bitmap,"王二","[在线]我是王二");
        //头像
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.contacts_normal);
        //联系人2
        ContactsPageListAdapter.ContactInfo contact2=new ContactsPageListAdapter.ContactInfo(
                bitmap,"王三","[离线]我没有状态");
        
        //添加联系人到指定组
        adapter.addContactToGroup(groupNode2, contact1);
        adapter.addContactToGroup(groupNode2, contact2);

        //获取页面里的RecyclerView，为它设置Adapter
        RecyclerView recyclerView = v.findViewById(R.id.contactListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 设置Adapter，禁用底部搜索框，因为我们使用顶部的搜索框
        adapter.setShowSearchBox(false);
        
        recyclerView.setAdapter(adapter);
        
        // 将联系人数据添加到静态tree对象中，用于搜索功能
        // 先清空tree
        tree.clear();
        
        // 直接将适配器中已创建的GroupNode添加到tree作为根节点
        tree.addRootNode(groupNode2); // 这是"我的好友"组
        
        return v;
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
            image.setBounds(0, 0, 40, 40);
            //Replace blank spaces with image icon
            SpannableString sb = new SpannableString(" \n"+title);
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
}

