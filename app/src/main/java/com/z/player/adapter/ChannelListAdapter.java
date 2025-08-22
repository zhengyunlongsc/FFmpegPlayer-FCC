package com.z.player.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.z.player.R;
import com.z.player.bean.ChannelZT;

import java.util.List;

public class ChannelListAdapter extends BaseRecyclerViewAdapter<ChannelListAdapter.ViewHolder> {
    private Context context;
    private List<ChannelZT> mDatas;

    public ChannelListAdapter(Context context, List<ChannelZT> list1) {
        this.context = context;
        this.mDatas = list1;
    }

    public void setItems(List<ChannelZT> datas) {
        this.mDatas = datas;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rv_channel, parent, false);
        return new ViewHolder(view);
    }

    public ChannelZT getItem(int position) {
        if (mDatas != null && mDatas.size() > position) {
            return mDatas.get(position);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ChannelZT c = mDatas.get(position);
        String name = TextUtils.isEmpty(c.name) ? c.channelName : c.name;
        String channelNum = c.num <= 0 ? c.channelNumber : String.valueOf(c.num);
        holder.tv_name.setText(name);
        holder.tv_no.setText(channelNum);
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();//返回总条数
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_no;
        private TextView tv_name;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_no = itemView.findViewById(R.id.tv_no);
        }
    }
}

