package m.drl.mqtt.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class StringHelper {
	
	/**
	 * 筛除非法字符
	 * @param text
	 * @return
	 */
	public static boolean isText(String text){
		if(text==null||text.length()==0||text.equals("")||text.equals("null")||text.equals("NULL")){
			return false;
		}else {
			return true;
		}
	}
	/**
	 * bytes[]转换成Hex字符,可用于URL转换，IP地址转换.
	 * */
	public static String bytesToHexString(byte[] bytes) {
		// http://stackoverflow.com/questions/332079
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	
	
	public static String prettyBytes(long value) {

		String args[] = { "B", "KB", "MB", "GB", "TB" };
		StringBuilder sb = new StringBuilder();
		int i;
		if (value < 1024L) {
			sb.append(String.valueOf(value));
			i = 0;
		} else if (value < 1048576L) {
			sb.append(String.format("%.1f", value / 1024.0));
			i = 1;
		} else if (value < 1073741824L) {
			sb.append(String.format("%.2f", value / 1048576.0));
			i = 2;
		} else if (value < 1099511627776L) {
			sb.append(String.format("%.3f", value / 1073741824.0));
			i = 3;
		} else {
			sb.append(String.format("%.4f", value / 1099511627776.0));
			i = 4;
		}
		sb.append(' ');
		sb.append(args[i]);
		return sb.toString();
	}
	
	/**
	 * 将Throwable 信息转换成String字符
	 * */
	public static String exceptionToString(Throwable t) {
		if (t == null)
			return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			t.printStackTrace(new PrintStream(baos));
		} finally {
			try {
				baos.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return baos.toString();
	}

}
