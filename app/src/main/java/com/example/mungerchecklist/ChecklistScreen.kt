package com.example.mungerchecklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity



// 【飞行员警报：硬核、严肃、实用的芒格专属色板】
object ChecklistColors {
    val AlertCrimson = Color(0xFF991B1B) // 沉稳的砖红色，代表警惕、停下思考、防错
    val InkBlack = Color(0xFF1C1917) // 极深的墨黑，用于核心文字，严肃有力
    val SlateGrey = Color(0xFF57534E) // 工业石板灰，用于次要说明文字

    val BackgroundParchment = Color(0xFFFAFAF9) // 极其接近真实纸张的背景色
    val DrawerBackground = Color(0xFFF5F5F4) // 抽屉背景使用稍暗一点的纸张灰
    val BorderLight = Color(0xFFE7E5E4) // 工业风的细灰线边框
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistApp(viewModel: ChecklistViewModel) {
    val categories by viewModel.categories.collectAsState()
    val models by viewModel.models.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) } // 【新增】暂存准备删除的对象

    val filteredModels = remember(searchQuery, models) {
        if (searchQuery.isBlank()) models else models.filter { model ->
            model.name.contains(searchQuery, ignoreCase = true) ||
                    model.definition.contains(searchQuery, ignoreCase = true) ||
                    model.examples.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    val filteredCategories = remember(searchQuery, categories, filteredModels) {
        if (searchQuery.isBlank()) categories else categories.filter { category ->
            category.name.contains(searchQuery, ignoreCase = true) ||
                    filteredModels.any { it.categoryId == category.id }
        }
    }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex -> viewModel.moveCategory(fromIndex, toIndex) },
        onDropToDelete = { index ->
            // 【新增】拖拽到垃圾桶松手后，调起确认弹窗
            categoryToDelete = filteredCategories.getOrNull(index)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(88.dp),
                title = {
                    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Text(text = "芒格检查清单", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChecklistColors.AlertCrimson)
            )
        },
        floatingActionButton = {
            // 【修改】当发生拖拽时，自动隐藏加号悬浮窗
            AnimatedVisibility(
                visible = dragDropState.draggingItemIndex == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ChecklistColors.AlertCrimson,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "新增模型")
                }
            }
        }
    ) { innerPadding ->
        // 【新增】用 Box 包裹，方便在底部放置拖拽销毁区
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ChecklistColors.BackgroundParchment)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("检索防错原则...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索", tint = ChecklistColors.SlateGrey) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "清空", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ChecklistColors.AlertCrimson,
                        unfocusedBorderColor = ChecklistColors.BorderLight
                    )
                )

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (searchQuery.isEmpty()) dragDropState.pointerInputModifier else Modifier)
                ) {
                    itemsIndexed(items = filteredCategories, key = { _, category -> category.id }) { index, category ->
                        val isDragging = dragDropState.draggingItemIndex == index
                        val dragOffset = if (isDragging) dragDropState.draggingItemOffset else 0f

                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = dragOffset }
                        ) {
                            val categoryModels = filteredModels.filter { it.categoryId == category.id }
                            CategoryCard(
                                category = category,
                                models = categoryModels,
                                onDelete = { modelId -> viewModel.deleteModel(modelId) },
                                onDeleteExample = { modelId, exampleIndex -> viewModel.deleteExample(modelId, exampleIndex) },
                                onAddExample = { modelId, text -> viewModel.addExample(modelId, text) },
                                isSearchActive = searchQuery.isNotEmpty()
                            )
                        }
                    }
                }
            }

            // 【升级版】底部椭圆形弧面销毁区
            AnimatedVisibility(
                visible = dragDropState.draggingItemIndex != null,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val isOver = dragDropState.isOverDeleteZone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp) // 增加高度，给椭圆弧度留出足够的生长空间
                        .graphicsLayer {
                            clip = true
                            shape = GenericShape { size, _ ->
                                // 绘制逻辑：底部完全封死，顶部呈椭圆弧形
                                moveTo(0f, size.height)      // 1. 左下角
                                lineTo(size.width, size.height) // 2. 连到右下角
                                lineTo(size.width, size.height * 0.4f) // 3. 连到右侧边缘中点

                                // 4. 核心：画一条优美的椭圆弧线连接到左侧中点
                                // 控制点设在屏幕水平中心且垂直坐标为负数，确保弧顶圆润且覆盖全宽
                                quadraticTo(
                                    size.width / 2f, -size.height * 0f,
                                    0f, size.height * 0.4f
                                )
                                close()
                            }
                        }
                        // 动态切换颜色：检测到卡片进入时加深红色
                        .background(if (isOver) Color(0xFFB91C1C) else Color(0xFFEF4444).copy(alpha = 0.95f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 36.dp), // 将文字抬高，使其处于椭圆弧形的视觉中心
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isOver) "立即释放以销毁" else "拖动到此维度销毁",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        // 【新增】全局防误触确认弹窗
        categoryToDelete?.let { category ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                containerColor = Color.White,
                title = { Text("删除核查维度", fontWeight = FontWeight.ExtraBold, color = ChecklistColors.InkBlack) },
                text = { Text("确定要删除【${category.name}】及其包含的所有思维模型吗？此操作无法撤销。", color = ChecklistColors.SlateGrey) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCategory(category.id)
                        categoryToDelete = null
                    }) {
                        Text("确认删除", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("取消", color = Color.Gray)
                    }
                }
            )
        }

        if (showAddDialog) {
            AddDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { categoryId, name, def ->
                    viewModel.addModel(categoryId, name, def)
                    showAddDialog = false
                },
                onAddCategory = { newCategoryName -> viewModel.addCategory(newCategoryName) }
            )
        }
    }
}

// ---------------- 一级分类卡片 ----------------
@Composable
fun CategoryCard(
    category: Category,
    models: List<MentalModel>,
    onDelete: (Int) -> Unit,
    onDeleteExample: (Int, Int) -> Unit,
    onAddExample: (Int, String) -> Unit,
    isSearchActive: Boolean
) {
    var expanded by remember(isSearchActive) { mutableStateOf(isSearchActive) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, ChecklistColors.BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ChecklistColors.InkBlack)
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null, tint = ChecklistColors.SlateGrey
                )
            }

            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Divider(color = ChecklistColors.BorderLight, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (models.isEmpty()) {
                        Text("暂无数据", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        models.forEach { model ->
                            MentalModelItem(model = model, onDelete = onDelete, onDeleteExample = onDeleteExample, onAddExample = onAddExample, isSearchActive = isSearchActive)
                            Divider(color = ChecklistColors.BorderLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 二级事项与三级内容组件 ----------------
@Composable
fun MentalModelItem(
    model: MentalModel,
    onDelete: (Int) -> Unit,
    onDeleteExample: (Int, Int) -> Unit,
    onAddExample: (Int, String) -> Unit,
    isSearchActive: Boolean
) {
    var isDetailExpanded by remember(isSearchActive) { mutableStateOf(isSearchActive) }
    var showAddExampleDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().clickable { isDetailExpanded = !isDetailExpanded }.padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "• ${model.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChecklistColors.InkBlack
                )
                Icon(imageVector = if (isDetailExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp).padding(start = 4.dp))
            }
            IconButton(onClick = { onDelete(model.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }
        AnimatedVisibility(visible = isDetailExpanded, enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)), exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChecklistColors.DrawerBackground)
                    .padding(16.dp)
            ) {
                Text(text = "【核心要点】", fontSize = 13.sp, fontWeight = FontWeight.Black, color = ChecklistColors.AlertCrimson)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = model.definition, fontSize = 15.sp, color = ChecklistColors.InkBlack, lineHeight = 24.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "【反面案例】", fontSize = 13.sp, fontWeight = FontWeight.Black, color = ChecklistColors.AlertCrimson)
                    Text(text = "+ 添加案例", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChecklistColors.SlateGrey, modifier = Modifier.clickable { showAddExampleDialog = true })
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (model.examples.isEmpty()) {
                    Text("请记录您犯过的错误案例...", fontSize = 13.sp, color = Color.Gray)
                } else {
                    model.examples.forEachIndexed { index, example ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text(text = "${index + 1}. $example", fontSize = 14.sp, color = ChecklistColors.SlateGrey, lineHeight = 22.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            IconButton(onClick = { onDeleteExample(model.id, index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.Red.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAddExampleDialog) {
        AddExampleDialog(onDismiss = { showAddExampleDialog = false }, onConfirm = { newText -> onAddExample(model.id, newText); showAddExampleDialog = false })
    }
}

// ---------------- 新增弹窗组件群 ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDialog(categories: List<Category>, onDismiss: () -> Unit, onConfirm: (Int, String, String) -> Unit, onAddCategory: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(categories.size) { if (categories.isNotEmpty()) selectedCategoryId = categories.last().id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("录入防错模型", fontWeight = FontWeight.Black, color = ChecklistColors.InkBlack) },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("所属分类") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = ChecklistColors.AlertCrimson), modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                        categories.forEach { category ->
                            DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategoryId = category.id; expanded = false })
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { showNewCategoryDialog = true }) { Text("+ 新建维度", color = ChecklistColors.SlateGrey, fontSize = 13.sp) }
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChecklistColors.AlertCrimson)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = definition, onValueChange = { definition = it }, label = { Text("防错原则 / 核心解释") }, maxLines = 3, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChecklistColors.AlertCrimson)
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank() && definition.isNotBlank()) onConfirm(selectedCategoryId, title, definition) }) { Text("确认录入", color = ChecklistColors.AlertCrimson, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) } }
    )

    if (showNewCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            containerColor = Color.White,
            title = { Text("新建核查维度", fontWeight = FontWeight.Black, color = ChecklistColors.InkBlack) },
            text = { OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("例如：投资、人际关系") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChecklistColors.AlertCrimson)) },
            confirmButton = { TextButton(onClick = { if (newCategoryName.isNotBlank()) { onAddCategory(newCategoryName); showNewCategoryDialog = false } }) { Text("确定", color = ChecklistColors.AlertCrimson) } },
            dismissButton = { TextButton(onClick = { showNewCategoryDialog = false }) { Text("取消", color = Color.Gray) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExampleDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("记录反面案例", fontWeight = FontWeight.Black, color = ChecklistColors.InkBlack) },
        text = { OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("描述你曾犯下的错误...") }, maxLines = 5, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChecklistColors.AlertCrimson)) },
        confirmButton = { TextButton(onClick = { if (content.isNotBlank()) onConfirm(content) }) { Text("保存记录", color = ChecklistColors.AlertCrimson, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) } }
    )
}

// ---------------- 列表底层拖拽手势核心引擎 ----------------
class DragDropState(
    val state: LazyListState,
    val density: Float,
    val onMove: (Int, Int) -> Unit,
    val onDropToDelete: (Int) -> Unit // 【新增】拖拽删除回调
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    var draggingItemOffset by mutableStateOf(0f)
        private set
    var isOverDeleteZone by mutableStateOf(false) // 【新增】是否进入了底部删除区域
        private set

    val pointerInputModifier = Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                state.layoutInfo.visibleItemsInfo
                    .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                    ?.also { itemInfo -> draggingItemIndex = itemInfo.index }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                draggingItemOffset += dragAmount.y
                val currentIdx = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                val currentItem = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIdx } ?: return@detectDragGesturesAfterLongPress

                // 【核心计算】：判断手指是否进入了屏幕最下方 100dp 的删除区域
                val deleteZoneHeightPx = 100 * density
                val viewportHeight = state.layoutInfo.viewportSize.height
                isOverDeleteZone = change.position.y > (viewportHeight - deleteZoneHeightPx)

                val startOffset = currentItem.offset + draggingItemOffset
                val middleOffset = startOffset + currentItem.size / 2f

                val targetItem = state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    middleOffset.toInt() in item.offset..(item.offset + item.size) && item.index != currentIdx
                }

                // 只有在【未进入】删除区域时，才允许正常的卡片换位排序
                if (targetItem != null && !isOverDeleteZone) {
                    onMove(currentIdx, targetItem.index)
                    draggingItemIndex = targetItem.index
                    draggingItemOffset += (currentItem.offset - targetItem.offset)
                }
            },
            onDragEnd = {
                // 如果在删除区域松手，则触发删除回调
                if (isOverDeleteZone && draggingItemIndex != null) {
                    onDropToDelete(draggingItemIndex!!)
                }
                resetDragState()
            },
            onDragCancel = { resetDragState() }
        )
    }

    private fun resetDragState() {
        draggingItemIndex = null
        draggingItemOffset = 0f
        isOverDeleteZone = false
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onDropToDelete: (Int) -> Unit // 【新增】
): DragDropState {
    val density = LocalDensity.current.density
    return remember { DragDropState(lazyListState, density, onMove, onDropToDelete) }
}