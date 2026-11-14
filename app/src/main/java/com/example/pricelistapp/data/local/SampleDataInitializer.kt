package com.example.pricelistapp.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object SampleDataInitializer {
    private const val TAG = "SampleDataInitializer"

    suspend fun initializeIfNeeded(context: Context, database: AppDatabase) {
        if (!SampleDataConfig.ENABLE_SAMPLE_DATA) {
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val productDao = database.productDao()
                val existingProducts = productDao.getAllProductsWithVariants()
                if (existingProducts.isNotEmpty()) {
                    Log.d(TAG, "Database already contains products. Skipping sample data initialization.")
                    return@withContext
                }

                Log.d(TAG, "Loading sample data from SampleData.json...")
                val jsonString = readJsonFromAssets(context, "SampleData.json") ?: return@withContext
                val jsonArray = JSONArray(jsonString)

                val categoryDao = database.categoryDao()
                val supplierDao = database.supplierDao()
                val customerDao = database.customerDao()
                val userDao = database.userDao()

                // Ensure a default user exists for price history
                val defaultUserName = "Sample Admin"
                var user = userDao.findUserByName(defaultUserName)
                if (user == null) {
                    user = User(userName = defaultUserName)
                    userDao.insertUser(user)
                }
                val userId = user.id

                val categoryMap = mutableMapOf<String, String>() // name -> id
                val supplierMap = mutableMapOf<String, String>() // name -> id
                val customerMap = mutableMapOf<String, String>() // name -> id

                val now = System.currentTimeMillis()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val categoryName = item.optString("Product Category").trim()
                    val productName = item.optString("Product Name").trim()
                    val productDesc = item.optString("Product Description", "").trim()
                    val supplierName = item.optString("Product Supplier").trim()

                    if (categoryName.isEmpty() || productName.isEmpty() || supplierName.isEmpty()) {
                        continue
                    }

                    // 1. Category
                    var catId = categoryMap[categoryName]
                    if (catId == null) {
                        var cat = categoryDao.findCategoryByName(categoryName)
                        if (cat == null) {
                            cat = CategoryEntity(
                                name = categoryName,
                                description = "Category for $categoryName"
                            )
                            try {
                                categoryDao.insertCategory(cat)
                            } catch (e: Exception) {
                                cat = categoryDao.findCategoryByName(categoryName) ?: cat
                            }
                        }
                        catId = cat.id
                        categoryMap[categoryName] = catId
                    }

                    // 2. Supplier
                    var supplierId = supplierMap[supplierName]
                    if (supplierId == null) {
                        var sup = supplierDao.findSupplierByName(supplierName)
                        if (sup == null) {
                            sup = SupplierEntity(
                                name = supplierName,
                                address = null,
                                contactNum = null,
                                notes = null
                            )
                            try {
                                supplierDao.insertSupplier(sup)
                            } catch (e: Exception) {
                                sup = supplierDao.findSupplierByName(supplierName) ?: sup
                            }
                        }
                        supplierId = sup.id
                        supplierMap[supplierName] = supplierId
                    }

                    // 3. Product
                    val productEntity = ProductEntity(
                        name = productName,
                        description = productDesc.ifEmpty { null },
                        catId = catId,
                        supplierId = supplierId,
                        lastModifiedDate = now
                    )
                    productDao.insertProduct(productEntity)

                    // 4. Variants
                    val variantsArray = item.optJSONArray("Variants")
                    if (variantsArray != null) {
                        for (vIdx in 0 until variantsArray.length()) {
                            val vObj = variantsArray.getJSONObject(vIdx)
                            val variantName = vObj.optString("Variant Name").trim()
                            val supplierPrice = vObj.optDouble("Supplier Price", 0.0)
                            val sellingPrice = vObj.optDouble("Selling Price", 0.0)

                            if (variantName.isEmpty()) continue

                            val variantEntity = ProductVariantEntity(
                                productId = productEntity.id,
                                name = variantName,
                                supplierPrice = supplierPrice,
                                unit = variantName,
                                sellingPrice = sellingPrice
                            )
                            productDao.insertVariant(variantEntity)

                            // Supplier Price Hist
                            val supHist = SupplierPriceHistEntity(
                                userId = userId,
                                date = now,
                                variantId = variantEntity.id
                            )
                            productDao.insertSupplierPriceHist(supHist)

                            // Custom Pricing
                            val customPricingArray = vObj.optJSONArray("Custom Pricing")
                            if (customPricingArray != null) {
                                for (cpIdx in 0 until customPricingArray.length()) {
                                    val cpObj = customPricingArray.getJSONObject(cpIdx)
                                    val customerName = cpObj.optString("Customer Name").trim()
                                    if (customerName.isNotEmpty()) {
                                        var custId = customerMap[customerName]
                                        if (custId == null) {
                                            var cust = customerDao.findCustomerByName(customerName)
                                            if (cust == null) {
                                                cust = CustomerEntity(
                                                    name = customerName,
                                                    address = null,
                                                    contactNum = null,
                                                    notes = null
                                                )
                                                try {
                                                    customerDao.insertCustomer(cust)
                                                } catch (e: Exception) {
                                                    cust = customerDao.findCustomerByName(customerName) ?: cust
                                                }
                                            }
                                            custId = cust.id
                                            customerMap[customerName] = custId
                                        }

                                        val custHist = CustomerPriceHistEntity(
                                            userId = userId,
                                            date = now,
                                            variantId = variantEntity.id,
                                            customerId = custId,
                                            isDeleted = 0,
                                            isDefaultPrice = 0
                                        )
                                        productDao.insertCustomerPriceHist(custHist)
                                    }
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "Sample data loaded successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sample data", e)
            }
        }
    }

    private fun readJsonFromAssets(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading $fileName from assets", e)
            null
        }
    }
}
