package com.tianji.pay;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeCloseResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled("Manual integration test: calls the real Alipay API")
public class AliPayTest {
    @BeforeEach
    public void init(){
        Factory.setOptions(getOptions());
    }

    String orderNo = "1564894253014872066";
    String refundOrderNo1 = "21294126713451";
    String refundOrderNo2 = "21294129213452";

    @Test
    void testPreCreate() {
        try {
            AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace()
                    .preCreate("pen lv2", orderNo, "2.00");
            if (ResponseChecker.success(response)) {
                System.out.println(response.getQrCode());
                System.out.println(response.getHttpBody());
                System.out.println(response.getCode());
                System.out.println(response.getMsg());
                System.out.println(response.getSubCode());
                System.out.println(response.getSubMsg());
                System.out.println("调用成功");
            } else {
                System.err.println("调用失败，原因：" + response.msg + "，" + response.subMsg);
            }
        } catch (Exception e) {
            System.err.println("调用遭遇异常，原因：" + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    void testQueryPayStatus() throws Exception {
        AlipayTradeQueryResponse response = Factory.Payment.Common().query(orderNo);
        System.out.println("responseBody = " + response.getHttpBody());
        System.out.println("response = " + response);
    }

    @Test
    void testRefund() throws Exception {
        AlipayTradeRefundResponse response = Factory.Payment.Common()
                .optional("query_options", List.of("refund_detail_item_list"))
                .optional("out_request_no", refundOrderNo1)
                .refund(orderNo, "1");

        System.out.println("response = " + response.getHttpBody());
    }

    @Test
    void testQueryRefund() throws Exception {
        AlipayTradeFastpayRefundQueryResponse response = Factory.Payment.Common()
                .queryRefund(orderNo, refundOrderNo1);

        System.out.println("response = " + response.getHttpBody());
    }

    @Test
    void testClose() throws Exception {
        AlipayTradeCloseResponse response = Factory.Payment.Common().close(orderNo);
        System.out.println("response = " + response.getHttpBody());
    }

    private static Config getOptions() {
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = "openapi.alipay.com";
        config.signType = "RSA2";
        config.appId = requireEnv("ALI_APP_ID");
        config.merchantPrivateKey = requireEnv("ALI_MERCHANT_PRIVATE_KEY");
        config.alipayPublicKey = requireEnv("ALI_PUBLIC_KEY");
        config.notifyUrl = requireEnv("ALI_NOTIFY_URL");
        return config;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }
}
