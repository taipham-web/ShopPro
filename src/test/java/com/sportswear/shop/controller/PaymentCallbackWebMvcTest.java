package com.sportswear.shop.controller;

import com.sportswear.shop.config.SecurityConfig;
import com.sportswear.shop.controller.user.MomoController;
import com.sportswear.shop.controller.user.PayPalController;
import com.sportswear.shop.controller.user.VNPayController;
import com.sportswear.shop.entity.Order;
import com.sportswear.shop.repository.ProductRepository;
import com.sportswear.shop.repository.UserRepository;
import com.sportswear.shop.service.CartService;
import com.sportswear.shop.service.IOrderService;
import com.sportswear.shop.service.MomoService;
import com.sportswear.shop.service.PayPalService;
import com.sportswear.shop.service.VNPayService;
import com.sportswear.shop.service.impl.CustomOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({VNPayController.class, MomoController.class, PayPalController.class})
@Import(SecurityConfig.class)
class PaymentCallbackWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VNPayService vnPayService;

    @MockitoBean
    private MomoService momoService;

    @MockitoBean
    private PayPalService payPalService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private IOrderService orderService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void vnpayReturnSuccess_shouldSaveOrderAndClearCart() throws Exception {
        Order pending = new Order();
        pending.setTotalAmount(new BigDecimal("430000"));

        Order saved = new Order();
        saved.setOrderCode("ORD-VNPAY-1");
        saved.setTotalAmount(new BigDecimal("430000"));

        when(vnPayService.verifySignature(any())).thenReturn(true);
        when(orderService.save(any(Order.class))).thenReturn(saved);

        mockMvc.perform(get("/vnpay/return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNP123")
                        .param("vnp_TransactionNo", "TXN001")
                        .param("vnp_SecureHash", "dummy")
                        .sessionAttr("pendingVnpayOrder", pending)
                        .sessionAttr("vnpayCartEmail", "user@sportwear.vn"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/checkout-success"));

        verify(orderService).save(any(Order.class));
        verify(cartService).clearCart("user@sportwear.vn");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void vnpayInvalidSignature_shouldRedirectError() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(false);

        mockMvc.perform(get("/vnpay/return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNP123")
                        .param("vnp_SecureHash", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout?error=vnpay_invalid_signature"));

        verify(orderService, never()).save(any(Order.class));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void vnpaySuccessWithoutSession_shouldRedirectSessionExpired() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(true);

        mockMvc.perform(get("/vnpay/return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNP123")
                        .param("vnp_SecureHash", "ok"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout?error=vnpay_session_expired"));

        verify(orderService, never()).save(any(Order.class));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void momoReturnSuccess_shouldSaveOrderAndClearCart() throws Exception {
        Order pending = new Order();
        pending.setTotalAmount(new BigDecimal("500000"));

        Order saved = new Order();
        saved.setOrderCode("ORD-MOMO-1");
        saved.setTotalAmount(new BigDecimal("500000"));

        when(momoService.verifySignature(any())).thenReturn(true);
        when(orderService.save(any(Order.class))).thenReturn(saved);

        mockMvc.perform(get("/momo/return")
                        .param("resultCode", "0")
                        .param("orderId", "MOMO123")
                        .param("transId", "TRANS001")
                        .param("signature", "ok")
                        .sessionAttr("pendingMomoOrder", pending)
                        .sessionAttr("momoCartEmail", "user@sportwear.vn"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/checkout-success"));

        verify(orderService).save(any(Order.class));
        verify(cartService).clearCart("user@sportwear.vn");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void momoInvalidSignature_shouldRedirectError() throws Exception {
        when(momoService.verifySignature(any())).thenReturn(false);

        mockMvc.perform(get("/momo/return")
                        .param("resultCode", "0")
                        .param("orderId", "MOMO123")
                        .param("signature", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout?error=momo_invalid_signature"));

        verify(orderService, never()).save(any(Order.class));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void momoIpnInvalidSignature_shouldReturnInvalidSignatureMessage() throws Exception {
        when(momoService.verifySignature(any())).thenReturn(false);

        mockMvc.perform(post("/momo/ipn")
                .with(csrf())
                        .contentType("application/json")
                        .content("{\"resultCode\":\"0\",\"orderId\":\"MOMO123\",\"signature\":\"bad\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"resultCode\":\"97\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid signature")));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void paypalSuccessCompleted_shouldSaveOrderAndClearCart() throws Exception {
        Order pending = new Order();
        pending.setTotalAmount(new BigDecimal("470000"));

        Order saved = new Order();
        saved.setOrderCode("ORD-PAYPAL-1");
        saved.setTotalAmount(new BigDecimal("470000"));

        Map<String, Object> capture = new HashMap<>();
        capture.put("status", "COMPLETED");

        doReturn(capture).when(payPalService).captureOrder(eq("TOKEN123"));
        when(orderService.save(any(Order.class))).thenReturn(saved);

        mockMvc.perform(get("/paypal/success")
                        .param("token", "TOKEN123")
                        .sessionAttr("pendingPaypalOrder", pending)
                        .sessionAttr("paypalCartEmail", "user@sportwear.vn"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/checkout-success"));

        verify(orderService).save(any(Order.class));
        verify(cartService).clearCart("user@sportwear.vn");
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void paypalNotCompleted_shouldRedirectError() throws Exception {
        Map<String, Object> capture = new HashMap<>();
        capture.put("status", "PAYER_ACTION_REQUIRED");
        doReturn(capture).when(payPalService).captureOrder(eq("TOKEN123"));

        mockMvc.perform(get("/paypal/success")
                        .param("token", "TOKEN123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout?error=paypal_not_completed"));

        verify(orderService, never()).save(any(Order.class));
    }

    @Test
    @WithMockUser(username = "user@sportwear.vn", roles = {"USER"})
    void paypalCancel_shouldRedirectCheckoutCancelled() throws Exception {
        mockMvc.perform(get("/paypal/cancel")
                        .sessionAttr("pendingPaypalOrder", new Order())
                        .sessionAttr("paypalCartEmail", "user@sportwear.vn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout?error=paypal_cancelled"));
    }
}
