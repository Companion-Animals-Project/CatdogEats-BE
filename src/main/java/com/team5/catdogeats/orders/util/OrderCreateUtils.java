package com.team5.catdogeats.orders.util;

import com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.users.domain.mapping.Sellers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class OrderCreateUtils {

    public static long calculateTotalDeliveryFee(Map<Sellers, List<OrderItemInfo>> bySeller){
        return bySeller.entrySet().stream()
                .mapToLong(e -> {
                    Sellers s = e.getKey();
                    long sub = e.getValue().stream().mapToLong(OrderItemInfo::totalPrice).sum();
                    return sub >= s.getFreeShippingThreshold() ? 0L : s.getDeliveryFee();
                }).sum();
    }

    public static long applyCouponDiscount(List<GroupSellerAndCouponsDTO> coupons,
                                     Map<Sellers, List<OrderItemInfo>> bySeller){
        if(coupons==null || coupons.isEmpty()) return 0L;

        return coupons.stream().mapToLong(c -> {
            Sellers seller = c.sellers();
            long sub = bySeller.getOrDefault(seller, List.of())
                    .stream().mapToLong(OrderItemInfo::totalPrice).sum();

            return switch (c.coupons().getDiscountType()){
                case PERCENT -> applyCouponDiscountPercent(sub, c.coupons().getDiscountValue());
                case AMOUNT  -> applyCouponDiscountAmount(sub, c.coupons().getDiscountValue());
            };
        }).sum();
    }

    public static Long calculateOriginalTotalPrice(Map<Sellers, List<OrderItemInfo>> groupedOrderItems) {
        return groupedOrderItems.values().stream()
                .flatMap(List::stream)
                .mapToLong(OrderItemInfo::totalPrice)
                .sum();
    }

    public static Long applyCouponDiscountPercent(Long subTotal, Integer couponDiscountRate) {
        if (couponDiscountRate <= 0 || couponDiscountRate > 100) {
            throw new IllegalArgumentException("할인율 0이하 또는 100을 초과할 수 없습니다.");
        }
        return Math.round(subTotal * (couponDiscountRate / 100.0));
    }

    public static long applyCouponDiscountAmount(long subTotal, int amount) {
        if (amount <= 0 || amount > subTotal)
            throw new IllegalArgumentException("할인 금액 오류");
        return amount;
    }

    public static String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(10000, 100000);
        return timestamp + randomSuffix;
    }
}
