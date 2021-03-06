package com.angcyo.uiview.view;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.R;
import com.angcyo.uiview.RApplication;
import com.angcyo.uiview.base.UILayoutActivity;
import com.angcyo.uiview.container.ILayout;
import com.angcyo.uiview.container.UILayoutImpl;
import com.angcyo.uiview.container.UIParam;
import com.angcyo.uiview.model.TitleBarPattern;
import com.angcyo.uiview.recycler.RBaseViewHolder;
import com.angcyo.uiview.recycler.RRecyclerView;
import com.angcyo.uiview.resources.AnimUtil;
import com.angcyo.uiview.skin.ISkin;
import com.angcyo.uiview.utils.RUtils;
import com.angcyo.uiview.utils.ThreadExecutor;
import com.angcyo.uiview.widget.RSoftInputLayout;
import com.angcyo.uiview.widget.viewpager.UIViewPager;

import java.util.ArrayList;
import java.util.List;

import static com.angcyo.uiview.RCrashHandler.getMemoryInfo;

/**
 * 接口的实现, 仅处理了一些动画, 其他实现都为空
 * 对对话框做了区分处理
 * <p>
 * Created by angcyo on 2016-11-12.
 */

public abstract class UIIViewImpl implements IView {

    public static final int DEFAULT_ANIM_TIME = 200;
    public static final int DEFAULT_FINISH_ANIM_TIME = 200;
    public static final int DEFAULT_DIALOG_FINISH_ANIM_TIME = 150;
    public static final int DEFAULT_CLICK_DELAY_TIME = 300;

    protected ILayout mILayout;
    protected ILayout mParentILayout;//上层ILayout,
    protected ILayout mChildILayout;//用来管理上层IView的生命周期, 如果有值, 会等于mILayout
    protected UILayoutActivity mActivity;
    /**
     * 根布局
     */
    protected View mRootView;

    /**
     * 用来管理rootView
     */
    protected RBaseViewHolder mViewHolder;
    protected IViewShowState mIViewStatus = IViewShowState.STATE_NORMAL;
    /**
     * 最后一次显示的时间
     */
    protected long mLastShowTime = 0;
    protected List<ILifecycle> mILifecycleList = new ArrayList<>();
    /**
     * 当{@link #onViewShow(Bundle)}被调用一次, 计数器就会累加
     */
    protected long viewShowCount = 0;
    protected long showInPagerCount = 0;
    protected int mBaseSoftInputMode;
    protected OnUIViewListener mOnUIViewListener;
    private boolean mIsRightJumpLeft = false;

    public static void setDefaultConfig(Animation animation, boolean isFinish) {
        if (isFinish) {
            animation.setDuration(DEFAULT_FINISH_ANIM_TIME);
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setFillAfter(true);
        } else {
            animation.setDuration(DEFAULT_ANIM_TIME);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setFillAfter(false);
        }
        animation.setFillBefore(true);
    }

    /**
     * 判断设备是否是低端
     */
    public static boolean isLowDevice() {
        boolean isLowMen = true;
        try {
            final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo(RApplication.getApp());
            isLowMen = memoryInfo.totalMem < 1000 * 1000 * 1000 * 2L;//小于2G的内存
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!isLollipop()) {
            return true;
        }
        return isLowMen;
    }

    public static boolean isHighDevice() {
        boolean isHighMen = true;
        try {
            final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo(RApplication.getApp());
            isHighMen = memoryInfo.totalMem >= 1000 * 1000 * 1000 * 3.8f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isHighMen;
    }

    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public TitleBarPattern loadTitleBar(Context context) {
        L.d(this.getClass().getSimpleName(), "loadTitleBar: ");
        return null;
    }

    @Override
    public void onAttachedToILayout(ILayout iLayout) {
        mILayout = iLayout;
        if (mChildILayout != null) {
            mILayout.setChildILayout(mChildILayout);
        }
        if (mParentILayout == null) {
            mParentILayout = iLayout;
        }
    }

    @Override
    public View inflateContentView(UILayoutActivity activity, ILayout iLayout, FrameLayout container, LayoutInflater inflater) {
        L.d(this.getClass().getSimpleName(), "inflateContentView: ");
        mActivity = activity;
        return inflateBaseView(container, inflater);
    }

    protected abstract View inflateBaseView(FrameLayout container, LayoutInflater inflater);

    @CallSuper
    @Override
    public void loadContentView(View rootView) {
        L.d(this.getClass().getSimpleName(), "loadContentView: ");
        //不使用 butterknife
//        try {
//            ButterKnife.bind(this, rootView);
//        } catch (Exception e) {
//            String message = e.getMessage();
//        }
    }

    /**
     * 开始布局动画
     */
    public void startLayoutAnim(View parent) {
        final Animation layoutAnimation = loadLayoutAnimation();
        if (layoutAnimation != null && parent instanceof ViewGroup) {
            AnimUtil.applyLayoutAnimation(findChildViewGroup((ViewGroup) parent), layoutAnimation);
        }
    }

    /**
     * 查找具有多个子View的ViewGroup, 用来播放布局动画
     */
    private ViewGroup findChildViewGroup(ViewGroup parent) {
        final int childCount = parent.getChildCount();
        if (childCount == 1) {
            View childAt = parent.getChildAt(0);
            if (childAt instanceof ViewGroup) {
                return findChildViewGroup((ViewGroup) childAt);
            } else {
                return parent;
            }
        } else {
            return parent;
        }
    }

    @CallSuper
    @Override
    @Deprecated
    public void onViewCreate(View rootView) {
        L.d(this.getClass().getSimpleName(), "onViewCreate: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_CREATE;
        mRootView = rootView;
        mViewHolder = new RBaseViewHolder(mRootView);
    }

    @Override
    public void onViewCreate(View rootView, UIParam param) {
        L.d(this.getClass().getSimpleName(), "onViewCreate 2: " + mIViewStatus);
        mBaseSoftInputMode = mActivity.getWindow().getAttributes().softInputMode;
    }

    @CallSuper
    @Override
    public void onViewLoad() {
        L.d(this.getClass().getSimpleName(), "onViewLoad: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_LOAD;
    }

    @Deprecated
    @Override
    @CallSuper
    public void onViewShow() {
        onViewShow(null);
    }

    @CallSuper
    @Override
    public void onViewShow(Bundle bundle) {
        L.d(this.getClass().getSimpleName(), "onViewShow: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_SHOW;
        long lastShowTime = mLastShowTime;
        viewShowCount++;
        mLastShowTime = System.currentTimeMillis();

        for (ILifecycle life : mILifecycleList) {
            life.onLifeViewShow();
        }

        if (lastShowTime == 0) {
            onViewShowFirst(bundle);
        } else {
            onViewShowNotFirst(bundle);
        }

        onViewShow(viewShowCount);
        if (mChildILayout != null) {
            mChildILayout.onLastViewShow(bundle);
        }

    }

    public void onViewShowFirst(Bundle bundle) {
        L.v(this.getClass().getSimpleName(), "onViewShowFirst: ");
    }

    public void onViewShowNotFirst(Bundle bundle) {
        L.v(this.getClass().getSimpleName(), "onViewShowNotFirst: ");
    }

    //星期五 2017-2-17
    public void onViewShow(long viewShowCount) {
        L.v(this.getClass().getSimpleName(), "onViewShowCount " + viewShowCount);
    }

    @CallSuper
    @Override
    public void onViewReShow(Bundle bundle) {
        L.d(this.getClass().getSimpleName(), "onViewReShow: " + mIViewStatus);

        if (mChildILayout != null) {
            mChildILayout.onLastViewReShow(bundle);
        }
    }

    @CallSuper
    @Override
    public void onViewHide() {
        L.d(this.getClass().getSimpleName(), "onViewHide: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_HIDE;

        for (ILifecycle life : mILifecycleList) {
            life.onLifeViewHide();
        }

        if (mChildILayout != null) {
            mChildILayout.onLastViewHide();
        }
    }

    @CallSuper
    @Override
    public void onViewUnload() {
        L.d(this.getClass().getSimpleName(), "onViewUnload: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_UNLOAD;
        if (mOnUIViewListener != null) {
            mOnUIViewListener.onViewUnload(this);
        }
    }

    @Override
    public Animation loadStartAnimation() {
        L.v(this.getClass().getSimpleName(), "loadStartAnimation: " + mIViewStatus);
        TranslateAnimation translateAnimation;
        if (mIsRightJumpLeft) {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.99f, Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        } else {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.99f, Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        }
        setDefaultConfig(translateAnimation, false);
        return translateAnimation;
    }

    @Override
    public Animation loadFinishAnimation() {
        L.v(this.getClass().getSimpleName(), "loadFinishAnimation: ");
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        setDefaultConfig(translateAnimation, true);
        return translateAnimation;
    }

    @Override
    public Animation loadShowAnimation() {
        L.v(this.getClass().getSimpleName(), "loadShowAnimation: ");
        return loadStartAnimation();
    }

    @Override
    public Animation loadHideAnimation() {
        L.v(this.getClass().getSimpleName(), "loadHideAnimation: ");
        return loadFinishAnimation();
    }

    @Override
    public Animation loadOtherExitAnimation() {
        L.v(this.getClass().getSimpleName(), "loadOtherExitAnimation: ");
        TranslateAnimation translateAnimation;
        if (mIsRightJumpLeft) {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        } else {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1f,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        }
        setDefaultConfig(translateAnimation, false);
        return translateAnimation;
    }

    @Override
    public Animation loadOtherEnterAnimation() {
        L.v(this.getClass().getSimpleName(), "loadOtherEnterAnimation: ");
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        setDefaultConfig(translateAnimation, false);
        return translateAnimation;
    }

    @Override
    public Animation loadOtherHideAnimation() {
        return loadOtherExitAnimation();
    }

    @Override
    public Animation loadOtherShowAnimation() {
        return loadOtherEnterAnimation();
    }

    @Override
    public Animation loadLayoutAnimation() {
        L.v(this.getClass().getSimpleName(), "loadLayoutAnimation: ");
//        if (mIsRightJumpLeft) {
//
//        } else {
//
//        }
//
//        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1f,
//                Animation.RELATIVE_TO_PARENT, 0f,
//                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f);
//        setDefaultConfig(translateAnimation);
//        return translateAnimation;
        return null;
    }

    @Override
    public boolean isDialog() {
        return false;
    }

    @Override
    public boolean isDimBehind() {
        return true;
    }

    @Override
    public boolean canCanceledOnOutside() {
        return true;
    }

    @Override
    public boolean canTouchOnOutside() {
        return true;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

    @Override
    public View getView() {
        return mRootView;
    }

    @Override
    public View getDialogDimView() {
        //请在对话框中实现
        return null;
    }

    /**
     * 在inflateContentView之前调用, 返回的都是null
     */
    @Override
    public ILayout getILayout() {
        return mILayout;
    }

    /**
     * 此方法只在UIVIewPager中会调用, 当前IView显示时
     */
    @Override
    public void onShowInPager(UIViewPager viewPager) {
        showInPagerCount++;
        L.i(this.getClass().getSimpleName(), "onShowInPager: " + showInPagerCount);
    }

    /**
     * 此方法只在UIVIewPager中会调, 当前IView隐藏时
     */
    @Override
    public void onHideInPager(UIViewPager viewPager) {
        L.i(this.getClass().getSimpleName(), "onHideInPager: ");
    }

    public void startIView(final IView iView) {
        startIView(iView, true);
    }

    public void startIView(final IView iView, boolean anim) {
        startIView(iView, new UIParam(anim));
    }

    public void startIView(final IView iView, final UIParam param) {
        if (iView == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        mILayout.startIView(iView, param);
    }

    public void finishIView() {
        finishIView(this);
    }

    public void finishIView(final IView iView) {
        finishIView(iView, true);
    }

    public void finishIView(final IView iView, final UIParam param) {
        if (iView == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        mILayout.finishIView(iView, param);
    }

    public void finishIView(final IView iView, boolean anim) {
        finishIView(iView, anim, false);
    }

    public void finishIView(final IView iView, boolean anim, boolean quiet) {
        if (iView == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        mILayout.finishIView(iView, anim, quiet);
    }

    public void showIView(final View view) {
        showIView(view, true);
    }

    public void showIView(final View view, final boolean needAnim) {
        showIView(view, needAnim, null);
    }

    public void showIView(final View view, final boolean needAnim, final Bundle bundle) {
        if (view == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        mILayout.showIView(view, new UIParam(needAnim).setBundle(bundle));
    }

    public void showIView(IView iview, boolean needAnim) {
        showIView(iview, needAnim, null);
    }

    public void showIView(IView iview) {
        showIView(iview, true);
    }

    public void showIView(IView iview, boolean needAnim, Bundle bundle) {
        if (iview == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        mILayout.showIView(iview, new UIParam(needAnim).setBundle(bundle));
    }

    public void replaceIView(IView iView, boolean needAnim) {
        replaceIView(iView, new UIParam(needAnim));
    }

    public void replaceIView(IView iView) {
        replaceIView(iView, true);
    }

    public void replaceIView(IView iView, UIParam param) {
        if (iView == null) {
            return;
        }
        if (mILayout == null) {
            throw new IllegalArgumentException("ILayout 还未初始化");
        }
        param.setReplaceIView(this);
        mILayout.replaceIView(iView, param);
    }

    public void post(Runnable action) {
        if (mRootView != null) {
            mRootView.post(action);
        }
    }

    public void postDelayed(Runnable action, long delayMillis) {
        if (mRootView != null) {
            mRootView.postDelayed(action, delayMillis);
        }
    }

    public void postDelayed(long delayMillis, Runnable action) {
        if (mRootView != null) {
            mRootView.postDelayed(action, delayMillis);
        }
    }

    public void removeCallbacks(Runnable action) {
        if (mRootView != null) {
            mRootView.removeCallbacks(action);
        }
    }

    /**
     * @return true 允许退出
     */
    @Override
    public boolean onBackPressed() {
        return true;
    }

    @Override
    public boolean canSwipeBackPressed() {
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public IView setIsRightJumpLeft(boolean isRightJumpLeft) {
        mIsRightJumpLeft = isRightJumpLeft;
        return this;
    }

    @Override
    public int getDimColor() {
        return Color.parseColor("#60000000");
    }

    @Override
    public View getAnimView() {
        return mRootView;
    }

    @Override
    public IView bindParentILayout(ILayout otherILayout) {
        mParentILayout = otherILayout;
        return this;
    }

    @Override
    public boolean haveParentILayout() {
        return mParentILayout != mILayout;
    }

    @Override
    public boolean haveChildILayout() {
        return mChildILayout != mILayout;
    }

    @Override
    public boolean canTryCaptureView() {
        return true;
    }

    public Resources getResources() {
        if (mActivity == null) {
            return RApplication.getApp().getResources();
        }
        return mActivity.getResources();
    }

    //星期二 2017-2-28
    public String getString(@StringRes int id) {
        return getResources().getString(id);
    }

    //星期二 2017-2-28
    public String getString(@StringRes int id, Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }

    /**
     * 是否全屏
     *
     * @param enable 是
     */
    //星期三 2017-3-1
    public void fullscreen(boolean enable) {
        fullscreen(enable, false);
    }

    public void fullscreen(final boolean enable, boolean checkSdk) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final View decorView = mActivity.getWindow().getDecorView();
                if (enable) {
                    decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_FULLSCREEN);
                } else {
                    decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        };

        if (checkSdk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                runnable.run();
            }
        } else {
            runnable.run();
        }
    }

    //星期一 2017-3-13
    @ColorInt
    public int getColor(@ColorRes int id) {
        return ContextCompat.getColor(mActivity, id);
    }

    //2017-06-10
    public ColorStateList getColorList(@ColorRes int id) {
        return ContextCompat.getColorStateList(mActivity, id);
    }

    public Drawable getDrawable(@DrawableRes int id) {
        return ContextCompat.getDrawable(mActivity, id);//.mutate();
    }

    public int getDimensionPixelOffset(@DimenRes int id) {
        return getResources().getDimensionPixelOffset(id);
    }

    public float density() {
        return getResources().getDisplayMetrics().density;
    }

    public float scaledDensity() {
        return getResources().getDisplayMetrics().scaledDensity;
    }

    public int widthPixels() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    public int heightPixels() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 冻结界面, 拦截所有Touch事件
     */
    public void setLayoutFrozen(boolean frozen) {
        if (mRootView != null) {
            mRootView.setEnabled(frozen);
        }
    }

    @Override
    public boolean showOnDialog() {
        return false;
    }

    @Override
    public boolean canDoubleCancel() {
        return false;
    }

    @Override
    public void onSkinChanged(ISkin skin) {
        L.v(this.getClass().getSimpleName(), "onSkinChanged: " + skin.skinName());
        notifySkinChanged(mRootView, skin);
    }

    @Override
    public IViewShowState getIViewShowState() {
        return mIViewStatus;
    }

    public <T extends View> T v(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.v(id);
    }

    public void click(@IdRes int id, final View.OnClickListener listener) {
        v(id).setOnClickListener(new RClickListener(DEFAULT_CLICK_DELAY_TIME) {
            @Override
            public void onRClick(View view) {
                if (listener != null) {
                    listener.onClick(view);
                }
            }
        });
    }

    public void setChildILayout(ILayout childILayout) {
        mChildILayout = childILayout;
        if (mILayout != null) {
            mILayout.setChildILayout(mChildILayout);
        }
    }

    protected void notifySkinChanged(View view, ISkin skin) {
        if (view != null) {
            if (view instanceof UILayoutImpl) {
                ((UILayoutImpl) view).onSkinChanged(skin);
            }
            if (view instanceof RecyclerView) {
                RRecyclerView.ensureGlow(((RecyclerView) view), skin.getThemeSubColor());
            }
            if (view instanceof ViewPager) {
                UIViewPager.ensureGlow((ViewPager) view, skin.getThemeSubColor());
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < (viewGroup).getChildCount(); i++) {
                    notifySkinChanged(viewGroup.getChildAt(i), skin);
                }
            }
        }
    }

    public float getTitleBarHeight() {
//        float density = getResources().getDisplayMetrics().density;
        if (isLollipop()) {
            return getDimensionPixelOffset(R.dimen.title_bar_height);
            //return density * 65f;
        } else {
            //return density * 40f;
            return getDimensionPixelOffset(R.dimen.action_bar_height);
        }
    }

    /**
     * 注册IView生命周期的回调
     */
    public void registerLifecycler(ILifecycle lifecycle) {
        if (!mILifecycleList.contains(lifecycle)) {
            mILifecycleList.add(lifecycle);
        }
    }

    /**
     * 保持屏幕常亮
     */
    public void keepScreenOn(boolean keep) {
        if (mActivity != null) {
            Window window = mActivity.getWindow();
            if (keep) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    /**
     * 强制主线程执行
     */
    public void runOnUiThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (RUtils.isMainThread()) {
            runnable.run();
        } else {
            if (mActivity == null) {
                ThreadExecutor.instance().onMain(runnable);
            } else {
                mActivity.runOnUiThread(runnable);
            }
        }
    }

    /**
     * 设置窗口键盘弹出模式
     */
    public void adjustPan(boolean adjust) {
        if (adjust) {
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        } else {
            mActivity.getWindow().setSoftInputMode(mBaseSoftInputMode);
        }
    }

    public UIIViewImpl setOnUIViewListener(OnUIViewListener onUIViewListener) {
        mOnUIViewListener = onUIViewListener;
        return this;
    }

    /**
     * 显示软键盘
     *
     * @param editText 尽量是EditText
     */
    public void showSoftInput(final View editText) {
        RSoftInputLayout.showSoftInput(editText);
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftInput(final View view) {
        RSoftInputLayout.hideSoftInput(view);
    }

    @Override
    public boolean needTransitionStartAnim() {
        return false;
    }

    @Override
    public boolean needTransitionExitAnim() {
        return false;
    }
}
