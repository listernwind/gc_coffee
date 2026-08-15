package com.gccoffee.module.drink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gccoffee.common.BizException;
import com.gccoffee.common.PageResult;
import com.gccoffee.module.coupon.service.CouponService;
import com.gccoffee.module.drink.dto.DrinkOrderRequest;
import com.gccoffee.module.drink.entity.DrinkCategory;
import com.gccoffee.module.drink.entity.DrinkOrder;
import com.gccoffee.module.drink.entity.DrinkOrderItem;
import com.gccoffee.module.drink.entity.DrinkProduct;
import com.gccoffee.module.drink.mapper.DrinkCategoryMapper;
import com.gccoffee.module.drink.mapper.DrinkOrderItemMapper;
import com.gccoffee.module.drink.mapper.DrinkOrderMapper;
import com.gccoffee.module.drink.mapper.DrinkProductMapper;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.service.UserService;
import com.gccoffee.util.OrderNoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DrinkService {

    private final DrinkCategoryMapper categoryMapper;
    private final DrinkProductMapper productMapper;
    private final DrinkOrderMapper orderMapper;
    private final DrinkOrderItemMapper itemMapper;
    private final UserService userService;
    private final CouponService couponService;

    /** 菜单：分类 + 商品 */
    public List<Map<String, Object>> menu() {
        List<DrinkCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<DrinkCategory>().orderByAsc(DrinkCategory::getSort));
        List<DrinkProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<DrinkProduct>().eq(DrinkProduct::getActive, 1)
                        .orderByAsc(DrinkProduct::getSort));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DrinkCategory c : categories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("products", products.stream().filter(p -> p.getCategoryId().equals(c.getId())).toList());
            result.add(m);
        }
        return result;
    }

    /** 下单（模拟支付，即时出单） */
    @Transactional
    public DrinkOrder createOrder(Long uid, DrinkOrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException("订单不能为空");
        }
        BigDecimal total = BigDecimal.ZERO;
        List<DrinkOrderItem> items = new ArrayList<>();
        for (DrinkOrderRequest.Item it : req.getItems()) {
            DrinkProduct p = productMapper.selectById(it.getProductId());
            if (p == null || p.getActive() == null || p.getActive() != 1) {
                throw new BizException("商品不存在或已下架");
            }
            if (it.getQuantity() == null || it.getQuantity() <= 0) {
                throw new BizException("商品数量不正确");
            }
            if (p.getStock() != null && p.getStock() >= 0 && p.getStock() < it.getQuantity()) {
                throw new BizException("「" + p.getName() + "」库存不足");
            }
            if (p.getStock() != null && p.getStock() >= 0) {
                productMapper.update(null, new LambdaUpdateWrapper<DrinkProduct>()
                        .eq(DrinkProduct::getId, p.getId())
                        .set(DrinkProduct::getStock, p.getStock() - it.getQuantity()));
            }
            DrinkOrderItem item = new DrinkOrderItem();
            item.setProductId(p.getId());
            item.setProductName(p.getName());
            item.setPrice(p.getPrice());
            item.setQuantity(it.getQuantity());
            items.add(item);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }

        String orderNo = OrderNoUtil.drink();
        BigDecimal discount = BigDecimal.ZERO;
        if (req.getCouponId() != null) {
            discount = couponService.useCoupon(uid, req.getCouponId(), total, orderNo);
        }
        BigDecimal payAmount = total.subtract(discount).max(BigDecimal.ZERO);
        String payType = "BALANCE".equals(req.getPayType()) ? "BALANCE" : "WX_MOCK";

        if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("BALANCE".equals(payType)) {
                userService.deductBalance(uid, payAmount, "CONSUME", orderNo, "饮品下单");
            }
            userService.addSpend(uid, payAmount);
            userService.earnPoints(uid, payAmount, orderNo, "饮品消费");
        }

        DrinkOrder order = new DrinkOrder();
        order.setOrderNo(orderNo);
        order.setUserId(uid);
        order.setStatus("PAID");
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setPayAmount(payAmount);
        order.setPayType(payType);
        order.setCouponId(req.getCouponId());
        order.setPickupCode(orderNo.substring(orderNo.length() - 4));
        order.setRemark(req.getRemark());
        order.setPaidAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        for (DrinkOrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        return order;
    }

    public PageResult<DrinkOrder> myOrders(Long uid, long page, long size, String status) {
        LambdaQueryWrapper<DrinkOrder> qw = new LambdaQueryWrapper<DrinkOrder>()
                .eq(DrinkOrder::getUserId, uid);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            qw.eq(DrinkOrder::getStatus, status);
        }
        qw.orderByDesc(DrinkOrder::getId);
        return PageResult.of(orderMapper.selectPage(new Page<>(page, size), qw));
    }

    public Map<String, Object> detail(Long uid, Long id) {
        DrinkOrder order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(uid)) {
            throw new BizException("订单不存在");
        }
        List<DrinkOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<DrinkOrderItem>().eq(DrinkOrderItem::getOrderId, id));
        User user = userService.getById(uid);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", order);
        m.put("items", items);
        m.put("nickname", user.getNickname());
        return m;
    }

    /** 取消：仅 PAID 状态，退款 + 退券 + 回补库存 */
    @Transactional
    public DrinkOrder cancel(Long uid, Long id) {
        DrinkOrder order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(uid)) {
            throw new BizException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
        if (order.getPayAmount() != null && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
            userService.refundBalance(uid, order.getPayAmount(), "REFUND", order.getOrderNo(), "取消饮品订单退款");
        }
        couponService.restoreCoupon(uid, order.getCouponId());
        List<DrinkOrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<DrinkOrderItem>().eq(DrinkOrderItem::getOrderId, id));
        for (DrinkOrderItem item : items) {
            DrinkProduct p = productMapper.selectById(item.getProductId());
            if (p != null && p.getStock() != null && p.getStock() >= 0) {
                productMapper.update(null, new LambdaUpdateWrapper<DrinkProduct>()
                        .eq(DrinkProduct::getId, p.getId())
                        .set(DrinkProduct::getStock, p.getStock() + item.getQuantity()));
            }
        }
        return order;
    }

    /** 管理端推进状态 */
    @Transactional
    public DrinkOrder adminSetStatus(Long id, String status) {
        DrinkOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        String cur = order.getStatus();
        boolean ok = switch (status) {
            case "MAKING" -> "PAID".equals(cur);
            case "READY" -> "MAKING".equals(cur);
            case "FINISHED" -> "MAKING".equals(cur) || "READY".equals(cur);
            case "CANCELLED" -> "PAID".equals(cur);
            default -> false;
        };
        if (!ok) {
            throw new BizException("当前状态「" + cur + "」不能变更为「" + status + "」");
        }
        order.setStatus(status);
        if ("FINISHED".equals(status)) {
            order.setFinishedAt(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        return order;
    }
}
