package com.lsjr.zizi.mvp.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.lsjr.zizi.chat.db.ChatMessage;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.loader.ImageLoader;
import com.ymz.baselibrary.utils.L_;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class DataService extends IntentService {
    public DataService() {
        super("");
    }

    public static void startService(Context context, List datas, String subtype) {
        Intent intent = new Intent(context, DataService.class);
        intent.putParcelableArrayListExtra("data", (ArrayList) datas);
        intent.putExtra("subtype", subtype);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        List datas = intent.getParcelableArrayListExtra("data");
        String subtype = intent.getStringExtra("subtype");
        handleGirlItemData(datas, subtype);
    }

    private void handleGirlItemData(List<ChatMessage> datas, String subtype) {
        if (datas.size() == 0) {
            EventBus.getDefault().post("finish");
            return;
        }
        for (ChatMessage chatMessage: datas) {
            if (chatMessage.getType()== XmppMessage.TYPE_IMAGE){
                Bitmap bitmap = ImageLoader.load(chatMessage.getContent());
                chatMessage.setWidth(bitmap.getWidth());
                chatMessage.setHeight(bitmap.getHeight());
                L_.e("得到宽高----->bitmap.getWidth()"+bitmap.getWidth()+"bitmap.getHeight()"+bitmap.getHeight());
            }
            //data.setSubtype(subtype);
        }
        EventBus.getDefault().post(datas);
    }
}