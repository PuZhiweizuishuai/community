package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.ContentReport;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import static com.buguagaoshu.community.model.table.ContentReportTableDef.CONTENT_REPORT;

@Mapper
public interface ContentReportMapper extends BaseMapper<ContentReport> {

    default long countByStatus(int status) {
        return selectCountByQuery(QueryWrapper.create().where(CONTENT_REPORT.STATUS.eq(status)));
    }

    /**
     * 提交举报：INSERT ... WHERE NOT EXISTS 单语句完成判重与写入，
     * 避免并发重复举报绕过先查后写。
     */
    @Insert("insert into content_reports(targetType, targetId, questionId, targetUserId, targetSnapshot, " +
            "reporterId, reason, description, status, handlerId, handleNote, handleTime, contentRemoved, createTime) " +
            "select #{targetType}, #{targetId}, #{questionId}, #{targetUserId}, #{targetSnapshot}, " +
            "#{reporterId}, #{reason}, #{description}, #{status}, 0, null, 0, 0, #{createTime} from dual " +
            "where not exists (select 1 from content_reports where targetType=#{targetType} " +
            "and targetId=#{targetId} and reporterId=#{reporterId} and status=0)")
    int insertIfNoPending(ContentReport report);

    /**
     * 条件处理：只有待处理举报才能被受理或驳回，避免并发重复处理
     */
    @Update("update content_reports set status=#{status}, handlerId=#{handlerId}, handleNote=#{handleNote}, "
            + "handleTime=#{handleTime}, contentRemoved=#{contentRemoved} "
            + "where id=#{id} and status=0")
    int handle(@Param("id") long id,
               @Param("status") int status,
               @Param("handlerId") long handlerId,
               @Param("handleNote") String handleNote,
               @Param("handleTime") long handleTime,
               @Param("contentRemoved") int contentRemoved);
}
