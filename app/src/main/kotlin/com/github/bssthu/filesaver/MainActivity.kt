package com.github.bssthu.filesaver

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    /** 待保存的文件列表: (源 URI, 用户确认的文件名) */
    private val pendingFiles = mutableListOf<Pair<Uri, String>>()

    /** SAF 目录选择器 */
    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            saveAllFiles(treeUri)
        } else {
            toast("已取消")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        handleShareIntent()
    }

    private fun handleShareIntent() {
        val uris = extractUris(intent)
        if (uris.isEmpty()) {
            toast("不支持的分享类型")
            finish()
            return
        }
        lifecycleScope.launch {
            collectFileNames(uris)
        }
    }

    /**
     * 从 Intent 提取所有共享的 URI
     */
    private fun extractUris(intent: Intent?): List<Uri> {
        intent ?: return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(getParcelableExtraCompat(intent))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableArrayListExtraCompat(intent) ?: emptyList()
            }
            else -> emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private fun getParcelableExtraCompat(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    @Suppress("DEPRECATION")
    private fun getParcelableArrayListExtraCompat(intent: Intent): ArrayList<Uri>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }

    /**
     * 依次为每个 URI 弹出文件名确认对话框，全部确认后打开目录选择器
     */
    private suspend fun collectFileNames(uris: List<Uri>) {
        for (uri in uris) {
            val defaultName = FileUtils.getFileName(this@MainActivity, uri)
            val chosenName = showFileNameDialog(uri, defaultName)
            if (chosenName != null) {
                pendingFiles.add(uri to chosenName)
            }
        }
        if (pendingFiles.isEmpty()) {
            finish()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAllToDownloads()
        } else {
            openDocumentTreeLauncher.launch(null)
        }
    }

    /**
     * 挂起协程，弹出文件名编辑对话框。返回 null 表示跳过该文件。
     */
    private suspend fun showFileNameDialog(uri: Uri, defaultName: String): String? =
        suspendCancellableCoroutine { cont ->
            val editText = EditText(this).apply {
                setText(defaultName)
                selectAll()
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle("保存文件")
                .setMessage(defaultName)
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton("保存") { _, _ ->
                    val name = editText.text.toString().trim().ifEmpty { defaultName }
                    cont.resume(name)
                }
                .setNegativeButton("跳过") { _, _ ->
                    cont.resume(null)
                }
                .show()
            cont.invokeOnCancellation { dialog.dismiss() }
        }

    /**
     * 将所有待保存文件复制到用户选择的 SAF 目录
     */
    private fun saveAllFiles(treeUri: Uri) {
        lifecycleScope.launch {
            var saved = 0
            for ((srcUri, name) in pendingFiles) {
                if (FileUtils.copyToTree(this@MainActivity, srcUri, treeUri, name)) {
                    saved++
                }
            }
            toast("已保存 $saved/${pendingFiles.size} 个文件")
            finish()
        }
    }

    /**
     * Android 10+ 直接写入系统 Downloads，避免 SAF 对 Download 根目录的限制。
     */
    private fun saveAllToDownloads() {
        lifecycleScope.launch {
            var saved = 0
            for ((srcUri, name) in pendingFiles) {
                val mimeType = FileUtils.getMimeType(this@MainActivity, srcUri)
                if (FileUtils.copyToDownloads(this@MainActivity, srcUri, name, mimeType)) {
                    saved++
                }
            }
            toast("已保存到下载目录 $saved/${pendingFiles.size} 个文件")
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
