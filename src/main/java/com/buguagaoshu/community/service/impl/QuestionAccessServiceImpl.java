package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.enums.QuestionVisibilityEnum;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionAccessService;
import com.buguagaoshu.community.service.TopicAccessService;
import org.springframework.stereotype.Service;

/**
 * 先判话题，再判帖子可见性；作者本人与管理员拥有内容访问权。
 */
@Service
public class QuestionAccessServiceImpl implements QuestionAccessService {

    private final TopicAccessService topicAccessService;

    public QuestionAccessServiceImpl(TopicAccessService topicAccessService) {
        this.topicAccessService = topicAccessService;
    }

    @Override
    public QuestionAccessDto resolve(Question question, User viewer, User admin) {
        if (question == null) {
            return new QuestionAccessDto(false, false, false);
        }
        if (admin != null) {
            return new QuestionAccessDto(true, true, false);
        }
        boolean author = viewer != null && question.getUserId() == viewer.getId();
        TopicAccessDto topic = topicAccessService.resolve(question.getTopicId() == null ? 0 : question.getTopicId(), viewer);
        if (topic.topic() == null) {
            return new QuestionAccessDto(false, false, false);
        }
        if (!topic.visible() && !author) {
            return new QuestionAccessDto(false, false, false);
        }
        if (!topic.canViewContent() && !author) {
            // 公开话题的「登录可见」帖：匿名允许看到标题，登录后即可查看
            if (topic.topic().getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()
                    && question.getVisibility() == QuestionVisibilityEnum.LOGIN_REQUIRED.getCode()
                    && viewer == null) {
                return new QuestionAccessDto(true, false, true);
            }
            return new QuestionAccessDto(true, false, false);
        }
        if (question.getVisibility() == QuestionVisibilityEnum.LOGIN_REQUIRED.getCode() && viewer == null) {
            return new QuestionAccessDto(true, false, true);
        }
        return new QuestionAccessDto(true, true, false);
    }
}
