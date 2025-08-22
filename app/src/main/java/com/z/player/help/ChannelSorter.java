package com.z.player.help;

import android.text.TextUtils;

import com.z.player.bean.ChannelZT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChannelSorter {
    public static void sort(List<ChannelZT> list) {
        if (list == null) {
            return;
        }

        ChannelZT channelZT = list.get(0);
        if (TextUtils.equals(channelZT.channelNumber, "1")) {
            return;
        }

        // 分离包含数字的频道和不包含数字的频道
        List<ChannelZT> channelsWithNumbers = new ArrayList<>();
        List<ChannelZT> channelsWithoutNumbers = new ArrayList<>();

        for (ChannelZT channel : list) {
            if (channel.channelName.matches(".*\\d.*")) {
                channelsWithNumbers.add(channel);
            } else {
                channelsWithoutNumbers.add(channel);
            }
        }

        // 排序包含数字的频道
        Collections.sort(channelsWithNumbers, new Comparator<ChannelZT>() {
            @Override
            public int compare(ChannelZT o1, ChannelZT o2) {
                int num1 = Integer.parseInt(o1.channelName.replaceAll("[^0-9]", ""));
                int num2 = Integer.parseInt(o2.channelName.replaceAll("[^0-9]", ""));
                return Integer.compare(num1, num2);
            }
        });

        // 排序不包含数字的频道（按字母顺序）
        Collections.sort(channelsWithoutNumbers, new Comparator<ChannelZT>() {
            @Override
            public int compare(ChannelZT o1, ChannelZT o2) {
                return o1.channelName.compareTo(o2.channelName);
            }
        });

        // 合并排序结果
        list.clear();
        list.addAll(channelsWithNumbers);
        list.addAll(channelsWithoutNumbers);

        for (int i = 0; i < list.size(); i++) {
            ChannelZT c = list.get(i);
            c.channelNumber = String.valueOf(i + 1);
        }
    }
}
