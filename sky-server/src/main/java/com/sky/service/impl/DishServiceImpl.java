package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 向菜品表以及口味表中插入数据
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 删除缓存数据
        claeanCache("dish_" + dishDTO.getCategoryId().toString());
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表插入一条数据
        dishMapper.insert(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            // 获取插入数据的id
            Long dishId = dish.getId();
            // 为要插入的口味设置id，用于关联菜品表
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            // 向口味表插入数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        claeanCache("dish_*");
        // 判断当前菜品是否能删除 -- 是否存在起售中的产品
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            // 菜品起售中，不能删除
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断当前菜品是否能删除 -- 是否被套餐关联了
        List<Long> setmeals = setmealDishMapper.seleteByDeleteIds(ids);
        // 当前菜品被套餐关联，不能删除
        if (setmeals != null && setmeals.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        for (Long id : ids) {
//            // 删除菜品表中的菜品数据
//            dishMapper.deleteById(id);
//            // 删除口味表中关于该菜品口味的数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        // 批量删除菜品表中数据，通过一条sql的方式，对上述代码进行优化
        dishMapper.deleteByIds(ids);
        // 批量删除口味表中数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO selectByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.selectById(id);
        // 根据id查询口味数据
        List<DishFlavor> dishFlavor = dishFlavorMapper.selectByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavor);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        claeanCache("dish_*");
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品数据
        dishMapper.update(dish);
        // 获取插入数据的id
        Long dishId = dish.getId();
        // 先删除该菜品关联的口味数据
        dishFlavorMapper.deleteByDishId(dishId);

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {

            // 为要插入的口味设置id，用于关联菜品表
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }
            // 向口味表插入数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    // TODO 个人直接通过categoryId进行查询，修改了黑马通过dish动态查询的方法
    public List<Dish> list(Long categoryId) {
//        Dish dish = Dish.builder()
//                .categoryId(categoryId)
//                .status(StatusConstant.ENABLE)
//                .build();
//        return dishMapper.list(dish);
        return dishMapper.list(categoryId);
    }

    /**
     * 条件查询菜品和口味
     * @param categoryId
     * @return
     */
    public List<DishVO> listWithFlavor(Long categoryId) {
        List<Dish> dishList = dishMapper.list(categoryId);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 设置菜品停售起售
     * @param status
     * @param id
     */
    @Override
    public void setStatus(Integer status, Long id) {
        claeanCache("dish_*");
        dishMapper.setStatus(status, id);
    }

    /**
     * 根据pattern从Redis中删除相关的key
     * @param pattern
     */
    private void claeanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
