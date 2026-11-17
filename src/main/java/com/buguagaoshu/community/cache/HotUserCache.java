package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.dto.UserDto;
import lombok.Data;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 热门用户榜单：构建新集合后原子引用替换，刷新过程中不会读到空榜/半榜。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 15:01
 */
@Component
@Data
public class HotUserCache {
    private volatile List<UserDto> userDtoList = List.of();

    public void updateUserDtoList(List<UserDto> userDtoList) {
        int max = 50;
        PriorityQueue<UserDto> priorityQueue = new PriorityQueue<>(max);
        userDtoList.forEach((userDto) -> {
            if(priorityQueue.size() < max) {
                priorityQueue.add(userDto);
            } else {
                UserDto minUser = priorityQueue.peek();
                if(userDto.compareTo(minUser) > 0) {
                    priorityQueue.poll();
                    priorityQueue.add(userDto);
                }
            }
        });

        List<UserDto> refreshed = new ArrayList<>(priorityQueue.size());
        UserDto userDto = priorityQueue.poll();
        while (userDto != null) {
            refreshed.add(userDto);
            userDto = priorityQueue.poll();
        }
        this.userDtoList = List.copyOf(refreshed);
    }

}