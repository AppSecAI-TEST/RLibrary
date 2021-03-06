package com.angcyo.uiview.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.angcyo.uiview.R;
import com.angcyo.uiview.container.ContentLayout;
import com.angcyo.uiview.container.UIParam;
import com.angcyo.uiview.recycler.RRecyclerView;
import com.angcyo.uiview.recycler.adapter.RBaseAdapter;
import com.angcyo.uiview.recycler.adapter.RExBaseAdapter;
import com.angcyo.uiview.rsen.BasePointRefreshView;
import com.angcyo.uiview.rsen.RGestureDetector;
import com.angcyo.uiview.rsen.RefreshLayout;
import com.angcyo.uiview.widget.viewpager.UIViewPager;

import java.util.List;

/**
 * Created by angcyo on 2017-03-11.
 */

public abstract class UIRecyclerUIView<H, T, F> extends UIContentView
        implements RefreshLayout.OnRefreshListener, RBaseAdapter.OnAdapterLoadMoreListener {

    /**
     * 刷新控件
     */
    protected RefreshLayout mRefreshLayout;
    /**
     * 列表
     */
    protected RRecyclerView mRecyclerView;
    protected RExBaseAdapter<H, T, F> mExBaseAdapter;

    protected int mBaseOffsetSize;
    protected int mBaseLineSize;
    protected int page = 1;

    @NonNull
    @Override
    protected LayoutState getDefaultLayoutState() {
        return LayoutState.LOAD;
    }

    @Override
    public void onViewCreate(View rootView, UIParam param) {
        super.onViewCreate(rootView, param);
        mBaseOffsetSize = getDimensionPixelOffset(R.dimen.base_xhdpi);
        mBaseLineSize = getDimensionPixelOffset(R.dimen.base_line);
    }

    @Override
    final protected void inflateContentLayout(ContentLayout baseContentLayout, LayoutInflater inflater) {
        beforeInflateView(baseContentLayout);

        createRecyclerRootView(baseContentLayout, inflater);

        afterInflateView(baseContentLayout);

        baseInitLayout();
    }

    /**
     * 复写此方法, 重写根布局
     */
    protected void createRecyclerRootView(ContentLayout baseContentLayout, LayoutInflater inflater) {
        mRefreshLayout = new RefreshLayout(mActivity);
        initRefreshLayout(mRefreshLayout, baseContentLayout);
        mRecyclerView = new RRecyclerView(mActivity);
        initRecyclerView(mRecyclerView, baseContentLayout);
    }

    /**
     * 自动监听滚动事件, 设置标题栏的透明度
     */
    protected boolean hasScrollListener() {
        return false;
    }

    /**
     * 双击自动自动滚动置顶
     */
    public void onDoubleScrollToTop() {
        if (mRecyclerView != null) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    private void baseInitLayout() {
        if (getUITitleBarContainer() != null) {
            //双击标题, 自动滚动到顶部
            RGestureDetector.onDoubleTap(getUITitleBarContainer(), new RGestureDetector.OnDoubleTapListener() {
                @Override
                public void onDoubleTap() {
                    onDoubleScrollToTop();
                }
            });
        }

        if (mRecyclerView != null && hasScrollListener()) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (mUITitleBarContainer != null) {
                        mUITitleBarContainer.evaluateBackgroundColorSelf(recyclerView.computeVerticalScrollOffset());
                    }
                }
            });
        }
    }

    /**
     * 填充试图之前调用
     */
    protected void beforeInflateView(ContentLayout baseContentLayout) {

    }

    /**
     * 内容试图填充之后调用
     */
    protected void afterInflateView(ContentLayout baseContentLayout) {

    }

    protected void initRecyclerView(RRecyclerView recyclerView, ContentLayout baseContentLayout) {
        if (recyclerView == null) {
            return;
        }
        final RecyclerView.ItemDecoration itemDecoration = createItemDecoration();
        if (itemDecoration != null) {
            recyclerView.addItemDecoration(itemDecoration);
        }
        mExBaseAdapter = createAdapter();
        recyclerView.setAdapter(mExBaseAdapter);

        if (mExBaseAdapter != null) {
            mExBaseAdapter.setOnLoadMoreListener(this);
        }

        if (recyclerView.getParent() == null) {
            if (mRefreshLayout == null) {
                baseContentLayout.addView(recyclerView, new ViewGroup.LayoutParams(-1, -1));
            } else {
                mRefreshLayout.addView(recyclerView, new ViewGroup.LayoutParams(-1, -1));
            }
        }
    }

    protected abstract RExBaseAdapter<H, T, F> createAdapter();

    protected RecyclerView.ItemDecoration createItemDecoration() {
        return null;
    }

    protected void initRefreshLayout(RefreshLayout refreshLayout, ContentLayout baseContentLayout) {
        if (refreshLayout == null) {
            return;
        }
        refreshLayout.setRefreshDirection(RefreshLayout.TOP);
        refreshLayout.addOnRefreshListener(this);
        refreshLayout.setTopView(new BasePointRefreshView(mActivity));
        refreshLayout.setBottomView(new BasePointRefreshView(mActivity));
        if (refreshLayout.getParent() == null) {
            baseContentLayout.addView(refreshLayout, new ViewGroup.LayoutParams(-1, -1));
        }
    }

    /**
     * 刷新控件, 刷新事件回调
     */
    @Override
    public void onRefresh(@RefreshLayout.Direction int direction) {
        if (direction == RefreshLayout.TOP) {
            //刷新事件
            onBaseLoadData();
        } else if (direction == RefreshLayout.BOTTOM) {
            //加载更多事件
            onBaseLoadMore();
        }
    }

    public void onBaseLoadMore() {
        page++;
        onUILoadData(page);
    }

    public void onBaseLoadData() {
        page = 1;
        onUILoadData(page);
    }

    public void onUILoadData(int page) {
        showLoadView();
    }

    public boolean isShowInViewPager() {
        return false;
    }

    /**
     * 重置UI状态
     */
    public void resetUI() {
        hideLoadView();
        if (mRefreshLayout != null) {
            mRefreshLayout.setRefreshEnd();
        }
        if (mExBaseAdapter != null) {
            if (mExBaseAdapter.isEnableLoadMore()) {
                mExBaseAdapter.setLoadMoreEnd();
            }
        }
    }

    /**
     * 调用此方法自动设置视图
     */
    public void onUILoadFinish(List<T> datas) {
        resetUI();
        if (datas == null || datas.isEmpty()) {
            onUILoadEmpty();
        } else {
            showContentLayout();
            if (mExBaseAdapter != null) {
                if (isUIFirstLoadData()) {
                    mExBaseAdapter.resetAllData(datas);
                } else {
                    mExBaseAdapter.appendAllData(datas);
                }

                if (mExBaseAdapter.isEnableLoadMore()) {
                    if (isUIHaveLoadMore(datas)) {
                        mExBaseAdapter.setLoadMoreEnd();
                    } else {
                        mExBaseAdapter.setNoMore(true);
                    }
                }
            }
        }
    }

    /**
     * 数据为空时,回调
     */
    public void onUILoadEmpty() {
        showEmptyLayout();
    }

    @Override
    public void showNonetLayout() {
        showNonetLayout(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadLayout();
                onBaseLoadData();
            }
        });
    }

    public boolean isUIFirstLoadData() {
        return page == 1;
    }

    public boolean isUIHaveLoadMore(List<T> datas) {
        return datas != null && datas.size() > 20;
    }

    @Override
    public void onViewShowFirst(Bundle bundle) {
        super.onViewShowFirst(bundle);
        if (getDefaultLayoutState() == LayoutState.LOAD && !isShowInViewPager()) {
            onBaseLoadData();
        }
    }

    @Override
    public void onShowInPager(UIViewPager viewPager) {
        super.onShowInPager(viewPager);
        if (getDefaultLayoutState() == LayoutState.LOAD && isShowInViewPager()) {
            onBaseLoadData();
        }
    }

    /**
     * adapter加载更多回调
     */
    @Override
    public void onAdapterLodeMore(RBaseAdapter baseAdapter) {
        onBaseLoadMore();
    }
}
