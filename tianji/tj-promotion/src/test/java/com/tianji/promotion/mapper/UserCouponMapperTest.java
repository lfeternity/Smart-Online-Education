package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Disabled("Manual integration test: requires the promotion database and service infrastructure")
class UserCouponMapperTest {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Test
    void queryMyCoupons() {
        List<Coupon> cs = userCouponMapper.queryMyCoupons(2L);
        System.out.println("cs = " + cs);
    }
}
