package com.lsjr.zizi.mvp.upload;

import com.ymz.baselibrary.mvp.BasePresenter;

import java.io.File;
import java.util.List;
import java.util.Map;


public class UploadPresenter extends BasePresenter<IUploadView> {
    public UploadPresenter(IUploadView mvpView) {
        super(mvpView);
    }

    public void onUploadImage(Map<String,String> map,List <File> fileList){

    }

}
