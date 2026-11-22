package com.example.pricelistapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pricelistapp.data.local.*
import com.example.pricelistapp.ui.login.UserRole

@Composable
fun BrowseTabContent(
    user: User,
    currentRole: UserRole,
    onProductClick: (ProductWithVariants, String) -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val selectedSubTab by viewModel.selectedSubTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val tabWidth = maxOf(screenWidth / 3, 120.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Sideways scrollable tab row with equal width tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            val tabs = listOf("Category", "Supplier", "Customer")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { viewModel.selectSubTab(index) },
                    modifier = Modifier.width(tabWidth),
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content based on sub-tab and drilldown state
        when (selectedSubTab) {
            0 -> {
                if (selectedCategory == null) {
                    CategorySubTab(viewModel = viewModel)
                } else {
                    CategoryProductsDrilldown(
                        category = selectedCategory!!,
                        viewModel = viewModel,
                        onProductClick = { prod -> onProductClick(prod, "Category") },
                        onBack = { viewModel.clearDrilldown() }
                    )
                }
            }
            1 -> {
                if (selectedSupplier == null) {
                    SupplierSubTab(viewModel = viewModel)
                } else {
                    SupplierDetailDrilldown(
                        supplier = selectedSupplier!!,
                        viewModel = viewModel,
                        currentRole = currentRole,
                        onProductClick = { prod -> onProductClick(prod, "Supplier") },
                        onBack = { viewModel.clearDrilldown() }
                    )
                }
            }
            2 -> {
                if (selectedCustomer == null) {
                    CustomerSubTab(viewModel = viewModel)
                } else {
                    CustomerDetailDrilldown(
                        customer = selectedCustomer!!,
                        viewModel = viewModel,
                        currentRole = currentRole,
                        onProductClick = { prod -> onProductClick(prod, "Customer") },
                        onBack = { viewModel.clearDrilldown() }
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySubTab(viewModel: BrowseViewModel) {
    val query by viewModel.categoryQuery.collectAsState()
    val categories by viewModel.filteredCategories.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search categories (300ms debounce)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateCategoryQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { if (!isFocused) Text("Search categories...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateCategoryQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val label = if (query.isBlank()) "Last ${categories.size} categories" else "${categories.size} categories found"
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (categories.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No categories found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(categories) { index, cat ->
                    CategoryCard(index = index + 1, category = cat, onClick = { viewModel.selectCategory(cat) })
                }
            }
        }
    }
}

@Composable
fun CategoryCard(index: Int, category: CategoryEntity, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!category.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryProductsDrilldown(
    category: CategoryEntity,
    viewModel: BrowseViewModel,
    onProductClick: (ProductWithVariants) -> Unit,
    onBack: () -> Unit
) {
    val paginatedProducts by viewModel.paginatedDrilldownProducts.collectAsState()
    val currentPage by viewModel.drilldownCurrentPage.collectAsState()
    val totalPages by viewModel.drilldownTotalPages.collectAsState()
    val totalRecords by viewModel.drilldownTotalRecords.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Category (${category.name})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        PaginatorBar(
            currentPage = currentPage,
            totalPages = totalPages,
            onFirst = { viewModel.firstDrilldownPage() },
            onPrev = { viewModel.prevDrilldownPage() },
            onNext = { viewModel.nextDrilldownPage() },
            onLast = { viewModel.lastDrilldownPage() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "$totalRecords products in category '${category.name}'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (paginatedProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No products found in this category.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(paginatedProducts) { index, item ->
                    val globalIndex = (currentPage - 1) * viewModel.pageSize + index + 1
                    ProductCard(index = globalIndex, item = item, onClick = { onProductClick(item) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                PaginatorBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onFirst = { viewModel.firstDrilldownPage() },
                    onPrev = { viewModel.prevDrilldownPage() },
                    onNext = { viewModel.nextDrilldownPage() },
                    onLast = { viewModel.lastDrilldownPage() }
                )
            }
        }
    }
}

// Supplier Sub-Tab & Drilldown
@Composable
fun SupplierSubTab(viewModel: BrowseViewModel) {
    val query by viewModel.supplierQuery.collectAsState()
    val suppliers by viewModel.filteredSuppliers.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search suppliers (300ms debounce)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSupplierQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { if (!isFocused) Text("Search suppliers...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSupplierQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val label = if (query.isBlank()) "Last ${suppliers.size} suppliers" else "${suppliers.size} suppliers found"
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (suppliers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No suppliers found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(suppliers) { index, sup ->
                    SupplierCard(index = index + 1, supplier = sup, onClick = { viewModel.selectSupplier(sup) })
                }
            }
        }
    }
}

@Composable
fun SupplierCard(index: Int, supplier: SupplierEntity, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!supplier.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Address: ${supplier.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!supplier.contactNum.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Contact: ${supplier.contactNum}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SupplierDetailDrilldown(
    supplier: SupplierEntity,
    viewModel: BrowseViewModel,
    currentRole: UserRole,
    onProductClick: (ProductWithVariants) -> Unit,
    onBack: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val paginatedProducts by viewModel.paginatedDrilldownProducts.collectAsState()
    val currentPage by viewModel.drilldownCurrentPage.collectAsState()
    val totalPages by viewModel.drilldownTotalPages.collectAsState()
    val totalRecords by viewModel.drilldownTotalRecords.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Supplier (${supplier.name})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Collapsible Supplier Info Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!supplier.contactNum.isNullOrBlank() && !isExpanded) {
                            Text(text = "Contact: ${supplier.contactNum}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Supplier", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Collapse"
                            )
                        }
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Address:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!supplier.address.isNullOrBlank()) supplier.address else "No address provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!supplier.address.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Contact Info:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!supplier.contactNum.isNullOrBlank()) supplier.contactNum else "No contact info provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!supplier.contactNum.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Notes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!supplier.notes.isNullOrBlank()) supplier.notes else "No notes provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!supplier.notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaginatorBar(
            currentPage = currentPage,
            totalPages = totalPages,
            onFirst = { viewModel.firstDrilldownPage() },
            onPrev = { viewModel.prevDrilldownPage() },
            onNext = { viewModel.nextDrilldownPage() },
            onLast = { viewModel.lastDrilldownPage() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "$totalRecords products associated with '${supplier.name}'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (paginatedProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No products found for this supplier.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(paginatedProducts) { index, item ->
                    val globalIndex = (currentPage - 1) * viewModel.pageSize + index + 1
                    ProductCard(index = globalIndex, item = item, onClick = { onProductClick(item) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                PaginatorBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onFirst = { viewModel.firstDrilldownPage() },
                    onPrev = { viewModel.prevDrilldownPage() },
                    onNext = { viewModel.nextDrilldownPage() },
                    onLast = { viewModel.lastDrilldownPage() }
                )
            }
        }
    }

    if (showEditDialog) {
        EditSupplierDialog(
            supplier = supplier,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateSupplier(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditSupplierDialog(
    supplier: SupplierEntity,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf(supplier.name) }
    var address by remember { mutableStateOf(supplier.address ?: "") }
    var contactNum by remember { mutableStateOf(supplier.contactNum ?: "") }
    var notes by remember { mutableStateOf(supplier.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Supplier Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactNum,
                    onValueChange = { contactNum = it },
                    label = { Text("Contact Info (Text)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Plain Text)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(supplier.copy(name = name, address = address, contactNum = contactNum, notes = notes))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Customer Sub-Tab & Drilldown
@Composable
fun CustomerSubTab(viewModel: BrowseViewModel) {
    val query by viewModel.customerQuery.collectAsState()
    val customers by viewModel.filteredCustomers.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search customers (300ms debounce)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateCustomerQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { if (!isFocused) Text("Search customers...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateCustomerQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val label = if (query.isBlank()) "Last ${customers.size} customers" else "${customers.size} customers found"
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (customers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No customers found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(customers) { index, cust ->
                    CustomerCard(index = index + 1, customer = cust, onClick = { viewModel.selectCustomer(cust) })
                }
            }
        }
    }
}

@Composable
fun CustomerCard(index: Int, customer: CustomerEntity, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!customer.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Address: ${customer.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!customer.contactNum.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Contact: ${customer.contactNum}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerDetailDrilldown(
    customer: CustomerEntity,
    viewModel: BrowseViewModel,
    currentRole: UserRole,
    onProductClick: (ProductWithVariants) -> Unit,
    onBack: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val paginatedProducts by viewModel.paginatedDrilldownProducts.collectAsState()
    val currentPage by viewModel.drilldownCurrentPage.collectAsState()
    val totalPages by viewModel.drilldownTotalPages.collectAsState()
    val totalRecords by viewModel.drilldownTotalRecords.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Customer (${customer.name})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Collapsible Customer Info Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!customer.contactNum.isNullOrBlank() && !isExpanded) {
                            Text(text = "Contact: ${customer.contactNum}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Collapse"
                            )
                        }
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Address:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!customer.address.isNullOrBlank()) customer.address else "No address provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!customer.address.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Contact Info:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!customer.contactNum.isNullOrBlank()) customer.contactNum else "No contact info provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!customer.contactNum.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Notes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!customer.notes.isNullOrBlank()) customer.notes else "No notes provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!customer.notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PaginatorBar(
            currentPage = currentPage,
            totalPages = totalPages,
            onFirst = { viewModel.firstDrilldownPage() },
            onPrev = { viewModel.prevDrilldownPage() },
            onNext = { viewModel.nextDrilldownPage() },
            onLast = { viewModel.lastDrilldownPage() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "$totalRecords products associated with '${customer.name}'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (paginatedProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No products found for this customer.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                itemsIndexed(paginatedProducts) { index, item ->
                    val globalIndex = (currentPage - 1) * viewModel.pageSize + index + 1
                    ProductCard(index = globalIndex, item = item, onClick = { onProductClick(item) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                PaginatorBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onFirst = { viewModel.firstDrilldownPage() },
                    onPrev = { viewModel.prevDrilldownPage() },
                    onNext = { viewModel.nextDrilldownPage() },
                    onLast = { viewModel.lastDrilldownPage() }
                )
            }
        }
    }

    if (showEditDialog) {
        EditCustomerDialog(
            customer = customer,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateCustomer(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditCustomerDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var address by remember { mutableStateOf(customer.address ?: "") }
    var contactNum by remember { mutableStateOf(customer.contactNum ?: "") }
    var notes by remember { mutableStateOf(customer.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Customer Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactNum,
                    onValueChange = { contactNum = it },
                    label = { Text("Contact Info (Text)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Plain Text)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(customer.copy(name = name, address = address, contactNum = contactNum, notes = notes))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
