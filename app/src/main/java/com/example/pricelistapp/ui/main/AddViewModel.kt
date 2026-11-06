package com.example.pricelistapp.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricelistapp.data.local.AppDatabase
import com.example.pricelistapp.data.local.CategoryEntity
import com.example.pricelistapp.data.local.CustomerEntity
import com.example.pricelistapp.data.local.SupplierEntity
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

    fun updateSelectedCategory(category: CategoryEntity?) {
        selectedCategory = category
    }

    fun updateProductName(name: String) {
        // Single line behavior: remove any newline characters
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
            return Result.failure(IllegalArgumentException("Category name cannot be empty."))
        }
        val existing = categoryDao.findCategoryByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Category '$trimmed' already exists."))
        }
        val entity = CategoryEntity(name = trimmed)
        val id = categoryDao.insertCategory(entity)
        val created = entity.copy(id = id)
        selectedCategory = created
        return Result.success(created)
    }

    suspend fun addSupplierToDb(name: String): Result<SupplierEntity> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Supplier name cannot be empty."))
        }
        val existing = supplierDao.findSupplierByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Supplier '$trimmed' already exists."))
        }
        val entity = SupplierEntity(name = trimmed)
        val id = supplierDao.insertSupplier(entity)
        val created = entity.copy(id = id)
        selectedSupplier = created
        return Result.success(created)
    }

    suspend fun addCustomerToDb(name: String): Result<CustomerEntity> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Customer name cannot be empty."))
        }
        val existing = customerDao.findCustomerByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Customer '$trimmed' already exists."))
        }
        val entity = CustomerEntity(name = trimmed)
        val id = customerDao.insertCustomer(entity)
        return Result.success(entity.copy(id = id))
    }
}
