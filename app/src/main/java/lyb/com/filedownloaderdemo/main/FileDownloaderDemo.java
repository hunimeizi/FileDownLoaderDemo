package lyb.com.filedownloaderdemo.main;

import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import lyb.com.filedownloaderdemo.R;
import lyb.com.filedownloaderdemo.downloader.DownloadProgressListener;
import lyb.com.filedownloaderdemo.downloader.FileDownloader;


public class FileDownloaderDemo extends ActionBarActivity {

    private EditText pathText;// 下载输入文本框
    private TextView resultView;// 实现进度显示百分比文本框
    private Button downloadButton;// 下载按钮，可以触发下载事件
    private Button stopbutton;// 停止按钮，可以停止下载
    private ProgressBar progressBar;// 下载进度条，实时图形化的显示进度信息
    private FileDownloader loader;// 文件下载器（下载线程的容器）
    private String path;// 下载路径
    private File saveDir;// 下载到保存到的文件
    private static final int MAX_THREAD_COUNT = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_downloader_demo);
        findView();
    }
    private void findView() {
        pathText = (EditText) this.findViewById(R.id.path);// 获取下载URL的文本输入对象
        resultView = (TextView) this.findViewById(R.id.resultView);// 获取显示下载百分比文本控制对象
        downloadButton = (Button) this.findViewById(R.id.downloadbutton);// 获取下载按钮对象
        stopbutton = (Button) this.findViewById(R.id.stopbutton);// 获取停止下载按钮对象
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);// 获取进度条对象
        initData();
        ButtonClickListener listener = new ButtonClickListener();// 声明并定义按钮监听对象
        downloadButton.setOnClickListener(listener);
        stopbutton.setOnClickListener(listener);
    }

    private void initData() {
        final String path = pathText.getText().toString();// 下载路径
        if (!TextUtils.isEmpty(path)) {
            final File saveDir = new File(Environment.getExternalStorageDirectory() + "/.snscity");// 下载到保存到的文件
            new Thread() {
                public void run() {
                    loader = new FileDownloader(getApplicationContext(), path, saveDir, MAX_THREAD_COUNT);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressBar.setMax(loader.getFileSize());// 设置进度条的最大刻度
                            progressBar.setProgress(loader.getDownloadedSize());
                            resultView.setText(loader.getDownloadedSize() / (loader.getFileSize() / 100) + "%");
                        }
                    });
                };
            }.start();
        }
    }
    /**
     * 按钮监听器实现类
     *
     * @zhangxiaobo
     */
    private final class ButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            // 该方法在注册了该按钮监听器的对象被单击时会自动调用，用力响应单击事件
            switch (v.getId()) {
                case R.id.downloadbutton:// 获取点击对象的id
                    String path = getDownloadPath();
                    startDownload(path);
                    downloadButton.setEnabled(false);
                    stopbutton.setEnabled(true);
                    while (loader == null) {
                        SystemClock.sleep(50);
                    }
                    loader.setExited(false);
                    break;
                case R.id.stopbutton:
                    exit();// 停止下载
                    downloadButton.setEnabled(true);
                    stopbutton.setEnabled(false);
                    Toast.makeText(getApplicationContext(), "暂停下载", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        private String getDownloadPath() {
            String path = pathText.getText().toString();// 获取下载路径
            return path;
        }



        private void startDownload(String path) {
            if (Environment.getExternalStorageState().endsWith(Environment.MEDIA_MOUNTED)) {
                // 获取SDCard是否存在，当SDCard存在时
                File saveDir = getSaveDir();
                // getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                Toast.makeText(getApplicationContext(), "开始下载", Toast.LENGTH_SHORT).show();

                download(path, saveDir, downloadProgressListener);
            } else {
                // 当SDCard不存在时
                Toast.makeText(getApplicationContext(), "SD卡不能用", Toast.LENGTH_LONG).show();// 提示用户SDCard不存在
            }
        }

        private File getSaveDir() {
            File saveDir = new File(Environment.getExternalStorageDirectory() + "/.snscity");
            return saveDir;
        }

    }

    DownloadProgressListener downloadProgressListener = new DownloadProgressListener<File>() {
        /**
         * 下载的文件长度会不断地被传入该回调方法
         */
        public void onDownloadSize(final int downloadSize, final int totalSize, final int progress) {
            runOnUiThread(new Runnable() {
                public void run() {
                    progressBar.setProgress(downloadSize);
                    resultView.setText(progress + "%");
                }
            });
        };

        @Override
        public void onSuccess(File file) {
            // Message msg = handler.obtainMessage();
            // msg.what = SUCCESS;
            // handler.sendMessage(msg);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "下载成功", Toast.LENGTH_LONG).show();
                    downloadButton.setEnabled(true);
                    stopbutton.setEnabled(false);
                }
            });
        }
    };
    // 由于用户的输入事件（点击button，触摸屏幕...）是由主线程负责处理的
    // 如果主线程处于工作状态
    // 此时用户产生的输入时间如果没能在5秒内得到处理，系统就会报应用无响应的错误
    // 所以在主线程里不能执行一件比较耗时的工作，否则会因主线程阻塞而无法处理用户的输入事件
    // 导致“应用无响应”错误的出现，耗时的工作应该在子线程里执行
    private DownloadTask task;// 声明下载执行者

    /**
     * 退出下载
     */
    public void exit() {
        if (task != null)
            task.exit();
    }

    /**
     * 下载资源，声明下载执行者并开辟线程开始下载
     *
     * @param path
     *            下载的路径
     * @param saveDir
     *            保存文件
     */
    private void download(String path, File saveDir, DownloadProgressListener downloadProgressListener) {
        task = new DownloadTask(path, saveDir, downloadProgressListener);// 实例化下载业务
        new Thread(task).start();
    }

    /**
     * UI控制画面的重绘（更新）是由主线程负责处理的，如果在子线程中更新UI控件值，更新后值不会重绘到屏幕上 一定要在主线程里更新UI控件的值，这样才能在屏幕上显示出来，不能在子线程中更新UI控件的值
     */
    private final class DownloadTask implements Runnable {

        private String path;// 下载路径
        private File saveDir;// 下载到保存到的文件
        private DownloadProgressListener downloadProgressListener;

        /**
         * 构造方法，实现变量的初始化
         *
         * @param path
         *            下载路径
         * @param saveDir
         *            下载要保存到的文件
         */
        public DownloadTask(String path, File saveDir, DownloadProgressListener downloadProgressListener) {
            this.path = path;
            this.saveDir = saveDir;
            this.downloadProgressListener = downloadProgressListener;
        }

        /**
         * 退出下载
         */
        public void exit() {
            if (loader != null)
                loader.exit();// 如果下载器存在的话则退出下载
        }

        public void run() {
            try {
                loader.download(downloadProgressListener);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "下载错误", Toast.LENGTH_LONG).show();// 提示用户下载失败
                    }
                });

            }
        }

    }

}
