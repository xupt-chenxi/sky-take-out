package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 向口味表中插入多条数据
     *
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 通过dish_id删除口味数据
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 通过dish_id集合批量删除口味数据
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);
}
