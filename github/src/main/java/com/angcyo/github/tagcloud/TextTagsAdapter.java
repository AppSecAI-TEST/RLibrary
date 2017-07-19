package com.angcyo.github.tagcloud;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by moxun on 16/1/19.
 */
public class TextTagsAdapter extends TagsAdapter {

    private List<String> dataSet = new ArrayList<>();

    public TextTagsAdapter() {
    }


    public TextTagsAdapter(List<String> dataSet) {
        this.dataSet.addAll(dataSet);
    }

    public TextTagsAdapter(@NonNull String... data) {
        dataSet.clear();
        Collections.addAll(dataSet, data);
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    public void setDataSet(List<String> dataSet) {
        this.dataSet.addAll(dataSet);
    }

    @Override
    public View getView(Context context, int position, ViewGroup parent) {
        TextView tv = new TextView(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-2, -2);
        tv.setLayoutParams(lp);
        tv.setText(dataSet.get(position));
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    @Override
    public Object getItem(int position) {
        return dataSet.get(position);
    }

    @Override
    public int getPopularity(int position) {
        return position % 7;
    }

    @Override
    public void onThemeColorChanged(View view, int themeColor) {
        ((TextView) view).setTextColor(themeColor);
    }
}
