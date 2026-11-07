package com.example.pricelistapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pricelistapp.data.local.CategoryEntity
import com.example.pricelistapp.data.local.CustomerEntity
import com.example.pricelistapp.data.local.SupplierEntity
import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.ui.login.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun matchesFuzzy(query: String, text: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    val t = text.lowercase()
    if (t.contains(q)) return true
    var qIdx = 0
    for (char in t) {
        if (qIdx < q.length && char == q[qIdx]) {
            qIdx++
        }
    }
    return qIdx == q.length
}

@Composable
fun AddTabContent(
    user: User,
    currentRole: UserRole,
    viewModel: AddViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val categories by viewModel.categories.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val customers by viewModel.customers.collectAsState()

    // Dialog states
    var showCategorySearchDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf<String?>(null) }

    var showSupplierSearchDialog by remember { mutableStateOf(false) }
    var showNewSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var supplierError by remember { mutableStateOf<String?>(null) }

    var showCustomerSearchDialog by remember { mutableStateOf(false) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var customerError by remember { mutableStateOf<String?>(null) }

    var activeCustomPriceTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (variantId, customPriceId)

    val greenColor = Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Title Area
        Text(
            text = "Add Product",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider()

        // 2. Category Dropdown with Fuzzy Search & Plus Button
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCategorySearchDialog = true },
                    colors = CardDefaults.outlinedCardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.selectedCategory?.name ?: "Select Category...",
                            color = if (viewModel.selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                    }
                }

                IconButton(
                    onClick = {
                        newCategoryName = ""
                        categoryError = null
                        showNewCategoryDialog = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }

        // 3. Product Textbox
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Product Name", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = viewModel.productName,
                onValueChange = { viewModel.updateProductName(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter product name") },
                singleLine = false,
                minLines = 1,
                maxLines = Int.MAX_VALUE
            )
        }

        // 4. Description Textbox
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Description", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.updateDescription(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter description (optional)") },
                singleLine = false,
                minLines = 1,
                maxLines = Int.MAX_VALUE
            )
        }

        // 5. Supplier Dropdown
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Primary Supplier", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSupplierSearchDialog = true },
                    colors = CardDefaults.outlinedCardColors()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.selectedSupplier?.name ?: "Select Supplier...",
                            color = if (viewModel.selectedSupplier != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Supplier")
                    }
                }

                IconButton(
                    onClick = {
                        newSupplierName = ""
                        supplierError = null
                        showNewSupplierDialog = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Supplier")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Add Variant Link with Green Circular Plus Icon (Left-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(
                onClick = { viewModel.addVariant() },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = greenColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Product Variant", color = greenColor, style = MaterialTheme.typography.titleMedium)
            }
        }

        viewModel.variants.forEachIndexed { index, variant ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Variant #${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.removeVariant(variant.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Variant", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    // a. Variant Name textbox
                    OutlinedTextField(
                        value = variant.variantName,
                        onValueChange = { viewModel.updateVariantName(variant.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Variant Name") },
                        singleLine = false,
                        minLines = 1,
                        maxLines = Int.MAX_VALUE
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // b. Supplier Price Numeric textbox (2 decimal places)
                        OutlinedTextField(
                            value = variant.supplierPrice,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                    viewModel.updateVariantSupplierPrice(variant.id, input)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Supplier Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        // c. Unit
                        OutlinedTextField(
                            value = variant.unit,
                            onValueChange = { viewModel.updateVariantUnit(variant.id, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Unit") },
                            singleLine = true
                        )
                    }

                    // d. Selling Price Numeric textbox (2 decimal places)
                    OutlinedTextField(
                        value = variant.sellingPrice,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                viewModel.updateVariantSellingPrice(variant.id, input)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Selling Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    // Custom Prices Section inside Variant (Customer Dropdown + Price)
                    if (variant.customPrices.isNotEmpty()) {
                        Text("Custom Prices", style = MaterialTheme.typography.labelMedium)
                        variant.customPrices.forEach { cp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Custom Price Item", style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { viewModel.removeCustomPrice(variant.id, cp.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove Custom Price", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Customer Dropdown for Custom Price
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    activeCustomPriceTarget = Pair(variant.id, cp.id)
                                                    showCustomerSearchDialog = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = cp.customer?.name ?: "Select Customer...",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                activeCustomPriceTarget = Pair(variant.id, cp.id)
                                                newCustomerName = ""
                                                customerError = null
                                                showNewCustomerDialog = true
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Customer", modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // Price Numeric textbox (2 decimal places)
                                    OutlinedTextField(
                                        value = cp.price,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                                viewModel.updateCustomPriceValue(variant.id, cp.id, input)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Custom Price") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // e. Add Custom Price link/button with green plus icon
                    TextButton(
                        onClick = { viewModel.addCustomPrice(variant.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = greenColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Price", color = greenColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Clear and Save Buttons at the bottom (Clear on left, Save on right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearForm() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveProduct(user.id)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- Category Search Dialog (Debounced, Auto-focus, Last 5, Quick-Add with Green Plus) ---
    if (showCategorySearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        var recentCategories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            recentCategories = viewModel.getRecentCategories()
            focusRequester.requestFocus()
        }

        LaunchedEffect(searchQuery) {
            delay(500)
            debouncedQuery = searchQuery
        }

        val displayedCategories = remember(debouncedQuery, categories, recentCategories) {
            if (debouncedQuery.isBlank()) {
                recentCategories
            } else {
                categories.filter { matchesFuzzy(debouncedQuery, it.name) }
            }
        }

        AlertDialog(
            onDismissRequest = { showCategorySearchDialog = false },
            title = { Text("Select Category") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Fuzzy search categories...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (debouncedQuery.isNotBlank() && displayedCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.addCategoryToDb(debouncedQuery)
                                        if (res.isSuccess) {
                                            viewModel.updateSelectedCategory(res.getOrNull())
                                            showCategorySearchDialog = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = greenColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add \"$debouncedQuery\" to the Database?", color = greenColor)
                            }
                        }
                    } else if (displayedCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No categories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        if (debouncedQuery.isBlank()) {
                            Text("Recent Categories:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayedCategories.forEach { cat ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedCategory(cat)
                                            showCategorySearchDialog = false
                                        },
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = cat.name,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategorySearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- New Category Popup Form ---
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = {
                            newCategoryName = it
                            categoryError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category Name") },
                        singleLine = true,
                        isError = categoryError != null
                    )
                    if (categoryError != null) {
                        Text(
                            text = categoryError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.addCategoryToDb(newCategoryName)
                            if (result.isSuccess) {
                                showNewCategoryDialog = false
                            } else {
                                categoryError = result.exceptionOrNull()?.message ?: "Failed to add category"
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Supplier Search Dialog (Debounced, Auto-focus, Last 5, Quick-Add with Green Plus) ---
    if (showSupplierSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        var recentSuppliers by remember { mutableStateOf<List<SupplierEntity>>(emptyList()) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            recentSuppliers = viewModel.getRecentSuppliers()
            focusRequester.requestFocus()
        }

        LaunchedEffect(searchQuery) {
            delay(500)
            debouncedQuery = searchQuery
        }

        val displayedSuppliers = remember(debouncedQuery, suppliers, recentSuppliers) {
            if (debouncedQuery.isBlank()) {
                recentSuppliers
            } else {
                suppliers.filter { matchesFuzzy(debouncedQuery, it.name) }
            }
        }

        AlertDialog(
            onDismissRequest = { showSupplierSearchDialog = false },
            title = { Text("Select Supplier") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Fuzzy search suppliers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (debouncedQuery.isNotBlank() && displayedSuppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.addSupplierToDb(debouncedQuery)
                                        if (res.isSuccess) {
                                            viewModel.updateSelectedSupplier(res.getOrNull())
                                            showSupplierSearchDialog = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = greenColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add \"$debouncedQuery\" to the Database?", color = greenColor)
                            }
                        }
                    } else if (displayedSuppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No suppliers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        if (debouncedQuery.isBlank()) {
                            Text("Recent Suppliers:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayedSuppliers.forEach { sup ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedSupplier(sup)
                                            showSupplierSearchDialog = false
                                        },
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = sup.name,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierSearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- New Supplier Popup Form ---
    if (showNewSupplierDialog) {
        AlertDialog(
            onDismissRequest = { showNewSupplierDialog = false },
            title = { Text("New Supplier") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSupplierName,
                        onValueChange = {
                            newSupplierName = it
                            supplierError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Supplier Name") },
                        singleLine = true,
                        isError = supplierError != null
                    )
                    if (supplierError != null) {
                        Text(
                            text = supplierError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.addSupplierToDb(newSupplierName)
                            if (result.isSuccess) {
                                showNewSupplierDialog = false
                            } else {
                                supplierError = result.exceptionOrNull()?.message ?: "Failed to add supplier"
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSupplierDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Customer Search Dialog (Debounced, Auto-focus, Last 5, Quick-Add with Green Plus) ---
    if (showCustomerSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        var recentCustomers by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            recentCustomers = viewModel.getRecentCustomers()
            focusRequester.requestFocus()
        }

        LaunchedEffect(searchQuery) {
            delay(500)
            debouncedQuery = searchQuery
        }

        val displayedCustomers = remember(debouncedQuery, customers, recentCustomers) {
            if (debouncedQuery.isBlank()) {
                recentCustomers
            } else {
                customers.filter { matchesFuzzy(debouncedQuery, it.name) }
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomerSearchDialog = false },
            title = { Text("Select Customer") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Fuzzy search customers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (debouncedQuery.isNotBlank() && displayedCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.addCustomerToDb(debouncedQuery)
                                        if (res.isSuccess) {
                                            val created = res.getOrNull()
                                            val target = activeCustomPriceTarget
                                            if (target != null && created != null) {
                                                viewModel.updateCustomPriceCustomer(target.first, target.second, created)
                                            }
                                            showCustomerSearchDialog = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = greenColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add \"$debouncedQuery\" to the Database?", color = greenColor)
                            }
                        }
                    } else if (displayedCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No customers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        if (debouncedQuery.isBlank()) {
                            Text("Recent Customers:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayedCustomers.forEach { cust ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val target = activeCustomPriceTarget
                                            if (target != null) {
                                                viewModel.updateCustomPriceCustomer(target.first, target.second, cust)
                                            }
                                            showCustomerSearchDialog = false
                                        },
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = cust.name,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerSearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- New Customer Popup Form ---
    if (showNewCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            title = { Text("New Customer") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = {
                            newCustomerName = it
                            customerError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Customer Name") },
                        singleLine = true,
                        isError = customerError != null
                    )
                    if (customerError != null) {
                        Text(
                            text = customerError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.addCustomerToDb(newCustomerName)
                            if (result.isSuccess) {
                                val created = result.getOrNull()
                                val target = activeCustomPriceTarget
                                if (target != null && created != null) {
                                    viewModel.updateCustomPriceCustomer(target.first, target.second, created)
                                }
                                showNewCustomerDialog = false
                            } else {
                                customerError = result.exceptionOrNull()?.message ?: "Failed to add customer"
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
