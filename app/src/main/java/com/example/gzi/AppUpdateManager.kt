package com.example.gzi

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AppUpdateManager(private val context: Context) {

    fun downloadAndInstallApk(url: String, fileName: String) {
        // 1. Проверка наличия дубликата в локальной папке загрузок телефона
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val existingApkFile = File(downloadsDir, fileName)

        // Если файл уже был успешно скачан ранее, сразу запускаем его установку
        if (existingApkFile.exists() && existingApkFile.length() > 0) {
            Toast.makeText(context, "Файл обновления уже скачан. Запуск установки...", Toast.LENGTH_SHORT).show()
            installApk(fileName)
            return
        }

        // 2. Исходная конфигурация загрузчика для скачивания по прямой ссылке
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)

        val request = DownloadManager.Request(uri).apply {
            setTitle("Обновление ПСГиИ")
            setDescription("Скачивание новой версии...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setAllowedOverRoaming(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setRequiresCharging(false)
                setRequiresDeviceIdle(false)
            }
        }

        try {
            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Скачивание началось...", Toast.LENGTH_SHORT).show()

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        context.unregisterReceiver(this)
                        installApk(fileName)
                    }
                }
            }

            // Стабильная регистрация слушателя с флагом безопасности для targetSdk 35
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при скачивании: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApk(fileName: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadsDir, fileName)

        if (!apkFile.exists()) {
            Toast.makeText(context, "Файл обновления не найден", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Необходимо дать разрешение на установку обновлений", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_INSTALL_PACKAGE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            intent.data = contentUri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось запустить установку: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
