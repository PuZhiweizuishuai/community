package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.HotUserCache;
import com.buguagaoshu.community.dto.UserDto;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 14:59
 */
@Slf4j
@Component
public class HotUserTasks {
    private final QuestionService questionService;

    private final UserService userService;

    private final UserPermissionService userPermissionService;


    private final HotUserCache hotUserCache;

    @Autowired
    public HotUserTasks(QuestionService questionService, UserService userService, UserPermissionService userPermissionService, HotUserCache hotUserCache) {
        this.questionService = questionService;
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.hotUserCache = hotUserCache;
    }

    @Scheduled(fixedRate = 10800000)
    public void hotUserCurrentTime() {
        long offset = 0;
        long limit = 100;
        log.info("开始计算活跃用户！");
        List<Question> questionList = questionService.getQuestionListForTag(offset, limit);

        Set<Long> userId = questionList.stream().map(question -> question.getUserId()).collect(Collectors.toSet());

        Map<Long, Long> count = new HashMap<>(128);

        // 初始化
        for(Long id : userId) {
            count.put(id, 0L);
        }

        // 计算排序权值，一个回复为 5， 一个点赞为 3
        // TODO 待添加点赞数
        for(Question question : questionList) {
            count.put(question.getUserId(), count.get(question.getUserId())+1L+ question.getCommentCount() * 5 + question.getLikeCount() * 3 + question.getViewCount());
        }


        List<UserDto> userList = new ArrayList<>();
        for (Long id : userId) {
            User user = userService.selectUserById(id);
            UserPermission userPermission = userPermissionService.selectUserPermissionById(id);
            user.setPower(userPermission.getPower());
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            userDto.setQuestionCount(questionService.getUserQuestionCount(userDto.getId(), 1));
            // TODO 待添加关注数和点赞数
            userDto.setLikeCount(user.getLikeCount());
            userDto.setFollowCount(0);
            userDto.setSortWeight(count.get(userDto.getId()));
            userList.add(userDto);
        }
        hotUserCache.updateUserDtoList(userList);
        log.info("活跃用户计算完成！");
    }
}
