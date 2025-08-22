package com.z.player.adapter;

import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.z.player.R;

/**
 * 作者: zyl on 2019/5/15 10:42
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public abstract class BaseRecyclerViewAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {
    public static final String TAG = BaseRecyclerViewAdapter.class.getName();
    public OnItemClickListener mOnItemClickListener;
    private int mCurrentFocusePosition = -1;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickLitener) {
        this.mOnItemClickListener = mOnItemClickLitener;
    }

    @Override
    public void onBindViewHolder(V holder, int i) {
        setItem(holder, i);
    }

    public int getCurrentFocusePosition() {
        return mCurrentFocusePosition;
    }

    public void setItem(V holder, int i) {
        final int position = i;
        Log.d(TAG, "setItem: ---> tag i=" + i);
        holder.itemView.setBackgroundResource(R.drawable.sl_common_focus);
        holder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "onFocusChange: ---> tag 0-0 hasFocus=" + hasFocus);
                if (hasFocus) {
                    mCurrentFocusePosition = position;
                }
                onFocusChangeAnimate(v, hasFocus);
            }
        });

        setOnItemClickListener(holder, position);
    }


    /**
     * 子类调用设置item点击事件,外部不可调用
     *
     * @param holder
     * @param position
     */
    protected void setOnItemClickListener(final V holder, int position) {
        if (holder != null) {
            holder.itemView.setTag(position);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(holder, v, (int) v.getTag());
                }
            });
        }
    }

    /**
     * Focuse改变时动画
     *
     * @param v
     * @param hasFocus
     */
    public void onFocusChangeAnimate(View v, boolean hasFocus) {
        if (isZoom()) {
            if (hasFocus) {
                ViewCompat.animate(v).setDuration(100).scaleX(1.1f).scaleY(1.1f).start();
            } else {
                ViewCompat.animate(v).setDuration(100).scaleX(1.f).scaleY(1.f).start();
            }
        }
        v.setSelected(hasFocus);
        Log.d(TAG, "onFocusChangeAnimate: " + v.getId());
    }

    public boolean isZoom() {
        return true;
    }

    /**
     * 可在focuse事件中模拟点击事件
     *
     * @param v
     * @param position
     */
    public void onItemClick(V holder, View v, int position) {
        this.mCurrentFocusePosition = position;

        Log.d(TAG, "onItemClick: ---> get_position=" + position);

        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(holder, v, position);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder holder, View view, int position);
    }
}
