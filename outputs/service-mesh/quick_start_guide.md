# HƯỚNG DẪN VẤN ĐÁP NHANH - SERVICE MESH (ISTIO)

Tài liệu này tổng hợp toàn bộ các lệnh cần dùng để kiểm tra, vận hành và chứng minh các tính năng của Service Mesh (Istio) khi báo cáo và vấn đáp với Giáo viên.

---

## 1. KIỂM TRA TRẠNG THÁI HỆ THỐNG (Trước khi bắt đầu)

Trước khi giáo viên yêu cầu demo, hãy chạy các lệnh sau để đảm bảo mọi Pod trong namespace kiểm thử đang hoạt động ổn định:

*   **Kiểm tra danh sách các Pod (đảm bảo tất cả đều `Running` và `READY 2/2`):**
    ```bash
    kubectl get pods -n mesh-demo
    ```
*   **Trường hợp Pod client (`allowed-client` hoặc `blocked-client`) bị lỗi `Unknown/Failed` (do Node restart):**
    Hãy chạy lệnh xóa và tạo lại nhanh chóng:
    ```bash
    # Xóa các Pod cũ
    kubectl delete pod allowed-client blocked-client -n mesh-demo --force --grace-period=0
    
    # Tạo lại các Pod mới (sử dụng file manifest trong thư mục hiện tại)
    kubectl apply -f allowed-client.yaml
    kubectl apply -f blocked-client.yaml
    ```

---

## 2. KÍCH HOẠT TRAFFIC NỀN & MỞ DASHBOARDS (Chuẩn bị Demo)

Để giáo viên thấy luồng dữ liệu động (mũi tên chuyển động, màu xanh lá/cam) trên Kiali, hãy mở 3 tab terminal và chạy các lệnh sau:

*   **Tab 1: Chạy Script tạo traffic tự động trong 15 phút:**
    *   *Trên Windows (PowerShell):*
        ```powershell
        powershell.exe -ExecutionPolicy Bypass -File ./traffic_generator.ps1
        ```
    *   *Trên macOS / Linux (Terminal):*
        ```bash
        chmod +x ./traffic_generator.sh
        ./traffic_generator.sh
        ```
*   **Tab 2: Mở Port-Forward cho Kiali Dashboard:**
    ```bash
    kubectl port-forward -n istio-system svc/kiali 20001:20001
    ```
    👉 Truy cập Kiali: **[http://localhost:20001/kiali/](http://localhost:20001/kiali/)** (Vào Graph -> Chọn namespace `mesh-demo` -> Tích chọn `Traffic Animation` và `Security` trong phần **Display**).

*   **Tab 3: Mở Port-Forward cho Grafana Dashboard (Nếu GV yêu cầu xem Observability):**
    ```bash
    kubectl port-forward -n observability svc/prometheus-grafana 3000:80
    ```
    👉 Truy cập Grafana: **[http://localhost:3000](http://localhost:3000)** (Tài khoản: `admin` / `admin`).

---

## 3. CÁC LỆNH DEMO TRỰC TIẾP CHO GIÁO VIÊN XEM

Khi giáo viên yêu cầu thực hiện test trực tiếp bằng dòng lệnh:

### Yêu cầu 1: Chứng minh tính năng Phân quyền (AuthorizationPolicy)
Chúng ta gọi vào API công khai `/tax/v3/api-docs` để kiểm tra phân quyền rõ rệt nhất:

*   **Lệnh Test 1: Truy cập được phép (Allowed Client - SA `order`):**
    ```bash
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -sI http://tax/tax/v3/api-docs
    ```
    👉 *Kết quả kỳ vọng:* Trả về **`HTTP/1.1 200 OK`** và có header **`server: envoy`** (đường nối trên Kiali màu **Xanh lá**).

*   **Lệnh Test 2: Truy cập bị từ chối (Blocked Client - SA `default`):**
    ```bash
    kubectl exec -n mesh-demo blocked-client -c curl -- curl -i -s http://tax/tax/v3/api-docs
    ```
    👉 *Kết quả kỳ vọng:* Trả về **`HTTP/1.1 403 Forbidden`** kèm thông báo **`RBAC: access denied`** (đường nối trên Kiali màu **Cam**).

---

### Yêu cầu 2: Chứng minh bảo mật đường truyền (mTLS STRICT)
*   **Lệnh in cấu hình mTLS STRICT của namespace:**
    ```bash
    kubectl get peerauthentication default -n mesh-demo -o yaml
    ```
    👉 *Giải thích:* Cấu hình `mode: STRICT` ép buộc tất cả các service trong namespace `mesh-demo` bắt buộc phải giao tiếp qua TLS mã hóa, các kết nối HTTP thông thường từ ngoài mesh sẽ bị từ chối.

*   **Lệnh in chính sách AuthorizationPolicy chặn mặc định:**
    ```bash
    kubectl get authorizationpolicy -n mesh-demo -o yaml
    ```
    👉 *Giải thích:* Chính sách `default-deny` chặn toàn bộ traffic theo mặc định, sau đó chúng ta sử dụng chính sách `allow-order-to-tax` để chỉ mở quyền cho đúng ServiceAccount `order`.

---

### Yêu cầu 3: Chứng minh chính sách tự động thử lại (Retry Policy)
Chúng ta sẽ gửi request lỗi 500 tới service `faulty` để xem Envoy tự động thử lại:

*   **Bước A: Chạy lệnh gửi request lỗi:**
    ```bash
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -s -o /dev/null http://faulty/status/500
    ```
*   **Bước B: Show log của Envoy proxy để xem kết quả retry:**
    ```bash
    kubectl logs -n mesh-demo -l app=faulty -c istio-proxy --tail=10
    ```
    👉 *Kết quả kỳ vọng:* Trong log của container `istio-proxy` xuất hiện **chính xác 4 dòng log liên tiếp** gọi `GET /status/500` có cùng một Request ID trùng khớp (bao gồm 1 lần gọi chính và 3 lần tự động retry của Envoy).
