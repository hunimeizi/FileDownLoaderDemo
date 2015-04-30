package lyb.com.filedownloaderdemo.downloader;


/**
 * 下载进度监听器
 * @author Wang Jialin
 *
 */
public interface DownloadProgressListener<T> {
	/**
	 * 下载进度监听方法 获取和处理下载点数据的大小
	 */
	public void onDownloadSize(int downloadSize, int totalSize, int progress);
	/**
	 * 下载完成回调函数
	 * @MethodName: onSuccess 
	 * @author Chenyuanming
	 * @date 2014年11月17日 上午10:15:07
	 */
	public void onSuccess(T t);
}
