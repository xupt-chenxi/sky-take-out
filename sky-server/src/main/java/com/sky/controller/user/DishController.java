package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 先查询Redis中是否有菜品数据
        String dishId = "dish_" + categoryId;
        ValueOperations valueOperations = redisTemplate.opsForValue();
        List<DishVO> list = (List<DishVO>)valueOperations.get(dishId);

        if (list != null && list.size() > 0) {
            // Redis中有菜品数据，直接返回
            return Result.success(list);
        }
        // Redis中没有菜品数据，从数据库中查询
//        Dish dish = new Dish();
//        dish.setCategoryId(categoryId);
//        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        list = dishService.listWithFlavor(categoryId);
        // 将菜品数据加入Redis中
        valueOperations.set(dishId, list);

        return Result.success(list);
    }

}
