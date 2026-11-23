package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;

/**
 * 问题访问控制：话题可见性 + 帖子「登录可见」的统一判定。
 */
public interface QuestionAccessService {

    QuestionAccessDto resolve(Question question, User viewer, User admin);
}
