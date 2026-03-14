package com.example.mungerchecklist

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. 实体类定义 (Entity)
// ==========================================

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sortOrder: Int = 0 // 【新增】：专门用于记忆用户拖拽排序的结果
)

@Entity(tableName = "mental_models")
data class MentalModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val definition: String,
    val examples: List<String> = emptyList()
)

// ==========================================
// 2. 类型转换器 (TypeConverter)
// 解决 SQLite 无法直接存 List 的问题
// ==========================================
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
}

// ==========================================
// 3. 数据访问对象 (DAO)
// ==========================================
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long // 【修复】：添加 Long 返回值

    @Update
    suspend fun updateCategories(categories: List<Category>): Int // 【修复】：添加 Int 返回值

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface MentalModelDao {
    @Query("SELECT * FROM mental_models")
    fun getAllModels(): Flow<List<MentalModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: MentalModel): Long // 【修复】：添加 Long 返回值

    @Update
    suspend fun updateModel(model: MentalModel): Int // 【修复】：添加 Int 返回值

    @Query("DELETE FROM mental_models WHERE id = :modelId")
    suspend fun deleteModelById(modelId: Int): Int // 【修复】：添加 Int 返回值

    @Query("SELECT COUNT(*) FROM mental_models")
    suspend fun getModelCount(): Int
}

// ==========================================
// 4. 数据库本体配置
// ==========================================
@Database(entities = [Category::class, MentalModel::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun mentalModelDao(): MentalModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "munger_database" // 本地数据库文件名
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}