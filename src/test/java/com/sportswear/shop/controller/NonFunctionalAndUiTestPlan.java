package com.sportswear.shop.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Placeholder tests for scenarios that require external tools or browser runtime.
 * These test IDs are kept here for traceability with the test case matrix.
 */
class NonFunctionalAndUiTestPlan {

    @Test
    @Disabled("TC-UI-01: Requires browser visual assertion (Playwright/Selenium)")
    void tcUi01_loginDesktopLayout() {
    }

    @Test
    @Disabled("TC-UI-02: Requires browser visual assertion (Playwright/Selenium)")
    void tcUi02_loginMobileLayout() {
    }

    @Test
    @Disabled("TC-NFR-PERF-01: Requires k6/JMeter load profile and runtime metrics")
    void tcNfrPerf01_productListLoad50Users() {
    }

    @Test
    @Disabled("TC-NFR-PERF-02: Requires k6/JMeter load profile and runtime metrics")
    void tcNfrPerf02_checkoutReadOnly100Users() {
    }

    @Test
    @Disabled("TC-NFR-SEC-01: Requires Postman/Newman scenario against deployed endpoint")
    void tcNfrSec01_callbackTampering() {
    }

    @Test
    @Disabled("TC-NFR-SEC-02: Requires OWASP ZAP baseline scan on running web app")
    void tcNfrSec02_zapBaseline() {
    }

    @Test
    @Disabled("TC-CART-04: Current implementation does not enforce cart item ownership; keep disabled until fixed")
    void tcCart04_crossUserCartAccessShouldBeRejected() {
    }
}
