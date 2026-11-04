package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.camera.core.ImageProxy
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.BorrowedItem
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

object MediaHelper {

    fun createTempImageUri(context: Context): Uri? {
        return try {
            val directory = File(context.cacheDir, "camera_captures").apply {
                if (!exists()) mkdirs()
            }
            val file = File.createTempFile("memory_photo_${System.currentTimeMillis()}_", ".jpg", directory)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy, maxDimension: Int = 720): Bitmap? {
        return try {
            val rawBitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotated = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
            scaleBitmap(rotated, maxDimension)
        } catch (e: Exception) {
            null
        }
    }

    fun loadAndCompressBitmap(context: Context, uri: Uri, maxDimension: Int = 720): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            scaleBitmap(original, maxDimension)
        } catch (e: Exception) {
            null
        }
    }

    fun scaleBitmap(bitmap: Bitmap, maxDimension: Int = 720): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun encodeBitmapToBase64(bitmap: Bitmap, quality: Int = 68): String {
        val scaled = scaleBitmap(bitmap, 560)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        val byteArray = output.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

object MemoryNotificationHelper {
    private const val CHANNEL_BORROW_ID = "whereditgo_borrow_reminders"
    private const val CHANNEL_MEMORY_ID = "whereditgo_memory_updates"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val borrowChannel = NotificationChannel(
                CHANNEL_BORROW_ID,
                "Borrowed Item Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when items lent to others are due or overdue"
            }
            val memoryChannel = NotificationChannel(
                CHANNEL_MEMORY_ID,
                "Memory & Shared Space Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily memory summaries and shared space notifications"
            }
            manager.createNotificationChannel(borrowChannel)
            manager.createNotificationChannel(memoryChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendOverdueBorrowedNotification(context: Context, item: BorrowedItem): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_BORROW_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Overdue Item: ${item.itemName}")
            .setContentText("${item.borrowerName} borrowed your ${item.itemName}. Tap to review or mark returned.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun sendDailySummaryNotification(context: Context, totalMemories: Int, activeBorrows: Int): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_MEMORY_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Your Where'd It Go? Memory Summary")
            .setContentText("You have $totalMemories saved locations and $activeBorrows active lent items tracked.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(9001, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
