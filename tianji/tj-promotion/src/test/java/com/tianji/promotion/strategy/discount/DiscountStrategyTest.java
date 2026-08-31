package com.tianji.promotion.strategy.discount;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.DiscountType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountStrategyTest {

    @Test
    void shouldCalculatePriceDiscount() {
        Coupon coupon = coupon(10_000, 2_000, 0);
        Discount discount = DiscountStrategy.getDiscount(DiscountType.PRICE_DISCOUNT);

        assertTrue(discount.canUse(12_000, coupon));
        assertEquals(2_000, discount.calculateDiscount(12_000, coupon));
    }

    @Test
    void shouldCapPerPriceDiscount() {
        Coupon coupon = coupon(10_000, 1_500, 4_000);
        Discount discount = DiscountStrategy.getDiscount(DiscountType.PER_PRICE_DISCOUNT);

        assertTrue(discount.canUse(35_000, coupon));
        assertEquals(4_000, discount.calculateDiscount(35_000, coupon));
    }

    @Test
    void shouldCalculateRateDiscount() {
        Coupon coupon = coupon(10_000, 80, 5_000);
        Discount discount = DiscountStrategy.getDiscount(DiscountType.RATE_DISCOUNT);

        assertTrue(discount.canUse(20_000, coupon));
        assertEquals(4_000, discount.calculateDiscount(20_000, coupon));
    }

    @Test
    void shouldRequirePayableBalanceForNoThresholdDiscount() {
        Coupon coupon = coupon(0, 1_000, 0);
        Discount discount = DiscountStrategy.getDiscount(DiscountType.NO_THRESHOLD);

        assertFalse(discount.canUse(1_000, coupon));
        assertTrue(discount.canUse(1_001, coupon));
        assertEquals(1_000, discount.calculateDiscount(1_001, coupon));
    }

    private Coupon coupon(int thresholdAmount, int discountValue, int maxDiscountAmount) {
        return new Coupon()
                .setThresholdAmount(thresholdAmount)
                .setDiscountValue(discountValue)
                .setMaxDiscountAmount(maxDiscountAmount);
    }
}
