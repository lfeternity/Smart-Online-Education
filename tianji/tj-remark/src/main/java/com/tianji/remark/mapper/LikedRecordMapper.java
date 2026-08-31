package com.tianji.remark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.remark.domain.po.LikedRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 点赞记录表 Mapper 接口
 * </p>
 *
 * @author 虎哥
 */
public interface LikedRecordMapper extends BaseMapper<LikedRecord> {

    @Select("SELECT biz_id AS bizId, COUNT(*) AS likedTimes " +
            "FROM liked_record WHERE biz_type = #{bizType} " +
            "GROUP BY biz_id ORDER BY MIN(update_time) LIMIT #{maxBizSize}")
    List<LikedTimesDTO> selectLikedTimes(@Param("bizType") String bizType,
                                         @Param("maxBizSize") int maxBizSize);

}
