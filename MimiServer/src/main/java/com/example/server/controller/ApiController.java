package com.example.server.controller;

import com.example.server.entity.User;
import com.example.server.entity.Contact;
import com.example.server.entity.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@RequestMapping("/apis")
@Configuration
public class ApiController implements WebMvcConfigurer {
    // 模拟数据库：用户、联系人、消息
    private final Map<Long, User> userMap = new ConcurrentHashMap<>();
    private final Map<Long, List<Contact>> contactMap = new ConcurrentHashMap<>();
    private final List<Message> messageList = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    // 数据持久化文件路径
    private static final String DATA_FILE_PATH = "data/users.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 构造函数，初始化时加载持久化数据
    public ApiController() {
        loadPersistedData();
    }

    // 0. 检查用户名是否存在接口 /apis/checkUsername
    @GetMapping("/checkUsername")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        boolean exists = false;
        for (User user : userMap.values()) {
            if (user.getUsername().equals(username)) {
                exists = true;
                break;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", exists ? "用户名已存在" : "用户名可用");
        result.put("data", exists);
        return result;
    }
    
    // 辅助方法：检查用户是否已存在
    private Map<String, Object> checkUserExists(String username, String phone) {
        // 检查用户名是否已存在
        for (User existingUser : userMap.values()) {
            if (existingUser.getUsername().equals(username)) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                return result;
            }
        }
        
        // 检查手机号是否已存在
        for (User existingUser : userMap.values()) {
            if (existingUser.getPhone() != null && existingUser.getPhone().equals(phone)) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 400);
                result.put("msg", "该手机号已被注册");
                return result;
            }
        }
        
        return null; // 用户不存在，可以注册
    }
    
    // 1. 注册接口 /apis/register
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        // 设置手机号（如果前端没有设置的话）
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            user.setPhone(user.getPhoneEmail());
        }
        
        // 检查用户是否已存在
        Map<String, Object> checkResult = checkUserExists(user.getUsername(), user.getPhone());
        if (checkResult != null) {
            return checkResult;
        }
        
        user.setId(idGenerator.getAndIncrement());
        
        // 设置电话号码（如果前端没有设置的话）
        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            // 假设phoneEmail字段包含电话号码
            user.setPhone(user.getPhoneEmail());
        }
        
        // 生成10位随机账号
        if (user.getAccount() == null || user.getAccount().isEmpty()) {
            long randomAccount = (long)(Math.random() * 9000000000L) + 1000000000L;
            user.setAccount(String.valueOf(randomAccount));
        }
        
        // 设置默认头像URL
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl("/image/head/" + user.getId() + ".png");
        }
        
        userMap.put(user.getId(), user);
        // 保存用户数据到文件
        saveUserData();
        
        // 创建与客户端ContactInfo对象匹配的数据结构
        Map<String, Object> contactInfo = new HashMap<>();
        contactInfo.put("id", user.getId());
        contactInfo.put("name", user.getUsername());
        contactInfo.put("account", user.getAccount());
        contactInfo.put("phone", user.getPhone());
        contactInfo.put("avatarUrl", user.getAvatarUrl());
        contactInfo.put("status", "在线"); // 默认状态
        
        Map<String, Object> result = new HashMap<>();
        result.put("retCode", 0); // 成功代码
        result.put("errMsg", ""); // 空错误消息
        result.put("data", contactInfo);
        return result;
    }
    
    // 通过手机号查询用户信息
    @GetMapping("/getUserByPhone")
    public Map<String, Object> getUserByPhone(@RequestParam String phone) {
        User foundUser = null;
        for (User user : userMap.values()) {
            if (user.getPhone() != null && user.getPhone().equals(phone)) {
                foundUser = user;
                break;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        if (foundUser != null) {
            // 创建与客户端ContactInfo对象匹配的数据结构
            Map<String, Object> contactInfo = new HashMap<>();
            contactInfo.put("id", foundUser.getId());
            contactInfo.put("name", foundUser.getUsername());
            contactInfo.put("account", foundUser.getAccount());
            contactInfo.put("phone", foundUser.getPhone());
            contactInfo.put("avatarUrl", foundUser.getAvatarUrl());
            contactInfo.put("status", "在线"); // 默认状态
            
            // 使用与客户端ServerResult对象一致的数据结构
            result.put("retCode", 0); // 成功代码
            result.put("errMsg", ""); // 空错误消息
            result.put("data", contactInfo);
        } else {
            result.put("retCode", 404);
            result.put("errMsg", "用户不存在");
            result.put("data", null);
        }
        return result;
    }
    
    // 搜索用户接口 /apis/searchUser
    @GetMapping("/searchUser")
    public Map<String, Object> searchUser(@RequestParam String keyword) {
        User foundUser = null;
        
        // 通过账号搜索
        for (User user : userMap.values()) {
            if (user.getAccount() != null && user.getAccount().equals(keyword)) {
                foundUser = user;
                break;
            }
        }
        
        // 通过手机号搜索
        if (foundUser == null) {
            for (User user : userMap.values()) {
                if (user.getPhone() != null && user.getPhone().equals(keyword)) {
                    foundUser = user;
                    break;
                }
            }
        }
        
        // 通过用户名搜索
        if (foundUser == null) {
            for (User user : userMap.values()) {
                if (user.getUsername() != null && user.getUsername().equals(keyword)) {
                    foundUser = user;
                    break;
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        if (foundUser != null) {
            // 创建与客户端ContactInfo对象匹配的数据结构
            Map<String, Object> contactInfo = new HashMap<>();
            contactInfo.put("id", foundUser.getId());
            contactInfo.put("name", foundUser.getUsername());
            contactInfo.put("account", foundUser.getAccount());
            contactInfo.put("phone", foundUser.getPhone());
            contactInfo.put("avatarUrl", foundUser.getAvatarUrl());
            contactInfo.put("status", "在线"); // 默认状态
            
            // 使用与客户端ServerResult对象一致的数据结构
            result.put("retCode", 0); // 成功代码
            result.put("errMsg", ""); // 空错误消息
            result.put("data", contactInfo);
        } else {
            result.put("retCode", 404);
            result.put("errMsg", "用户不存在");
            result.put("data", null);
        }
        return result;
    }
    
    // 带头像上传的注册接口 /apis/register_with_avatar
    @PostMapping("/register_with_avatar")
    public Map<String, Object> registerWithAvatar(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("phoneEmail") String phoneEmail,
            @RequestParam(value = "file", required = false) MultipartFile avatarFile) {
        
        // 检查用户是否已存在
        Map<String, Object> checkResult = checkUserExists(username, phoneEmail);
        if (checkResult != null) {
            return checkResult;
        }
        
        // 创建用户对象
        User user = new User();
        user.setId(idGenerator.getAndIncrement());
        user.setUsername(username);
        user.setPassword(password);
        user.setPhoneEmail(phoneEmail);
        // 设置电话号码
        user.setPhone(phoneEmail);
        // 生成10位随机账号
        long randomAccount = (long)(Math.random() * 9000000000L) + 1000000000L;
        user.setAccount(String.valueOf(randomAccount));
        
        // 处理头像上传
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                // 创建头像保存目录
                String uploadDir = System.getProperty("user.dir") + "/uploads/head/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                
                // 保存头像文件
                String fileName = user.getId() + ".png";
                File avatarFileOnDisk = new File(dir, fileName);
                avatarFile.transferTo(avatarFileOnDisk);
                
                // 设置头像URL
                user.setAvatarUrl("/uploads/head/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
                // 头像上传失败，使用默认头像
                user.setAvatarUrl("/image/head/" + user.getId() + ".png");
            }
        } else {
            // 没有上传头像，使用默认头像
            user.setAvatarUrl("/image/head/" + user.getId() + ".png");
        }
        
        userMap.put(user.getId(), user);
        // 保存用户数据到文件
        saveUserData();
        
        // 创建与客户端ContactInfo对象匹配的数据结构
        Map<String, Object> contactInfo = new HashMap<>();
        contactInfo.put("id", user.getId());
        contactInfo.put("name", user.getUsername());
        contactInfo.put("account", user.getAccount());
        contactInfo.put("phone", user.getPhone());
        contactInfo.put("avatarUrl", user.getAvatarUrl());
        contactInfo.put("status", "在线"); // 默认状态
        
        Map<String, Object> result = new HashMap<>();
        result.put("retCode", 0); // 成功代码
        result.put("errMsg", ""); // 空错误消息
        result.put("data", contactInfo);
        return result;
    }

    // 2. 登录接口 /apis/login
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginParam) {
        String username = loginParam.get("username");
        String account = loginParam.get("account"); // 新增支持账号登录
        String password = loginParam.get("password");
        
        for (User user : userMap.values()) {
            // 检查用户名和密码（兼容旧版本）
            if (username != null && user.getUsername().equals(username) && user.getPassword().equals(password)) {
                // 创建与客户端ContactInfo对象匹配的数据结构
                Map<String, Object> contactInfo = new HashMap<>();
                contactInfo.put("id", user.getId());
                contactInfo.put("name", user.getUsername());
                contactInfo.put("account", user.getAccount());
                contactInfo.put("phone", user.getPhone());
                contactInfo.put("avatarUrl", user.getAvatarUrl());
                contactInfo.put("status", "在线"); // 默认状态
                
                Map<String, Object> result = new HashMap<>();
                result.put("retCode", 0); // 成功代码
                result.put("errMsg", ""); // 空错误消息
                result.put("data", contactInfo);
                return result;
            }
            // 检查账号和密码（新功能）
            if (account != null && user.getAccount().equals(account) && user.getPassword().equals(password)) {
                // 创建与客户端ContactInfo对象匹配的数据结构
                Map<String, Object> contactInfo = new HashMap<>();
                contactInfo.put("id", user.getId());
                contactInfo.put("name", user.getUsername());
                contactInfo.put("account", user.getAccount());
                contactInfo.put("phone", user.getPhone());
                contactInfo.put("avatarUrl", user.getAvatarUrl());
                contactInfo.put("status", "在线"); // 默认状态
                
                Map<String, Object> result = new HashMap<>();
                result.put("retCode", 0); // 成功代码
                result.put("errMsg", ""); // 空错误消息
                result.put("data", contactInfo);
                return result;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("retCode", 401);
        result.put("errMsg", "账号或密码错误");
        result.put("data", null);
        return result;
    }

    // 3. 获取联系人接口 /apis/get_contacts
    @GetMapping("/get_contacts")
    public Map<String, Object> getContacts(@RequestParam(required = false) Long userId) {
        // 如果没有提供userId，返回空列表
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("retCode", 0);
            result.put("errMsg", "");
            result.put("data", new ArrayList<>());
            return result;
        }
        
        List<Contact> contacts = contactMap.getOrDefault(userId, new ArrayList<>());
        
        // 转换为前端需要的ContactInfo格式
        List<Map<String, Object>> contactInfoList = new ArrayList<>();
        for (Contact contact : contacts) {
            User contactUser = userMap.get(contact.getContactId());
            if (contactUser != null) {
                Map<String, Object> contactInfo = new HashMap<>();
                contactInfo.put("id", contactUser.getId());
                contactInfo.put("name", contact.getRemark() != null ? contact.getRemark() : contactUser.getUsername());
                contactInfo.put("account", contactUser.getAccount());
                contactInfo.put("phone", contactUser.getPhone());
                contactInfo.put("avatarUrl", contactUser.getAvatarUrl());
                contactInfo.put("status", "在线");
                contactInfoList.add(contactInfo);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("retCode", 0);
        result.put("errMsg", "");
        result.put("data", contactInfoList);
        return result;
    }

    // 添加好友接口 /apis/add_friend
    @PostMapping("/add_friend")
    public Map<String, Object> addFriend(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Long friendId = Long.valueOf(params.get("friendId").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        
        Map<String, Object> result = new HashMap<>();
        
        // 检查用户是否存在
        if (!userMap.containsKey(userId) || !userMap.containsKey(friendId)) {
            result.put("retCode", 404);
            result.put("errMsg", "用户不存在");
            return result;
        }
        
        // 检查是否已经是好友
        List<Contact> userContacts = contactMap.getOrDefault(userId, new ArrayList<>());
        for (Contact contact : userContacts) {
            if (contact.getContactId().equals(friendId)) {
                result.put("retCode", 400);
                result.put("errMsg", "已经是好友");
                return result;
            }
        }
        
        // 添加好友关系（双向）
        // 用户A添加用户B
        Contact contact1 = new Contact(idGenerator.getAndIncrement(), userId, friendId, remark);
        if (!contactMap.containsKey(userId)) {
            contactMap.put(userId, new ArrayList<>());
        }
        contactMap.get(userId).add(contact1);
        
        // 用户B自动添加用户A
        Contact contact2 = new Contact(idGenerator.getAndIncrement(), friendId, userId, null);
        if (!contactMap.containsKey(friendId)) {
            contactMap.put(friendId, new ArrayList<>());
        }
        contactMap.get(friendId).add(contact2);
        
        // 保存数据
        saveUserData();
        
        result.put("retCode", 0);
        result.put("errMsg", "");
        result.put("data", "添加好友成功");
        return result;
    }

    // 4. 上传消息接口 /apis/upload_message
    @PostMapping("/upload_message")
    public Map<String, Object> uploadMessage(@RequestBody Message message) {
        message.setId(idGenerator.getAndIncrement());
        message.setSendTime(System.currentTimeMillis() + ""); // 模拟时间戳
        messageList.add(message);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "上传成功");
        result.put("data", message);
        return result;
    }

    // 5. 获取消息接口 /apis/get_messages
    @GetMapping("/get_messages")
    public Map<String, Object> getMessages(@RequestParam Long senderId, @RequestParam Long receiverId) {
        List<Message> resultMsg = new ArrayList<>();
        for (Message msg : messageList) {
            if ((msg.getSenderId().equals(senderId) && msg.getReceiverId().equals(receiverId))
                    || (msg.getSenderId().equals(receiverId) && msg.getReceiverId().equals(senderId))) {
                resultMsg.add(msg);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "获取成功");
        result.put("data", resultMsg);
        return result;
    }
    
    // 6. 删除用户接口 /apis/delete_user
    @DeleteMapping("/delete_user")
    public Map<String, Object> deleteUser(@RequestParam(required = false) Long userId, 
                                         @RequestParam(required = false) String phone) {
        Map<String, Object> result = new HashMap<>();
        
        // 根据用户ID删除
        if (userId != null) {
            User removedUser = userMap.remove(userId);
            if (removedUser != null) {
                saveUserData();
                result.put("code", 200);
                result.put("msg", "用户删除成功");
                return result;
            }
        }
        
        // 根据手机号删除
        if (phone != null) {
            User userToRemove = null;
            for (User user : userMap.values()) {
                if (user.getPhone() != null && user.getPhone().equals(phone)) {
                    userToRemove = user;
                    break;
                }
            }
            
            if (userToRemove != null) {
                userMap.remove(userToRemove.getId());
                saveUserData();
                result.put("code", 200);
                result.put("msg", "用户删除成功");
                return result;
            }
        }
        
        // 没有找到用户
        result.put("code", 404);
        result.put("msg", "用户不存在");
        return result;
    }
    
    // 7. 删除所有用户接口 /apis/delete_all_users
    @DeleteMapping("/delete_all_users")
    public Map<String, Object> deleteAllUsers() {
        userMap.clear();
        idGenerator.set(1);
        saveUserData();
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "所有用户数据已删除");
        return result;
    }
    
    // 静态资源映射，使得客户端可以访问上传的头像文件
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传的头像文件
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        System.out.println("上传路径: " + uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
        
        // 映射默认头像资源
        registry.addResourceHandler("/image/**")
                .addResourceLocations("classpath:/static/image/");
        
        System.out.println("静态资源映射配置完成");
    }
    
    // 数据持久化方法
    private void loadPersistedData() {
        try {
            File dataFile = new File(DATA_FILE_PATH);
            if (dataFile.exists()) {
                // 加载已保存的用户数据
                Map<String, Object> dataMap = objectMapper.readValue(dataFile, new TypeReference<Map<String, Object>>() {});
                
                // 恢复用户数据
                if (dataMap.containsKey("users")) {
                    List<User> users = objectMapper.convertValue(dataMap.get("users"), new TypeReference<List<User>>() {});
                    for (User user : users) {
                        userMap.put(user.getId(), user);
                    }
                }
                
                // 恢复联系人关系数据
                if (dataMap.containsKey("contacts")) {
                    List<Contact> contacts = objectMapper.convertValue(dataMap.get("contacts"), new TypeReference<List<Contact>>() {});
                    for (Contact contact : contacts) {
                        Long userId = contact.getUserId();
                        if (!contactMap.containsKey(userId)) {
                            contactMap.put(userId, new ArrayList<>());
                        }
                        contactMap.get(userId).add(contact);
                    }
                    System.out.println("联系人数据加载成功，联系人关系数量: " + contacts.size());
                }
                
                // 恢复ID生成器
                if (dataMap.containsKey("lastId")) {
                    long lastId = objectMapper.convertValue(dataMap.get("lastId"), Long.class);
                    idGenerator.set(lastId + 1);
                }
                
                System.out.println("数据加载成功，用户数量: " + userMap.size());
            } else {
                System.out.println("数据文件不存在，将创建新数据文件");
            }
        } catch (Exception e) {
            System.err.println("数据加载失败: " + e.getMessage());
        }
    }
    
    private void persistData() {
        try {
            // 创建数据目录
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // 准备要保存的数据
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("users", new ArrayList<>(userMap.values()));
            
            // 保存联系人关系数据
            List<Contact> allContacts = new ArrayList<>();
            for (List<Contact> contacts : contactMap.values()) {
                allContacts.addAll(contacts);
            }
            dataMap.put("contacts", allContacts);
            
            dataMap.put("lastId", idGenerator.get() - 1);
            
            // 保存到文件
            File dataFile = new File(DATA_FILE_PATH);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, dataMap);
            System.out.println("数据保存成功，用户数量: " + userMap.size() + ", 联系人关系数量: " + allContacts.size());
        } catch (Exception e) {
            System.err.println("数据保存失败: " + e.getMessage());
        }
    }
    
    // 在所有修改用户数据的方法中添加持久化调用
    private void saveUserData() {
        new Thread(() -> {
            try {
                Thread.sleep(100); // 延迟保存，避免频繁IO操作
                persistData();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}