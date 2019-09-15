package com.buguagaoshu.community.cache;


import com.buguagaoshu.community.dto.HotTagDTO;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 15:00
 * 热门话题
 */
@Component
@Data
public class HotTagCache {
    private List<String> hots;
    public void updateTags(Map<String, Long> tags) {
        int max = 20;
        PriorityQueue<HotTagDTO> priorityQueue = new PriorityQueue<>(max);

        tags.forEach((name, priority) -> {
            HotTagDTO hotTagDTO = new HotTagDTO();
            hotTagDTO.setName(name);
            hotTagDTO.setPriority(priority);
            if (priorityQueue.size() < max) {
                priorityQueue.add(hotTagDTO);
            } else {
                HotTagDTO minHot = priorityQueue.peek();
                if (hotTagDTO.compareTo(minHot) > 0) {
                    priorityQueue.poll();
                    priorityQueue.add(hotTagDTO);
                }
            }
        });


        List<String> sortedTags = new ArrayList<>();

        HotTagDTO poll = priorityQueue.poll();
        while (poll != null) {
            sortedTags.add(0, poll.getName());
            poll = priorityQueue.poll();
        }
        hots = sortedTags;
    }
}
