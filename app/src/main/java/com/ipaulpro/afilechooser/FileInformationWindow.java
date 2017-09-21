package com.ipaulpro.afilechooser;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sk.weichat.R;

import java.io.File;
import java.io.FileInputStream;

/**
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui.message
 * @作者:王阳
 * @创建时间: 2015年10月21日 下午4:51:59
 * @描述: 文件详细信息
 * @SVN版本号: $Rev: 2143 $
 * @修改人: $Author: luorc $
 * @修改时间: $Date: 2015-10-23 09:31:46 +0800 (Fri, 23 Oct 2015) $
 * @修改的内容: TODO
 */
public class FileInformationWindow extends PopupWindow {
    private View mMenuView;
    private ImageView mIcon;
    private TextView mName, mSize,mPath;
    private Context mContext;

    public FileInformationWindow(Activity context, String filePath) {
        super(context);
        this.mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.file_information, null);
        mIcon = (ImageView) mMenuView.findViewById(R.id.iv_file_icon);
        mName = (TextView) mMenuView.findViewById(R.id.tv_file_name);
        mSize = (TextView) mMenuView.findViewById(R.id.tv_file_size);
        mPath = (TextView) mMenuView.findViewById(R.id.tv_file_path);


        // 设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        // 设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        // 设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        // 设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        // 设置SelectPicPopupWindow弹出窗体动画效果
//        this.setAnimationStyle(R.style.Buttom_Popwindow);
        // 实例化一个ColorDrawable颜色为半透明
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        // 设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);


        setFileIcon(mIcon, filePath);
        if (filePath != null) {
            int start = filePath.lastIndexOf("/");
            String fileName = filePath.substring(start + 1).toLowerCase();
            mName.setText("文件名:"+fileName);
            File file=new File(filePath);
            try {

               mSize.setText("大小:"+Formatter.formatFileSize(mContext,getFileSize(file)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int bottom = mMenuView.findViewById(R.id.pop_layout).getBottom();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    } else if (y > bottom) {
                        dismiss();
                    }

                }
                return true;
            }
        });

    }

    private static long getFileSize(File file) throws Exception {
        long size = 0;
            FileInputStream fis = null;
        if (file.exists()) {
            fis = new FileInputStream(file);
            size = fis.available();
        }
        fis.close();
        return size;
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
                        int resId = mContext.getResources().getIdentifier(type, "drawable", mContext.getPackageName());
                        v.setImageResource(resId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }
}
