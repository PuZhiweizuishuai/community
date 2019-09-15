package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.dto.HotQuestionDTO;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-07 18:32
 * 热门问题
 */
@Component
@Data
public class HotQuestionCache {
    List<HotQuestionDTO> hotQuestionDTOList = new ArrayList<>();

    public void updateHotQuestion(List<HotQuestionDTO> hotQuestionDTOList) {
        // 计算热门的十个问题
        int i = 0, max = 10;
        Collections.sort(hotQuestionDTOList);
        for(HotQuestionDTO hotQuestionDTO : hotQuestionDTOList) {
            this.hotQuestionDTOList.add(hotQuestionDTO);
            i++;
            if(i >= max) {
                break;
            }
        }
    }
}
