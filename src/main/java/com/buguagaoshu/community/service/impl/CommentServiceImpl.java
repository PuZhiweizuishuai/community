package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:19
 */
@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    CommentMapper commentMapper;

    @Autowired
    QuestionMapper questionMapper;


    /**
     * @Transactional spring 事务注解
     * */
    @Override
    @Transactional(rollbackFor = CustomizeException.class)
    public int insertComment(Comment comment) {
        if (comment.getParentId() == 0) {
            throw new CustomizeException(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND);
        }
        if (!CommentTypeEnum.isExist(comment.getType())) {
            throw new CustomizeException(CustomizeErrorCode.TYPE_PARAM_WRONG);
        }

        Question question = questionMapper.selectQuestionById(comment.getQuestionId());
        if (CommentTypeEnum.COMMENT.getType().equals(comment.getType())) {
            // 回复评论
            // TODO 可能有bug 待测试
            if(question == null) {
                throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
            }
            Comment dbComment = selectCommentByCommentId(comment.getParentId());
            if(dbComment == null) {
                throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
            }
            question.setCommentCount(1);
            questionMapper.updateQuestionCommentCount(question);
            return commentMapper.insertComment(comment);
        } else {
            // 回复问题
            if(question == null || (comment.getQuestionId() != comment.getParentId())) {
                throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
            }
            question.setCommentCount(1);
            questionMapper.updateQuestionCommentCount(question);
            return commentMapper.insertComment(comment);
        }
    }

    @Override
    public Comment selectCommentByCommentId(long commentId) {
        return commentMapper.selectCommentByCommentId(commentId);
    }

    @Override
    public Comment selectCommentByParentId(long parentId) {
        return commentMapper.selectCommentByParentId(parentId);
    }
}
