package com.example.microchat.service;

import io.reactivex.Observable;
import com.example.microchat.ServerResult;
import com.example.microchat.adapter.ContactsPageListAdapter;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ChatService {
    @Multipart
    @POST("/apis/register")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestRegister(
            @Part MultipartBody.Part fileData,
            @Query("name")String name,
            @Query("password") String password);
}
