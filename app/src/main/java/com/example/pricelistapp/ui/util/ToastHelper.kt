package com.example.pricelistapp.ui.util

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.pricelistapp.R

enum class ToastType {
    SUCCESS, ERROR, WARNING
}

object ToastHelper {
    fun show(context: Context, message: String, type: ToastType) {
        try {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.custom_toast, null)

            val imageView = layout.findViewById<ImageView>(R.id.toast_icon)
            val textView = layout.findViewById<TextView>(R.id.toast_text)

            textView.text = message

            when (type) {
                ToastType.SUCCESS -> {
                    imageView.setImageResource(R.drawable.ic_check)
                    imageView.clearColorFilter()
                }
                ToastType.ERROR -> {
                    imageView.setImageResource(android.R.drawable.stat_notify_error)
                    imageView.setColorFilter(Color.parseColor("#F44336")) // Red error
                }
                ToastType.WARNING -> {
                    imageView.setImageResource(android.R.drawable.stat_sys_warning)
                    imageView.setColorFilter(Color.parseColor("#FF9800")) // Orange warning
                }
            }

            val toast = Toast(context)
            toast.duration = Toast.LENGTH_SHORT
            toast.view = layout
            toast.show()
        } catch (e: Exception) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
