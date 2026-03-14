package com.example.mungerchecklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistViewModel(private val repository: ChecklistRepository) : ViewModel() {

    init {
        // ViewModel 初始化时，检查是否需要写入默认数据
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // 从数据库直连状态流，自动将底层的变更投射到 UI
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val models: StateFlow<List<MentalModel>> = repository.models
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val currentList = categories.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)

            // 将拖拽后的新顺序（sortOrder）保存回数据库
            val updatedList = currentList.mapIndexed { index, category ->
                category.copy(sortOrder = index)
            }
            viewModelScope.launch {
                repository.updateCategories(updatedList)
            }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val order = categories.value.size
            repository.insertCategory(Category(name = name, sortOrder = order))
        }
    }

    fun addModel(categoryId: Int, name: String, definition: String) {
        viewModelScope.launch {
            repository.insertModel(
                MentalModel(categoryId = categoryId, name = name, definition = definition)
            )
        }
    }

    fun deleteModel(modelId: Int) {
        viewModelScope.launch {
            repository.deleteModel(modelId)
        }
    }

    fun deleteExample(modelId: Int, exampleIndex: Int) {
        val model = models.value.find { it.id == modelId } ?: return
        val newExamples = model.examples.toMutableList().apply {
            if (exampleIndex in indices) removeAt(exampleIndex)
        }
        viewModelScope.launch {
            repository.updateModel(model.copy(examples = newExamples))
        }
    }

    fun addExample(modelId: Int, newExample: String) {
        val model = models.value.find { it.id == modelId } ?: return
        val newExamples = model.examples.toMutableList().apply { add(newExample) }
        viewModelScope.launch {
            repository.updateModel(model.copy(examples = newExamples))
        }
    }
}