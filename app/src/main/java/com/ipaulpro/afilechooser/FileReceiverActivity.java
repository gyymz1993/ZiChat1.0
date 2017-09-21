package com.ipaulpro.afilechooser;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.ChatMessage;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.message.InstantMessageActivity;
import com.sk.weichat.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/11/8.
 * 查看接收文件
 */
public class FileReceiverActivity extends ActionBackActivity {
    private ListView mLvReceiver;
    private List<File> mDatas;
    private SelectFileWindow menuWindow;
    private FileReceiverAdapter receiverAdapter;
    private ChatMessage message;
    private FileInformationWindow menuWindow2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filereceiver);
        initView();
        loadData();
    }

    private void loadData() {
        message = (ChatMessage) getIntent().getParcelableExtra(Constants.INSTANT_MESSAGE);
        mDatas = new ArrayList<File>();
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sk");
        if (f != null) {
            File[] files = f.listFiles();
            for (File file : files) {
                mDatas.add(file);
            }
        }
        receiverAdapter = new FileReceiverAdapter();
        mLvReceiver.setAdapter(receiverAdapter);
        /* 长按弹出转发,删除,取消等操作*/
        mLvReceiver.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Vibrator vib = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
                vib.vibrate(40);// 只震动一秒，一次
                menuWindow = new SelectFileWindow(FileReceiverActivity.this, new ClickListener(position, mDatas.get(position)));
                // 显示窗口
                menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

                return false;
            }
        });
        /* 点击打开指定方式*/
        mLvReceiver.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileOpenWays openWays = new FileOpenWays(FileReceiverActivity.this);
                openWays.openFiles(mDatas.get(position).getAbsolutePath());


            }
        });
    }

    private void initView() {
        mLvReceiver = (ListView) findViewById(R.id.lv_file_receiver);
        getSupportActionBar().setTitle("ReceiverFile");
    }

    private class FileReceiverAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mDatas != null) {
                return mDatas.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mDatas != null) {
                return mDatas.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(FileReceiverActivity.this, R.layout.item_filereceiver, null);
            }
            ((TextView) convertView.findViewById(R.id.tv_filereceiver)).setText(mDatas.get(position).getName().toString());
            ImageView iv = (ImageView) convertView.findViewById(R.id.iv_filereceiver);
            setFileIcon(iv, mDatas.get(position).getAbsolutePath());
            return convertView;
        }
    }

    /**
     * 实现点击事件
     */
    public class ClickListener implements View.OnClickListener {
        private int position;
        private File file;

        public ClickListener(int position, File file) {
            this.position = position;
            this.file = file;
        }

        @Override
        public void onClick(View v) {
            menuWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_instant:// 转发消息
                    Intent intent = new Intent(FileReceiverActivity.this, InstantMessageActivity.class);
                    intent.putExtra(Constants.INSTANT_MESSAGE_FILE, file.getAbsolutePath().toString());
                    /*此处传文件这里message是没有什么用的,不过因为之前在chatactivity中已经对逻辑有了instantmessage非空的判断,所以这里就带上 */
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.INSTANT_MESSAGE, message);
                    intent.putExtras(bundle);
                    startActivity(intent);
                    break;
                case R.id.btn_delete:// 删除
                    File file = mDatas.get(position);
                    file.delete();
                    mDatas.remove(position);
                    receiverAdapter.notifyDataSetChanged();

                    break;
                case R.id.btn_information:// 详情
                     showPopuWindow(v,mDatas.get(position).getAbsolutePath());
                    break;
                case R.id.btn_cancle:// 取消

                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 弹出popuwindow
     * @param view
     * @param filePath
     */
    private void showPopuWindow(View view,String filePath) {
        menuWindow2 = new FileInformationWindow(FileReceiverActivity.this,filePath);
        // 显示窗口
        menuWindow2.showAtLocation(view,  Gravity.CENTER, 0, 0);
    }

    /**
     * 为文件名设置图标
     *
     * @param v
     * @param filePath
     */
    public void setFileIcon(ImageView v, String filePath) {
        if (filePath == null) {
            v.setImageResource(R.drawable.ic_file);
            return;
        }
        String[] fileTypes = new String[]{"apk", "avi", "bat", "bin", "bmp", "chm", "css", "dat", "dll", "doc", "docx",
                "dos", "dvd", "gif", "html", "ifo", "inf", "iso", "java", "jpeg", "jpg", "log", "m4a", "mid", "mov",
                "movie", "mp2", "mp2v", "mp3", "mp4", "mpe", "mpeg", "mpg", "pdf", "php", "png", "ppt", "pptx", "psd",
                "rar", "tif", "ttf", "txt", "wav", "wma", "wmv", "xls", "xlsx", "xml", "xsl", "zip"};
        int pointIndex = filePath.lastIndexOf(".");
        if (pointIndex != -1) {
            String type = filePath.substring(pointIndex + 1).toLowerCase();
            for (int i = 0; i < fileTypes.length; i++) {
                if (type.equals(fileTypes[i])) {
                    try {
                        int resId = getResources().getIdentifier(type, "drawable", mContext.getPackageName());
                        v.setImageResource(resId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }
}
