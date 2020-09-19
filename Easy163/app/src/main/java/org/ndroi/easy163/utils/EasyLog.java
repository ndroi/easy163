package org.ndroi.easy163.utils;

import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EasyLog {

  private static Logger logger = null;

  public static void log(String info) {
    logger.log(info);
  }

  public static void setTextView(TextView textView) {
    textView.setOnLongClickListener(v -> true);
    logger = new Logger(textView);
  }

  private static class Logger {

    private WeakReference<TextView> textView;

    public Logger(TextView textView) {
      this.textView = new WeakReference<>(textView);
    }

    private String genTime() {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[HH:mm:ss]", Locale.CHINA);
      return simpleDateFormat.format(new Date());
    }

    private void log(String info) {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(genTime()).append(" ").append(info).append("\n");
      textView.get().post(() -> textView.get().append(stringBuilder));
    }
  }
}
