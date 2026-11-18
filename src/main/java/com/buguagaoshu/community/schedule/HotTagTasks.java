package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.mapper.TagMapper;
import com.buguagaoshu.community.model.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 热门标签定时器。
 *
 * <p>标签改为独立表后，热度直接以审核通过标签的使用数（关联帖子数）为准，
 * 不再从帖子冗余 tag 串里拆分统计，避免大小写/重复标签造成的漂移。
 */
@Slf4j
@Component
public class HotTagTasks {

    private final TagMapper tagMapper;
    private final HotTagCache hotTagCache;
    private final RedisSchedulerLock schedulerLock;

    public HotTagTasks(TagMapper tagMapper, HotTagCache hotTagCache,
                       RedisSchedulerLock schedulerLock) {
        this.tagMapper = tagMapper;
        this.hotTagCache = hotTagCache;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(fixedRate = 3600000)
    public void hotTagsCurrentTime() {
        if (!schedulerLock.tryLock("hotTag", Duration.ofMinutes(10))) {
            return;
        }
        try {
            log.info("开始计算热门标签");
            List<Tag> tags = tagMapper.selectApproved(0, 50);
            Map<String, Long> priorities = new LinkedHashMap<>();
            for (Tag tag : tags) {
                if (tag.getName() == null || tag.getName().isBlank() || tag.getUsageCount() <= 0) {
                    continue;
                }
                priorities.put(tag.getName(), tag.getUsageCount());
            }
            hotTagCache.updateTags(priorities);
            log.info("热门标签计算完成，候选标签 {} 个，上榜 {} 个", priorities.size(), hotTagCache.getHots().size());
        } finally {
            schedulerLock.unlock("hotTag");
        }
    }
}
