package com.lsjr.zizi.mvp.home.photo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.andview.listener.OnItemClickListener;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.view.FileOpenWays;
import com.ymz.baselibrary.AppCache;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.UIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;

/**
 * 查看接收文件
 */
public class FileReceiverActivity extends MvpActivity {


    @BindView(R.id.lv_file_receiver)
    RecyclerView lvFileReceiver;
    private List<File> mDatas;
    //private SelectFileWindow menuWindow;
    private FileReceiverAdapter receiverAdapter;
    private ChatMessage message;
   // private FileInformationWindow menuWindow2;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_receiver_file;
    }

    @Override
    protected void initView() {
        setTitleText("以下载文件");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mDatas=new ArrayList<>();
        receiverAdapter = new FileReceiverAdapter(UIUtils.getContext(),mDatas,R.layout.item_file_receive);
        lvFileReceiver.setLayoutManager(new LinearLayoutManager(UIUtils.getContext()){
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        lvFileReceiver.setAdapter(receiverAdapter);
        loadData();
    }

    private void loadData() {
        mDatas.clear();
        message = getIntent().getParcelableExtra(Constants.INSTANT_MESSAGE);
       // File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sk");
        File f = new File(AppCache.getInstance().mFilesDir);
        if (f != null) {
            File[] files = f.listFiles();
            mDatas.addAll(Arrays.asList(files));
        }
        receiverAdapter.notifyDataSetChanged();
        /* 长按弹出转发,删除,取消等操作*/
//        receiverAdapter.setOnItemClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Vibrator vib = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
//                vib.vibrate(40);// 只震动一秒，一次
//                menuWindow = new SelectFileWindow(FileReceiverActivity.this, new GestureDetector.ClickListener(position, mDatas.get(position)));
//                // 显示窗口
//                menuWindow.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
//
//                return false;
//            }
//        });
        /* 点击打开指定方式*/
        receiverAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerHolder baseRecyclerHolder, int position, Object item) {
                FileOpenWays openWays = new FileOpenWays(FileReceiverActivity.this);
                openWays.openFiles(mDatas.get(position).getAbsolutePath());
            }
        });
    }



    public class FileReceiverAdapter extends ABaseRefreshAdapter<File>{

        public FileReceiverAdapter(Context context, List<File> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }

        @Override
        protected void convert(BaseRecyclerHolder convertView , File var2, int position) {
            ((TextView) convertView.getView(R.id.tv_filereceiver)).setText(var2.getName());
            ImageView iv = convertView.getView(R.id.iv_filereceiver);
            setFileIcon(iv, mDatas.get(position).getAbsolutePath());
        }
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
            for (String fileType : fileTypes) {
                if (type.equals(fileType)) {
                    int resId = getResources().getIdentifier(type, "drawable", getPackageName());
                    v.setImageResource(resId);

                }
            }
        }
    }
}
