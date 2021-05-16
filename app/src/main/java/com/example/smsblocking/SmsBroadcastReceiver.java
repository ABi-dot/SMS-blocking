package com.example.smsblocking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class SmsBroadcastReceiver extends BroadcastReceiver {
    SQLiteDatabase dbWrite, dbRead;
    private boolean block = false;
    public static final String SMS_BUNDLE = "pdus";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();

        if(intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            ContentValues cv = new ContentValues();
            //拆分短信数据包
            String smsMessageStr = "";
            String smsBody = null;
            String address = null;
            SmsMessage smsMessage = null;
            for (int i = 0; i < sms.length; ++i) {
                String format = intentExtras.getString("format");
                smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);

                smsBody = smsMessage.getMessageBody().toString();
                address = smsMessage.getOriginatingAddress();

                smsMessageStr += "来自" + address + "的信息：" + "\n";
                smsMessageStr += smsBody + "\n";

            }

            if (address.contains("+86")) {
                address = address.substring(3);
            }

            dbWrite = MainActivity.blockingDatabase.getWritableDatabase();
            dbRead = MainActivity.blockingDatabase.getReadableDatabase();
            Cursor cursorBlockWords = dbRead.query("blockWords", new String[]{"keywords"},
                    null, null, null, null, null);
            Cursor cursorBlockNumber = dbRead.query("blockNumber", new String[]{"number"},
                    "number=?", new String[]{address}, null, null, null);
            if (cursorBlockNumber.moveToPosition(0)) {//如果查询结果有匹配address，那就拦截
                block = true;
                abortBroadcast();
            }
            if (!block) {
                cursorBlockWords.moveToPosition(-1);//直接定位到-1
                while (cursorBlockWords.moveToNext()) {
                    if (smsBody.contains(cursorBlockWords
                            .getString(cursorBlockWords.getColumnIndex("keywords")))) {
                        block = true;
                        abortBroadcast();
                        break;
                    }
                }
            }
            if (!cursorBlockNumber.isClosed()) {
                cursorBlockNumber.close();
            }
            if (!cursorBlockWords.isClosed()) {
                cursorBlockWords.close();
            }
            if (block) {
                long date = smsMessage.getTimestampMillis();         //短信时间
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//创建格式化日期时间对象
                String dateStr = format.format(date);              //格式化日期时间对象
                cv.put("address", address);
                cv.put("body", smsBody);
                cv.put("date", dateStr);
                dbWrite.insert("sms", null, cv);
                if (callback != null)
                    callback.callMainActivityViewRefresh();
            }
            dbRead.close();
            dbWrite.close();
            if(!block) {
                Toast.makeText(context, "收到了一条短信！", Toast.LENGTH_SHORT).show();
                if (MainActivity.active) {
                    MainActivity inst = MainActivity.instance();
                    inst.updateInbox(smsMessageStr);
                } else {
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                }
            }
            block = false;
        }
    }
    public  static ICallBack callback=null;
}
