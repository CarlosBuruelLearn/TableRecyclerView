package com.app.feng.fixtablelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.app.feng.fixtablelayout.adapter.TableAdapter;
import com.app.feng.fixtablelayout.inter.IDataAdapter;
import com.app.feng.fixtablelayout.inter.ILoadMoreListener;
import com.app.feng.fixtablelayout.widget.SingleLineItemDecoration;
import com.app.feng.fixtablelayout.widget.TableLayoutManager;

import java.lang.ref.WeakReference;

/**
 * Created by feng on 2017/4/2
 */
public class FixTableLayout extends FrameLayout {

    public static final int MESSAGE_FIX_TABLE_LOAD_COMPLETE = 1001;

    RecyclerView recyclerView;
    HorizontalScrollView titleView;
    RecyclerView leftViews;
    TextView left_top_view;
    View leftViewShadow;
    FrameLayout fl_load_mask;

    int divider_height;
    int divider_color;
    int col_1_color;
    int col_2_color;
    int title_color;
    int item_width;
    int item_padding;
    int item_gravity;

    boolean show_left_shadow = false;
    private IDataAdapter dataAdapter;

    private boolean isLoading = false;
    private ILoadMoreListener loadMoreListener;
    private boolean hasMoreData = true;

    public void setLoadMoreListener(ILoadMoreListener loadMoreListener) {
        this.loadMoreListener = loadMoreListener;
    }

    public FixTableLayout(Context context) {
        this(context,null);
    }

    public FixTableLayout(
            Context context,@Nullable AttributeSet attrs) {
        this(context,attrs,0);
    }

    public FixTableLayout(Context context,@Nullable AttributeSet attrs,int defStyleAttr) {
        super(context,attrs,defStyleAttr);

        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.FixTableLayout);

        divider_height = array.getDimensionPixelOffset(
                R.styleable.FixTableLayout_fixtable_divider_height,4);
        divider_color = array.getColor(R.styleable.FixTableLayout_fixtable_divider_color,
                                       Color.BLACK);

        col_1_color = array.getColor(R.styleable.FixTableLayout_fixtable_column_1_color,Color.BLUE);
        col_2_color = array.getColor(R.styleable.FixTableLayout_fixtable_column_2_color,
                                     Color.WHITE);
        title_color = array.getColor(R.styleable.FixTableLayout_fixtable_title_color,Color.GRAY);
        item_width = array.getDimensionPixelOffset(R.styleable.FixTableLayout_fixtable_item_width,
                                                   400);
        item_padding = array.getDimensionPixelOffset(
                R.styleable.FixTableLayout_fixtable_item_top_bottom_padding,0);
        item_gravity = array.getInteger(R.styleable.FixTableLayout_fixtable_item_gravity,0);

        switch (item_gravity) {
            case 0:
                item_gravity = Gravity.CENTER;
                break;
            case 1:
                item_gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                break;
            case 2:
                item_gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                break;
        }

        show_left_shadow = array.getBoolean(
                R.styleable.FixTableLayout_fixtable_show_left_view_shadow,false);

        array.recycle();

        View view = inflate(context,R.layout.table_view,null);
        init(view);
        addView(view);
    }

    private void init(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        titleView = (HorizontalScrollView) view.findViewById(R.id.titleView);
        leftViews = (RecyclerView) view.findViewById(R.id.leftViews);
        left_top_view = (TextView) view.findViewById(R.id.left_top_view);
        leftViewShadow = view.findViewById(R.id.leftView_shadows);
        fl_load_mask = (FrameLayout) view.findViewById(R.id.load_mask);

        TableLayoutManager t1 = new TableLayoutManager();
        TableLayoutManager t2 = new TableLayoutManager();
//        Log.i("feng"," -- t : " + t1.toString().substring(54) + " t_left: " + t2.toString()
//                .substring(54));
        recyclerView.setLayoutManager(t1);
        leftViews.setLayoutManager(t2);

        leftViews.addItemDecoration(new SingleLineItemDecoration(divider_height,divider_color));
        leftViews.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v,MotionEvent event) {
                //将事件发送到RV
                recyclerView.onTouchEvent(event);
                return true;
            }
        });

        if (show_left_shadow) {
            leftViewShadow.setVisibility(VISIBLE);
        } else {
            leftViewShadow.setVisibility(GONE);
        }

        recyclerView.addItemDecoration(new SingleLineItemDecoration(divider_height,divider_color));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView,int dx,int dy) {
                super.onScrolled(recyclerView,dx,dy);
                titleView.scrollBy(dx,0);
                leftViews.scrollBy(0,dy);
            }
        });

    }

    public void setAdapter(
            IDataAdapter dataAdapter) {
        this.dataAdapter = dataAdapter;
        initAdapter();
    }

    int lastVisablePos = -1;

    public void enableLoadMoreData() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,int newState) {
                super.onScrollStateChanged(recyclerView,newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisablePos == recyclerView.getAdapter()
                        .getItemCount() - 1) {

                    if (!isLoading && hasMoreData) {
                        isLoading = true;
                        fl_load_mask.setVisibility(VISIBLE);

                        loadMoreListener.loadMoreData(
                                new FixTableHandler(FixTableLayout.this,recyclerView));
                    }
                }
                //                    Log.i("feng","滑动到底部 -- 此时的View Bottom：" + recyclerView.getLayoutManager()
                //                            .getDecoratedBottom
                //                            (bottomView) + " recyclerView Height:" +recyclerView.getHeight());

            }

            @Override
            public void onScrolled(RecyclerView recyclerView,int dx,int dy) {
                super.onScrolled(recyclerView,dx,dy);
                View bottomView = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                lastVisablePos = recyclerView.getChildAdapterPosition(bottomView);

            }
        });
    }

    private void initAdapter() {
        TableAdapter.Builder builder = new TableAdapter.Builder();
        TableAdapter tableAdapter = builder.setLeft_top_view(left_top_view)
                .setTitleView(titleView)
                .setParametersHolder(
                        new TableAdapter.ParametersHolder(col_1_color,col_2_color,title_color,
                                                          item_width,item_padding,item_gravity))
                .setLeftViews(leftViews)
                .setDataAdapter(dataAdapter)
                .create();
        recyclerView.setAdapter(tableAdapter);
    }

    static class FixTableHandler extends Handler {
        WeakReference<RecyclerView> recyclerViewWeakReference;
        WeakReference<FixTableLayout> fixTableLayoutWeakReference;

        public FixTableHandler(FixTableLayout fixTableLayout,RecyclerView recyclerView) {
            recyclerViewWeakReference = new WeakReference<>(recyclerView);
            fixTableLayoutWeakReference = new WeakReference<>(fixTableLayout);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_FIX_TABLE_LOAD_COMPLETE) {

                RecyclerView recyclerView = recyclerViewWeakReference.get();
                FixTableLayout fixTableLayout = fixTableLayoutWeakReference.get();
                TableAdapter tableAdapter = (TableAdapter) recyclerView.getAdapter();
                int startPos = tableAdapter.getItemCount() - 1;
                int loadNum = msg.arg1;
                if (msg.arg1 > 0) {
                    //通知Adapter更新数据
                    tableAdapter.notifyLoadData(startPos,loadNum);
//                    Log.i("fixtablelayout","load more completed loadNum :" + loadNum + "scrollTo " +
//                            ":" + fixTableLayout.lastVisableMask);

                }else{
                    //没有数据了
                    fixTableLayout.hasMoreData = false;
                }
                fixTableLayout.fl_load_mask.setVisibility(GONE);
                fixTableLayout.isLoading = false;
            }
        }
    }




    float zoom = 1.0F;
    float maxZoom = 2.0F;
    float smoothZoom = 1.0F;
    float zoomX;
    float zoomY;
    float smoothZoomX;
    float smoothZoomY;
    private boolean scrolling;
    private boolean showMinimap = false;
    private int miniMapColor = -16777216;
    private int miniMapHeight = -1;
    private String miniMapCaption;
    private float miniMapCaptionSize = 10.0F;
    private int miniMapCaptionColor = -1;
    private long lastTapTime;
    private float touchStartX;
    private float touchStartY;
    private float touchLastX;
    private float touchLastY;
    private float startd;
    private boolean pinching;
    private float lastd;
    private float lastdx1;
    private float lastdy1;
    private float lastdx2;
    private float lastdy2;
    private final Matrix m = new Matrix();
    private final Paint p = new Paint();
    ZoomViewListener listener;
    private Bitmap ch;

    public void smoothZoomTo(float zoom, float x, float y)
    {
        this.smoothZoom = clamp(1.0F, zoom, this.maxZoom);
        this.smoothZoomX = x;
        this.smoothZoomY = y;
        if (this.listener != null) {
            this.listener.onZoomStarted(this.smoothZoom, x, y);
        }
    }

    private float clamp(float min, float value, float max)
    {
        return Math.max(min, Math.min(value, max));
    }

    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (ev.getPointerCount() == 1) {
            processSingleTouchEvent(ev);
        }
        if (ev.getPointerCount() == 2) {
            processDoubleTouchEvent(ev);
        }
        getRootView().invalidate();
        invalidate();

        return true;
    }

    private void processDoubleTouchEvent(MotionEvent ev)
    {
        float x1 = ev.getX(0);
        float dx1 = x1 - this.lastdx1;
        this.lastdx1 = x1;
        float y1 = ev.getY(0);
        float dy1 = y1 - this.lastdy1;
        this.lastdy1 = y1;
        float x2 = ev.getX(1);
        float dx2 = x2 - this.lastdx2;
        this.lastdx2 = x2;
        float y2 = ev.getY(1);
        float dy2 = y2 - this.lastdy2;
        this.lastdy2 = y2;

        float d = (float)Math.hypot(x2 - x1, y2 - y1);
        float dd = d - this.lastd;
        this.lastd = d;
        float ld = Math.abs(d - this.startd);

        Math.atan2(y2 - y1, x2 - x1);
        switch (ev.getAction())
        {
            case 0:
                this.startd = d;
                this.pinching = false;
                break;
            case 2:
                if ((this.pinching) || (ld > 30.0F))
                {
                    this.pinching = true;
                    float dxk = 0.5F * (dx1 + dx2);
                    float dyk = 0.5F * (dy1 + dy2);
                    smoothZoomTo(Math.max(1.0F, this.zoom * d / (d - dd)), this.zoomX - dxk / this.zoom, this.zoomY - dyk / this.zoom);
                }
                break;
            case 1:
            default:
                this.pinching = false;
        }
        ev.setAction(3);
        super.dispatchTouchEvent(ev);
    }

    private void processSingleTouchEvent(MotionEvent ev)
    {
        float x = ev.getX();
        float y = ev.getY();

        float w = this.miniMapHeight * getWidth() / getHeight();
        float h = this.miniMapHeight;
        boolean touchingMiniMap = (x >= 10.0F) && (x <= 10.0F + w) && (y >= 10.0F) && (y <= 10.0F + h);
        if ((this.showMinimap) && (this.smoothZoom > 1.0F) && (touchingMiniMap)) {
            processSingleTouchOnMinimap(ev);
        } else {
            processSingleTouchOutsideMinimap(ev);
        }
    }

    private void processSingleTouchOnMinimap(MotionEvent ev)
    {
        float x = ev.getX();
        float y = ev.getY();

        float w = this.miniMapHeight * getWidth() / getHeight();
        float h = this.miniMapHeight;
        float zx = (x - 10.0F) / w * getWidth();
        float zy = (y - 10.0F) / h * getHeight();
        smoothZoomTo(this.smoothZoom, zx, zy);
    }

    private void processSingleTouchOutsideMinimap(MotionEvent ev)
    {
        float x = ev.getX();
        float y = ev.getY();
        float lx = x - this.touchStartX;
        float ly = y - this.touchStartY;
        float l = (float)Math.hypot(lx, ly);
        float dx = x - this.touchLastX;
        float dy = y - this.touchLastY;
        this.touchLastX = x;
        this.touchLastY = y;
        switch (ev.getAction())
        {
            case 0:
                this.touchStartX = x;
                this.touchStartY = y;
                this.touchLastX = x;
                this.touchLastY = y;
                dx = 0.0F;
                dy = 0.0F;
                lx = 0.0F;
                ly = 0.0F;
                this.scrolling = false;
                break;
            case 2:
                if ((this.scrolling) || ((this.smoothZoom > 1.0F) && (l > 30.0F)))
                {
                    if (!this.scrolling)
                    {
                        this.scrolling = true;
                        ev.setAction(3);
                        super.dispatchTouchEvent(ev);
                    }
                    this.smoothZoomX -= dx / this.zoom;
                    this.smoothZoomY -= dy / this.zoom; return;
                }
                break;
            case 1:
            case 4:
                if (l < 30.0F)
                {
                    if (System.currentTimeMillis() - this.lastTapTime < 500L)
                    {
                        if (this.smoothZoom == 1.0F) {
                            smoothZoomTo(this.maxZoom, x, y);
                        } else {
                            smoothZoomTo(1.0F, getWidth() / 2.0F, getHeight() / 2.0F);
                        }
                        this.lastTapTime = 0L;
                        ev.setAction(3);
                        super.dispatchTouchEvent(ev);
                        return;
                    }
                    this.lastTapTime = System.currentTimeMillis();

                    performClick();
                }
                break;
        }
        ev.setLocation(this.zoomX + (x - 0.5F * getWidth()) / this.zoom, this.zoomY + (y - 0.5F * getHeight()) / this.zoom);

        ev.getX();
        ev.getY();

        super.dispatchTouchEvent(ev);
    }

    private float lerp(float a, float b, float k)
    {
        return a + (b - a) * k;
    }

    private float bias(float a, float b, float k)
    {
        return Math.abs(b - a) >= k ? a + k * Math.signum(b - a) : b;
    }

    protected void dispatchDraw(Canvas canvas)
    {
        this.zoom = lerp(bias(this.zoom, this.smoothZoom, 0.05F), this.smoothZoom, 0.2F);
        this.smoothZoomX = clamp(0.5F * getWidth() / this.smoothZoom, this.smoothZoomX, getWidth() - 0.5F * getWidth() / this.smoothZoom);
        this.smoothZoomY = clamp(0.5F * getHeight() / this.smoothZoom, this.smoothZoomY, getHeight() - 0.5F * getHeight() / this.smoothZoom);

        this.zoomX = lerp(bias(this.zoomX, this.smoothZoomX, 0.1F), this.smoothZoomX, 0.35F);
        this.zoomY = lerp(bias(this.zoomY, this.smoothZoomY, 0.1F), this.smoothZoomY, 0.35F);
        if ((this.zoom != this.smoothZoom) && (this.listener != null)) {
            this.listener.onZooming(this.zoom, this.zoomX, this.zoomY);
        }
        boolean animating = (Math.abs(this.zoom - this.smoothZoom) > 1.0E-7F) ||
          (Math.abs(this.zoomX - this.smoothZoomX) > 1.0E-7F) || (Math.abs(this.zoomY - this.smoothZoomY) > 1.0E-7F);
        if (getChildCount() == 0) {
            return;
        }
        this.m.setTranslate(0.5F * getWidth(), 0.5F * getHeight());
        this.m.preScale(this.zoom, this.zoom);
        this.m.preTranslate(-clamp(0.5F * getWidth() / this.zoom, this.zoomX, getWidth() - 0.5F * getWidth() / this.zoom),
          -clamp(0.5F * getHeight() / this.zoom, this.zoomY, getHeight() - 0.5F * getHeight() / this.zoom));

        View v = getChildAt(0);
        this.m.preTranslate(v.getLeft(), v.getTop());
        if ((animating) && (this.ch == null) && (isAnimationCacheEnabled()))
        {
            v.setDrawingCacheEnabled(true);
            this.ch = v.getDrawingCache();
        }
        if ((animating) && (isAnimationCacheEnabled()) && (this.ch != null))
        {
            this.p.setColor(-1);
            canvas.drawBitmap(this.ch, this.m, this.p);
        }
        else
        {
            this.ch = null;
            canvas.save();
            canvas.concat(this.m);
            v.draw(canvas);
            canvas.restore();
        }
        if (this.showMinimap)
        {
            if (this.miniMapHeight < 0) {
                this.miniMapHeight = (getHeight() / 4);
            }
            canvas.translate(10.0F, 10.0F);

            this.p.setColor(0x80000000 | 0xFFFFFF & this.miniMapColor);
            float w = this.miniMapHeight * getWidth() / getHeight();
            float h = this.miniMapHeight;
            canvas.drawRect(0.0F, 0.0F, w, h, this.p);
            if ((this.miniMapCaption != null) && (this.miniMapCaption.length() > 0))
            {
                this.p.setTextSize(this.miniMapCaptionSize);
                this.p.setColor(this.miniMapCaptionColor);
                this.p.setAntiAlias(true);
                canvas.drawText(this.miniMapCaption, 10.0F, 10.0F + this.miniMapCaptionSize, this.p);
                this.p.setAntiAlias(false);
            }
            this.p.setColor(0x80000000 | 0xFFFFFF & this.miniMapColor);
            float dx = w * this.zoomX / getWidth();
            float dy = h * this.zoomY / getHeight();
            canvas.drawRect(dx - 0.5F * w / this.zoom, dy - 0.5F * h / this.zoom, dx + 0.5F * w / this.zoom, dy + 0.5F * h / this.zoom, this.p);

            canvas.translate(-10.0F, -10.0F);
        }
        getRootView().invalidate();
        invalidate();
    }

    public static abstract interface ZoomViewListener
    {
        public abstract void onZoomStarted(float paramFloat1, float paramFloat2, float paramFloat3);

        public abstract void onZooming(float paramFloat1, float paramFloat2, float paramFloat3);

        public abstract void onZoomEnded(float paramFloat1, float paramFloat2, float paramFloat3);
    }
}
