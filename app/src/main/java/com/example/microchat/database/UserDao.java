package com.example.microchat.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * 用户数据访问对象，定义数据库操作方法
 */
@Dao
public interface UserDao {
    // 插入新用户
    @Insert
    Completable insert(UserEntity user);

    // 根据手机号查询用户
    @Query("SELECT * FROM users WHERE phone = :phone")
    Maybe<UserEntity> findByPhone(String phone);

    // 根据账号查询用户
    @Query("SELECT * FROM users WHERE account = :account")
    Maybe<UserEntity> findByAccount(String account);

    // 查询所有用户
    @Query("SELECT * FROM users")
    Single<UserEntity[]> getAllUsers();

    // 更新用户信息
    @Update
    Completable update(UserEntity user);

    // 根据手机号删除用户
    @Query("DELETE FROM users WHERE phone = :phone")
    Completable deleteByPhone(String phone);

    // 检查手机号是否已存在
    @Query("SELECT COUNT(*) FROM users WHERE phone = :phone")
    Single<Integer> countByPhone(String phone);

    // 删除所有用户数据
    @Query("DELETE FROM users")
    Completable deleteAllUsers();
}