package com.example.microchat.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room数据库实例类，使用单例模式
 */
@Database(entities = {UserEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "microchat_database";
    private static volatile AppDatabase INSTANCE;

    // 提供UserDao实例
    public abstract UserDao userDao();

    // 单例模式获取数据库实例
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .allowMainThreadQueries() // 允许在主线程执行查询（仅用于简单应用，生产环境应避免）
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}