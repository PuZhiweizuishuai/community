package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotUserCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.UserDto;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.HotRankingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活跃用户榜单：与热门问题共用 {@link HotRankingUtil} 打分（对数抑制 + 时间衰减），
 * 用户信息与问题数全部批量查询，不再逐用户查库。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 14:59
 */
@Slf4j
@Component
public class HotUserTasks {
    private final QuestionService questionService;

    private final QuestionMapper questionMapper;

    private final UserBatchLoader userBatchLoader;

    private final HotUserCache hotUserCache;

    private final RedisSchedulerLock schedulerLock;

    @Autowired
    public HotUserTasks(QuestionService questionService, QuestionMapper questionMapper,
                        UserBatchLoader userBatchLoader, HotUserCache hotUserCache,
                        RedisSchedulerLock schedulerLock) {
        this.questionService = questionService;
        this.questionMapper = questionMapper;
        this.userBatchLoader = userBatchLoader;
        this.hotUserCache = hotUserCache;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(fixedRate = 10800000)
    public void hotUserCurrentTime() {
        if (!schedulerLock.tryLock("hotUser", Duration.ofMinutes(10))) {
            return;
        }
        try {
            log.info("开始计算活跃用户！");
            long now = System.currentTimeMillis();
            List<Question> questionList = questionService.getQuestionListForTag(0, HotRankingUtil.CANDIDATE_LIMIT);

            // 每个用户的热度 = 其候选问题热度分之和
            Map<Long, Double> scores = new HashMap<>(128);
            for (Question question : questionList) {
                if (question == null || question.getStatus() == 0) {
                    continue;
                }
                scores.merge(question.getUserId(), HotRankingUtil.questionScore(question, now), Double::sum);
            }
            Set<Long> userIds = scores.keySet();
            if (userIds.isEmpty()) {
                hotUserCache.updateUserDtoList(List.of());
                return;
            }

            Map<Long, User> userMap = userBatchLoader.loadUsers(userIds, true);
            Map<Long, Long> questionCounts = new HashMap<>();
            for (Map<String, Object> row : questionMapper.countQuestionByUserIds(userIds)) {
                questionCounts.put(((Number) row.get("userId")).longValue(), ((Number) row.get("cnt")).longValue());
            }

            List<UserDto> userList = new ArrayList<>();
            for (Long id : userIds) {
                User user = userMap.get(id);
                if (user == null) {
                    continue;
                }
                UserDto userDto = new UserDto();
                BeanUtils.copyProperties(user, userDto);
                userDto.setQuestionCount(questionCounts.getOrDefault(id, 0L));
                userDto.setLikeCount(user.getLikeCount());
                userDto.setFollowCount(user.getFollowCount());
                userDto.setSortWeight(HotRankingUtil.scaled(scores.get(id)));
                userList.add(userDto);
            }
            hotUserCache.updateUserDtoList(userList);
            log.info("活跃用户计算完成！");
        } finally {
            schedulerLock.unlock("hotUser");
        }
    }
}