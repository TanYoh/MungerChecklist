package com.example.mungerchecklist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Category(val id: Int, val name: String)

data class MentalModel(
    val id: Int,
    val categoryId: Int,
    val name: String,
    val definition: String,
    val examples: List<String> = emptyList()
)

class ChecklistViewModel : ViewModel() {

    // 注入了全新的英文分类 Engineering
    private val initialCategories = listOf(
        Category(1, "心理学"),
        Category(2, "概率与统计"),
        Category(3, "Engineering")
    )

    private val initialModels = listOf(
        MentalModel(
            id = 1, categoryId = 1, name = "损失厌恶",
            definition = "人们面对同样数量的收益和损失时，认为损失更加令人难以忍受。损失带来的痛苦程度大约是同等收益带来快乐程度的两倍。",
            examples = listOf(
                "投资案例：投资者不愿割肉卖出亏损股票，哪怕理性分析该股票已经毫无基本面支撑。",
                "购物案例：商家推出不满意全额退款服务。消费者一旦把商品带回家，往往就不愿意再退回去了。"
            )
        ),
        MentalModel(
            id = 2, categoryId = 1, name = "蔡加尼克效应",
            definition = "人们天生容易忘记已完成的工作，却对未完成的、被打断的工作记忆犹新。这种心理张力往往是引发焦虑的原因。",
            examples = listOf(
                "睡眠焦虑：晚上躺在床上时，脑海中不断盘旋白天未完成的代码Bug或待办事项，导致无法入睡。应对策略是在睡前将未完成的事项写在纸上，物理清空大脑缓存。"
            )
        ),
        // 新增的英文测试案例 1 (挂载在心理学下)
        MentalModel(
            id = 3, categoryId = 1, name = "Confirmation Bias",
            definition = "The tendency to search for, interpret, favor, and recall information in a way that confirms or supports one's prior beliefs or values.",
            examples = listOf(
                "Social media echo chambers where algorithms only show you news that aligns with your political views.",
                "A developer ignoring crash logs because they firmly believe their code logic is flawless."
            )
        ),
        // 新增的英文测试案例 2 (挂载在全新的英文分类下)
        MentalModel(
            id = 4, categoryId = 3, name = "Redundancy System",
            definition = "The duplication of critical components or functions of a system with the intention of increasing reliability of the system, usually in the form of a backup or fail-safe.",
            examples = listOf(
                "Using multiple hard drives in a RAID 1 array so that if one disk fails, no data is lost.",
                "Deploying backup power generators in a database center."
            )
        )
    )

    private val _categories = MutableStateFlow(initialCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _models = MutableStateFlow(initialModels)
    val models: StateFlow<List<MentalModel>> = _models.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val currentList = _categories.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _categories.value = currentList
        }
    }

    fun addCategory(name: String) {
        val currentList = _categories.value
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        val newCategory = Category(newId, name)
        _categories.value = currentList + listOf(newCategory)
    }

    fun addModel(categoryId: Int, name: String, definition: String) {
        val currentList = _models.value
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        val newModel = MentalModel(newId, categoryId, name, definition)
        _models.value = currentList + listOf(newModel)
    }

    fun deleteModel(modelId: Int) {
        _models.value = _models.value.filter { it.id != modelId }
    }

    fun deleteExample(modelId: Int, exampleIndex: Int) {
        _models.value = _models.value.map { model ->
            if (model.id == modelId) {
                val newExamples = model.examples.toMutableList().apply {
                    if (exampleIndex in indices) removeAt(exampleIndex)
                }
                model.copy(examples = newExamples)
            } else {
                model
            }
        }
    }

    fun addExample(modelId: Int, newExample: String) {
        _models.value = _models.value.map { model ->
            if (model.id == modelId) {
                val newExamples = model.examples.toMutableList().apply { add(newExample) }
                model.copy(examples = newExamples)
            } else {
                model
            }
        }
    }
}