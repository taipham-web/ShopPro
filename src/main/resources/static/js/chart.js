/**
 * Chart.js helper functions for admin dashboard
 */

// Revenue Chart
function initRevenueChart(ctx, labels, data) {
  return new Chart(ctx, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Doanh thu",
          data: data,
          borderColor: "#4e73df",
          backgroundColor: "rgba(78, 115, 223, 0.1)",
          borderWidth: 2,
          fill: true,
          tension: 0.3,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      animation: false,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: function (value) {
              return value.toLocaleString("vi-VN") + " đ";
            },
          },
        },
      },
    },
  });
}

// Orders Chart (Bar)
function initOrdersChart(ctx, labels, data) {
  return new Chart(ctx, {
    type: "bar",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Số đơn hàng",
          data: data,
          backgroundColor: "#1cc88a",
          borderRadius: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            stepSize: 1,
          },
        },
      },
    },
  });
}

// Category Sales Chart (Doughnut)
function initCategorySalesChart(ctx, labels, data) {
  return new Chart(ctx, {
    type: "doughnut",
    data: {
      labels: labels,
      datasets: [
        {
          data: data,
          backgroundColor: [
            "#4e73df",
            "#1cc88a",
            "#36b9cc",
            "#f6c23e",
            "#e74a3b",
          ],
          borderWidth: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      animation: false,
      plugins: {
        legend: {
          position: "bottom",
        },
      },
      cutout: "70%",
    },
  });
}

// Format currency
function formatCurrency(amount) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
}

// Update dashboard stats
function updateDashboardStats(stats) {
  if (stats.totalRevenue) {
    document.getElementById("total-revenue").textContent = formatCurrency(
      stats.totalRevenue,
    );
  }
  if (stats.totalOrders) {
    document.getElementById("total-orders").textContent = stats.totalOrders;
  }
  if (stats.totalProducts) {
    document.getElementById("total-products").textContent = stats.totalProducts;
  }
  if (stats.totalUsers) {
    document.getElementById("total-users").textContent = stats.totalUsers;
  }
}
