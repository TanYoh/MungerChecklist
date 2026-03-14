package com.example.mungerchecklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 获取 ViewModel 实例，生命周期与 Activity 绑定
            val viewModel: ChecklistViewModel = viewModel()
            // 启动 APP 界面，将大脑传递给视图
            ChecklistApp(viewModel)
        }
    }
}