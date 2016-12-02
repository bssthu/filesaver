package com.github.bssthu.filesaver;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class SaveActivity extends Activity {
    // 保存路径
    public static final String SAVE_PATH = "Download/saver";
    // 剩余文件个数
    private int fileLeft = 0;

    @Override
    protected void onResume() {
        super.onResume();
        // 处理分享请求
        try {
            handleIntent();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    /**
     * 处理 Intent
     */
    private void handleIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            requireIoPermissions();
            handleSend(intent);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            requireIoPermissions();
            handleSendMultiple(intent);
        }

        if (fileLeft <= 0) {
            showTip("不支持的类型");
            finish();
        }
    }

    /**
     * 分享一个文件
     */
    private void handleSend(final Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        fileLeft = 1;
        askSavePath(uri);
    }

    /**
     * 分享多个文件
     */
    private void handleSendMultiple(final Intent intent) {
        List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        fileLeft = uris.size();
        for (Uri uri : uris) {
            askSavePath(uri);
        }
    }

    /**
     * 选择保存的文件名
     */
    private void askSavePath(final Uri uri) {
        final EditText fileNameEdit = new EditText(this);
        final String[] uriSplit = uri.toString().split("/");
        final String defaultName = Uri.decode(uriSplit[uriSplit.length - 1]);
        fileNameEdit.setText(defaultName);
        fileNameEdit.setHint(defaultName);

        new AlertDialog.Builder(this)
                .setTitle("请选择文件名")
                .setMessage(uri.toString())
                .setView(fileNameEdit)
                .setCancelable(false)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = fileNameEdit.getText().toString().trim();
                        if ("".equals(name)) {
                            name = defaultName;
                        }
                        saveUriFile(uri, name);
                        onHandledOne();
                    }
                })
                .setNegativeButton("跳过", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onHandledOne();
                    }
                })
                .show();
    }

    /**
     * 保存到预设位置
     */
    private void saveUriFile(final Uri uri, final String name) {
        try {
            // 保存的路径
            File dir = new File(Environment.getExternalStorageDirectory(), SAVE_PATH);
            if (dir.isFile() || (!dir.exists() && !dir.mkdir())) {
                // 存在了同名文件，或者文件夹不存在但创建失败
                showTip(String.format("无法创建文件夹 %s", dir.toString()));
                finish();
            }
            // 使文件夹可见
            MediaScannerConnection.scanFile(this, new String[] {dir.toString()}, null, null);

            File input = new File(uri.getPath());
            File output = new File(dir, name);
            // 拷贝文件
            copyFile(input, output);

            // 成功后显示
            showTip(name);
        } catch (Exception e) {
            e.printStackTrace();
            showTip(String.format("%s 保存失败", name));
        }
    }

    /**
     * 拷贝文件
     */
    private void copyFile(final File input, final File output) throws IOException {
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);

        byte[] buffer = new byte[1024];
        int n;
        while ((n = fis.read(buffer)) != -1) {
            fos.write(buffer, 0, n);
        }

        fis.close();
        fos.close();
    }

    /**
     * 请求读写权限
     */
    private void requireIoPermissions() {
        if (!permit(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        if (!permit(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    /**
     * 是否允许此项权限
     */
    private boolean permit(final String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 处理完一个
     */
    private void onHandledOne() {
        if (--fileLeft <= 0) {
            // 文件都处理完后退出
            finish();
        }
    }

    /**
     * 显示消息
     */
    private void showTip(final String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
