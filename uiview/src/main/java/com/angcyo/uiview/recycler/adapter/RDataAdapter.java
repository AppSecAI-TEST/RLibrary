package com.angcyo.uiview.recycler.adapter;

import android.content.Context;

import com.angcyo.uiview.recycler.RBaseViewHolder;

import java.util.List;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：
 * 创建人员：Robi
 * 创建时间：2017/08/10 14:26
 * 修改人员：Robi
 * 修改时间：2017/08/10 14:26
 * 修改备注：
 * Version: 1.0.0
 */
public class RDataAdapter extends RExBaseAdapter<String, RBaseDataItem, String> {
    public RDataAdapter(Context context) {
        super(context);
    }

    public RDataAdapter(Context context, List<RBaseDataItem> datas) {
        super(context, datas);
    }

    @Override
    final public int getItemType(int position) {
        return super.getItemType(position);
    }

    @Override
    final protected int getHeaderItemType(int posInHeader) {
        return super.getHeaderItemType(posInHeader);
    }

    /**
     * 对应的ItemType
     */
    @Override
    protected int getDataItemType(int posInData) {
        return getAllDatas().get(posInData).getDataItemType();
    }

    @Override
    final protected int getFooterItemType(int posInFooter) {
        return super.getFooterItemType(posInFooter);
    }

    /**
     * 对应的LayoutId
     */
    @Override
    protected int getItemLayoutId(int viewType) {
        for (RBaseDataItem item : getAllDatas()) {
            if (item.getDataItemType() == viewType) {
                return item.getItemLayoutId();
            }
        }
        return super.getItemLayoutId(viewType);
    }


    /**
     * 绑定视图数据
     */
    @Override
    protected void onBindDataView(RBaseViewHolder holder, int posInData, RBaseDataItem dataBean) {
        super.onBindDataView(holder, posInData, dataBean);
        dataBean.onBindDataView(this, holder, posInData);
    }
}
