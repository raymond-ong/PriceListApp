package com.example.pricelistapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricelistapp.data.local.AppDatabase
import com.example.pricelistapp.data.local.ProductWithVariants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val productDao = AppDatabase.getInstance(application).productDao()

    private var allProducts: List<ProductWithVariants> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredProducts = MutableStateFlow<List<ProductWithVariants>>(emptyList())

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages

    private val _totalRecords = MutableStateFlow(0)
    val totalRecords: StateFlow<Int> = _totalRecords

    val pageSize = 20

    val paginatedProducts: StateFlow<List<ProductWithVariants>> = combine(_filteredProducts, _currentPage) { filtered, page ->
        val total = filtered.size
        val pages = if (total == 0) 1 else (total + pageSize - 1) / pageSize
        _totalPages.value = pages
        _totalRecords.value = total

        val safePage = page.coerceIn(1, pages)
        val startIndex = (safePage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, total)
        if (startIndex < total) {
            filtered.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProducts()
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collect { query ->
                    filterAndSortProducts(query)
                }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            allProducts = productDao.getAllProductsWithVariants()
            filterAndSortProducts(_searchQuery.value)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
    }

    fun setPage(page: Int) {
        val maxPage = _totalPages.value.coerceAtLeast(1)
        _currentPage.value = page.coerceIn(1, maxPage)
    }

    fun nextPage() {
        if (_currentPage.value < _totalPages.value) {
            _currentPage.value++
        }
    }

    fun prevPage() {
        if (_currentPage.value > 1) {
            _currentPage.value--
        }
    }

    fun firstPage() {
        _currentPage.value = 1
    }

    fun lastPage() {
        _currentPage.value = _totalPages.value.coerceAtLeast(1)
    }

    private fun filterAndSortProducts(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            // If user did not type anything in the Search textbox, show the last 10 products modified.
            val last10 = allProducts
                .sortedByDescending { it.product.lastModifiedDate }
                .take(10)
            _filteredProducts.value = last10
        } else {
            // Fuzzy search with scoring and typo tolerance
            val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val scoredMatches = allProducts.mapNotNull { item ->
                val score = calculateMatchScore(item, tokens)
                if (score > 0) item to score else null
            }

            // Sort by fuzzy search score descending, then by last modified date descending
            val sortedMatches = scoredMatches
                .sortedWith(compareByDescending<Pair<ProductWithVariants, Int>> { it.second }
                    .thenByDescending { it.first.product.lastModifiedDate })
                .map { it.first }

            _filteredProducts.value = sortedMatches
        }
    }

    private fun calculateMatchScore(item: ProductWithVariants, tokens: List<String>): Int {
        val prodName = item.product.name.lowercase()
        val prodDesc = (item.product.description ?: "").lowercase()
        val variantNames = item.variants.map { it.name.lowercase() }

        var totalScore = 0

        for (token in tokens) {
            var tokenScore = 0
            if (prodName == token) {
                tokenScore = maxOf(tokenScore, 1000)
            } else if (prodName.startsWith(token)) {
                tokenScore = maxOf(tokenScore, 800)
            } else if (prodName.contains(token)) {
                tokenScore = maxOf(tokenScore, 600)
            } else if (variantNames.any { it == token }) {
                tokenScore = maxOf(tokenScore, 550)
            } else if (variantNames.any { it.contains(token) }) {
                tokenScore = maxOf(tokenScore, 500)
            } else if (prodDesc.contains(token)) {
                tokenScore = maxOf(tokenScore, 300)
            } else {
                val allTexts = listOf(prodName) + variantNames + listOf(prodDesc)
                val matchedByTypo = allTexts.any { text ->
                    isTokenTypoMatch(token, text)
                }
                if (matchedByTypo) {
                    tokenScore = maxOf(tokenScore, 150)
                }
            }

            if (tokenScore == 0) {
                return 0 // All tokens must match
            }
            totalScore += tokenScore
        }
        return totalScore
    }

    private fun isTokenTypoMatch(token: String, targetText: String): Boolean {
        // For short tokens (length <= 4), do not allow fuzzy typo/subsequence matching to prevent false positives like "hard" matching "hammer"
        if (token.length <= 4) {
            return targetText.contains(token)
        }

        val targetWords = targetText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        for (word in targetWords) {
            if (word.contains(token) || token.contains(word)) return true

            val maxAllowedDistance = when {
                token.length <= 6 -> 1
                token.length <= 9 -> 2
                else -> 3
            }
            if (levenshteinDistance(token, word) <= maxAllowedDistance) return true

            // Subsequence match only for longer tokens (length >= 6)
            if (token.length >= 6 && isSubsequenceMatch(token, word)) return true
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        var prevRow = IntArray(len2 + 1) { it }
        var currRow = IntArray(len2 + 1)

        for (i in 1..len1) {
            currRow[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(
                    prevRow[j] + 1,
                    currRow[j - 1] + 1,
                    prevRow[j - 1] + cost
                )
            }
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }
        return prevRow[len2]
    }

    private fun isSubsequenceMatch(query: String, target: String): Boolean {
        if (query.length < 6) return false
        if (query.length > target.length + 2) return false
        var qIndex = 0
        var tIndex = 0
        while (qIndex < query.length && tIndex < target.length) {
            if (query[qIndex] == target[tIndex]) {
                qIndex++
            }
            tIndex++
        }
        return qIndex >= (query.length * 0.85).toInt()
    }
}
