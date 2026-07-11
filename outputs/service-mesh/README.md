# Hướng dẫn Vận hành, Giám sát và Kiểm thử Service Mesh (Istio) & Observability - Dự án YAS

Tài liệu này hướng dẫn chi tiết cách chạy cổng giám sát (Kiali, Grafana) và thực hiện các kịch bản kiểm thử để chứng minh tính năng bảo mật mTLS, phân quyền kết nối (AuthorizationPolicy), và chính sách tự động thử lại (Retry Policy) trong dự án.

---

## 1. Hướng dẫn Giám sát Hệ thống (Port-Forwarding)

Để truy cập các bảng điều khiển (Dashboards) từ máy cá nhân của bạn tới Kubernetes cluster, hãy chạy các lệnh port-forward sau:

### A. Kiali Dashboard (Giám sát Service Mesh).
1.  **Chạy lệnh port-forward:**
    ```bash
    kubectl port-forward -n istio-system svc/kiali 20001:20001
    ```
2.  **Truy cập trình duyệt:** **[http://localhost:20001/kiali/](http://localhost:20001/kiali/)**
3.  **Cách xem sơ đồ Topology:**
    *   Vào menu **Graph** ở cột trái.
    *   Chọn Namespace: `mesh-demo` (hoặc các namespace khác như `staging`, `dev`).
    *   Chọn thời gian hiển thị: `Last 10m` hoặc `Last 1h`.
    *   Tích chọn các tùy chọn trong menu **Display** (ở thanh trên cùng của đồ thị):
        *   `Traffic Animation` (hiệu ứng luồng request động).
        *   `Security` (hiển thị biểu tượng ổ khóa 🔒 thể hiện mTLS).
        *   `Service Nodes` (hiển thị các service dạng hình tam giác).

### B. Grafana Dashboard (Giám sát Logs, Metrics, Traces)
1.  **Chạy lệnh port-forward:**
    ```bash
    kubectl port-forward -n observability svc/prometheus-grafana 3000:80
    ```
2.  **Truy cập trình duyệt:** **[http://localhost:3000](http://localhost:3000)**
3.  **Đăng nhập:**
    *   **Username:** `admin`
    *   **Password:** `admin`
4.  **Cách truy vấn dữ liệu giám sát (Observability):**
    *   Vào menu **Explore** ở thanh công cụ bên trái.
    *   **Xem Metrics (Prometheus):**
        *   Chọn datasource `Prometheus`.
        *   Nhập câu truy vấn: `up` (xem trạng thái hoạt động của các pod) hoặc `http_server_requests_seconds_count` (thống kê HTTP request).
    *   **Xem Logs (Loki):**
        *   Chọn datasource `Loki`.
        *   Nhập câu truy vấn: `{namespace="mesh-demo"}` hoặc `{namespace="staging"}` để lọc log theo môi trường.
    *   **Xem Traces (Tempo):**
        *   Chọn datasource `Tempo`.
        *   Chuyển sang tab **Search** và chọn dịch vụ `telemetrygen` để kiểm tra phân tích vết luồng đi của request.

---

## 2. Kịch bản Kiểm thử Service Mesh (mesh-demo)

Dưới đây là các kịch bản thực tế bạn có thể chạy bằng dòng lệnh hoặc trực tiếp gửi request từ trong Cluster để chứng minh tính đúng đắn trước các thầy.

### Kịch bản 1: Phân quyền kết nối rõ rệt nhất (Xanh vs Cam trên Kiali)
Để chứng minh rõ nhất sự phân quyền (Allowed vs Blocked):
*   **allowed-client** (được cấp quyền bằng SA `order`): Truy cập thành công và nhận mã **HTTP 200** vì gọi vào endpoint tài liệu Swagger công khai không bắt đăng nhập.
*   **blocked-client** (bị chặn bằng SA `default`): Bị Envoy proxy chặn ngay ở cổng và nhận mã **HTTP 403**.

1.  **Chạy test cho Allowed Client (Kỳ vọng HTTP 200 OK -> Kiali vẽ đường màu XANH LÁ):**
    ```bash
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://tax/tax/v3/api-docs
    ```
2.  **Chạy test cho Blocked Client (Kỳ vọng HTTP 403 Forbidden -> Kiali vẽ đường màu CAM):**
    ```bash
    kubectl exec -n mesh-demo blocked-client -c curl -- curl -i http://tax/tax/v3/api-docs
    # Kết quả trả về: RBAC: access denied từ server: envoy
    ```

### Kịch bản 2: Gọi API cần đăng nhập (Kiểm thử ứng dụng YAS)
Gọi vào API nghiệp vụ `/tax/backoffice/tax-classes`:
*   **allowed-client** vượt qua được mTLS/AuthorizationPolicy của Istio nhưng bị ứng dụng Spring Boot chặn lại do chưa truyền JWT Token bảo mật.
*   **blocked-client** bị Istio chặn ngay từ cổng vào.

1.  **Allowed Client (Kỳ vọng HTTP 401 Unauthorized từ Tomcat ứng dụng):**
    ```bash
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://tax/tax/backoffice/tax-classes
    # Header trả về: server: envoy và set-cookie: JSESSIONID=...
    ```
2.  **Blocked Client (Kỳ vọng HTTP 403 Forbidden từ Envoy proxy):**
    ```bash
    kubectl exec -n mesh-demo blocked-client -c curl -- curl -i http://tax/tax/backoffice/tax-classes
    # Phản hồi chứa: RBAC: access denied
    ```

### Kịch bản 3: Tự động thử lại (Retry Policy)
Gửi yêu cầu tới service lỗi `faulty` cố tình trả về mã lỗi 500:
1.  **Gửi request từ client:**
    ```bash
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://faulty/status/500
    ```
2.  **Kiểm tra số lần thử lại thực tế trong log của proxy:**
    ```bash
    kubectl logs -n mesh-demo -l app=faulty -c istio-proxy --tail=10
    ```
    *Kỳ vọng:* Xuất hiện chính xác **4 dòng log** ghi nhận yêu cầu `GET /status/500` có cùng một mã request ID duy nhất (gồm 1 lượt gọi gốc và 3 lượt Envoy tự động thực hiện lại).

---

## 3. Tạo Traffic liên tục (phục vụ thuyết trình/demo)
Nếu bạn đang demo trực tiếp cho các thầy xem Kiali Graph động và không muốn gõ lệnh tay liên tục để tạo traffic, hãy chạy các file script tự động gửi request được đính kèm sẵn trong thư mục này:

*   **Nếu chạy trên Windows (bằng PowerShell):**
    ```powershell
    powershell.exe -ExecutionPolicy Bypass -File ./traffic_generator.ps1
    ```
*   **Nếu chạy trên macOS/Linux (bằng Terminal):**
    ```bash
    chmod +x ./traffic_generator.sh
    ./traffic_generator.sh
    ```
Script này sẽ chạy liên tục trong 15 phút, gửi request đều đặn để Kiali vẽ sơ đồ động (màu xanh lá đối với `allowed-client` và màu cam đối với `blocked-client`) kèm các chấm chuyển động. Bạn chỉ cần nhấn `Ctrl+C` trong terminal để dừng script bất cứ lúc nào.
