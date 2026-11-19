package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.dto.HotTagDTO;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 热门话题缓存。
 *
 * <p>旧实现用 PriorityQueue + 自定义 compareTo，遇到空标签 / 相同权重时结果不稳定。
 * 现在直接按权重降序 + 名称升序排序后截取，保证结果确定、无空值。
 */
@Component
@Data
public class HotTagCache {

    /** 榜单容量 */
    private static final int MAX = 20;

    private volatile List<String> hots = new ArrayList<>();

    public void updateTags(Map<String, Long> tags) {
        if (tags == null || tags.isEmpty()) {
            hots = new ArrayList<>();
            return;
        }
        List<HotTagDTO> ranked = new ArrayList<>();
        tags.forEach((name, priority) -> {
            if (name == null || name.isBlank()) {
                return;
            }
            HotTagDTO dto = new HotTagDTO();
            dto.setName(name.trim());
            dto.setPriority(priority == null ? 0L : priority);
            ranked.add(dto);
        });
        ranked.sort(Comparator.comparingLong(HotTagDTO::getPriority).reversed()
                .thenComparing(HotTagDTO::getName));
        hots = ranked.stream().limit(MAX).map(HotTagDTO::getName).toList();
    }
}
