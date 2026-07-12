# Handoff cho bạn làm Service Mesh Project 2 YAS

Ngày cập nhật: 2026-07-08

File này dành cho người vào hỗ trợ phần Service Mesh khi project đã chạy được phần CD, GitOps, dev/staging và observability. Đọc file này trước khi chạy lệnh.

## 1. Tình trạng hiện tại

### Cluster

Cluster đang chạy trên Google Cloud Compute Engine bằng K3s:

| Node | Vai trò | Internal IP | Ghi chú |
|---|---|---|---|
| `yas-master` | control-plane | `10.148.0.7` | Đang chạy nhiều pod hệ thống |
| `yas-worker` | worker | `10.148.0.8` | Node chính để expose NodePort/demo |

Các namespace quan trọng:

```text
argocd
dev
staging
postgres
kafka
elasticsearch
keycloak
redis
observability
istio-system
```

### CD/GitOps đã chạy

Luồng chính hiện tại:

```text
Commit branch developer
-> Jenkins detect service thay đổi
-> build/test
-> push Docker Hub tag branch và commit SHA

Merge main
-> Jenkins build/push image
-> update repo GitOps DoubleHo05/yas-deployment
-> Argo CD auto sync namespace dev

Release tag vX.Y.Z
-> Jenkins build/push image tag release
-> update envs/staging trong GitOps repo
-> Argo CD sync namespace staging
```

Repo liên quan:

```text
Source/chart repo: https://github.com/hoanghaitapcode/DevOps_Lab1.git
GitOps repo:       https://github.com/DoubleHo05/yas-deployment.git
Docker Hub user:   doubleho
```

Không đề xuất thêm GitHub Actions. Jenkins là CI/CD chính.

### Dev/staging application

Dev và staging đã chạy được web/API sau một số runtime fix thủ công:

```text
storefront: http://storefront.yas.local.com:30303
worker external IP dùng trong hosts file: 35.198.213.72
```

File hosts local cần có:

```text
35.198.213.72 storefront.yas.local.com
35.198.213.72 backoffice.yas.local.com
35.198.213.72 identity.yas.local.com
```

API test đã chạy OK:

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

Sample data đã seed thành công:

```bash
curl -i -X POST 'http://storefront.yas.local.com:30303/api/sampledata/storefront/sampledata' \
  -H 'Content-Type: application/json' \
  -d '{"message":"seed"}'
```

### Runtime fix đã làm, không xóa bừa

Các fix này được làm trực tiếp trên cluster để app chạy được:

1. `ingress-nginx-controller` bị ép chạy trên `yas-worker` để NodePort `30303` đi vào worker.
2. BFF trong `dev` và `staging` được patch `hostAliases` để resolve `identity.yas.local.com` sang service Keycloak.
3. Tạo service alias `storefront-nextjs` trong `dev` và `staging` trỏ đến `storefront-ui:3000`.
4. Tạo `nginx` deployment/service/configmap trong `dev` và `staging` để BFF route `/api/...` đến các backend service.
5. Firewall GCP đã mở TCP `30303`.

Không reset namespace `dev` hoặc `staging` khi chưa hỏi lại.

### Dependency đã chạy

Các dependency hiện đã chạy:

```text
Postgres: namespace postgres, chart/operator, pod postgresql-0
Kafka: namespace kafka, Strimzi 0.45.2, Kafka/Zookeeper/Connect Running
Elasticsearch: namespace elasticsearch, ECK, version 9.2.3 Ready
Redis: namespace redis
Keycloak: namespace keycloak, keycloak-service ClusterIP 10.43.39.168
```

Lưu ý Kafka:

```text
Không dùng Strimzi mới nhất.
Chart YAS Kafka dùng kafka.strimzi.io/v1beta2.
Strimzi 1.1.0 sinh CRD v1 nên không hợp.
Strimzi 0.45.2 đang là version đã chạy được.
```

Lưu ý Elasticsearch:

```text
Ban đầu ES 8.8.1 làm search service lỗi.
Đã upgrade 8.8.1 -> 8.19.6 -> 9.2.3.
Không hạ version Elasticsearch.
```

## 2. Observability đã xong

Đã triển khai trong namespace `observability`:

```text
Loki
Promtail
Tempo
cert-manager
OpenTelemetry Operator
OpenTelemetry Collector
kube-prometheus-stack
Grafana
```

Grafana truy cập bằng port-forward:

```bash
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Mở:

```text
http://localhost:3000
user: admin
password: admin
```

Datasource đã có:

```text
Prometheus
Loki
Tempo
```

Bằng chứng đã có:

| Mục | Cách kiểm chứng | Trạng thái |
|---|---|---|
| Metrics | Grafana Explore Prometheus query `up` | OK, có nhiều series value `1` |
| Logs | Grafana Explore Loki query `{namespace="staging"}` | OK, có log |
| Traces | Grafana Explore Tempo query `{resource.service.name="telemetrygen"}` | OK, có trace `telemetrygen / lets-go` |

Lưu ý trung thực cho báo cáo:

```text
Promtail hiện chỉ chạy trên yas-worker vì chạy trên yas-master bị lỗi "too many open files".
Điều này vẫn đủ demo vì đã query được log namespace staging.

Trace app-level của YAS chưa thấy rõ. Đã dùng telemetrygen để chứng minh đường:
telemetrygen -> OpenTelemetry Collector -> Tempo -> Grafana.
```

Lệnh đã dùng để tạo trace test:

```bash
kubectl run telemetrygen -n observability --rm -i --restart=Never \
  --image=ghcr.io/open-telemetry/opentelemetry-collector-contrib/telemetrygen:latest \
  --command -- /telemetrygen traces \
  --otlp-endpoint opentelemetry-collector:4317 \
  --otlp-insecure \
  --traces 20
```

## 3. Istio hiện tại

Istio đã được tải vào repo local:

```text
/Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2
```

Istio precheck đã pass:

```text
✔ No issues found when checking the cluster. Istio is safe to install or upgrade!
```

Istio core đã cài bằng sidecar mode, không cài gateway:

```bash
/Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2/bin/istioctl install \
  -f /Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2/samples/bookinfo/demo-profile-no-gateways.yaml \
  -y
```

Pod đã chạy:

```text
namespace istio-system:
istiod 1/1 Running
```

Chưa cài Kiali.

## 4. Mục tiêu Service Mesh cần hoàn thành

Yêu cầu cần chứng minh:

1. Enable mTLS giữa các service.
2. Có Kiali topology.
3. Có retry policy, ví dụ service trả 500 thì retry.
4. Có AuthorizationPolicy cho allow/deny.
5. Test curl từ pod trong cluster để chứng minh allow/deny.
6. Có YAML manifest, screenshot, test plan, logs evidence, README hướng dẫn.

Khuyến nghị: không bật mesh toàn bộ namespace `dev`/`staging` ngay. Dùng namespace riêng `mesh-demo` để không phá demo web/CD đang ổn.

## 5. Chiến lược Service Mesh an toàn

Làm trong namespace riêng:

```text
mesh-demo
```

Triển khai service demo:

```text
tax: service YAS thật để chứng minh app YAS có sidecar và mTLS.
order: service YAS thật hoặc curl client dùng serviceAccount order để chứng minh allow.
blocked-client: curl client dùng serviceAccount default để chứng minh deny.
faulty: service test trả 500 để chứng minh retry policy rõ ràng.
```

Vì các chart YAS có thể phụ thuộc DB/config, nếu `tax` hoặc `order` mất thời gian xử lý thì vẫn nên giữ demo mesh bằng workload nhẹ:

```text
allowed-client -> tax/httpbin/faulty
blocked-client -> tax/httpbin/faulty
```

Trong báo cáo ghi rõ: `mesh-demo` là namespace kiểm thử service mesh tách biệt để không phá dev/staging.

## 6. Chạy Service Mesh từng bước

### Bước 1. Cài Kiali

Chạy:

```bash
kubectl apply -f /Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2/samples/addons/kiali.yaml
```

Kiểm tra:

```bash
kubectl rollout status deployment/kiali -n istio-system --timeout=180s
```

Mở Kiali:

```bash
kubectl port-forward -n istio-system svc/kiali 20001:20001
```

Mở browser:

```text
http://localhost:20001/kiali
```

Nếu Kiali báo không có Prometheus hoặc topology không hiện metric, chạy thêm Prometheus sample của Istio:

```bash
kubectl apply -f /Users/giabao/Documents/devops/DevOps_Lab1/istio-1.30.2/samples/addons/prometheus.yaml
```

Giải thích nếu bị hỏi: observability chính của project dùng Prometheus/Grafana trong namespace `observability`; Prometheus sample trong `istio-system` chỉ phục vụ Kiali topology nhanh cho service mesh demo.

### Bước 2. Tạo namespace mesh-demo có sidecar injection

Manifest đã có:

```text
outputs/service-mesh/01-mesh-demo-namespace.yaml
```

Chạy:

```bash
kubectl apply -f outputs/service-mesh/01-mesh-demo-namespace.yaml
```

Kiểm tra:

```bash
kubectl get ns mesh-demo --show-labels
```

Kỳ vọng có:

```text
istio-injection=enabled
```

### Bước 3. Cài cấu hình YAS vào mesh-demo

Các backend chart cần `yas-configuration-configmap` và `yas-postgresql-credentials-secret`.

Chạy:

```bash
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n mesh-demo --create-namespace
```

Kiểm tra:

```bash
kubectl get cm,secret -n mesh-demo | grep yas
```

### Bước 4. Deploy service YAS `tax`

Build dependency chart nếu cần:

```bash
helm dependency build k8s/charts/tax
```

Deploy:

```bash
helm upgrade --install tax k8s/charts/tax \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-tax \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false \
  --set backend.ingress.enabled=false
```

Kiểm tra:

```bash
kubectl get pods -n mesh-demo
```

Kỳ vọng pod tax có `2/2 Running`, vì có container app và `istio-proxy`.

Kiểm tra container:

```bash
kubectl get pod -n mesh-demo -l app.kubernetes.io/name=tax \
  -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

Kỳ vọng:

```text
tax-... containers=tax istio-proxy
```

Nếu tax crash vì DB/config, đừng mất quá nhiều thời gian. Chụp lỗi rồi chuyển sang workload demo ở Bước 5 để lấy điểm mesh.

### Bước 5. Tạo client allow và deny

Tạo serviceAccount `order` để giả lập service được phép gọi tax:

```bash
kubectl create serviceaccount order -n mesh-demo --dry-run=client -o yaml | kubectl apply -f -
```

Tạo client hợp lệ:

```bash
kubectl run allowed-client -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

Tạo client bị chặn:

```bash
kubectl run blocked-client -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

Kiểm tra sidecar:

```bash
kubectl get pods -n mesh-demo
```

Kỳ vọng `allowed-client` và `blocked-client` cũng là `2/2 Running`.

### Bước 6. Apply mTLS STRICT

Manifest đã có:

```text
outputs/service-mesh/02-mtls-strict.yaml
```

Chạy:

```bash
kubectl apply -f outputs/service-mesh/02-mtls-strict.yaml
```

Kiểm tra:

```bash
kubectl get peerauthentication -n mesh-demo -o yaml
```

Chụp màn hình/lưu output có:

```text
mtls:
  mode: STRICT
```

### Bước 7. Apply AuthorizationPolicy allow/deny

Manifest default deny:

```text
outputs/service-mesh/04-authz-default-deny.yaml
```

Manifest allow order gọi tax:

```text
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
```

Chạy:

```bash
kubectl apply -f outputs/service-mesh/04-authz-default-deny.yaml
```

Chạy:

```bash
kubectl apply -f outputs/service-mesh/05-authz-allow-order-to-tax.yaml
```

Kiểm tra:

```bash
kubectl get authorizationpolicy -n mesh-demo -o yaml
```

### Bước 8. Test allow/deny

Test allowed:

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- \
  curl -i http://tax:8090/actuator/health
```

Nếu port `8090` không đi được, thử port HTTP app:

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- \
  curl -i http://tax/tax/actuator/health
```

Test denied:

```bash
kubectl exec -n mesh-demo blocked-client -c curl -- \
  curl -i http://tax:8090/actuator/health
```

Kỳ vọng:

```text
allowed-client: HTTP 200 hoặc response health của tax.
blocked-client: HTTP 403 RBAC access denied.
```

Nếu `tax` chưa chạy được, dùng service demo thay thế ở phần dưới để chứng minh policy.

## 7. Fallback nếu service YAS khó chạy trong mesh-demo

Nếu deploy `tax` bị crash và không kịp sửa, dùng workload HTTP nhẹ nhưng đặt tên theo kịch bản mesh để chứng minh kỹ thuật.

Tạo service `tax-mock` trả HTTP:

```bash
kubectl create deployment tax-mock -n mesh-demo --image=hashicorp/http-echo:1.0 \
  --dry-run=client -o yaml -- \
  -text='tax mock ok' -listen=:8080 | kubectl apply -f -
```

Expose:

```bash
kubectl expose deployment tax-mock -n mesh-demo --port=80 --target-port=8080
```

Patch AuthorizationPolicy allow sang label của `tax-mock` nếu cần:

```bash
kubectl get pod -n mesh-demo --show-labels | grep tax-mock
```

Sau đó tạo AuthorizationPolicy riêng cho `tax-mock`.

Trong báo cáo ghi: `tax-mock` là workload kiểm thử retry/authorization deterministic, còn namespace `dev/staging` đang chạy YAS thật.

## 8. Retry policy

Manifest retry hiện có:

```text
outputs/service-mesh/03-tax-retry-virtualservice.yaml
```

Apply:

```bash
kubectl apply -f outputs/service-mesh/03-tax-retry-virtualservice.yaml
```

Kiểm tra:

```bash
kubectl get virtualservice tax-retry -n mesh-demo -o yaml
```

Kỳ vọng có:

```text
retries:
  attempts: 3
  perTryTimeout: 2s
  retryOn: 5xx,connect-failure,refused-stream
```

Để có bằng chứng retry rõ hơn, nên tạo service cố tình trả 500:

```bash
kubectl create deployment faulty -n mesh-demo --image=kennethreitz/httpbin --dry-run=client -o yaml | kubectl apply -f -
```

Expose:

```bash
kubectl expose deployment faulty -n mesh-demo --port=80 --target-port=80
```

Tạo VirtualService retry cho `faulty`:

```bash
kubectl apply -n mesh-demo -f - <<'EOF'
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: faulty-retry
spec:
  hosts:
    - faulty
  http:
    - route:
        - destination:
            host: faulty
            port:
              number: 80
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx,connect-failure,refused-stream
EOF
```

Gọi endpoint trả 500:

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- \
  curl -i http://faulty/status/500
```

Bằng chứng cần chụp:

```text
VirtualService faulty-retry có attempts=3.
curl trả 500.
Kiali topology hoặc Envoy metrics cho thấy traffic qua service.
```

Nếu cần log Envoy:

```bash
kubectl logs -n mesh-demo deploy/faulty -c istio-proxy --tail=80
```

## 9. Tạo traffic cho Kiali topology

Chạy vài request:

```bash
for i in $(seq 1 30); do
  kubectl exec -n mesh-demo allowed-client -c curl -- curl -s http://tax:8090/actuator/health >/dev/null || true
done
```

Nếu dùng `faulty`:

```bash
for i in $(seq 1 30); do
  kubectl exec -n mesh-demo allowed-client -c curl -- curl -s http://faulty/status/500 >/dev/null || true
done
```

Mở Kiali:

```bash
kubectl port-forward -n istio-system svc/kiali 20001:20001
```

Vào:

```text
http://localhost:20001/kiali
```

Chọn:

```text
Graph -> Namespace mesh-demo -> Last 10m -> Display traffic animation/edges
```

Chụp ảnh topology.

## 10. Evidence phải nộp cho Service Mesh

Chụp/sao lưu các output sau:

### Istio/Kiali

```bash
kubectl get pods -n istio-system
```

```bash
kubectl get svc -n istio-system
```

Ảnh Kiali topology namespace `mesh-demo`.

### Sidecar injection

```bash
kubectl get pods -n mesh-demo
```

```bash
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

### mTLS

```bash
kubectl get peerauthentication -n mesh-demo -o yaml
```

### AuthorizationPolicy

```bash
kubectl get authorizationpolicy -n mesh-demo -o yaml
```

### Retry

```bash
kubectl get virtualservice -n mesh-demo -o yaml
```

### Curl allow/deny

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://tax:8090/actuator/health
```

```bash
kubectl exec -n mesh-demo blocked-client -c curl -- curl -i http://tax:8090/actuator/health
```

Nếu dùng `faulty`:

```bash
kubectl exec -n mesh-demo allowed-client -c curl -- curl -i http://faulty/status/500
```

## 11. Những việc không nên làm

Không làm các việc sau khi chưa hỏi lại:

```text
Không reset namespace dev/staging.
Không xóa dependency postgres/kafka/elasticsearch/keycloak/redis.
Không chạy lại toàn bộ setup-cluster.sh.
Không cài lại Strimzi version mới nhất.
Không đổi Elasticsearch khỏi version 9.2.3.
Không bật default-deny AuthorizationPolicy trực tiếp trong namespace dev/staging.
Không label istio-injection=enabled cho dev/staging nếu chưa có kế hoạch rollback.
```

## 12. Câu trả lời nếu thầy hỏi

### Vì sao Service Mesh làm trong `mesh-demo`?

Vì namespace `dev` và `staging` đang là môi trường CD/GitOps demo chính. Nhóm tạo `mesh-demo` để kiểm thử mTLS, AuthorizationPolicy, retry và Kiali topology mà không làm gián đoạn luồng Jenkins -> Argo CD -> dev/staging.

### Vì sao observability có trace từ `telemetrygen`?

YAS hiện đã chạy metrics/logs. Đường trace được kiểm chứng bằng `telemetrygen` theo chuẩn OpenTelemetry để chứng minh collector và Tempo hoạt động:

```text
telemetrygen -> OpenTelemetry Collector -> Tempo -> Grafana
```

Nếu còn thời gian, nhóm sẽ bổ sung Java agent hoặc tracing bridge cho app image để có span app-level.

### Vì sao Promtail chỉ chạy worker?

Promtail trên master gặp lỗi:

```text
too many open files
```

Do master đang giữ nhiều pod/control-plane log. Để demo ổn định, nhóm chạy Promtail trên worker, nơi vẫn thu được log workload và chứng minh Loki/Grafana hoạt động.

## 13. Thứ tự ưu tiên cho người làm tiếp

1. Cài Kiali.
2. Tạo `mesh-demo`.
3. Deploy `yas-configuration` và `tax`.
4. Kiểm tra sidecar `istio-proxy`.
5. Apply mTLS STRICT.
6. Tạo allowed/blocked client.
7. Apply AuthorizationPolicy.
8. Test allow/deny bằng curl.
9. Apply retry VirtualService.
10. Tạo traffic và chụp Kiali topology.
11. Gom YAML/output/screenshot vào báo cáo.

Nếu còn dưới 2 giờ:

```text
Ưu tiên Kiali + mTLS + AuthorizationPolicy allow/deny.
Retry có thể dùng faulty/httpbin để chứng minh nhanh.
Không cố sửa tax nếu nó crash quá lâu.
```
