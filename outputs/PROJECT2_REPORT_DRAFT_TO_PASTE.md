# Đồ án 2: Xây dựng hệ thống CD cho YAS

## Thông tin nhóm

Môn học: DevOps  
Đề tài: Xây dựng hệ thống CD, monitoring và service mesh cho YAS: Yet Another Shop  
Repository gốc: https://github.com/nashtech-garage/yas  
Repository nhóm: https://github.com/hoanghaitapcode/DevOps_Lab1  
Repository GitOps: https://github.com/DoubleHo05/yas-deployment  
Docker Hub namespace: `doubleho`  

| STT | Họ tên | MSSV | Vai trò |
|---|---|---|---|
| 1 | <Điền họ tên> | <MSSV> | Infrastructure, K3s, dependency |
| 2 | <Điền họ tên> | <MSSV> | Jenkins CI/CD, Docker Hub, observability |
| 3 | <Điền họ tên> | <MSSV> | GitOps, Argo CD, developer_build, cleanup |
| 4 | <Điền họ tên> | <MSSV> | Service Mesh, QA, report evidence |

## 1. Tổng quan

YAS là hệ thống microservices thương mại điện tử sử dụng Java, Spring Boot, Next.js, Keycloak, Kafka, Elasticsearch và Kubernetes. Trong đồ án này, nhóm xây dựng hệ thống CD cho YAS theo hướng Jenkins-first. Jenkins là công cụ CI/CD chính, Docker Hub là image registry, K3s trên Google Cloud là nền tảng Kubernetes, Argo CD được dùng cho GitOps dev/staging, Grafana/Loki/Prometheus/Tempo/OpenTelemetry được dùng cho observability và Istio/Kiali được dùng cho service mesh.

Nhóm không bổ sung GitHub Actions cho phần triển khai mới. Các luồng build, push image, cập nhật GitOps và deploy preview đều được thực hiện qua Jenkins hoặc Argo CD theo đúng phạm vi.

## 2. Kiến trúc hệ thống

Kiến trúc tổng thể gồm các thành phần:

- GitHub source repository chứa source code và Helm chart của YAS.
- Jenkins quét branch, build/test service thay đổi, build Docker image và push lên Docker Hub.
- Docker Hub lưu image của từng service với tag `main`, branch name và commit SHA.
- GitOps repository `DoubleHo05/yas-deployment` lưu values cho dev/staging.
- Argo CD theo dõi GitOps repository và sync vào namespace `dev` hoặc `staging`.
- K3s cluster gồm 1 master node và 1 worker node trên Google Cloud.
- Các dependency gồm PostgreSQL, Kafka, Elasticsearch, Redis và Keycloak.
- Observability stack gồm Prometheus, Grafana, Loki, Promtail, Tempo và OpenTelemetry Collector.
- Service mesh sử dụng Istio và Kiali trong namespace kiểm thử `mesh-demo`.

Chèn hình: Sơ đồ kiến trúc Jenkins -> Docker Hub -> GitOps -> Argo CD -> K3s.

## 3. Kubernetes cluster và dependency

Nhóm triển khai K3s cluster trên Google Cloud Compute Engine với 1 master node và 1 worker node. Mô hình này đáp ứng yêu cầu cluster gồm 1 master và 1 worker.

Chèn hình: Output `kubectl get nodes -o wide`.

Các dependency nền tảng được triển khai trước khi chạy YAS microservices:

- PostgreSQL trong namespace `postgres`.
- Kafka/Strimzi trong namespace `kafka`.
- Elasticsearch/ECK trong namespace `elasticsearch`.
- Redis trong namespace `redis`.
- Keycloak trong namespace `keycloak`.

Chèn hình: Output pod của các dependency.

Trong quá trình triển khai, nhóm dùng Strimzi 0.45.2 vì chart Kafka của YAS sử dụng CRD `kafka.strimzi.io/v1beta2`. Elasticsearch được nâng cấp lên version 9.2.3 để tương thích với service `search` hiện tại.

## 4. Jenkins CI/CD và Docker Hub

Jenkins được sử dụng làm công cụ CI/CD chính. Với mỗi branch developer, Jenkins phát hiện service thay đổi trong monorepo, chạy test/build, sau đó build Docker image và push lên Docker Hub.

Chiến lược image tag:

- `main`: image mặc định cho service ổn định.
- `<branch-name>`: image theo branch developer.
- `<commit-sha>`: image theo commit cuối cùng của branch.

Chèn hình: Jenkins branch build thành công.  
Chèn hình: Docker Hub tag của service, ví dụ `doubleho/yas-tax`.

## 5. Job developer_build và cleanup

Job Jenkins `developer_build` cho phép developer nhập branch/tag muốn deploy cho từng service. Service không nhập sẽ dùng tag mặc định `main` hoặc `latest`. Sau khi deploy, developer có thể truy cập preview bằng domain local và NodePort.

Chèn hình: Parameters của Jenkins job `developer_build`.  
Chèn hình: Console output job `developer_build`.  
Chèn hình: Output `kubectl get pods -n <preview-namespace>`.  
Chèn hình: Output `kubectl get svc -n <preview-namespace>`.

Nhóm cũng tạo cleanup job để xóa preview deployment sau khi developer test xong.

Chèn hình: Jenkins cleanup job và console output.

## 6. GitOps dev/staging bằng Argo CD

Nhóm sử dụng Argo CD cho phần nâng cao dev/staging. Jenkins không deploy trực tiếp lên namespace `dev` hoặc `staging`; thay vào đó Jenkins cập nhật image tag trong GitOps repository `DoubleHo05/yas-deployment`. Argo CD theo dõi repository này và tự động sync vào Kubernetes.

Luồng dev:

```text
Merge main -> Jenkins build/push image -> update envs/dev/<service>-values.yaml -> Argo CD sync namespace dev
```

Luồng staging:

```text
Push release tag vX.Y.Z -> Jenkins build/push image tag release -> update envs/staging/<service>-values.yaml -> Argo CD sync namespace staging
```

Chèn hình: Output `kubectl get applications -n argocd`.  
Chèn hình: Argo CD UI của `yas-root-dev` và `yas-root-staging`.  
Chèn hình: GitOps values file có image tag mới.

## 7. Triển khai và kiểm thử ứng dụng YAS

Vì không có DNS thật, nhóm sử dụng file hosts trên máy developer để trỏ domain về external IP của worker node:

```text
35.198.213.72 storefront.yas.local.com
35.198.213.72 backoffice.yas.local.com
35.198.213.72 identity.yas.local.com
```

Ingress-nginx expose NodePort `30303`, do đó storefront được truy cập qua:

```text
http://storefront.yas.local.com:30303
```

Nhóm đã seed sample data và kiểm thử API product:

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

Kết quả API trả về danh sách sản phẩm như iPhone 15, iPhone 15 Pro, Dell XPS, iPad Pro.

Chèn hình: Browser storefront hiển thị sản phẩm.  
Chèn hình: Curl API trả về `productList`.

## 8. Observability

Nhóm triển khai observability stack trong namespace `observability`, gồm:

- Prometheus để thu thập metrics.
- Grafana để hiển thị dashboard và explore dữ liệu.
- Loki để lưu log.
- Promtail để thu log từ pod.
- Tempo để lưu trace.
- OpenTelemetry Collector để nhận trace OTLP và gửi sang Tempo.

Grafana được truy cập bằng port-forward:

```bash
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Datasource trong Grafana:

- Prometheus
- Loki
- Tempo

Kết quả kiểm thử:

- Prometheus query `up` trả về nhiều target có value `1`.
- Loki query `{namespace="staging"}` hiển thị log của workload.
- Tempo query `{resource.service.name="telemetrygen"}` hiển thị trace `telemetrygen / lets-go`.

Chèn hình: `kubectl get pods -n observability`.  
Chèn hình: Grafana data sources.  
Chèn hình: Prometheus query `up`.  
Chèn hình: Loki query `{namespace="staging"}`.  
Chèn hình: Tempo trace `telemetrygen`.

Ghi chú: Promtail được chạy trên worker node để tránh lỗi `too many open files` trên master. Trace pipeline được kiểm chứng bằng `telemetrygen` theo đường `telemetrygen -> OpenTelemetry Collector -> Tempo -> Grafana`.

## 9. Service Mesh

Nhóm triển khai service mesh bằng Istio sidecar mode và Kiali. Để không ảnh hưởng môi trường demo chính `dev` và `staging`, phần service mesh được thực hiện trong namespace riêng `mesh-demo`.

Các cấu hình chính:

- `PeerAuthentication` bật mTLS STRICT.
- `AuthorizationPolicy` giới hạn service/pod được phép gọi service đích.
- `VirtualService` cấu hình retry khi gặp lỗi 5xx.
- Kiali dùng để quan sát topology.

Chèn hình: `kubectl get pods -n istio-system`.  
Chèn hình: `kubectl get pods -n mesh-demo` có sidecar `istio-proxy`.  
Chèn hình: YAML `PeerAuthentication`.  
Chèn hình: YAML `AuthorizationPolicy`.  
Chèn hình: YAML `VirtualService` retry.  
Chèn hình: Curl allowed.  
Chèn hình: Curl denied/RBAC access denied.  
Chèn hình: Retry evidence.  
Chèn hình: Kiali topology namespace `mesh-demo`.

## 10. Test plan

| Nhóm kiểm thử | Lệnh/Thao tác | Kết quả mong đợi | Trạng thái |
|---|---|---|---|
| Cluster | `kubectl get nodes -o wide` | 1 master, 1 worker Ready | Đạt |
| Docker image | Xem Docker Hub tags | Có tag main/branch/commit SHA | Đạt |
| Argo CD dev | `kubectl get applications -n argocd` | Apps Synced/Healthy | Đạt |
| App web | Mở storefront domain NodePort | Web truy cập được | Đạt |
| API | Curl product featured | Có productList | Đạt |
| Observability metrics | Grafana Prometheus query `up` | Có target value 1 | Đạt |
| Observability logs | Grafana Loki query `{namespace="staging"}` | Có log | Đạt |
| Observability traces | Grafana Tempo query telemetrygen | Có trace | Đạt |
| mTLS | PeerAuthentication STRICT | Có mTLS policy | Đạt |
| Authorization | Curl allowed/denied | Allow 200, deny 403 | Đạt |
| Retry | VirtualService retry + curl 500 | Có retry config/evidence | Đạt |

## 11. Khó khăn và cách xử lý

| Vấn đề | Nguyên nhân | Cách xử lý |
|---|---|---|
| Strimzi chart không tương thích | Version mới dùng CRD `v1`, chart YAS dùng `v1beta2` | Dùng Strimzi 0.45.2 |
| Search service lỗi với Elasticsearch | Elasticsearch 8.8.1 không tương thích client 9.2.3 | Nâng cấp Elasticsearch 8.8.1 -> 8.19.6 -> 9.2.3 |
| BFF không resolve Keycloak | Domain `identity.yas.local.com` không có DNS trong cluster | Patch `hostAliases` tới `keycloak-service` |
| Storefront API lỗi | BFF cần `storefront-nextjs` và internal nginx route | Tạo service alias và nginx route |
| Promtail lỗi trên master | Quá nhiều file log, lỗi `too many open files` | Chạy Promtail trên worker |
| Tempo datasource sai port | Tempo chart expose query port 3200 | Sửa Grafana datasource Tempo sang port 3200 |

## 12. Kết luận

Nhóm đã hoàn thành hệ thống CD cho YAS với Jenkins-first pipeline, Docker Hub image registry, K3s cluster, developer preview, Argo CD GitOps cho dev/staging, observability stack và service mesh. Hệ thống chứng minh được các yêu cầu chính: build/push image theo branch/commit SHA, deploy preview qua Jenkins job `developer_build`, cleanup preview, tự động sync dev/staging bằng Argo CD, truy cập ứng dụng qua NodePort/domain local, quan sát metrics/logs/traces bằng Grafana và kiểm soát service-to-service traffic bằng Istio mTLS, retry policy và AuthorizationPolicy.

## 13. Phụ lục

Đưa các output dài vào phần này:

- Console log Jenkins quan trọng.
- YAML PeerAuthentication.
- YAML AuthorizationPolicy.
- YAML VirtualService.
- Curl allow/deny.
- Danh sách command triển khai observability.
- Danh sách command triển khai service mesh.

