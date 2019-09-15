package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.dto.UserDto;
import lombok.Data;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 15:01
 */
@Component
@Data
public class HotUserCache {
    private List<UserDto> userDtoList = new ArrayList<>();

    public void updateUserDtoList(List<UserDto> userDtoList) {
        int max = 50;
        // Collections.sort(userDtoList);
        // this.userDtoList = userDtoList;
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

        UserDto userDto = priorityQueue.poll();
        while (userDto != null) {
            this.userDtoList.add(userDto);
            userDto = priorityQueue.poll();
        }
    }

}
