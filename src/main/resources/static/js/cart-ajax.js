/**
 * Cart AJAX - Xử lý giỏ hàng không cần reload trang
 */

function getCsrfHeaders() {
  const token =
    document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
  const header =
    document
      .querySelector('meta[name="_csrf_header"]')
      ?.getAttribute("content") || "X-CSRF-TOKEN";
  const headers = { "Content-Type": "application/x-www-form-urlencoded" };
  if (token) headers[header] = token;
  return headers;
}

function formatVND(amount) {
  return Math.round(amount).toLocaleString("en-US") + " đ";
}

function updateSummary(total, grandTotal, freeShipping) {
  const subtotalEl = document.getElementById("summary-subtotal");
  if (subtotalEl) subtotalEl.textContent = formatVND(total);

  const shippingEl = document.getElementById("summary-shipping");
  if (shippingEl) {
    shippingEl.textContent = freeShipping ? "Miễn phí" : "30,000 đ";
    shippingEl.className = freeShipping ? "ship-free" : "";
  }

  const totalEl = document.getElementById("summary-total");
  if (totalEl) totalEl.textContent = formatVND(grandTotal);

  const barSection = document.getElementById("ship-bar-section");
  if (barSection) {
    if (freeShipping) {
      barSection.innerHTML = `<div class="ship-bar" style="background:#f0fff4">
        <i class="fas fa-check-circle" style="color:#28a745;margin-right:6px"></i>
        <strong style="color:#28a745">Bạn được miễn phí vận chuyển!</strong>
      </div>`;
    } else {
      const pct = Math.min((total / 500000) * 100, 100).toFixed(1);
      const remaining = formatVND(500000 - total);
      barSection.innerHTML = `<div class="ship-bar">
        <div class="bar-lbl">Mua thêm <strong>${remaining}</strong> để được miễn phí vận chuyển</div>
        <div class="bar-bg"><div class="bar-fill" style="width:${pct}%"></div></div>
      </div>`;
    }
  }
}

const CartManager = {
  addToCart: function (productId, quantity = 1, size = null) {
    let body = `productId=${productId}&quantity=${quantity}`;
    if (size !== null && size !== undefined && size !== "")
      body += `&size=${size}`;
    fetch("/cart/add", {
      method: "POST",
      headers: getCsrfHeaders(),
      body: body,
    })
      .then((r) => r.json())
      .then((data) => {
        if (data.success) {
          this.showNotification("Đã thêm vào giỏ hàng!", "success");
          this.updateCartCount();
        } else {
          this.showNotification(data.message || "Có lỗi xảy ra!", "error");
        }
      })
      .catch(() => this.showNotification("Có lỗi xảy ra!", "error"));
  },

  updateQuantity: function (cartItemId, quantity) {
    if (quantity < 1) {
      this.removeFromCart(cartItemId);
      return;
    }
    fetch("/cart/update", {
      method: "POST",
      headers: getCsrfHeaders(),
      body: `cartItemId=${cartItemId}&quantity=${quantity}`,
    })
      .then((r) => r.json())
      .then((data) => {
        if (data.success) {
          const row = document.querySelector(`[data-item-id="${cartItemId}"]`);
          if (row) {
            const subtotalEl = row.querySelector(".item-subtotal");
            if (subtotalEl) subtotalEl.textContent = formatVND(data.subtotal);
          }
          updateSummary(data.total, data.grandTotal, data.freeShipping);
          this.updateCartCount();
        } else {
          this.showNotification(data.message || "Có lỗi xảy ra!", "error");
        }
      })
      .catch(() => this.showNotification("Có lỗi xảy ra!", "error"));
  },

  removeFromCart: function (cartItemId) {
    if (!confirm("Bạn có chắc muốn xóa sản phẩm này?")) return;
    fetch("/cart/remove", {
      method: "POST",
      headers: getCsrfHeaders(),
      body: `cartItemId=${cartItemId}`,
    })
      .then((r) => r.json())
      .then((data) => {
        if (data.success) {
          const row = document.querySelector(`[data-item-id="${cartItemId}"]`);
          if (row) row.remove();

          if (data.isEmpty) {
            const container = document.getElementById("cart-container");
            if (container) {
              container.outerHTML = `<div class="empty-cart">
                <i class="fas fa-shopping-cart"></i>
                <p>Giỏ hàng của bạn đang trống</p>
                <a href="/products" class="btn btn-primary"><i class="fas fa-arrow-left"></i> Tiếp tục mua sắm</a>
              </div>`;
            }
          } else {
            updateSummary(data.total, data.grandTotal, data.freeShipping);
          }
          this.showNotification("Đã xóa sản phẩm!", "success");
          this.updateCartCount();
        } else {
          this.showNotification(data.message || "Có lỗi xảy ra!", "error");
        }
      })
      .catch(() => this.showNotification("Có lỗi xảy ra!", "error"));
  },

  updateCartCount: function () {
    fetch("/cart/count")
      .then((r) => r.json())
      .then((data) => {
        const el = document.querySelector(".cart-count");
        if (el) {
          el.textContent = data.count || 0;
          el.style.display = data.count > 0 ? "block" : "none";
        }
      })
      .catch(() => {});
  },

  updateCartDisplay: function () {
    this.updateCartCount();
  },

  showNotification: function (message, type = "info") {
    const n = document.createElement("div");
    n.style.cssText = `position:fixed;top:20px;right:20px;padding:14px 20px;border-radius:8px;
      color:#fff;z-index:9999;display:flex;align-items:center;gap:10px;
      animation:slideIn 0.3s ease;font-size:0.9rem;font-weight:500;
      box-shadow:0 4px 12px rgba(0,0,0,0.15);
      background:${type === "success" ? "#28a745" : type === "error" ? "#dc3545" : "#17a2b8"}`;
    n.innerHTML = `<span>${message}</span><button onclick="this.parentElement.remove()" style="background:none;border:none;color:#fff;font-size:1.1rem;cursor:pointer;padding:0 0 0 4px;">&times;</button>`;
    document.body.appendChild(n);
    setTimeout(() => n.remove(), 3000);
  },

  increaseQuantity: function (cartItemId, inputEl) {
    const newQty = (parseInt(inputEl.value) || 1) + 1;
    inputEl.value = newQty;
    this.updateQuantity(cartItemId, newQty);
  },

  decreaseQuantity: function (cartItemId, inputEl) {
    const cur = parseInt(inputEl.value) || 1;
    if (cur > 1) {
      inputEl.value = cur - 1;
      this.updateQuantity(cartItemId, cur - 1);
    }
  },
};

const _style = document.createElement("style");
_style.textContent = `@keyframes slideIn{from{transform:translateX(100%);opacity:0}to{transform:translateX(0);opacity:1}}`;
document.head.appendChild(_style);

document.addEventListener("DOMContentLoaded", () =>
  CartManager.updateCartCount(),
);
