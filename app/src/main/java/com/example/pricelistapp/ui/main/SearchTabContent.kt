package com.example.pricelistapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pricelistapp.data.local.ProductWithVariants
import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.ui.login.UserRole
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@Composable
fun SearchTabContent(
    user: User,
    currentRole: UserRole,
    viewModel: SearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val paginatedProducts by viewModel.paginatedProducts.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val totalRecords by viewModel.totalRecords.collectAsState()

    val listState = rememberLazyListState()

    // Automatically scroll to the top of the list whenever the page changes
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Refresh products when tab is displayed / resumed
    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Small label on top of the search textbox
        Text(
            text = "Search by product name, description, or variant",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        var isFocused by remember { mutableStateOf(false) }

        // 1. Textbox for Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = {
                if (!isFocused) {
                    Text("Search products...")
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Record count label placed on top of the top paginator
        val countLabel = if (searchQuery.isBlank()) {
            "Last $totalRecords modified products"
        } else {
            "$totalRecords records found"
        }
        Text(
            text = countLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Top Paginator (Symmetrical)
        PaginatorBar(
            currentPage = currentPage,
            totalPages = totalPages,
            onFirst = { viewModel.firstPage() },
            onPrev = { viewModel.prevPage() },
            onNext = { viewModel.nextPage() },
            onLast = { viewModel.lastPage() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Search Results List
        if (paginatedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No recent modified products found." else "No products found matching '$searchQuery'.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(paginatedProducts) { index, item ->
                    val globalIndex = (currentPage - 1) * viewModel.pageSize + index + 1
                    ProductCard(index = globalIndex, item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Paginator (Symmetrical & Centered)
        PaginatorBar(
            currentPage = currentPage,
            totalPages = totalPages,
            onFirst = { viewModel.firstPage() },
            onPrev = { viewModel.prevPage() },
            onNext = { viewModel.nextPage() },
            onLast = { viewModel.lastPage() }
        )
    }
}

@Composable
fun PaginatorBar(
    currentPage: Int,
    totalPages: Int,
    onFirst: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLast: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onFirst,
                enabled = currentPage > 1
            ) {
                Icon(Icons.Default.FirstPage, contentDescription = "First Page")
            }
            IconButton(
                onClick = onPrev,
                enabled = currentPage > 1
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
            }

            Text(
                text = "Page $currentPage of ${totalPages.coerceAtLeast(1)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onNext,
                enabled = currentPage < totalPages
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page")
            }
            IconButton(
                onClick = onLast,
                enabled = currentPage < totalPages
            ) {
                Icon(Icons.Default.LastPage, contentDescription = "Last Page")
            }
        }
    }
}

@Composable
fun ProductCard(index: Int, item: ProductWithVariants) {
    val product = item.product
    val firstVariant = item.variants.firstOrNull()
    val supplierPrice = firstVariant?.supplierPrice ?: 0.0
    val sellingPrice = firstVariant?.sellingPrice ?: 0.0
    val unit = firstVariant?.unit ?: "pcs"
    val variantName = firstVariant?.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp) // Leave space for index badge on upper right
            ) {
                // a. Product name in bold
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!variantName.isNullOrBlank() && variantName != product.name) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Variant: $variantName ($unit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // b. Product description
                if (!product.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val numberFormat = remember {
                        java.text.NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                            minimumFractionDigits = 2
                            maximumFractionDigits = 2
                        }
                    }
                    val formattedSupplierPrice = "₱${numberFormat.format(supplierPrice)}"
                    val formattedSellingPrice = "₱${numberFormat.format(sellingPrice)}"

                    // c. Supplier Price
                    Column {
                        Text(
                            text = "Supplier Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedSupplierPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // d. Default selling price
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Selling Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedSellingPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // e. Index value starting at 1 with rounded border floating at upper right
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
