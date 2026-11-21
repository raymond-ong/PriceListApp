package com.example.pricelistapp.ui.main

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricelistapp.data.local.*
import com.example.pricelistapp.ui.login.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class EditCustomPriceState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customer: CustomerEntity? = null,
    val price: String = ""
)

data class EditVariantState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val variantId: String = "",
    val variantName: String = "",
    val supplierPrice: String = "",
    val unit: String = "",
    val sellingPrice: String = "",
    val customPrices: List<EditCustomPriceState> = emptyList()
)

@Composable
fun ProductTabContent(
    user: User,
    currentRole: UserRole,
    productWithVariants: ProductWithVariants?,
    isFromSearch: Boolean,
    onBackToSearch: () -> Unit,
    onProductDeleted: () -> Unit
) {
    if (productWithVariants == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No product selected.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBackToSearch) {
                    Text("Go to Search Tab")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val productDao = remember { db.productDao() }
    val supplierDao = remember { db.supplierDao() }
    val customerDao = remember { db.customerDao() }

    var currentProduct by remember { mutableStateOf(productWithVariants.product) }
    var currentVariants by remember { mutableStateOf(productWithVariants.variants) }

    var supplierName by remember { mutableStateOf("Loading supplier...") }
    LaunchedEffect(currentProduct.supplierId) {
        withContext(Dispatchers.IO) {
            val sup = productDao.getSupplierById(currentProduct.supplierId)
            supplierName = sup?.name ?: "Unknown Supplier"
        }
    }

    var lastModifiedUserName by remember { mutableStateOf("System") }
    LaunchedEffect(currentProduct.lastModifiedUserId) {
        withContext(Dispatchers.IO) {
            val uid = currentProduct.lastModifiedUserId
            if (!uid.isNullOrBlank()) {
                val u = productDao.getUserById(uid)
                lastModifiedUserName = u?.userName ?: "Unknown"
            } else {
                lastModifiedUserName = "System"
            }
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(currentProduct.name) }
    var editedDescription by remember { mutableStateOf(currentProduct.description ?: "") }
    var selectedSupplierForEdit by remember { mutableStateOf<SupplierEntity?>(null) }
    var editedVariants by remember { mutableStateOf<List<EditVariantState>>(emptyList()) }

    // Load initial supplier and variants into edit state when editing starts
    LaunchedEffect(isEditing) {
        if (isEditing) {
            editedName = currentProduct.name
            editedDescription = currentProduct.description ?: ""
            withContext(Dispatchers.IO) {
                val sup = productDao.getSupplierById(currentProduct.supplierId)
                val vList = currentVariants.map { v ->
                    val custHistList = productDao.getCustomerPriceHistoryForVariant(v.id)
                    val latestCustPrices = mutableMapOf<String, Double>()
                    for (h in custHistList) {
                        if (h.customerId != null && !latestCustPrices.containsKey(h.customerId)) {
                            if (h.isDeleted == 0) {
                                latestCustPrices[h.customerId] = h.price
                            }
                        }
                    }
                    val cpStates = latestCustPrices.map { (custId, prc) ->
                        val cust = productDao.getCustomerById(custId)
                        EditCustomPriceState(customer = cust, price = prc.toString())
                    }
                    EditVariantState(
                        variantId = v.id,
                        variantName = v.name,
                        supplierPrice = v.supplierPrice.toString(),
                        unit = v.unit,
                        sellingPrice = v.sellingPrice.toString(),
                        customPrices = cpStates
                    )
                }
                withContext(Dispatchers.Main) {
                    selectedSupplierForEdit = sup
                    editedVariants = vList
                }
            }
        }
    }

    // Dialog states for editing supplier and customer
    var showSupplierSearchDialog by remember { mutableStateOf(false) }
    var showNewSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var supplierError by remember { mutableStateOf<String?>(null) }

    var showCustomerSearchDialog by remember { mutableStateOf(false) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var customerError by remember { mutableStateOf<String?>(null) }
    var activeCustomPriceTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (variantStateId, customPriceId)

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showAuditTrailDialog by remember { mutableStateOf(false) }

    // History popup states
    var showSupplierHistVariantId by remember { mutableStateOf<String?>(null) }
    var showSellingHistVariantId by remember { mutableStateOf<String?>(null) }
    var showCustomerHistTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // (variantId, customerId, customerName)

    val greenColor = Color(0xFF2E7D32)

    Column(modifier = Modifier.fillMaxSize()) {
        if (isFromSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBackToSearch,
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Info Card
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { editedName = it },
                                    label = { Text("Product Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = editedDescription,
                                    onValueChange = { editedDescription = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Supplier Dropdown in Edit Mode
                                Text("Primary Supplier", style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.height(4.dp))
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
                                                text = selectedSupplierForEdit?.name ?: "Select Supplier...",
                                                color = if (selectedSupplierForEdit != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
                            } else {
                                Text(
                                    text = currentProduct.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!currentProduct.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentProduct.description!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!isEditing) {
                                // Supplier
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Supplier: ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = supplierName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Last Modified Date & User
                            val dateFormat = remember { SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.getDefault()) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Last Modified: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${dateFormat.format(Date(currentProduct.lastModifiedDate))} by $lastModifiedUserName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Variants Header & Content
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Variants (${if (isEditing) editedVariants.size else currentVariants.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isEditing) {
                    // Add Variant Button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = {
                                    editedVariants = editedVariants + EditVariantState()
                                },
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
                    }

                    items(editedVariants) { variantState ->
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
                                        text = "Variant",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(onClick = {
                                        editedVariants = editedVariants.filter { it.id != variantState.id }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Variant", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                OutlinedTextField(
                                    value = variantState.variantName,
                                    onValueChange = { name ->
                                        editedVariants = editedVariants.map {
                                            if (it.id == variantState.id) it.copy(variantName = name) else it
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Variant Name") },
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = variantState.supplierPrice,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                                editedVariants = editedVariants.map {
                                                    if (it.id == variantState.id) it.copy(supplierPrice = input) else it
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Supplier Price") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = variantState.unit,
                                        onValueChange = { unit ->
                                            editedVariants = editedVariants.map {
                                                if (it.id == variantState.id) it.copy(unit = unit) else it
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Unit") },
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = variantState.sellingPrice,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                            editedVariants = editedVariants.map {
                                                if (it.id == variantState.id) it.copy(sellingPrice = input) else it
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Selling Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )

                                // Custom Prices Section
                                if (variantState.customPrices.isNotEmpty()) {
                                    Text("Custom Prices", style = MaterialTheme.typography.labelMedium)
                                    variantState.customPrices.forEach { cp ->
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
                                                    IconButton(onClick = {
                                                        editedVariants = editedVariants.map { v ->
                                                            if (v.id == variantState.id) {
                                                                v.copy(customPrices = v.customPrices.filter { it.id != cp.id })
                                                            } else v
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Remove Custom Price", modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedCard(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                activeCustomPriceTarget = Pair(variantState.id, cp.id)
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
                                                            activeCustomPriceTarget = Pair(variantState.id, cp.id)
                                                            newCustomerName = ""
                                                            customerError = null
                                                            showNewCustomerDialog = true
                                                        },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Add Customer", modifier = Modifier.size(18.dp))
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = cp.price,
                                                    onValueChange = { input ->
                                                        if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                                            editedVariants = editedVariants.map { v ->
                                                                if (v.id == variantState.id) {
                                                                    val updatedCps = v.customPrices.map { c ->
                                                                        if (c.id == cp.id) c.copy(price = input) else c
                                                                    }
                                                                    v.copy(customPrices = updatedCps)
                                                                } else v
                                                            }
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

                                TextButton(
                                    onClick = {
                                        editedVariants = editedVariants.map { v ->
                                            if (v.id == variantState.id) {
                                                v.copy(customPrices = v.customPrices + EditCustomPriceState())
                                            } else v
                                        }
                                    },
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
                } else {
                    items(currentVariants) { variant ->
                        VariantDetailCard(
                            variant = variant,
                            onShowSupplierHistory = { showSupplierHistVariantId = variant.id },
                            onShowSellingHistory = { showSellingHistVariantId = variant.id },
                            onShowCustomerPriceHistory = { vId: String, cId: String, cName: String ->
                                showCustomerHistTarget = Triple(vId, cId, cName)
                            }
                        )
                    }
                }

                // View Change History Button (Read Only Mode Only)
                if (!isEditing) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showAuditTrailDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Change History", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Delete Product Button at bottom of scrollable content (Edit Mode Only)
                if (isEditing) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Product", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (isEditing) {
                        // Save changes
                        coroutineScope.launch(Dispatchers.IO) {
                            val trimmedName = editedName.trim()
                            if (trimmedName.isEmpty()) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Product name cannot be empty", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            if (selectedSupplierForEdit == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Please select a supplier", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            val oldSup = productDao.getSupplierById(currentProduct.supplierId)
                            val oldSupName = oldSup?.name ?: "Unknown"

                            // Generate audit trail changes before updating
                            val auditChanges = generateAuditChanges(
                                oldProduct = currentProduct,
                                oldVariants = currentVariants,
                                oldSupplierName = oldSupName,
                                newProductName = trimmedName,
                                newDescription = editedDescription.trim().ifEmpty { null },
                                newSupplierId = selectedSupplierForEdit!!.id,
                                newSupplierName = selectedSupplierForEdit!!.name,
                                newVariants = editedVariants,
                                productDao = productDao
                            )

                            val now = System.currentTimeMillis()
                            val updatedProduct = currentProduct.copy(
                                name = trimmedName,
                                description = editedDescription.trim().ifEmpty { null },
                                supplierId = selectedSupplierForEdit!!.id,
                                lastModifiedDate = now,
                                lastModifiedUserId = user.id
                            )
                            productDao.updateProduct(updatedProduct)

                            if (auditChanges.isNotEmpty()) {
                                val auditTrail = ProductAuditTrailEntity(
                                    date = now,
                                    userId = user.id,
                                    productId = updatedProduct.id,
                                    changeDetailsJson = AuditJsonConverter.toJson(auditChanges)
                                )
                                productDao.insertAuditTrail(auditTrail)
                            }

                            productDao.deleteVariantsForProduct(updatedProduct.id)
                            val savedVariants = mutableListOf<ProductVariantEntity>()

                            for (v in editedVariants) {
                                val variantId = v.variantId.ifBlank { UlidGenerator.generate() }
                                val variantEntity = ProductVariantEntity(
                                    id = variantId,
                                    productId = updatedProduct.id,
                                    name = v.variantName.ifBlank { trimmedName },
                                    supplierPrice = v.supplierPrice.toDoubleOrNull() ?: 0.0,
                                    unit = v.unit.ifBlank { "pcs" },
                                    sellingPrice = v.sellingPrice.toDoubleOrNull() ?: 0.0
                                )
                                productDao.insertVariant(variantEntity)
                                savedVariants.add(variantEntity)

                                // Insert new supplier price history entry
                                val supHist = SupplierPriceHistEntity(
                                    userId = user.id,
                                    date = now,
                                    variantId = variantEntity.id,
                                    price = variantEntity.supplierPrice
                                )
                                productDao.insertSupplierPriceHist(supHist)

                                // Insert new default selling price history entry
                                val defaultCustHist = CustomerPriceHistEntity(
                                    userId = user.id,
                                    date = now,
                                    variantId = variantEntity.id,
                                    customerId = null,
                                    isDeleted = 0,
                                    isDefaultPrice = 1,
                                    price = variantEntity.sellingPrice
                                )
                                productDao.insertCustomerPriceHist(defaultCustHist)

                                // Insert new custom price history entry for each custom price
                                for (cp in v.customPrices) {
                                    if (cp.customer != null && cp.price.isNotBlank()) {
                                        val custHist = CustomerPriceHistEntity(
                                            userId = user.id,
                                            date = now,
                                            variantId = variantEntity.id,
                                            customerId = cp.customer.id,
                                            isDeleted = 0,
                                            isDefaultPrice = 0,
                                            price = cp.price.toDoubleOrNull() ?: 0.0
                                        )
                                        productDao.insertCustomerPriceHist(custHist)
                                    }
                                }
                            }

                            withContext(Dispatchers.Main) {
                                currentProduct = updatedProduct
                                currentVariants = savedVariants
                                supplierName = selectedSupplierForEdit?.name ?: "Unknown Supplier"
                                isEditing = false
                                Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFFFFC107), // Yellow / Amber
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save Product" else "Edit Product"
                )
            }
        }
    }

    // --- Supplier Search Dialog ---
    if (showSupplierSearchDialog) {
        val suppliersFlow = remember { supplierDao.getAllSuppliers() }
        val suppliers by suppliersFlow.collectAsState(initial = emptyList())
        var searchQuery by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        var recentSuppliers by remember { mutableStateOf<List<SupplierEntity>>(emptyList()) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            recentSuppliers = supplierDao.getRecentSuppliers()
            focusRequester.requestFocus()
        }

        LaunchedEffect(searchQuery) {
            delay(300)
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
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val trimmed = debouncedQuery.trim()
                                        val existing = supplierDao.findSupplierByName(trimmed)
                                        val sup = existing ?: SupplierEntity(name = trimmed)
                                        if (existing == null) {
                                            supplierDao.insertSupplier(sup)
                                        }
                                        withContext(Dispatchers.Main) {
                                            selectedSupplierForEdit = sup
                                            showSupplierSearchDialog = false
                                            Toast.makeText(context, "Supplier selected", Toast.LENGTH_SHORT).show()
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
                                Text("Add \"$debouncedQuery\" to Database?", color = greenColor)
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
                                            selectedSupplierForEdit = sup
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

    // --- New Supplier Dialog ---
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
                        coroutineScope.launch(Dispatchers.IO) {
                            val trimmed = newSupplierName.trim()
                            if (trimmed.isEmpty()) {
                                withContext(Dispatchers.Main) { supplierError = "Supplier name cannot be empty" }
                                return@launch
                            }
                            val existing = supplierDao.findSupplierByName(trimmed)
                            if (existing != null) {
                                withContext(Dispatchers.Main) { supplierError = "Supplier already exists" }
                                return@launch
                            }
                            val sup = SupplierEntity(name = trimmed)
                            supplierDao.insertSupplier(sup)
                            withContext(Dispatchers.Main) {
                                selectedSupplierForEdit = sup
                                showNewSupplierDialog = false
                                Toast.makeText(context, "Supplier added successfully", Toast.LENGTH_SHORT).show()
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

    // --- Customer Search Dialog ---
    if (showCustomerSearchDialog) {
        val customersFlow = remember { customerDao.getAllCustomers() }
        val customers by customersFlow.collectAsState(initial = emptyList())
        var searchQuery by remember { mutableStateOf("") }
        var debouncedQuery by remember { mutableStateOf("") }
        var recentCustomers by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            recentCustomers = customerDao.getRecentCustomers()
            focusRequester.requestFocus()
        }

        LaunchedEffect(searchQuery) {
            delay(300)
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
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val trimmed = debouncedQuery.trim()
                                        val existing = customerDao.findCustomerByName(trimmed)
                                        val cust = existing ?: CustomerEntity(name = trimmed)
                                        if (existing == null) {
                                            customerDao.insertCustomer(cust)
                                        }
                                        withContext(Dispatchers.Main) {
                                            activeCustomPriceTarget?.let { (vId, cpId) ->
                                                editedVariants = editedVariants.map { v ->
                                                    if (v.id == vId) {
                                                        val updatedCps = v.customPrices.map { cp ->
                                                            if (cp.id == cpId) cp.copy(customer = cust) else cp
                                                        }
                                                        v.copy(customPrices = updatedCps)
                                                    } else v
                                                }
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
                                Text("Add \"$debouncedQuery\" to Database?", color = greenColor)
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
                                            activeCustomPriceTarget?.let { (vId, cpId) ->
                                                editedVariants = editedVariants.map { v ->
                                                    if (v.id == vId) {
                                                        val updatedCps = v.customPrices.map { cp ->
                                                            if (cp.id == cpId) cp.copy(customer = cust) else cp
                                                        }
                                                        v.copy(customPrices = updatedCps)
                                                    } else v
                                                }
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

    // --- New Customer Dialog ---
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
                        coroutineScope.launch(Dispatchers.IO) {
                            val trimmed = newCustomerName.trim()
                            if (trimmed.isEmpty()) {
                                withContext(Dispatchers.Main) { customerError = "Customer name cannot be empty" }
                                return@launch
                            }
                            val existing = customerDao.findCustomerByName(trimmed)
                            if (existing != null) {
                                withContext(Dispatchers.Main) { customerError = "Customer already exists" }
                                return@launch
                            }
                            val cust = CustomerEntity(name = trimmed)
                            customerDao.insertCustomer(cust)
                            withContext(Dispatchers.Main) {
                                activeCustomPriceTarget?.let { (vId, cpId) ->
                                    editedVariants = editedVariants.map { v ->
                                        if (v.id == vId) {
                                            val updatedCps = v.customPrices.map { cp ->
                                                if (cp.id == cpId) cp.copy(customer = cust) else cp
                                            }
                                            v.copy(customPrices = updatedCps)
                                        } else v
                                    }
                                }
                                showNewCustomerDialog = false
                                Toast.makeText(context, "Customer added successfully", Toast.LENGTH_SHORT).show()
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

    // --- Delete Product Confirmation Dialog ---
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete this product? This action will perform a soft delete.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            val softDeleted = currentProduct.copy(
                                isDeleted = 1,
                                lastModifiedDate = now,
                                lastModifiedUserId = user.id
                            )
                            productDao.updateProduct(softDeleted)
                            withContext(Dispatchers.Main) {
                                showDeleteConfirmationDialog = false
                                Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                                onProductDeleted()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audit Trail Dialog Popup
    if (showAuditTrailDialog) {
        AuditTrailDialog(
            productId = currentProduct.id,
            onDismiss = { showAuditTrailDialog = false }
        )
    }

    // Supplier Price History Dialog Popup
    if (showSupplierHistVariantId != null) {
        SupplierPriceHistoryDialog(
            variantId = showSupplierHistVariantId!!,
            variantName = currentVariants.find { it.id == showSupplierHistVariantId }?.name ?: "",
            onDismiss = { showSupplierHistVariantId = null }
        )
    }

    // Selling Price History Dialog Popup
    if (showSellingHistVariantId != null) {
        SellingPriceHistoryDialog(
            variantId = showSellingHistVariantId!!,
            variantName = currentVariants.find { it.id == showSellingHistVariantId }?.name ?: "",
            onDismiss = { showSellingHistVariantId = null }
        )
    }

    // Customer Price History Dialog Popup
    if (showCustomerHistTarget != null) {
        CustomerPriceHistoryDialog(
            variantId = showCustomerHistTarget!!.first,
            customerId = showCustomerHistTarget!!.second,
            variantName = currentVariants.find { it.id == showCustomerHistTarget!!.first }?.name ?: "",
            customerName = showCustomerHistTarget!!.third,
            onDismiss = { showCustomerHistTarget = null }
        )
    }
}

suspend fun generateAuditChanges(
    oldProduct: ProductEntity,
    oldVariants: List<ProductVariantEntity>,
    oldSupplierName: String,
    newProductName: String,
    newDescription: String?,
    newSupplierId: String,
    newSupplierName: String,
    newVariants: List<EditVariantState>,
    productDao: ProductDao
): List<AuditChangeRecord> {
    val changes = mutableListOf<AuditChangeRecord>()

    if (oldProduct.name != newProductName) {
        changes.add(AuditChangeRecord(fieldName = "Product Name", prevValue = oldProduct.name, newValue = newProductName))
    }

    val oldDesc = oldProduct.description ?: ""
    val newDesc = newDescription ?: ""
    if (oldDesc != newDesc) {
        changes.add(AuditChangeRecord(fieldName = "Description", prevValue = oldDesc.ifEmpty { "(none)" }, newValue = newDesc.ifEmpty { "(none)" }))
    }

    if (oldProduct.supplierId != newSupplierId) {
        changes.add(AuditChangeRecord(fieldName = "Supplier", prevValue = oldSupplierName, newValue = newSupplierName))
    }

    val oldVariantsMap = oldVariants.associateBy { it.id }
    val handledOldVariantIds = mutableSetOf<String>()

    for (newV in newVariants) {
        if (newV.variantId.isBlank() || !oldVariantsMap.containsKey(newV.variantId)) {
            val vName = newV.variantName.ifBlank { newProductName }
            changes.add(AuditChangeRecord(fieldName = "Variant Added", prevValue = null, newValue = vName))
            changes.add(AuditChangeRecord(fieldName = "Supplier Price ($vName)", prevValue = null, newValue = newV.supplierPrice.ifBlank { "0.0" }))
            changes.add(AuditChangeRecord(fieldName = "Unit ($vName)", prevValue = null, newValue = newV.unit.ifBlank { "pcs" }))
            changes.add(AuditChangeRecord(fieldName = "Selling Price ($vName)", prevValue = null, newValue = newV.sellingPrice.ifBlank { "0.0" }))
            for (cp in newV.customPrices) {
                if (cp.customer != null && cp.price.isNotBlank()) {
                    changes.add(AuditChangeRecord(fieldName = "Custom Price ($vName - ${cp.customer.name})", prevValue = null, newValue = cp.price))
                }
            }
        } else {
            val oldV = oldVariantsMap[newV.variantId]!!
            handledOldVariantIds.add(oldV.id)

            val vName = newV.variantName.ifBlank { newProductName }
            if (oldV.name != vName) {
                changes.add(AuditChangeRecord(variantId = oldV.id, fieldName = "Variant Name", prevValue = oldV.name, newValue = vName))
            }
            val newSupPrice = newV.supplierPrice.toDoubleOrNull() ?: 0.0
            if (oldV.supplierPrice != newSupPrice) {
                changes.add(AuditChangeRecord(variantId = oldV.id, fieldName = "Supplier Price ($vName)", prevValue = oldV.supplierPrice.toString(), newValue = newSupPrice.toString()))
            }
            if (oldV.unit != newV.unit) {
                changes.add(AuditChangeRecord(variantId = oldV.id, fieldName = "Unit ($vName)", prevValue = oldV.unit, newValue = newV.unit))
            }
            val newSellPrice = newV.sellingPrice.toDoubleOrNull() ?: 0.0
            if (oldV.sellingPrice != newSellPrice) {
                changes.add(AuditChangeRecord(variantId = oldV.id, fieldName = "Selling Price ($vName)", prevValue = oldV.sellingPrice.toString(), newValue = newSellPrice.toString()))
            }

            // Custom prices comparison
            val oldCustHist = productDao.getCustomerPriceHistoryForVariant(oldV.id)
            val oldCustPrices = mutableMapOf<String, Double>()
            for (h in oldCustHist) {
                if (h.customerId != null && !oldCustPrices.containsKey(h.customerId)) {
                    if (h.isDeleted == 0) {
                        oldCustPrices[h.customerId] = h.price
                    }
                }
            }

            val newCustPrices = mutableMapOf<String, Double>()
            val newCustEntities = mutableMapOf<String, String>()
            for (cp in newV.customPrices) {
                if (cp.customer != null && cp.price.isNotBlank()) {
                    val pVal = cp.price.toDoubleOrNull() ?: 0.0
                    newCustPrices[cp.customer.id] = pVal
                    newCustEntities[cp.customer.id] = cp.customer.name
                }
            }

            for ((custId, newPrice) in newCustPrices) {
                val custName = newCustEntities[custId] ?: "Customer"
                val oldPrice = oldCustPrices[custId]
                if (oldPrice == null) {
                    changes.add(AuditChangeRecord(variantId = oldV.id, customerId = custId, fieldName = "Custom Price ($vName - $custName)", prevValue = null, newValue = newPrice.toString()))
                } else if (oldPrice != newPrice) {
                    changes.add(AuditChangeRecord(variantId = oldV.id, customerId = custId, fieldName = "Custom Price ($vName - $custName)", prevValue = oldPrice.toString(), newValue = newPrice.toString()))
                }
            }

            for ((custId, oldPrice) in oldCustPrices) {
                if (!newCustPrices.containsKey(custId)) {
                    val cust = productDao.getCustomerById(custId)
                    val custName = cust?.name ?: "Customer"
                    changes.add(AuditChangeRecord(variantId = oldV.id, customerId = custId, fieldName = "Custom Price ($vName - $custName)", prevValue = oldPrice.toString(), newValue = "Removed"))
                }
            }
        }
    }

    for (oldV in oldVariants) {
        if (!handledOldVariantIds.contains(oldV.id)) {
            changes.add(AuditChangeRecord(variantId = oldV.id, fieldName = "Variant Removed", prevValue = oldV.name, newValue = null))
        }
    }

    return changes
}

@Composable
fun AuditTrailDialog(
    productId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productDao = remember { AppDatabase.getInstance(context).productDao() }
    var auditList by remember { mutableStateOf<List<Pair<ProductAuditTrailEntity, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(productId) {
        withContext(Dispatchers.IO) {
            val list = productDao.getAuditTrailForProduct(productId)
            val result = list.map { audit ->
                val userObj = productDao.getUserById(audit.userId)
                val userName = userObj?.userName ?: "System"
                audit to userName
            }
            withContext(Dispatchers.Main) {
                auditList = result
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Product Change History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (auditList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No change history found.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(auditList) { (audit, userName) ->
                            val changes = remember(audit.changeDetailsJson) {
                                AuditJsonConverter.fromJson(audit.changeDetailsJson)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(audit.date)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "By: $userName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    changes.forEach { change ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = change.fieldName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (change.prevValue != null) {
                                                    Text(
                                                        text = change.prevValue,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textDecoration = TextDecoration.LineThrough,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "to",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    if (change.newValue != null) {
                                                        Text(
                                                            text = change.newValue,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                } else {
                                                    if (change.newValue != null) {
                                                        Text(
                                                            text = change.newValue,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun VariantDetailCard(
    variant: ProductVariantEntity,
    onShowSupplierHistory: () -> Unit,
    onShowSellingHistory: () -> Unit,
    onShowCustomerPriceHistory: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val productDao = remember { AppDatabase.getInstance(context).productDao() }
    var customPricesList by remember { mutableStateOf<List<Triple<String, String, Double>>>(emptyList()) }

    LaunchedEffect(variant.id) {
        withContext(Dispatchers.IO) {
            val histList = productDao.getCustomerPriceHistoryForVariant(variant.id)
            val latestCustPrices = mutableMapOf<String, Double>()
            for (h in histList) {
                if (h.customerId != null && !latestCustPrices.containsKey(h.customerId)) {
                    if (h.isDeleted == 0) {
                        latestCustPrices[h.customerId] = h.price
                    }
                }
            }
            val result = latestCustPrices.mapNotNull { (custId, price) ->
                val cust = productDao.getCustomerById(custId)
                if (cust != null) Triple(custId, cust.name, price) else null
            }
            withContext(Dispatchers.Main) {
                customPricesList = result
            }
        }
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Variant Name & Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = variant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Unit: ${variant.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Supplier Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Supplier Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₱${numberFormat.format(variant.supplierPrice)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                IconButton(onClick = onShowSupplierHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Supplier Price History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Selling Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selling Price (Default)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₱${numberFormat.format(variant.sellingPrice)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                IconButton(onClick = onShowSellingHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Selling Price History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 3. Custom Prices Section in Read Only mode
            if (customPricesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Custom Prices",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                customPricesList.forEach { (custId, custName, price) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = custName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "₱${numberFormat.format(price)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        IconButton(onClick = { onShowCustomerPriceHistory(variant.id, custId, custName) }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Customer Price History",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun SupplierPriceHistoryDialog(
    variantId: String,
    variantName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productDao = remember { AppDatabase.getInstance(context).productDao() }
    var historyList by remember { mutableStateOf<List<Pair<SupplierPriceHistEntity, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(variantId) {
        withContext(Dispatchers.IO) {
            val list = productDao.getSupplierPriceHistoryForVariant(variantId)
            val result = list.map { hist ->
                val userObj = productDao.getUserById(hist.userId)
                val userName = userObj?.userName ?: "System"
                hist to userName
            }
            withContext(Dispatchers.Main) {
                historyList = result
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Supplier Price History ($variantName)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No price history found.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.getDefault()) }
                    val numberFormat = remember {
                        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                            minimumFractionDigits = 2
                            maximumFractionDigits = 2
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { (hist, userName) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dateFormat.format(Date(hist.date)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "By: $userName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "₱${numberFormat.format(hist.price)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun SellingPriceHistoryDialog(
    variantId: String,
    variantName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productDao = remember { AppDatabase.getInstance(context).productDao() }
    var historyList by remember { mutableStateOf<List<Triple<CustomerPriceHistEntity, String, String?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(variantId) {
        withContext(Dispatchers.IO) {
            val list = productDao.getCustomerPriceHistoryForVariant(variantId)
                .filter { it.isDefaultPrice == 1 || it.customerId == null }
            val result = list.map { hist ->
                val userObj = productDao.getUserById(hist.userId)
                val userName = userObj?.userName ?: "System"
                Triple(hist, userName, null)
            }
            withContext(Dispatchers.Main) {
                historyList = result
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Selling Price History ($variantName)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No selling price history found.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.getDefault()) }
                    val numberFormat = remember {
                        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                            minimumFractionDigits = 2
                            maximumFractionDigits = 2
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { (hist, userName, _) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = dateFormat.format(Date(hist.date)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "By: $userName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Default Selling Price",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    Text(
                                        text = "₱${numberFormat.format(hist.price)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun CustomerPriceHistoryDialog(
    variantId: String,
    customerId: String,
    variantName: String,
    customerName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productDao = remember { AppDatabase.getInstance(context).productDao() }
    var historyList by remember { mutableStateOf<List<Pair<CustomerPriceHistEntity, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(variantId, customerId) {
        withContext(Dispatchers.IO) {
            val list = productDao.getCustomerPriceHistoryForVariant(variantId)
                .filter { it.customerId == customerId && it.isDeleted == 0 }
            val result = list.map { hist ->
                val userObj = productDao.getUserById(hist.userId)
                val userName = userObj?.userName ?: "System"
                hist to userName
            }
            withContext(Dispatchers.Main) {
                historyList = result
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Customer Price History ($customerName - $variantName)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No customer price history found.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy-MMM-dd HH:mm", Locale.getDefault()) }
                    val numberFormat = remember {
                        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                            minimumFractionDigits = 2
                            maximumFractionDigits = 2
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { (hist, userName) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dateFormat.format(Date(hist.date)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "By: $userName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "₱${numberFormat.format(hist.price)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
