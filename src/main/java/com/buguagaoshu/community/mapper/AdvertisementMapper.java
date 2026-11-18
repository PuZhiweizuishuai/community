package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Advertisement;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import static com.buguagaoshu.community.model.table.AdvertisementTableDef.ADVERTISEMENT;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:36
 */
@Mapper
public interface AdvertisementMapper extends BaseMapper<Advertisement> {

    /**
     * 修改广告内容
     */
    default int alterAdvertisement(Advertisement advertisement) {
        Advertisement update = UpdateEntity.of(Advertisement.class, advertisement.getId());
        update.setTitle(advertisement.getTitle());
        update.setUrl(advertisement.getUrl());
        update.setImage(advertisement.getImage());
        update.setModifiedUser(advertisement.getModifiedUser());
        return update(update);
    }

    /**
     * 设置广告投放
     */
    default int updateAdvertisementSetting(Advertisement advertisement) {
        Advertisement update = UpdateEntity.of(Advertisement.class, advertisement.getId());
        update.setStartTime(advertisement.getStartTime());
        update.setEndTime(advertisement.getEndTime());
        update.setStatus(advertisement.getStatus());
        update.setModifiedUser(advertisement.getModifiedUser());
        update.setPosition(advertisement.getPosition());
        return update(update);
    }

    /**
     * 更新阅读数（原子增量）
     */
    @Update("update advertisements set viewCount=viewCount+#{viewCount} where id=#{id}")
    int updateAdvertisementViewCount(Advertisement advertisement);

    /**
     * 统计当前有效投放的广告总数
     */
    default long countActive(@Param("now") long now) {
        return selectCountByQuery(QueryWrapper.create()
                .where(ADVERTISEMENT.STATUS.eq(1))
                .and(ADVERTISEMENT.END_TIME.gt(now)));
    }

    /**
     * 统计某位置当前有效投放数（容量判断以数据库为准，excludeId 用于续期时排除自身）
     */
    default int countActiveByPosition(@Param("position") String position, @Param("now") long now, @Param("excludeId") Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ADVERTISEMENT.POSITION.eq(position))
                .and(ADVERTISEMENT.STATUS.eq(1))
                .and(ADVERTISEMENT.END_TIME.gt(now));
        if (excludeId != null) {
            query.and(ADVERTISEMENT.ID.ne(excludeId));
        }
        return (int) selectCountByQuery(query);
    }
}