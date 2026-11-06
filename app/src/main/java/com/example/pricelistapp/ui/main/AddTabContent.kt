package com.example.pricelistapp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.ui.login.UserRole
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

    // Active custom price target for customer dialog if triggered from custom price
    var activeCustomPriceTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (variantId, customPriceId)

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

        // 3. Product Textbox (Single-line behavior, no newlines, expands vertically if needed)
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

        // 4. Description Textbox (Multline, starts single line, expands vertically)
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

        // 5. Supplier Dropdown with Fuzzy Search & Plus Button
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

        // 6. Add Product Variant Button & Variants Section
        Button(
            onClick = { viewModel.addVariant() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Product Variant")
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

                    // e. Add Custom Price button
                    OutlinedButton(
                        onClick = { viewModel.addCustomPrice(variant.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Price")
                    }
                }
            }
        }
    }

    // --- Category Search Dialog with Fuzzy Search ---
    if (showCategorySearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredCategories = remember(categories, searchQuery) {
            categories.filter { matchesFuzzy(searchQuery, it.name) }
        }

        AlertDialog(
            onDismissRequest = { showCategorySearchDialog = false },
            title = { Text("Select Category") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Fuzzy search categories...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (filteredCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No categories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredCategories.forEach { cat ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedCategory(cat)
                                            showCategorySearchDialog = false
                                        },
                                    color = if (viewModel.selectedCategory?.id == cat.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = cat.name,
                                        modifier = Modifier.padding(12.dp),
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

    // --- Supplier Search Dialog with Fuzzy Search ---
    if (showSupplierSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredSuppliers = remember(suppliers, searchQuery) {
            suppliers.filter { matchesFuzzy(searchQuery, it.name) }
        }

        AlertDialog(
            onDismissRequest = { showSupplierSearchDialog = false },
            title = { Text("Select Supplier") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Fuzzy search suppliers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (filteredSuppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No suppliers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredSuppliers.forEach { sup ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedSupplier(sup)
                                            showSupplierSearchDialog = false
                                        },
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = sup.name,
                                        modifier = Modifier.padding(12.dp),
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

    // --- Customer Search Dialog with Fuzzy Search ---
    if (showCustomerSearchDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredCustomers = remember(customers, searchQuery) {
            customers.filter { matchesFuzzy(searchQuery, it.name) }
        }

        AlertDialog(
            onDismissRequest = { showCustomerSearchDialog = false },
            title = { Text("Select Customer") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Fuzzy search customers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (filteredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No customers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredCustomers.forEach { cust ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val target = activeCustomPriceTarget
                                            if (target != null) {
                                                viewModel.updateCustomPriceCustomer(target.first, target.second, cust)
                                            }
                                            showCustomerSearchDialog = false
                                        },
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = cust.name,
                                        modifier = Modifier.padding(12.dp),
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
