package com.example.microchat.service;

import retrofit2.Retrofit;

public interface FragmentListener {
    Retrofit getRetrofit(); //创建网络请求框架实例
    void showServerAddressSetDlg(); //请求显示配置弹窗
}
