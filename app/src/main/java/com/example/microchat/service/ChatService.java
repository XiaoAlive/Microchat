package com.example.microchat.service;

import io.reactivex.Observable;
import com.example.microchat.ServerResult;
import com.example.microchat.Message;
import com.example.microchat.adapter.ContactsPageListAdapter;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ChatService {
    @POST("/apis/login")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestLogin(
            @Body Map<String, String> loginParam);

    @POST("/apis/register")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestRegister(
            @Body Map<String, String> user);
    
    // 带头像上传的注册接口
    @Multipart
    @POST("/apis/register_with_avatar")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestRegisterWithAvatar(
            @Part("username") RequestBody username,
            @Part("password") RequestBody password,
            @Part("phoneEmail") RequestBody phoneEmail,
            @Part MultipartBody.Part avatar);
    
    // 检查用户名是否被注册
    @GET("/apis/checkUsername")
    Observable<ServerResult<Boolean>> checkUsernameExists(@Query("username") String username);
    
    // 通过手机号查询用户信息
    @GET("/apis/getUserByPhone")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> getUserByPhone(@Query("phone") String phone);
    
    // 获取联系人列表
    @GET("/apis/get_contacts")
    Observable<ServerResult<List<ContactsPageListAdapter.ContactInfo>>> getContacts(@Query("userId") Long userId);
    
    // 上传消息
    @POST("/apis/upload_message")
    Observable<ServerResult> uploadMessage(@Body Message msg);
    
    // 获取消息
    @GET("/apis/get_messages")
    Observable<ServerResult<List<Message>>> getMessagesFromIndex(@Query("from") int index);
    
    // 删除所有用户接口
    @DELETE("/apis/delete_all_users")
    Observable<ServerResult> deleteAllUsers();
    
    // 搜索用户接口
    @GET("/apis/searchUser")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> searchUser(@Query("keyword") String keyword);
    
    // 添加好友接口
    @POST("/apis/add_friend")
    Observable<ServerResult<String>> addFriend(@Body Map<String, Object> params);

}
