package com.example.smsblocking;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import static com.example.smsblocking.MainActivity.blockingDatabase;

public class Rubbish extends AppCompatActivity {

    private static final int READ_SMS_PERMISSIONS_REQUEST=1;
    ListView rubbish;
    private SQLiteDatabase dbWrite, dbRead;

    private SimpleCursorAdapter adapter;
    private TextView addressView, dateView, bodyView;
    private Cursor c = null;
    SmsManager smsManager = SmsManager.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rubbish);
        rubbish = (ListView)findViewById(R.id.rubbishList);
        dbWrite = blockingDatabase.getWritableDatabase();
        dbRead = blockingDatabase.getReadableDatabase();//数据库读写权限
        addressView = (TextView) findViewById(R.id.addressView);
        dateView = (TextView) findViewById(R.id.dateView);
        bodyView = (TextView) findViewById(R.id.bodyView);
        adapter = new SimpleCursorAdapter(this, R.layout.sms_block_list_cell, null, new String[]{"address", "date", "body"}
                , new int[]{R.id.addressView, R.id.dateView, R.id.bodyView});

        rubbish.setAdapter(adapter);

        SmsBroadcastReceiver.callback = new ICallBack() {
            @Override
            public void callMainActivityViewRefresh() {
                refreshListView();
            }
        };
        refreshListView();

        rubbish.setOnItemLongClickListener(listVievItemLongClickListener);

    }

    private AdapterView.OnItemLongClickListener listVievItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            new AlertDialog.Builder(Rubbish.this).setTitle("提醒").setMessage("您确定要删除该条吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dbWrite = blockingDatabase.getWritableDatabase();// 开启数据读写库权限
                    c = adapter.getCursor();
                    c.moveToPosition(position);  //position在内部类中需要是final 也就是常量
                    int itemId = c.getInt(c.getColumnIndex("_id"));//Db类中创建的用户表中的_id
                    dbWrite.delete("sms", "_id=?", new String[]{itemId + ""});//"_id="+itemId 此种写法不太安全,所以把itemId传到下一个参数中
                    refreshListView();
                }
            }).setNegativeButton("取消", null).show();
            return true;//返回一个true值表示让操作系统做出相应长按反馈动作，false表示此次长按是不成功的
        }
    };

    private void refreshListView() {
        dbRead = blockingDatabase.getReadableDatabase();
        c = dbRead.query("sms", null, null, null, null, null, null);
        adapter.changeCursor(c);
    }

    public void updateInbox(final String smsMessage) {

    }

}