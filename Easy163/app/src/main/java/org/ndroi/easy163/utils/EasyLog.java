package org.ndroi.easy163.utils;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EasyLog
{
    private static Logger logger = null;

    public static void log(String info)
    {
        if(logger != null)
        {
            logger.log(info);
        }
    }

    public static void setTextView(TextView textView)
    {
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logger = new Logger(textView);
    }

    private static class Logger
    {
        private TextView textView;
        public Logger(TextView textView)
        {
            this.textView = textView;
        }

        private String genTime()
        {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("[HH:mm:ss]");
            String time = simpleDateFormat.format(new Date());
            return time;
        }

        private void log(String info)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(genTime() + " " + info + "\n");
            textView.post(new Runnable()
            {
                @Override
                public void run()
                {
                    textView.append(stringBuilder);
                }
            });
        }
    }
}
