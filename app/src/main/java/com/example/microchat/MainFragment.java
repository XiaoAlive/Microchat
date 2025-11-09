package com.example.microchat;
// 2025年11月9日
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    // 用作测试背景的成员变量
    private ViewPager viewPager;
    private View listViews[] = {null, null, null};
    private TabLayout tabLayout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MainFragment() {
        // Required empty public constructor
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
        //仅用于测试，为了看到效果，不同的页设为不同背景色
        listViews[0].setBackgroundColor(Color.RED);
        listViews[1].setBackgroundColor(Color.GREEN);
        listViews[2].setBackgroundColor(Color.BLUE);
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        //获取ViewPager实例，将Adapter设置给它
        viewPager = v.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPageAdapter());

        //获取TabLayout并配置它
        tabLayout = v.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        
        return v;
    }

    // 为ViewPager派生一个适配器类
    class ViewPageAdapter extends PagerAdapter {
        // 成员变量
        private View listViews[] = {null, null, null};

        // 构造方法
        ViewPageAdapter(){}

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

