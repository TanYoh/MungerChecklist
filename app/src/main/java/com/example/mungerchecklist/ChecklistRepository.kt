package com.example.mungerchecklist

import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val db: AppDatabase) {
    val categories: Flow<List<Category>> = db.categoryDao().getAllCategories()
    val models: Flow<List<MentalModel>> = db.mentalModelDao().getAllModels()

    suspend fun insertCategory(category: Category) = db.categoryDao().insertCategory(category)
    suspend fun updateCategories(categories: List<Category>) = db.categoryDao().updateCategories(categories)
    suspend fun insertModel(model: MentalModel) = db.mentalModelDao().insertModel(model)
    suspend fun updateModel(model: MentalModel) = db.mentalModelDao().updateModel(model)
    suspend fun deleteModel(modelId: Int) = db.mentalModelDao().deleteModelById(modelId)

    // 初次打开 APP 时，注入你的默认样例数据
    suspend fun prepopulateIfEmpty() {
        if (db.categoryDao().getCategoryCount() == 0) {
            val initialCategories = listOf(
                Category(name = "心理学", sortOrder = 0),
                Category(name = "概率与统计", sortOrder = 1),
                Category(name = "Engineering", sortOrder = 2)
            )
            initialCategories.forEach { insertCategory(it) }
        }

        if (db.mentalModelDao().getModelCount() == 0) {
            val initialModels = listOf(
                MentalModel(categoryId = 1, name = "损失厌恶", definition = "人们面对同样数量的收益和损失时，认为损失更加令人难以忍受。损失带来的痛苦程度大约是同等收益带来快乐程度的两倍。", examples = listOf("投资案例：投资者不愿割肉卖出亏损股票。", "购物案例：商品一旦带回家，就不愿意再退回去了。")),
                MentalModel(categoryId = 1, name = "蔡加尼克效应", definition = "人们天生容易忘记已完成的工作，却对未完成的、被打断的工作记忆犹新。", examples = listOf("晚上脑海中不断盘旋未完成的待办事项导致失眠。")),
                MentalModel(categoryId = 1, name = "Confirmation Bias", definition = "The tendency to search for, interpret, favor, and recall information in a way that confirms one's prior beliefs.", examples = listOf("Social media echo chambers.", "Ignoring crash logs because you believe code is flawless.")),
                MentalModel(categoryId = 3, name = "Redundancy System", definition = "The duplication of critical components to increase reliability.", examples = listOf("Using RAID 1 array for hard drives.", "Backup power generators."))
            )
            initialModels.forEach { insertModel(it) }
        }
    }

    suspend fun deleteCategory(categoryId: Int) {
        // 先删除该分类下的所有思维模型
        db.mentalModelDao().deleteModelsByCategoryId(categoryId)
        // 再删除分类本身
        db.categoryDao().deleteCategoryById(categoryId)
    }
}