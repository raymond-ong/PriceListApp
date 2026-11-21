package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class AuditChangeRecord(
    val variantId: String? = null,
    val customerId: String? = null,
    val fieldName: String,
    val prevValue: String? = null,
    val newValue: String? = null
)

@Entity(tableName = "product_audit_trail")
data class ProductAuditTrailEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    val date: Long,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "product_id")
    val productId: String,
    @ColumnInfo(name = "change_details")
    val changeDetailsJson: String
)

object AuditJsonConverter {
    fun toJson(records: List<AuditChangeRecord>): String {
        val jsonArray = JSONArray()
        for (r in records) {
            val obj = JSONObject().apply {
                put("variantId", r.variantId ?: JSONObject.NULL)
                put("customerId", r.customerId ?: JSONObject.NULL)
                put("fieldName", r.fieldName)
                put("prevValue", r.prevValue ?: JSONObject.NULL)
                put("newValue", r.newValue ?: JSONObject.NULL)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun fromJson(json: String): List<AuditChangeRecord> {
        val list = mutableListOf<AuditChangeRecord>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val vId = if (obj.has("variantId") && !obj.isNull("variantId")) obj.getString("variantId") else null
                val cId = if (obj.has("customerId") && !obj.isNull("customerId")) obj.getString("customerId") else null
                val fieldName = obj.getString("fieldName")
                val prev = if (obj.has("prevValue") && !obj.isNull("prevValue")) obj.getString("prevValue") else null
                val newV = if (obj.has("newValue") && !obj.isNull("newValue")) obj.getString("newValue") else null
                list.add(AuditChangeRecord(vId, cId, fieldName, prev, newV))
            }
        } catch (_: Exception) {
        }
        return list
    }
}
