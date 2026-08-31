package com.tianji.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.course.domain.po.Category3PO;
import com.tianji.course.domain.po.Course;
import com.tianji.course.domain.vo.CourseStatisticsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 草稿课程 Mapper 接口
 * </p>
 *
 * @author wusongsong
 * @since 2022-07-22
 */
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * Count the current course version. A draft is the source of truth while a
     * course is being edited or is off the shelf; otherwise the formal course
     * row is used. This prevents the copy kept during down-shelf from being
     * counted twice.
     */
    @Select("SELECT COUNT(*) AS totalNum, " +
            "COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS onSaleNum, " +
            "COALESCE(SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END), 0) AS offShelfNum, " +
            "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS noSaleNum, " +
            "COALESCE(SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END), 0) AS finishedNum, " +
            "COALESCE(SUM(CASE WHEN course_type = 2 THEN 1 ELSE 0 END), 0) AS recordNum, " +
            "COALESCE(SUM(CASE WHEN course_type = 1 THEN 1 ELSE 0 END), 0) AS liveNum " +
            "FROM (" +
            "SELECT c.id, c.course_type, c.status FROM course c " +
            "WHERE c.deleted = 0 AND c.status IN (2, 4) " +
            "UNION ALL " +
            "SELECT c.id, c.course_type, c.status FROM course c " +
            "WHERE c.deleted = 0 AND c.status = 3 " +
            "AND NOT EXISTS (SELECT 1 FROM course_draft d " +
            "WHERE d.id = c.id AND d.deleted = 0) " +
            "UNION ALL " +
            "SELECT d.id, d.course_type, " +
            "CASE WHEN d.status IN (0, 1) THEN 1 ELSE 3 END AS status " +
            "FROM course_draft d WHERE d.deleted = 0 AND d.status IN (0, 1, 2, 3)" +
            ") current_course")
    CourseStatisticsVO statistics();

    @Select("select count(1) from course where name = #{name}")
    int countSameName(@Param("name") String name);

    int updateVariableById(@Param("po") Course course);

    /**
     * 批量查询老师所负责的课程数量
     * @param teacherIds
     * @return
     */
    @Select("<script>SELECT ct.teacher_id as id,count(*) as num " +
            " from course c LEFT JOIN course_teacher ct on c.id=ct.course_id " +
            "where c.status!=1 and c.deleted=0 and ct.teacher_id in (<foreach collection='teacherIds' " +
            "item='teacherId' separator=','>#{teacherId}</foreach>)" +
            " GROUP BY ct.teacher_id</script>")
    List<IdAndNumDTO> countCourseNumOfTeacher(@Param("teacherIds")List<Long> teacherIds);

    @Select("select distinct first_cate_id as 'firstCateId',second_cate_id as 'secondCateId'," +
            "third_cate_id as 'thirdCateId' from course where status=2")
    List<Category3PO> queryCategoryIdWithCourse();
}
