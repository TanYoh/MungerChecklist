package com.example.mungerchecklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 获取全局唯一的本地数据库实例
        val database = AppDatabase.getDatabase(applicationContext)
        // 2. 实例化数据仓库
        val repository = ChecklistRepository(database)

        setContent {
            // 3. 通过自定义工厂将数据仓库注入到 ViewModel 中
            val viewModel: ChecklistViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ChecklistViewModel(repository) as T
                    }
                }
            )
            // 启动 APP 界面
            ChecklistApp(viewModel)
        }
    }
}