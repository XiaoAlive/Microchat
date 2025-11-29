package com.example.microchat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.example.microchat.ServerResult;
import com.example.microchat.adapter.ContactsPageListAdapter;
import com.example.microchat.service.ChatService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.microchat.database.AppDatabase;
import com.example.microchat.database.UserEntity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private Retrofit retrofit;
    private Uri imageUri;//所选图像的URI
    //buttom sheet dialog for pickpig photo（用于获取头像）
    private BottomSheetDialog sheetDialog;
    private ImageView imageViewAvatar;
    
    // 错误提示TextView
    private TextView textViewNameError;
    private TextView textViewPhoneError;
    private TextView textViewPasswordError;
    private TextView textViewPassword2Error;

    public static final int TAKE_PHOTO = 1; //拍照
    public static final int SELECT_PHOTO = 2;//从图库选择
    public static final int CROP_PHOTO = 3;//剪切编辑
    public static final int ASK_PERMISSIONS = 4;//请求权限


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // 设置Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 设置显示返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // 初始化错误提示TextView
        textViewNameError = findViewById(R.id.textViewNameError);
        textViewPhoneError = findViewById(R.id.textViewPhoneError);
        textViewPasswordError = findViewById(R.id.textViewPasswordError);
        textViewPassword2Error = findViewById(R.id.textViewPassword2Error);

        findViewById(R.id.buttonCommit).setOnClickListener(v1 -> {
            //Retrofit跟据接口实现类并创建实例，这使用了动态代理技术，
            getRetrofit();
        });

        //响应头像点击，弹出菜单，让用户选择允何种方式获得头像
        this.imageViewAvatar = findViewById(R.id.imageViewAvatar);
        imageViewAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheetDialog = new BottomSheetDialog(RegisterActivity.this);
                View view = getLayoutInflater().inflate(R.layout.image_pick_sheet_menu, null);
                sheetDialog.setContentView(view);
                sheetDialog.show();
                //响应菜单项的选择
                view.findViewById(R.id.sheetItemTakePhoto).setOnClickListener(v1-> {
                    //从相机中获取
                    requestCameraAndStoragePermissions();
                    sheetDialog.dismiss();
                });

                view.findViewById(R.id.sheetItemSelectPicture).setOnClickListener(v1 -> {
                    //从图库中选
                    requestStoragePermission();
                    sheetDialog.dismiss();
                });

                view.findViewById(R.id.sheetItemCancel).setOnClickListener(v1 -> {
                    //隐藏SheetMenu
                    sheetDialog.dismiss();
                });
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理返回按钮点击事件
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public MultipartBody.Part createFilePart() {
        if(this.imageUri==null){
            //必须有个Part才行，所以创建一个吧
            return MultipartBody.Part.createFormData("none", "none");
        }
        InputStream inputStream = null;
        byte[] data=null;
        try {
            inputStream = getContentResolver().openInputStream(this.imageUri);
            data=new byte[inputStream.available()];
            inputStream.read(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("application/otcet-stream"), data);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", "png", requestFile);
        return body;
    }

    public void showMsg(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // 清除所有错误提示
    private void clearAllErrors() {
        textViewNameError.setVisibility(View.GONE);
        textViewPhoneError.setVisibility(View.GONE);
        textViewPasswordError.setVisibility(View.GONE);
        textViewPassword2Error.setVisibility(View.GONE);
    }
    
    public void doRegister() {
        ChatService chatService = retrofit.create(ChatService.class);
        TextView tvName = findViewById(R.id.editTextName);
        TextView tvPassword = findViewById(R.id.editTextPassword);
        EditText editTextPhoneEmail = findViewById(R.id.editTextPhoneEmail);
        EditText editTextPassword2 = findViewById(R.id.editTextPassword2);
        
        // 清除之前的错误提示
        clearAllErrors();
        
        String name = tvName.getText().toString();
        String password = tvPassword.getText().toString();
        String password2 = editTextPassword2.getText().toString();
        String phoneEmail = editTextPhoneEmail.getText().toString();
        
        // 验证电话号码格式（必须是11位数字）
        if (!phoneEmail.matches("\\d{11}")) {
            textViewPhoneError.setText("电话号码格式错误，请输入11位数字");
            textViewPhoneError.setVisibility(View.VISIBLE);
            return;
        } else {
            textViewPhoneError.setVisibility(View.GONE);
        }
        
        // 验证两次输入的密码是否一致
        if (!password.equals(password2)) {
            textViewPassword2Error.setText("两次输入的密码不一致");
            textViewPassword2Error.setVisibility(View.VISIBLE);
            return;
        } else {
            textViewPassword2Error.setVisibility(View.GONE);
        }
        
        // 首先检查用户名是否已经被注册
        checkUsernameExists(name, password, phoneEmail);
    }
    
    private void checkUsernameExists(final String username, final String password, final String phoneEmail) {
        ChatService chatService = retrofit.create(ChatService.class);
        
        Observable<ServerResult<Boolean>> observable = chatService.checkUsernameExists(username);
        
        observable.map(result -> {
            // 判断服务端是否正确返回
            if(result.getRetCode()==0) {
                // 服务端无错误，返回检查结果
                return result.getData();
            } else {
                // 服务端出错了，抛出异常
                throw new RuntimeException(result.getErrMsg());
            }
        }).subscribeOn(Schedulers.computation())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Observer<Boolean>() {
              @Override
              public void onSubscribe(Disposable d) {}
              
              @Override
                public void onNext(Boolean exists) {
                    if (exists) {
                        // 用户名已存在，在输入框下方显示错误信息
                        textViewNameError.setText("该用户名已被注册");
                        textViewNameError.setVisibility(View.VISIBLE);
                    } else {
                        // 用户名可用，清除错误提示并继续
                        textViewNameError.setVisibility(View.GONE);
                        // 接下来检查电话号码是否已被注册
                        checkPhoneExists(username, password, phoneEmail);
                    }
                }
              
              @Override
              public void onError(Throwable e) {
                  // 处理检查过程中的错误
                  showMsg("检查用户名时出错：" + e.getMessage());
              }
              
              @Override
              public void onComplete() {}
          });
    }
    
    // 检查电话号码是否已被注册
    private void checkPhoneExists(final String username, final String password, final String phoneEmail) {
        ChatService chatService = retrofit.create(ChatService.class);
        
        // 假设服务器端有checkPhoneExists接口
        // 如果没有这个接口，这里可以做一个简单处理：在注册时捕获异常
        // 由于可能没有现成的接口，这里我们直接进行注册，如果电话号码已存在，
        // 服务端会返回错误信息，我们在onError中处理
        proceedWithRegistration(username, password, phoneEmail);
    }
    
    private void proceedWithRegistration(String username, String password, String phoneEmail) {
        ChatService chatService = retrofit.create(ChatService.class);
        
        // 检查是否上传了头像
        if (this.imageUri != null) {
            // 使用带头像上传的注册接口
            RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);
            RequestBody passwordBody = RequestBody.create(MediaType.parse("text/plain"), password);
            RequestBody phoneEmailBody = RequestBody.create(MediaType.parse("text/plain"), phoneEmail);
            MultipartBody.Part avatarPart = createFilePart();
            
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable = 
                    chatService.requestRegisterWithAvatar(usernameBody, passwordBody, phoneEmailBody, avatarPart);

            observable.map(result -> {
                //判断服务端是否正确返回
                if(result.getRetCode()==0) {
                    //服务端无错误，处理返回的数据
                    ContactsPageListAdapter.ContactInfo contactInfo = result.getData();
                    // 不需要在这里设置电话号码和账号，因为服务器端已经设置好了
                    // 添加日志以便调试
                    if (contactInfo != null) {
                        Log.d("RegisterActivity", "Received user info - Phone: " + contactInfo.getPhone() + ", Account: " + contactInfo.getAccount());
                    }
                    return contactInfo;
                }else{
                    //服务端出错了，抛出异常，在Observer中捕获之
                    throw new RuntimeException(result.getErrMsg());
                }
            }).subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                            //保存下我的信息
                            MainActivity.myInfo = contactInfo;
                            
                            // 保存注册信息到SharedPreferences，以便下次登录时使用
                            SharedPreferences preferences = getSharedPreferences("qqapp", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("is_logged_in", true);
                            editor.putString("username", contactInfo.getName());
                            editor.putString("status", contactInfo.getStatus());
                            editor.putInt("userId", contactInfo.getId());
                            editor.putString("avatarUrl", contactInfo.getAvatarUrl());
                            editor.putString("phone", contactInfo.getPhone());
                            editor.putString("account", contactInfo.getAccount());
                            editor.commit();
                            
                            // 添加日志
                            Log.d("RegisterActivity", "Saved user info to SharedPreferences - Phone: " + contactInfo.getPhone() + ", Account: " + contactInfo.getAccount());
                            
                            // 保存用户信息到Room数据库
                            saveUserToDatabase(contactInfo);
                            
                            // 使用AlertDialog显示注册成功提示，更加明显
                            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                            builder.setTitle("注册成功")
                                   .setMessage("恭喜您，注册成功！")
                                   .setPositiveButton("确定", (dialog, which) -> {
                                       dialog.dismiss();
                                       finish(); // 点击确定后返回登录页面
                                   })
                                   .setCancelable(false); // 不允许点击外部区域关闭对话框
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errorMessage = e.getMessage();
                            if (errorMessage.contains("Failed to connect")) {
                                SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                                String addr = preferences.getString("server_addr","").toString();
                                preferences.edit().clear().commit();
                                retrofit = null;
                                showMsg("404 not found! "+addr);
                                getRetrofit();
                            } else if (errorMessage.contains("phone") || errorMessage.contains("电话")) {
                                // 处理电话号码已存在的情况
                                textViewPhoneError.setText("该电话号码已被注册");
                                textViewPhoneError.setVisibility(View.VISIBLE);
                            } else {
                                // 如果带头像上传失败，尝试使用普通注册接口
                                showMsg("头像上传失败，尝试使用普通注册：" + errorMessage);
                                useNormalRegistration(username, password, phoneEmail);
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } else {
            // 没有上传头像，使用普通注册接口
            useNormalRegistration(username, password, phoneEmail);
        }
    }
    
    // 使用普通注册接口（无头像上传）
    private void useNormalRegistration(String username, String password, String phoneEmail) {
        ChatService chatService = retrofit.create(ChatService.class);
        
        // 创建请求参数Map
        Map<String, String> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("password", password);
        userMap.put("phoneEmail", phoneEmail);
        
        Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable = 
                chatService.requestRegister(userMap);

        observable.map(result -> {
            //判断服务端是否正确返回
            if(result.getRetCode()==0) {
                //服务端无错误，处理返回的数据
                ContactsPageListAdapter.ContactInfo contactInfo = result.getData();
                // 添加日志以便调试
                if (contactInfo != null) {
                    Log.d("RegisterActivity", "Normal registration - Received user info - Phone: " + contactInfo.getPhone() + ", Account: " + contactInfo.getAccount());
                }
                return contactInfo;
            }else{
                //服务端出错了，抛出异常，在Observer中捕获之
                throw new RuntimeException(result.getErrMsg());
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                        //保存下我的信息
                        MainActivity.myInfo = contactInfo;
                        
                        // 保存注册信息到SharedPreferences，以便下次登录时使用
                        SharedPreferences preferences = getSharedPreferences("qqapp", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putString("username", contactInfo.getName());
                        editor.putString("status", contactInfo.getStatus());
                        editor.putInt("userId", contactInfo.getId());
                        editor.putString("avatarUrl", contactInfo.getAvatarUrl());
                        editor.putString("phone", contactInfo.getPhone());
                        editor.putString("account", contactInfo.getAccount());
                        editor.commit();
                        
                        // 添加日志
                        Log.d("RegisterActivity", "Normal registration - Saved user info to SharedPreferences - Phone: " + contactInfo.getPhone() + ", Account: " + contactInfo.getAccount());
                        
                        // 保存用户信息到Room数据库
                        saveUserToDatabase(contactInfo);
                        
                        // 使用AlertDialog显示注册成功提示，更加明显
                        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                        builder.setTitle("注册成功")
                               .setMessage("恭喜您，注册成功！")
                               .setPositiveButton("确定", (dialog, which) -> {
                                   dialog.dismiss();
                                   finish(); // 点击确定后返回登录页面
                               })
                               .setCancelable(false); // 不允许点击外部区域关闭对话框
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errorMessage = e.getMessage();
                        if (errorMessage.contains("Failed to connect")) {
                            SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                            String addr = preferences.getString("server_addr","").toString();
                            preferences.edit().clear().commit();
                            retrofit = null;
                            showMsg("404 not found! "+addr);
                            getRetrofit();
                        } else if (errorMessage.contains("phone") || errorMessage.contains("电话")) {
                            // 处理电话号码已存在的情况
                            textViewPhoneError.setText("该电话号码已被注册");
                            textViewPhoneError.setVisibility(View.VISIBLE);
                        } else {
                            showMsg(errorMessage);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getRetrofit() {
        if(retrofit==null){
            //从本地读取server host name，如果MainActivity中已有设置则优先使用
            SharedPreferences preferences=getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
            String serverHost = preferences.getString("server_addr", "");
            if (serverHost.isEmpty()){
                //弹出输入对话框，让用户设置server地址
                showServerAddressSetDlg();
            }else {
                //创建Retrofit对象
                retrofit = new Retrofit.Builder()
                        .baseUrl(serverHost)
                        //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                        //所以必须设置Call适配器将Observable与Call结合起来
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        //Json数据自动转换
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                doRegister();
            }
        }else {
            doRegister();
        }
    }

    // 保存用户信息到Room数据库
    private void saveUserToDatabase(ContactsPageListAdapter.ContactInfo contactInfo) {
        // 获取数据库实例
        AppDatabase db = AppDatabase.getInstance(this);
        
        // 创建用户实体
        String phone = contactInfo.getPhone();
        String account = contactInfo.getAccount();
        String name = contactInfo.getName();
        String avatarUrl = contactInfo.getAvatarUrl();
        
        // 注意：在实际应用中，密码应该加密存储
        // 这里我们从输入框获取密码（在真实场景中，应该考虑更安全的方式）
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        String password = editTextPassword.getText().toString();
        
        UserEntity user = new UserEntity(name, phone, account, password, avatarUrl);
        
        // 使用RxJava在后台线程保存用户信息
        db.userDao().insert(user)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(() -> {
                Log.d("RegisterActivity", "User saved to database successfully - Phone: " + phone);
            }, throwable -> {
                Log.e("RegisterActivity", "Failed to save user to database", throwable);
            });
    }
    
    public void showServerAddressSetDlg(){
        //弹出输入对话框，让用户设置server地址
        EditText editText = new EditText(this);
        editText.setHint("地址格式为: http://{IP地址}:{端口号}");
        // 为不同环境提供默认值提示
        String defaultHint = "注意：\n" +
                           "- 在模拟器上: http://10.0.2.2:8080\n" +
                           "- 在真实设备上: 使用服务器的实际IP地址\n" +
                           "  (确保手机和服务器在同一网络)";
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入服务器地址")
                 .setMessage(defaultHint)
                 .setView(editText);

        inputDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    String serverHostURL=editText.getText().toString();
                    //将服务端地址保存到本地
                    SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("server_addr",serverHostURL);
                    edit.commit();
                    //创建Retrofit对象
                    try {
                        retrofit = new Retrofit.Builder()
                                .baseUrl(serverHostURL)
                                //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                                //所以必须设置Call适配器将Observable与Call结合起来
                                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                //Json数据自动转换
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        doRegister();
                    } catch (Exception e) {
                        showMsg("请输入合法地址！");
                        retrofit = null;
                        getRetrofit();
                        preferences.edit().clear().commit();
                    }
                }).show();
    }

    // 请求相机和存储权限
    private void requestCameraAndStoragePermissions() {
        List<String> permissionsList = new ArrayList<>();
        // 检查相机权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.CAMERA);
        }
        // 检查存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissionsList.isEmpty()) {
            // 已有所需权限
            showTackPhotoView();
        } else {
            // 申请权限
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), ASK_PERMISSIONS);
        }
    }

    // 请求存储权限（用于从图库选择图片）
    private void requestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ASK_PERMISSIONS);
        } else {
            // 已有权限，启动图库选择图片
            openGallery();
        }
    }
    
    // 打开图库选择图片
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // 只选择图片类型
        startActivityForResult(intent, SELECT_PHOTO);
    }

    // 显示拍照界面的方法
    private void showTackPhotoView() {
        // 这里可以添加拍照相关的代码
        //Toast.makeText(this, "拍照功能待实现", Toast.LENGTH_SHORT).show();
        File imageOutputFile = generateOutPutFile(Environment.DIRECTORY_DCIM);
        this.imageUri = FileProvider.getUriForFile(this,"com.example.microchat.fileprovider", imageOutputFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //照相
        intent.putExtra(MediaStore.EXTRA_OUTPUT, this.imageUri); //指定图片输出地址
        startActivityForResult(intent, TAKE_PHOTO); //启动照相
        //隐藏底部的SheetMenu
        sheetDialog.dismiss();
    }

    // 产生图像文件路径
    private File generateOutPutFile(String pathInExternalStorage){
        //图片名称 时间命名
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String photoFileName = format.format(date)+".png";
        //存储至DCIM文件夹
        File path = Environment.getExternalStoragePublicDirectory(pathInExternalStorage);
        File outputImage = new File(path, photoFileName );
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return outputImage;
    }

    // 得到图像文件
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 拍照成功，启动裁剪
                    startCropImage(imageUri);
                }
                break;
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    // 从图库选择图片成功
                    imageUri = data.getData();
                    // 启动裁剪
                    startCropImage(imageUri);
                }
                break;
            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // 图片解析成Bitmap对象
                        Bitmap bitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(this.imageUri));
                        // 将剪裁后照片显示出来
                        this.imageViewAvatar.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
    
    // 启动图片裁剪功能
    private void startCropImage(Uri sourceUri) {
        Intent intent = new Intent("com.android.camera.action.CROP"); // 剪裁
        // 告诉剪裁Activity，要申请对Uri的读权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(sourceUri, "image/*");
        intent.putExtra("scale", true);
        intent.putExtra("crop", "true");
        // 设置宽高比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 设置裁剪图片宽高
        intent.putExtra("outputX", 340);
        intent.putExtra("outputY", 340);

        // 产生写出文件并获取Uri，注意！新版API不允许读和写是同一个文件
        File finalImage = generateOutPutFile(Environment.DIRECTORY_DCIM);
        // 写出的Uri不能是FileProvier形式的，Activity不支持！！！！
        Uri outputUri = Uri.fromFile(finalImage);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        this.imageUri = outputUri;
        startActivityForResult(intent, CROP_PHOTO); // 设置裁剪参数显示图片至ImageView
    }

    // 实现响应权限申请结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ASK_PERMISSIONS:
                boolean allGranted = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (!allGranted) {
                    Toast.makeText(this, "权限申请被拒绝，无法完成照片选择。", Toast.LENGTH_SHORT).show();
                } else {
                    // 检查是否包含相机权限（判断是拍照还是从图库选择）
                    boolean hasCameraPermission = false;
                    for (String permission : permissions) {
                        if (permission.equals(Manifest.permission.CAMERA)) {
                            hasCameraPermission = true;
                            break;
                        }
                    }
                    if (hasCameraPermission) {
                        showTackPhotoView();
                    } else {
                        // 如果只有存储权限，则打开图库
                        openGallery();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}