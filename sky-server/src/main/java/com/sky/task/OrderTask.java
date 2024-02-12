package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理用户支付超时的订单
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟处理一次
    public void processTimeoutOrder() {
        log.info("处理用户支付超时的订单");

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        // 从表中查到超时的订单
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        // 将订单状态设置为已取消
        if (orders != null && orders.size() > 0) {
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("用户支付订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点处理
    public void processDeliveryOrder() {
        log.info("处理一直处于派送中的订单");

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        // 从表中查到截至昨日24点，仍处于派送中的订单
        List<Orders> orders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        // 将订单状态设置为已完成
        if (orders != null && orders.size() > 0) {
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
