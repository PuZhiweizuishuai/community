package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.ReviewItemDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ReviewService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧审核接口：查看自己的待审核/被驳回内容，修改被驳回的评论并重新提交。
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewApiController {

    public record ResubmitRequest(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 5000, message = "评论内容过长")
            String content) {
    }

    private final ReviewService reviewService;

    public ReviewApiController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/mine")
    public ResultDTO<PaginationDto<ReviewItemDto>> mine(
            @RequestParam(value = "type", defaultValue = "question") String type,
            @RequestParam(value = "status", defaultValue = "0") String status,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        return ResultDTO.okOf(reviewService.listMine(user.getId(), type, status, page, size));
    }

    @PostMapping("/comments/{commentId}/resubmit")
    public ResultDTO<Object> resubmitComment(@PathVariable("commentId") long commentId,
                                             @Valid @RequestBody ResubmitRequest body,
                                             HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        reviewService.resubmitComment(commentId, body.content(), user);
        return ResultDTO.okOf();
    }
}
