package com.example.pricelistapp.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ProductWithVariants(
    @Embedded
    val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "product_id"
    )
    val variants: List<ProductVariantEntity>
)
