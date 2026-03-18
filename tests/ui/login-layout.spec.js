const { test, expect } = require("@playwright/test");

test.describe("Login Layout UI", () => {
  test("TC-UI-01: desktop 1366x768 login layout should be stable", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1366, height: 768 });
    await page.goto("/auth/login");

    const wrapper = page.locator(".auth-wrapper");
    const banner = page.locator(".auth-banner");
    const formSide = page.locator(".auth-form-side");

    await expect(wrapper).toBeVisible();
    await expect(banner).toBeVisible();
    await expect(formSide).toBeVisible();

    const wrapperBox = await wrapper.boundingBox();
    expect(wrapperBox).not.toBeNull();
    expect(wrapperBox.width).toBeGreaterThan(800);
    expect(wrapperBox.height).toBeGreaterThan(500);

    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(hasHorizontalOverflow).toBeFalsy();

    await expect(page.getByPlaceholder("yourname@email.com")).toBeVisible();
    await expect(page.getByPlaceholder("••••••••")).toBeVisible();
    await expect(page.getByRole("button", { name: "Đăng nhập" })).toBeVisible();
    await expect(page.getByRole("button", { name: "Đăng nhập" })).toBeEnabled();
  });

  test("TC-UI-02: mobile 390x844 should hide banner and keep form usable", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/auth/login");

    const banner = page.locator(".auth-banner");
    const formSide = page.locator(".auth-form-side");

    await expect(banner).toBeHidden();
    await expect(formSide).toBeVisible();

    const formBox = await formSide.boundingBox();
    expect(formBox).not.toBeNull();
    expect(formBox.width).toBeGreaterThan(300);

    const hasHorizontalOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > window.innerWidth;
    });
    expect(hasHorizontalOverflow).toBeFalsy();

    const emailInput = page.getByPlaceholder("yourname@email.com");
    const passwordInput = page.getByPlaceholder("••••••••");
    const submitButton = page.getByRole("button", { name: "Đăng nhập" });

    await expect(emailInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    await expect(submitButton).toBeVisible();

    await emailInput.fill("test@example.com");
    await passwordInput.fill("password123");

    await expect(emailInput).toHaveValue("test@example.com");
    await expect(passwordInput).toHaveValue("password123");
  });
});
