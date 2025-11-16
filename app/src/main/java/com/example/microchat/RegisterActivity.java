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
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    private Retrofit retrofit;
    private Uri imageUri;//所选图像的URI
    //buttom sheet dialog for pickpig photo（用于获取头像）
    private BottomSheetDialog sheetDialog;
    private ImageView imageViewAvatar;

    public static final int TAKE_PHOTO = 1; //拍照
    public static final int SELECT_PHOTO = 2;//从图库选择
    public static final int CROP_PHOTO = 3;//剪切编辑
    public static final int ASK_PERMISSIONS = 4;//请求权限


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

    public void doRegister() {
        ChatService chatService = retrofit.create(ChatService.class);
        //产生文件Part和文本Part
        MultipartBody.Part filePart = createFilePart();
        TextView tvName = findViewById(R.id.editTextName);
        TextView tvPassword = findViewById(R.id.editTextPassword);
        String name = tvName.getText().toString();
        String password = tvPassword.getText().toString();
        Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                chatService.requestRegister(filePart,name,password);

        observable.map(result -> {
            //判断服务端是否正确返回
            if(result.getRetCode()==0) {
                //服务端无错误，处理返回的数据
                return result.getData();
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
                        showMsg("注册成功！");
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e.getMessage().contains("Failed to connect")) {
                            SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                            String addr = preferences.getString("server_addr","");
                            preferences.edit().clear().commit();
                            retrofit = null;
                            showMsg("404 not found! "+addr);
                            getRetrofit();
                        }else {
                            showMsg(e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getRetrofit() {
        if(retrofit==null){
            //从本地读取server host name，
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

    public void showServerAddressSetDlg(){
        //弹出输入对话框，让用户设置server地址
        EditText editText = new EditText(this);
        editText.setHint("地址格式为: http://{IP地址}:{端口号}");
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入服务器地址").setView(editText);

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