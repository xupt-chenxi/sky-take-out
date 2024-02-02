package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据要删除的菜品id查询套餐表中是否含有套餐与要删除的菜品关联
     * @param ids
     * @return
     */
    List<Long> seleteByDeleteIds(List<Long> ids);
}
