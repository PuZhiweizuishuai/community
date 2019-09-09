package com.buguagaoshu.community.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-09 18:46
 * 热门问题定时器
 */
@Slf4j
@Component
public class HotQuestionTasks {

    @Scheduled(fixedRate = 10800000)
    public void HotQuestionCurrentTime() {

    }
}
