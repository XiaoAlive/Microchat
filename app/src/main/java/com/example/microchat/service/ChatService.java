package com.example.microchat.service;

import io.reactivex.Observable;
import com.example.microchat.ServerResult;
import com.example.microchat.adapter.ContactsPageListAdapter;
import okhttp3.MultipartBody;
import retrofit2.http.*;

import java.util.Map;

public interface ChatService {
    @POST("/apis/login")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestLogin(
            @Body Map<String, String> loginParam);

    @POST("/apis/register")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestRegister(
            @Body Map<String, String> user);
}
