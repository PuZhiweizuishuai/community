package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.enums.NotificationStatusEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.*;
import com.buguagaoshu.community.service.*;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-06 14:58
 */
@RestController
@Slf4j
public class AdminQuestionApiController {
    final QuestionMapper questionMapper;

    final
    UserService userService;

    final
    NotificationMapper notificationMapper;

    final
    CommentMapper commentMapper;

    final
    UserPermissionService userPermissionService;


    @Autowired
    public AdminQuestionApiController(UserService userService, QuestionMapper questionMapper, NotificationMapper notificationMapper, CommentMapper commentMapper,
                                      UserPermissionService userPermissionService) {
        this.userService = userService;
        this.questionMapper = questionMapper;

        this.notificationMapper = notificationMapper;
        this.commentMapper = commentMapper;

        this.userPermissionService = userPermissionService;
    }


    @PostMapping("/api/admin/delete/question")
    public Map<String, Object> deleteQuestion(Long questionId, HttpServletRequest request) {
        Question question = null;
        try {
            question = questionMapper.getQuestionIgnoreStatus(questionId);
        } catch (Exception e) {
            log.error("来自IP {} 的非法查询问题！", IpUtil.getIpAddr(request));
            return StringUtil.dealResultMessage(false, "非法操作 ");
        }

        if (question == null) {
            return StringUtil.dealResultMessage(false, "问题不存在");
        }
        User admin = (User) request.getSession().getAttribute("admin");
        User user = (User) request.getSession().getAttribute("user");

        if (admin == null && user == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        // 创建通知
        if (admin != null) {
            List<Comment> comments = commentMapper.getAllComment(questionId, 1);
            for (Comment comment : comments) {
                createNotification(question, admin, comment.getCommentator());
            }
            questionMapper.alterStatus(questionId, 0);
            log.info("管理员 {} 删除了问题 {}", admin.getId(), questionId);
            return StringUtil.dealResultMessage(true, "设置成功");
        }


        if (question.getUserId() != user.getId()) {
            return StringUtil.dealResultMessage(false, " 权限不足！");
        }
        if (question.getUserId() == user.getId()) {
            List<Comment> comments = commentMapper.getAllComment(questionId, 1);
            for (Comment comment : comments) {
                createNotification(question, admin, comment.getCommentator());
            }
            questionMapper.alterStatus(questionId, 0);
            log.info("用户 {} 删除了问题 {}", user.getId(), questionId);
            return StringUtil.dealResultMessage(true, "删除成功！");
        }
        return StringUtil.dealResultMessage(false, "非法操作 ");
    }


    @PostMapping("/api/admin/recovery/question")
    public Map<String, Object> recoveryQuestion(Long questionId, HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        Question question = null;
        try {
            question = questionMapper.getQuestionIgnoreStatus(questionId);
        } catch (Exception e) {
            log.error("来自IP {} 的非法查询问题！", IpUtil.getIpAddr(request));
            return StringUtil.dealResultMessage(false, "非法操作 ");
        }
        if (question == null) {
            return StringUtil.dealResultMessage(false, "问题不存在！ ");
        }
        questionMapper.alterStatus(questionId, 1);
        log.info("管理员 {} 恢复了问题 {}", admin.getId(), questionId);
        return StringUtil.dealResultMessage(true, "恢复成功！");
    }


    @PostMapping("/api/admin/update/user")
    public Map<String, Object> alterUserPower(Long userId, int power,HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        User user = null;
        try {
            user = userService.selectUserById(userId);
        } catch (Exception e) {
            log.info("管理员修改用户权限失败 {}", e.getMessage());
        }
        if (user != null) {
            userPermissionService.updateUserPermissionById(user.getId(), power, admin.getUserId(), StringUtil.getNowTime());
            return StringUtil.dealResultMessage(true, "修改成功！");
        }
        return StringUtil.dealResultMessage(false,"修改失败，请重试！");
    }

    @PostMapping("/api/admin/reset/password")
    public Map<String, Object> resetUserPassword(Long userId, HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        User user = null;
        try {
            user = userService.selectUserById(userId);
        } catch (Exception e) {
            log.info("管理员重置用户密码失败 {}", e.getMessage());
        }
        if (user != null) {
            userService.updateUserPasswordById(user.getId(), "123456");
            log.info("管理员 {} 成功重置用户 {} 密码", admin.getId(), user.getId());
            return StringUtil.dealResultMessage(true, "重置成功！");
        }
        return StringUtil.dealResultMessage(false,"重置失败，请重试！");
    }

    /**
     * 创建通知
     */
    private void createNotification(Question question, User admin, Long receiver) {
        if (question.getUserId() == admin.getId()) {
            return;
        }
        Notification notification = new Notification();
        // 消息创建时间
        notification.setCreateTime(System.currentTimeMillis());
        notification.setType(NotificationTypeEnum.SYSTEM_NOTIFICATION.getType());
        // 通知产生点
        notification.setOuterId(question.getQuestionId());
        // 通知发起人
        notification.setNotifier(admin.getId());
        // 通知状态
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        // 通知接收人
        notification.setReceiver(receiver);

        notificationMapper.insertNotification(notification);
    }
}
