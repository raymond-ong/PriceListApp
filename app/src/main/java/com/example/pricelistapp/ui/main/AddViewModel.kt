package com.example.pricelistapp.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricelistapp.data.local.*
import com.example.pricelistapp.ui.util.ToastHelper
import com.example.pricelistapp.ui.util.ToastType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class CustomPriceFormState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customer: CustomerEntity? = null,
    val price: String = ""
)

data class ProductVariantFormState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val variantName: String = "",
    val supplierPrice: String = "",
    val unit: String = "",
    val sellingPrice: String = "",
    val customPrices: List<CustomPriceFormState> = emptyList()
)

class AddViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val categoryDao = database.categoryDao()
    private val supplierDao = database.supplierDao()
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = customerDao.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form state
    var selectedCategory by mutableStateOf<CategoryEntity?>(null)
        private set

    var productName by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var selectedSupplier by mutableStateOf<SupplierEntity?>(null)
        private set

    var variants by mutableStateOf<List<ProductVariantFormState>>(emptyList())
        private set

    fun clearForm() {
        selectedCategory = null
        productName = ""
        description = ""
        selectedSupplier = null
        variants = emptyList()
    }

    fun updateSelectedCategory(category: CategoryEntity?) {
        selectedCategory = category
    }

    fun updateProductName(name: String) {
        productName = name.replace("\n", "").replace("\r", "")
    }

    fun updateDescription(desc: String) {
        description = desc
    }

    fun updateSelectedSupplier(supplier: SupplierEntity?) {
        selectedSupplier = supplier
    }

    fun addVariant() {
        variants = variants + ProductVariantFormState()
    }

    fun updateVariantName(variantId: String, name: String) {
        val sanitized = name.replace("\n", "").replace("\r", "")
        variants = variants.map { if (it.id == variantId) it.copy(variantName = sanitized) else it }
    }

    fun updateVariantSupplierPrice(variantId: String, price: String) {
        variants = variants.map { if (it.id == variantId) it.copy(supplierPrice = price) else it }
    }

    fun updateVariantUnit(variantId: String, unit: String) {
        variants = variants.map { if (it.id == variantId) it.copy(unit = unit) else it }
    }

    fun updateVariantSellingPrice(variantId: String, price: String) {
        variants = variants.map { if (it.id == variantId) it.copy(sellingPrice = price) else it }
    }

    fun removeVariant(variantId: String) {
        variants = variants.filter { it.id != variantId }
    }

    fun addCustomPrice(variantId: String) {
        variants = variants.map { variant ->
            if (variant.id == variantId) {
                variant.copy(customPrices = variant.customPrices + CustomPriceFormState())
            } else {
                variant
            }
        }
    }

    fun updateCustomPriceCustomer(variantId: String, customPriceId: String, customer: CustomerEntity?) {
        variants = variants.map { variant ->
            if (variant.id == variantId) {
                val updatedCustomPrices = variant.customPrices.map { cp ->
                    if (cp.id == customPriceId) cp.copy(customer = customer) else cp
                }
                variant.copy(customPrices = updatedCustomPrices)
            } else {
                variant
            }
        }
    }

    fun updateCustomPriceValue(variantId: String, customPriceId: String, price: String) {
        variants = variants.map { variant ->
            if (variant.id == variantId) {
                val updatedCustomPrices = variant.customPrices.map { cp ->
                    if (cp.id == customPriceId) cp.copy(price = price) else cp
                }
                variant.copy(customPrices = updatedCustomPrices)
            } else {
                variant
            }
        }
    }

    fun removeCustomPrice(variantId: String, customPriceId: String) {
        variants = variants.map { variant ->
            if (variant.id == variantId) {
                variant.copy(customPrices = variant.customPrices.filter { it.id != customPriceId })
            } else {
                variant
            }
        }
    }

    suspend fun addCategoryToDb(name: String): Result<CategoryEntity> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            ToastHelper.show(getApplication(), "Category name cannot be empty", ToastType.WARNING)
            return Result.failure(IllegalArgumentException("Category name cannot be empty."))
        }
        val existing = categoryDao.findCategoryByName(trimmed)
        if (existing != null) {
            ToastHelper.show(getApplication(), "Category '$trimmed' already exists", ToastType.ERROR)
            return Result.failure(IllegalArgumentException("Category '$trimmed' already exists."))
        }
        val entity = CategoryEntity(name = trimmed)
        categoryDao.insertCategory(entity)
        selectedCategory = entity
        ToastHelper.show(getApplication(), "Category saved successfully", ToastType.SUCCESS)
        return Result.success(entity)
    }

    suspend fun addSupplierToDb(name: String): Result<SupplierEntity> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            ToastHelper.show(getApplication(), "Supplier name cannot be empty", ToastType.WARNING)
            return Result.failure(IllegalArgumentException("Supplier name cannot be empty."))
        }
        val existing = supplierDao.findSupplierByName(trimmed)
        if (existing != null) {
            ToastHelper.show(getApplication(), "Supplier '$trimmed' already exists", ToastType.ERROR)
            return Result.failure(IllegalArgumentException("Supplier '$trimmed' already exists."))
        }
        val entity = SupplierEntity(name = trimmed)
        supplierDao.insertSupplier(entity)
        selectedSupplier = entity
        ToastHelper.show(getApplication(), "Supplier saved successfully", ToastType.SUCCESS)
        return Result.success(entity)
    }

    suspend fun addCustomerToDb(name: String): Result<CustomerEntity> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            ToastHelper.show(getApplication(), "Customer name cannot be empty", ToastType.WARNING)
            return Result.failure(IllegalArgumentException("Customer name cannot be empty."))
        }
        val existing = customerDao.findCustomerByName(trimmed)
        if (existing != null) {
            ToastHelper.show(getApplication(), "Customer '$trimmed' already exists", ToastType.ERROR)
            return Result.failure(IllegalArgumentException("Customer '$trimmed' already exists."))
        }
        val entity = CustomerEntity(name = trimmed)
        customerDao.insertCustomer(entity)
        ToastHelper.show(getApplication(), "Customer saved successfully", ToastType.SUCCESS)
        return Result.success(entity)
    }

    suspend fun getRecentCategories(): List<CategoryEntity> {
        return categoryDao.getRecentCategories()
    }

    suspend fun getRecentSuppliers(): List<SupplierEntity> {
        return supplierDao.getRecentSuppliers()
    }

    suspend fun getRecentCustomers(): List<CustomerEntity> {
        return customerDao.getRecentCustomers()
    }

    suspend fun saveProduct(userId: String): Result<Unit> {
        val trimmedName = productName.trim()
        if (trimmedName.isEmpty()) {
            ToastHelper.show(getApplication(), "Product name cannot be empty", ToastType.WARNING)
            return Result.failure(IllegalArgumentException("Product name cannot be empty"))
        }
        if (selectedCategory == null) {
            ToastHelper.show(getApplication(), "Please select a category", ToastType.WARNING)
            return Result.failure(IllegalArgumentException("Please select a category"))
        }

        try {
            val productEntity = ProductEntity(
                name = trimmedName,
                description = description.ifBlank { null },
                catId = selectedCategory!!.id,
                supplierId = selectedSupplier?.id ?: "",
                lastModifiedDate = System.currentTimeMillis()
            )
            productDao.insertProduct(productEntity)

            val now = System.currentTimeMillis()
            for (v in variants) {
                val variantEntity = ProductVariantEntity(
                    productId = productEntity.id,
                    name = v.variantName.ifBlank { trimmedName },
                    supplierPrice = v.supplierPrice.toDoubleOrNull() ?: 0.0,
                    unit = v.unit.ifBlank { "pcs" },
                    sellingPrice = v.sellingPrice.toDoubleOrNull() ?: 0.0
                )
                productDao.insertVariant(variantEntity)

                val supHist = SupplierPriceHistEntity(
                    userId = userId,
                    date = now,
                    variantId = variantEntity.id
                )
                productDao.insertSupplierPriceHist(supHist)

                for (cp in v.customPrices) {
                    if (cp.customer != null && cp.price.isNotBlank()) {
                        val custHist = CustomerPriceHistEntity(
                            userId = userId,
                            date = now,
                            variantId = variantEntity.id,
                            customerId = cp.customer.id,
                            isDeleted = 0,
                            isDefaultPrice = 0
                        )
                        productDao.insertCustomerPriceHist(custHist)
                    }
                }
            }

            ToastHelper.show(getApplication(), "Product saved successfully!", ToastType.SUCCESS)
            clearForm()
            return Result.success(Unit)
        } catch (e: Exception) {
            ToastHelper.show(getApplication(), "Failed to save product: ${e.localizedMessage}", ToastType.ERROR)
            return Result.failure(e)
        }
    }
}
