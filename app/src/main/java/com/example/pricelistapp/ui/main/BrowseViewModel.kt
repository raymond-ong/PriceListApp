package com.example.pricelistapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricelistapp.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val categoryDao = db.categoryDao()
    private val supplierDao = db.supplierDao()
    private val customerDao = db.customerDao()
    private val productDao = db.productDao()

    // Sub-tabs: 0 = Category, 1 = Supplier, 2 = Customer
    private val _selectedSubTab = MutableStateFlow(0)
    val selectedSubTab: StateFlow<Int> = _selectedSubTab

    // Drilldown state
    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory

    private val _selectedSupplier = MutableStateFlow<SupplierEntity?>(null)
    val selectedSupplier: StateFlow<SupplierEntity?> = _selectedSupplier

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer

    // Search queries
    private val _categoryQuery = MutableStateFlow("")
    val categoryQuery: StateFlow<String> = _categoryQuery

    private val _supplierQuery = MutableStateFlow("")
    val supplierQuery: StateFlow<String> = _supplierQuery

    private val _customerQuery = MutableStateFlow("")
    val customerQuery: StateFlow<String> = _customerQuery

    // Raw lists
    private var allCategories: List<CategoryEntity> = emptyList()
    private var allSuppliers: List<SupplierEntity> = emptyList()
    private var allCustomers: List<CustomerEntity> = emptyList()

    // Filtered lists
    private val _filteredCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val filteredCategories: StateFlow<List<CategoryEntity>> = _filteredCategories

    private val _filteredSuppliers = MutableStateFlow<List<SupplierEntity>>(emptyList())
    val filteredSuppliers: StateFlow<List<SupplierEntity>> = _filteredSuppliers

    private val _filteredCustomers = MutableStateFlow<List<CustomerEntity>>(emptyList())
    val filteredCustomers: StateFlow<List<CustomerEntity>> = _filteredCustomers

    // Drilldown products
    private val _drilldownProducts = MutableStateFlow<List<ProductWithVariants>>(emptyList())
    val drilldownProducts: StateFlow<List<ProductWithVariants>> = _drilldownProducts

    val pageSize = 20
    private val _drilldownCurrentPage = MutableStateFlow(1)
    val drilldownCurrentPage: StateFlow<Int> = _drilldownCurrentPage

    private val _drilldownTotalPages = MutableStateFlow(1)
    val drilldownTotalPages: StateFlow<Int> = _drilldownTotalPages

    private val _drilldownTotalRecords = MutableStateFlow(0)
    val drilldownTotalRecords: StateFlow<Int> = _drilldownTotalRecords

    val paginatedDrilldownProducts: StateFlow<List<ProductWithVariants>> = combine(_drilldownProducts, _drilldownCurrentPage) { products, page ->
        val total = products.size
        val pages = if (total == 0) 1 else (total + pageSize - 1) / pageSize
        _drilldownTotalPages.value = pages
        _drilldownTotalRecords.value = total

        val safePage = page.coerceIn(1, pages)
        val startIndex = (safePage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, total)
        if (startIndex < total) products.subList(startIndex, endIndex) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAllData()

        viewModelScope.launch {
            _categoryQuery.debounce(300L).collect { filterCategories(it) }
        }
        viewModelScope.launch {
            _supplierQuery.debounce(300L).collect { filterSuppliers(it) }
        }
        viewModelScope.launch {
            _customerQuery.debounce(300L).collect { filterCustomers(it) }
        }
    }

    fun loadAllData() {
        viewModelScope.launch {
            allCategories = categoryDao.getAllCategories().first()
            allSuppliers = supplierDao.getAllSuppliers().first()
            allCustomers = customerDao.getAllCustomers().first()

            filterCategories(_categoryQuery.value)
            filterSuppliers(_supplierQuery.value)
            filterCustomers(_customerQuery.value)
        }
    }

    fun selectSubTab(index: Int) {
        _selectedSubTab.value = index
        clearDrilldown()
    }

    fun updateCategoryQuery(q: String) {
        _categoryQuery.value = q
    }

    fun updateSupplierQuery(q: String) {
        _supplierQuery.value = q
    }

    fun updateCustomerQuery(q: String) {
        _customerQuery.value = q
    }

    fun selectCategory(cat: CategoryEntity) {
        _selectedCategory.value = cat
        _drilldownCurrentPage.value = 1
        viewModelScope.launch {
            _drilldownProducts.value = productDao.getProductsByCategoryId(cat.id)
        }
    }

    fun selectSupplier(sup: SupplierEntity) {
        _selectedSupplier.value = sup
        _drilldownCurrentPage.value = 1
        viewModelScope.launch {
            _drilldownProducts.value = productDao.getProductsBySupplierId(sup.id)
        }
    }

    fun selectCustomer(cust: CustomerEntity) {
        _selectedCustomer.value = cust
        _drilldownCurrentPage.value = 1
        viewModelScope.launch {
            _drilldownProducts.value = productDao.getProductsByCustomerId(cust.id)
        }
    }

    fun clearDrilldown() {
        _selectedCategory.value = null
        _selectedSupplier.value = null
        _selectedCustomer.value = null
        _drilldownProducts.value = emptyList()
        _drilldownCurrentPage.value = 1
    }

    fun setDrilldownPage(page: Int) {
        val max = _drilldownTotalPages.value.coerceAtLeast(1)
        _drilldownCurrentPage.value = page.coerceIn(1, max)
    }
    fun nextDrilldownPage() {
        if (_drilldownCurrentPage.value < _drilldownTotalPages.value) _drilldownCurrentPage.value++
    }
    fun prevDrilldownPage() {
        if (_drilldownCurrentPage.value > 1) _drilldownCurrentPage.value--
    }
    fun firstDrilldownPage() {
        _drilldownCurrentPage.value = 1
    }
    fun lastDrilldownPage() {
        _drilldownCurrentPage.value = _drilldownTotalPages.value.coerceAtLeast(1)
    }

    fun updateSupplier(sup: SupplierEntity) {
        viewModelScope.launch {
            supplierDao.updateSupplier(sup)
            _selectedSupplier.value = sup
            loadAllData()
        }
    }

    fun updateCustomer(cust: CustomerEntity) {
        viewModelScope.launch {
            customerDao.updateCustomer(cust)
            _selectedCustomer.value = cust
            loadAllData()
        }
    }

    private fun filterCategories(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _filteredCategories.value = allCategories.sortedByDescending { it.id }.take(20)
        } else {
            val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            _filteredCategories.value = allCategories.filter { cat ->
                matchTokens(listOf(cat.name, cat.description ?: ""), tokens)
            }.sortedByDescending { it.id }
        }
    }

    private fun filterSuppliers(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _filteredSuppliers.value = allSuppliers.sortedByDescending { it.id }.take(20)
        } else {
            val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            _filteredSuppliers.value = allSuppliers.filter { sup ->
                matchTokens(listOf(sup.name, sup.address ?: "", sup.contactNum ?: "", sup.notes ?: ""), tokens)
            }.sortedByDescending { it.id }
        }
    }

    private fun filterCustomers(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _filteredCustomers.value = allCustomers.sortedByDescending { it.id }.take(20)
        } else {
            val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            _filteredCustomers.value = allCustomers.filter { cust ->
                matchTokens(listOf(cust.name, cust.address ?: "", cust.contactNum ?: "", cust.notes ?: ""), tokens)
            }.sortedByDescending { it.id }
        }
    }

    private fun matchTokens(fields: List<String>, tokens: List<String>): Boolean {
        val lowerFields = fields.map { it.lowercase() }
        for (token in tokens) {
            val matched = lowerFields.any { field ->
                field.contains(token) || levenshteinDistance(token, field) <= 2
            }
            if (!matched) return false
        }
        return true
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.length <= 3) return if (s2.contains(s1)) 0 else 99
        val len1 = s1.length
        val len2 = s2.length
        var prevRow = IntArray(len2 + 1) { it }
        var currRow = IntArray(len2 + 1)

        for (i in 1..len1) {
            currRow[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(prevRow[j] + 1, currRow[j - 1] + 1, prevRow[j - 1] + cost)
            }
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }
        return prevRow[len2]
    }
}
