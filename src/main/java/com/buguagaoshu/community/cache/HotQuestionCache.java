package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.dto.HotQuestionDTO;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 热门问题缓存（Top N 榜单）。排序逻辑统一放在定时任务里，这里只做稳定的截取与快照。
 */
@Component
@Data
public class HotQuestionCache {

    private static final int MAX = 10;

    private List<HotQuestionDTO> hotQuestionDTOList = new ArrayList<>();

    public void updateHotQuestion(List<HotQuestionDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            hotQuestionDTOList = new ArrayList<>();
            return;
        }
        List<HotQuestionDTO> sorted = new ArrayList<>(candidates);
        sorted.removeIf(item -> item == null || item.getQuestionId() == null);
        sorted.sort(Comparator.comparingLong(HotQuestionDTO::getSortWeight).reversed()
                .thenComparingLong(HotQuestionDTO::getQuestionId));
        // 同一问题只保留一次，避免脏数据重复上榜
        List<HotQuestionDTO> result = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (HotQuestionDTO item : sorted) {
            if (seen.add(item.getQuestionId())) {
                result.add(item);
                if (result.size() >= MAX) {
                    break;
                }
            }
        }
        hotQuestionDTOList = result;
    }
}
