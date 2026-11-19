package com.example.pricelistapp.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pricelistapp.data.local.*
import com.example.pricelistapp.ui.login.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductTabContent(
    user: User,
    currentRole: UserRole,
    productWithVariants: ProductWithVariants?,
    onBackToSearch: () -> Unit
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
    val productDao = remember { AppDatabase.getInstance(context).productDao() }

    var currentProduct by remember { mutableStateOf(productWithVariants.product) }
    var currentVariants by remember { mutableStateOf(productWithVariants.variants) }

    var supplierName by remember { mutableStateOf("Loading supplier...") }
    LaunchedEffect(currentProduct.supplierId) {
        withContext(Dispatchers.IO) {
            val sup = productDao.getSupplierById(currentProduct.supplierId)
            supplierName = sup?.name ?: "Unknown Supplier"
        }
    }

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(currentProduct.name) }
    var editedDescription by remember { mutableStateOf(currentProduct.description ?: "") }

    // History popup states
    var showSupplierHistVariantId by remember { mutableStateOf<String?>(null) }
    var showSellingHistVariantId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToSearch) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Search")
                }
                Text(
                    text = "Product Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditing) {
                        // Save changes
                        coroutineScope.launch(Dispatchers.IO) {
                            val updated = currentProduct.copy(
                                name = editedName.trim(),
                                description = editedDescription.trim().ifEmpty { null },
                                lastModifiedDate = System.currentTimeMillis()
                            )
                            productDao.updateProduct(updated)
                            withContext(Dispatchers.Main) {
                                currentProduct = updated
                                isEditing = false
                                Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        isEditing = true
                    }
                },
                containerColor = Color(0xFFFFC107), // Yellow / Amber as requested
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save Product" else "Edit Product"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 4.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    }
                }
            }

            // Variants Header
            item {
                Text(
                    text = "Product Variants (${currentVariants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Variant Cards
            items(currentVariants) { variant ->
                VariantDetailCard(
                    variant = variant,
                    onShowSupplierHistory = { showSupplierHistVariantId = variant.id },
                    onShowSellingHistory = { showSellingHistVariantId = variant.id }
                )
            }
        }
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
}

@Composable
fun VariantDetailCard(
    variant: ProductVariantEntity,
    onShowSupplierHistory: () -> Unit,
    onShowSellingHistory: () -> Unit
) {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    // Left: Date + caption "By: <user>"
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

                                    // Right: Price
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
            val result = list.map { hist ->
                val userObj = productDao.getUserById(hist.userId)
                val userName = userObj?.userName ?: "System"
                val customerName = if (hist.customerId != null) {
                    productDao.getCustomerById(hist.customerId)?.name
                } else {
                    null
                }
                Triple(hist, userName, customerName)
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
                        items(historyList) { (hist, userName, customerName) ->
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
                                    // Left: Date + caption "By: <user>" + next line default or customer name
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
                                        val priceTypeLabel = if (hist.isDefaultPrice == 1 || customerName == null) {
                                            "Default Selling Price"
                                        } else {
                                            "Customer: $customerName"
                                        }
                                        Text(
                                            text = priceTypeLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    // Right: Price
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
