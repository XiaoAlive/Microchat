package com.example.microchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // 新增成员变量2025年11月8日00:06:44
    private ConstraintLayout layoutContext;// 正常内容部分，是一个ConstraintLayout
    private LinearLayout layoutHistory;// 历史菜单部分，是一个LinearLayout
    private EditText editTextQQNum;// 输入框


    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        // 初始化成员变量
        layoutContext = v.findViewById(R.id.layoutContext);
        layoutHistory = v.findViewById(R.id.layoutHistory);
        editTextQQNum = v.findViewById(R.id.editTextQQNum);

        // 注册点击事件
        v.findViewById(R.id.textViewRegister).setOnClickListener(view -> {
            // 启动注册Activity
            Intent intent = new Intent(getContext(),RegisterActivity.class);
            startActivity(intent);
        });

        // 响应下拉箭头的点击事件，弹出登录历史记录菜单
        v.findViewById(R.id.textViewHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutContext.setVisibility(View.INVISIBLE);
                layoutHistory.setVisibility(View.VISIBLE);

                // 创建两条历史记录菜单项，添加到layoutHistory中
                for(int i=0;i<3;i++) {
                    View layoutItem = getActivity().getLayoutInflater().inflate(R.layout.login_history_item, null);
                    //响应菜单项的点击，把它里面的信息填到输入框中。
                    layoutItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editTextQQNum.setText("1234567890");
                            layoutContext.setVisibility(View.VISIBLE);
                            layoutHistory.setVisibility(View.INVISIBLE);
                        }
                    });
                    layoutHistory.addView(layoutItem);
                }

                // 使用动画显示历史记录
                AnimationSet set = (AnimationSet) AnimationUtils.loadAnimation(
                        getContext(), R.anim.login_history_anim);
                layoutHistory.startAnimation(set);
            }
        });

        // 当点击菜单项之外的区域时，把历史菜单隐藏
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(layoutHistory.getVisibility()==View.VISIBLE){
                    layoutContext.setVisibility(View.VISIBLE);
                    layoutHistory.setVisibility(View.INVISIBLE);
                }
            }
        });

        //响应登录按钮的点击事件
        View buttonLogin = v.findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                MainFragment fragment = new MainFragment();
                //替换掉FrameLayout中现有的Fragment
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                //将这次切换放入后退栈中，这样可以在点后退键时自动返回上一个页面
                fragmentTransaction.addToBackStack("login");
                fragmentTransaction.commit();
            }
        });

        return v;
    }
}