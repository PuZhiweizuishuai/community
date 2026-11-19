package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息通知（原 /message/{userId} 系列页面）。
 * 用户身份一律从会话取，不再由 URL 传入 userId，避免越权读取。
 */
@RestController
@RequestMapping("/api/messages")
public class MessageApiController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public MessageApiController(NotificationService notificationService,
                                NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            @RequestParam(value = "type", defaultValue = "0") String type,
            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        PaginationDto<NotificationDTO> pagination =
                notificationService.getAllNotification(page, size, user.getId(), type);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notifications", pagination);
        Map<String, Long> unread = unreadCounts(user.getId());
        data.put("commentCount", unread.get("comment"));
        data.put("likeCount", unread.get("like"));
        data.put("systemCount", unread.get("system"));
        data.put("totalCount", unread.get("total"));
        data.put("type", type);
        data.put("user", AuthUtil.safeUser(user));
        return ResultDTO.okOf(data);
    }

    /** 一次 GROUP BY 聚合替代回复/点赞/系统/总数四次 COUNT */
    private Map<String, Long> unreadCounts(long userId) {
        long comment = 0;
        long like = 0;
        long system = 0;
        for (Map<String, Object> row : notificationMapper.countUnreadGroupByType(userId)) {
            Object typeValue = row.get("type");
            Object totalValue = row.get("total");
            if (!(typeValue instanceof Number type) || !(totalValue instanceof Number total)) {
                continue;
            }
            int code = type.intValue();
            long count = total.longValue();
            if (code <= 2) {
                comment += count;
            } else if (code <= 4) {
                like += count;
            } else {
                system += count;
            }
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("comment", comment);
        result.put("like", like);
        result.put("system", system);
        result.put("total", comment + like + system);
        return result;
    }

    /**
     * 阅读单条通知，返回应跳转的问题 id。
     */
    @PostMapping("/{id}/read")
    public ResultDTO<Map<String, Object>> read(@PathVariable("id") String id,
                                               HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        NotificationDTO notification = notificationService.readNotification(id, user);
        if (notification == null || notification.getReceiver() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.READ_NOTIFICATION_FAIL);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("outerId", notification.getOuterid());
        data.put("type", notification.getType());
        data.put("commentId", notification.getCommentId());
        return ResultDTO.okOf(data);
    }

    @PostMapping("/read-all")
    public ResultDTO<Map<String, Object>> readAll(HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        notificationService.readAll(user.getId());
        return ResultDTO.okOf(Map.of("read", true));
    }
}
