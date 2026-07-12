# Runbook Argo CD và Observability cho Project 2

Tài liệu này tổng hợp lại cách nhóm đã triển khai Argo CD và observability cho hệ thống YAS. Mục tiêu là để:

- Có lệnh copy-paste khi cần dựng lại hoặc kiểm tra.
- Có câu trả lời ngắn gọn khi vấn đáp.
- Có checklist ảnh/log cần chụp đưa vào báo cáo.

Nguyên tắc chính của đồ án:

- Jenkins là CI/CD chính.
- Argo CD chỉ làm GitOps deploy cho `dev` và `staging`.
- Observability được tách riêng trong namespace `observability`.
- Không dùng GitHub Actions cho luồng triển khai mới.

## 1. Kiến trúc Argo CD đã làm

Luồng `dev`:

```text
Developer commit/merge vào main
-> Jenkins detect service thay đổi
-> Jenkins build/test
-> Jenkins build image và push Docker Hub
-> Jenkins cập nhật repo GitOps DoubleHo05/yas-deployment
-> sửa envs/dev/<service>-values.yaml
-> Argo CD auto sync vào namespace dev
```

Luồng `staging`:

```text
Tạo release tag vX.Y.Z từ main
-> Jenkins build image tag vX.Y.Z
-> Jenkins push image lên Docker Hub
-> Jenkins cập nhật repo GitOps DoubleHo05/yas-deployment
-> sửa envs/staging/<service>-values.yaml
-> Argo CD auto sync vào namespace staging
```

Repo GitOps đang dùng:

```text
https://github.com/DoubleHo05/yas-deployment.git
```

Cấu trúc chính trong repo GitOps:

```text
argocd/root-dev.yaml
argocd/root-staging.yaml
apps/dev/applications.yaml
apps/staging/applications.yaml
envs/dev/*-values.yaml
envs/staging/*-values.yaml
```

Vai trò:

- Jenkins build image và update GitOps repo.
- Argo CD đọc GitOps repo và apply manifest/Helm values vào Kubernetes.
- Argo CD không build image.

## 2. Lệnh cài Argo CD

Tạo namespace:

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
```

Cài Argo CD:

```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Chờ các pod Argo CD chạy:

```bash
kubectl get pods -n argocd
```

Kỳ vọng các pod chính Running:

```text
argocd-application-controller
argocd-applicationset-controller
argocd-dex-server
argocd-notifications-controller
argocd-redis
argocd-repo-server
argocd-server
```

## 3. Apply root app dev/staging

Clone repo GitOps:

```bash
cd /tmp
git clone https://github.com/DoubleHo05/yas-deployment.git
cd /tmp/yas-deployment
```

Apply root app cho `dev`:

```bash
kubectl apply -f argocd/root-dev.yaml
```

Apply root app cho `staging`:

```bash
kubectl apply -f argocd/root-staging.yaml
```

Kiểm tra Argo CD applications:

```bash
kubectl get applications -n argocd
```

Kỳ vọng có các app dạng:

```text
yas-root-dev
yas-root-staging
yas-tax-dev
yas-tax-staging
yas-product-dev
yas-product-staging
...
```

## 4. Mở Argo CD UI

Port-forward Argo CD server:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Mở browser:

```text
https://localhost:8080
```

Lấy password admin:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d; echo
```

Đăng nhập:

```text
Username: admin
Password: password vừa lấy
```

Trong UI:

- App có hậu tố `-dev` là môi trường `dev`.
- App có hậu tố `-staging` là môi trường `staging`.
- Bấm vào app, chọn Deployment/Pod để xem image tag.

Ví dụ kiểm tra tag release của tax staging:

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng sau release `v1.0.0`:

```text
docker.io/doubleho/yas-tax:v1.0.0
```

## 5. Lệnh kiểm tra Argo CD khi demo

Xem toàn bộ app:

```bash
kubectl get applications -n argocd
```

Xem app tax dev:

```bash
kubectl get app yas-tax-dev -n argocd
```

Xem app tax staging:

```bash
kubectl get app yas-tax-staging -n argocd
```

Xem image đang chạy ở dev:

```bash
kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Xem image đang chạy ở staging:

```bash
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Ép Argo CD refresh nếu UI chưa cập nhật kịp:

```bash
kubectl annotate app yas-tax-staging -n argocd argocd.argoproj.io/refresh=hard --overwrite
```

Kiểm tra rollout của service:

```bash
kubectl rollout status deployment/tax -n staging --timeout=180s
```

## 6. Vì sao một số app BFF có thể hiện Progressing

Một số app như:

```text
yas-backoffice-bff-dev
yas-storefront-bff-dev
yas-backoffice-bff-staging
yas-storefront-bff-staging
```

có thể hiện `Progressing` trong Argo CD dù pod đã `1/1 Running`.

Nguyên nhân chính:

- Các app BFF có Ingress.
- Cluster dùng K3s + NodePort, không có cloud LoadBalancer thật cho Ingress.
- Ingress không có `ADDRESS`, nên Argo CD health có thể chưa đánh dấu `Healthy`.
- Thực tế Deployment vẫn chạy và truy cập được qua domain local + NodePort.

Lệnh chứng minh runtime vẫn ổn:

```bash
kubectl get deploy -n dev backoffice-bff storefront-bff -o wide
kubectl get pods -n dev | grep bff
curl -I -H 'Host: storefront.yas.local.com' http://35.198.213.72:30303
```

## 7. Kiến trúc Observability đã làm

Observability được cài trong namespace:

```text
observability
```

Các thành phần:

```text
Prometheus: thu thập metrics Kubernetes/node/pod.
Grafana: giao diện xem dashboard, query metrics/logs/traces.
Loki: lưu log.
Promtail: đọc log từ node rồi gửi vào Loki.
Tempo: lưu trace.
OpenTelemetry Collector: nhận trace OTLP rồi gửi sang Tempo.
OpenTelemetry Operator: quản lý OpenTelemetryCollector CRD.
cert-manager: dependency cho OpenTelemetry Operator.
```

Luồng metrics:

```text
Kubernetes / kube-state-metrics / node-exporter
-> Prometheus
-> Grafana
```

Luồng logs:

```text
Pod logs
-> Promtail
-> Loki
-> Grafana Explore
```

Luồng traces:

```text
App hoặc telemetrygen
-> OpenTelemetry Collector
-> Tempo
-> Grafana Explore
```

## 8. Lệnh thêm Helm repo cho observability

```bash
helm repo add grafana https://grafana.github.io/helm-charts
```

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
```

```bash
helm repo add jetstack https://charts.jetstack.io
```

```bash
helm repo update
```

## 9. Cài Loki

Đi tới thư mục deploy:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
```

Cài Loki:

```bash
helm upgrade --install loki grafana/loki \
  --create-namespace \
  --namespace observability \
  -f ./observability/loki.values.yaml \
  --set loki.useTestSchema=true \
  --set loki.limits_config.ingestion_rate_mb=32 \
  --set loki.limits_config.ingestion_burst_size_mb=64
```

Kiểm tra:

```bash
kubectl get pods -n observability | grep loki
```

## 10. Cài Tempo

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
```

```bash
helm upgrade --install tempo grafana/tempo \
  --create-namespace \
  --namespace observability \
  -f ./observability/tempo.values.yaml
```

Kiểm tra:

```bash
kubectl get pods -n observability | grep tempo
```

Lưu ý: Tempo query API dùng port `3200`, không phải `3100`.

Test Tempo API:

```bash
kubectl run tempo-test -n observability --rm -i --restart=Never \
  --image=curlimages/curl:8.8.0 \
  --command -- curl -i 'http://tempo:3200/api/search'
```

## 11. Cài cert-manager

```bash
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.12.0 \
  --set installCRDs=true \
  --set prometheus.enabled=false \
  --set webhook.timeoutSeconds=4 \
  --set admissionWebhooks.certManager.create=true
```

Kiểm tra:

```bash
kubectl get pods -n cert-manager
```

## 12. Cài OpenTelemetry Operator

```bash
helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
  --create-namespace \
  --namespace observability
```

Kiểm tra:

```bash
kubectl get pods -n observability -l app.kubernetes.io/instance=opentelemetry-operator
```

## 13. Cài OpenTelemetry Collector

Ban đầu có thể cài chart trong repo:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
```

```bash
helm upgrade --install opentelemetry-collector ./observability/opentelemetry \
  --create-namespace \
  --namespace observability
```

Nếu collector lỗi vì config cũ dùng receiver/exporter `loki` không còn hỗ trợ, apply lại collector tối giản cho traces:

```bash
kubectl apply -f - <<'EOF'
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector
metadata:
  name: opentelemetry
  namespace: observability
spec:
  mode: deployment
  ports:
    - name: otlp-grpc
      port: 4317
      protocol: TCP
      targetPort: 4317
    - name: otlp-http
      port: 4318
      protocol: TCP
      targetPort: 4318
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318
    processors:
      batch: {}
    exporters:
      otlphttp:
        endpoint: http://tempo:4318
    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch]
          exporters: [otlphttp]
EOF
```

Kiểm tra:

```bash
kubectl get pods -n observability | grep opentelemetry
```

Kỳ vọng:

```text
opentelemetry-collector-...   1/1 Running
opentelemetry-operator-...    1/1 Running
```

## 14. Cài Promtail

Promtail đọc log từ node và đẩy vào Loki.

Lệnh nhóm đã dùng:

```bash
helm upgrade --install promtail grafana/promtail \
  --namespace observability \
  --set "config.clients[0].url=http://loki-gateway.observability.svc.cluster.local/loki/api/v1/push" \
  --set "nodeSelector.kubernetes\.io/hostname=yas-worker"
```

Kiểm tra:

```bash
kubectl get pods -n observability -l app.kubernetes.io/name=promtail -o wide
```

Lưu ý vấn đáp:

- Ban đầu Promtail chạy trên cả `yas-master` và `yas-worker`.
- Pod trên `yas-master` lỗi `too many open files`.
- Nhóm pin Promtail vào `yas-worker`, là node chạy workload chính, để đảm bảo log pipeline hoạt động ổn định cho demo.

## 15. Cài Prometheus và Grafana

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
```

```bash
helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
  --create-namespace \
  --namespace observability \
  -f ./observability/prometheus.values.yaml \
  --set grafana.assertNoLeakedSecrets=false
```

Kiểm tra các pod Prometheus:

```bash
kubectl get pods -n observability -l release=prometheus
```

Kiểm tra toàn bộ namespace observability:

```bash
kubectl get pods -n observability
```

Kỳ vọng các thành phần chính Running:

```text
prometheus-grafana
prometheus-kube-prometheus-operator
prometheus-kube-state-metrics
prometheus-prometheus-kube-prometheus-prometheus-0
prometheus-prometheus-node-exporter
alertmanager-prometheus-kube-prometheus-alertmanager-0
loki-*
tempo-0
promtail-*
opentelemetry-collector
opentelemetry-operator
```

## 16. Mở Grafana UI

Port-forward Grafana:

```bash
kubectl port-forward -n observability svc/prometheus-grafana 3000:80
```

Mở browser:

```text
http://localhost:3000
```

Lấy password nếu cần:

```bash
kubectl --namespace observability get secrets prometheus-grafana -o jsonpath="{.data.admin-password}" | base64 -d; echo
```

Đăng nhập:

```text
Username: admin
Password: admin hoặc password vừa lấy
```

## 17. Thêm datasource Loki trong Grafana

Nếu Grafana chưa có Loki datasource, apply ConfigMap:

```bash
kubectl apply -n observability -f - <<'EOF'
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-loki-datasource
  labels:
    grafana_datasource: "1"
data:
  loki-datasource.yaml: |
    apiVersion: 1
    datasources:
      - name: Loki
        type: loki
        uid: loki
        access: proxy
        url: http://loki-gateway.observability.svc.cluster.local
        isDefault: false
        jsonData:
          maxLines: 1000
          derivedFields:
            - name: traceId
              matcherRegex: "traceId=(\\w+)"
              datasourceUid: tempo
              url: "$${__value.raw}"
EOF
```

Restart Grafana để sidecar reload datasource nếu cần:

```bash
kubectl rollout restart deployment/prometheus-grafana -n observability
```

## 18. Thêm datasource Tempo trong Grafana

Nếu Grafana chưa có Tempo datasource, apply ConfigMap:

```bash
kubectl apply -n observability -f - <<'EOF'
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-tempo-datasource
  labels:
    grafana_datasource: "1"
data:
  tempo-datasource.yaml: |
    apiVersion: 1
    datasources:
      - name: Tempo
        type: tempo
        uid: tempo
        access: proxy
        url: http://tempo.observability.svc.cluster.local:3200
        isDefault: false
        jsonData:
          tracesToLogsV2:
            datasourceUid: loki
EOF
```

Restart Grafana nếu cần:

```bash
kubectl rollout restart deployment/prometheus-grafana -n observability
```

## 19. Test Prometheus trong Grafana

Trong Grafana:

```text
Explore -> chọn Prometheus -> query: up -> Run query
```

Kỳ vọng có nhiều series như:

```text
job="kubelet"
job="node-exporter"
job="kubernetes"
```

Lệnh terminal để kiểm tra pod:

```bash
kubectl get pods -n observability | grep prometheus
```

## 20. Test Loki logs

Kiểm tra Loki có label:

```bash
kubectl run loki-test -n observability --rm -i --restart=Never \
  --image=curlimages/curl:8.8.0 \
  --command -- curl -G 'http://loki-gateway/loki/api/v1/labels'
```

Kiểm tra các namespace có log:

```bash
kubectl run loki-test -n observability --rm -i --restart=Never \
  --image=curlimages/curl:8.8.0 \
  --command -- curl -G 'http://loki-gateway/loki/api/v1/label/namespace/values'
```

Trong Grafana:

```text
Explore -> chọn Loki -> query: {namespace="staging"} -> Run query
```

Nếu muốn tạo traffic để có log:

```bash
curl -i 'http://storefront.yas.local.com:30303/api/product/storefront/products/featured?pageNo=0'
```

## 21. Test Tempo traces

Tạo trace test bằng `telemetrygen`:

```bash
kubectl run telemetrygen -n observability --rm -i --restart=Never \
  --image=ghcr.io/open-telemetry/opentelemetry-collector-contrib/telemetrygen:latest \
  --command -- /telemetrygen traces \
  --otlp-endpoint opentelemetry-collector:4317 \
  --otlp-insecure \
  --traces 20
```

Kiểm tra Tempo API:

```bash
kubectl run tempo-test -n observability --rm -i --restart=Never \
  --image=curlimages/curl:8.8.0 \
  --command -- curl -s 'http://tempo:3200/api/search'
```

Trong Grafana:

```text
Explore -> chọn Tempo -> query:
{resource.service.name="telemetrygen"}
```

Kỳ vọng thấy trace:

```text
Service: telemetrygen
Name: lets-go
```

Lưu ý vấn đáp:

- App YAS chưa nhất thiết gửi trace thật nếu chưa cấu hình OTLP env cho từng service.
- Nhóm đã chứng minh pipeline trace hoạt động bằng telemetrygen:
  `telemetrygen -> OpenTelemetry Collector -> Tempo -> Grafana`.

## 22. Lệnh kiểm tra full observability

```bash
kubectl get ns observability
```

```bash
kubectl get pods -n observability
```

```bash
kubectl get svc -n observability
```

```bash
kubectl logs -n observability deploy/opentelemetry-collector --tail=80
```

```bash
kubectl get pods -n observability -l app.kubernetes.io/name=promtail -o wide
```

```bash
kubectl get pod -n observability -l app.kubernetes.io/name=grafana -o jsonpath='{range .items[*].status.containerStatuses[*]}{.name}{" ready="}{.ready}{" restarts="}{.restartCount}{"\n"}{end}'
```

## 23. Câu trả lời vấn đáp ngắn gọn

Nếu thầy hỏi: "Argo CD dùng để làm gì?"

```text
Nhóm dùng Argo CD cho GitOps dev/staging. Jenkins vẫn là CI/CD chính: Jenkins build image, push Docker Hub và update GitOps repo. Argo CD chỉ theo dõi repo GitOps rồi sync manifest vào Kubernetes. Main thay đổi thì deploy vào namespace dev. Release tag vX.Y.Z thì deploy vào namespace staging.
```

Nếu thầy hỏi: "Vì sao không để Argo CD build image?"

```text
Vì Argo CD không phải CI tool. Argo CD chỉ đảm nhiệm GitOps/CD sync. Build/test/push image là trách nhiệm của Jenkins theo yêu cầu đồ án.
```

Nếu thầy hỏi: "Observability gồm những gì?"

```text
Nhóm triển khai Prometheus, Grafana, Loki, Promtail, Tempo và OpenTelemetry Collector trong namespace observability. Prometheus thu metrics, Loki lưu logs, Tempo lưu traces, OpenTelemetry Collector nhận trace OTLP, Grafana là UI để xem cả metrics, logs và traces.
```

Nếu thầy hỏi: "Đã chứng minh observability thế nào?"

```text
Nhóm chứng minh Prometheus bằng query up trong Grafana, chứng minh Loki bằng query log namespace staging, chứng minh Tempo bằng telemetrygen gửi traces qua OpenTelemetry Collector vào Tempo và xem được trong Grafana.
```

Nếu thầy hỏi: "Vì sao Promtail chỉ chạy ở worker?"

```text
Ban đầu Promtail chạy cả master và worker, nhưng node master bị lỗi too many open files. Nhóm pin Promtail vào worker để bảo đảm log pipeline chạy ổn định cho workload demo. Đây là quyết định demo-scope, không phải thiết kế production.
```

## 24. Screenshot cần chụp cho báo cáo

Argo CD:

- Argo CD Applications list có `yas-root-dev`, `yas-root-staging`.
- `yas-tax-dev` Synced/Healthy.
- `yas-tax-staging` Synced/Healthy.
- Deployment/Pod trong Argo UI có image tag release `v1.0.0`.
- Jenkins tag build `v1.0.0` thành công.
- GitOps repo `envs/staging/tax-values.yaml` có `tag: v1.0.0`.

Observability:

- `kubectl get pods -n observability` tất cả thành phần chính Running.
- Grafana login thành công.
- Grafana Prometheus datasource query `up`.
- Grafana Loki query `{namespace="staging"}` có log.
- Grafana Tempo query `{resource.service.name="telemetrygen"}` có trace.
- Terminal output `tempo:3200/api/search` trả về traces.

## 25. Trạng thái hiện tại đã đạt

Argo CD:

- Đã có `dev` và `staging`.
- `main` deploy vào `dev`.
- release tag `v1.0.0` deploy vào `staging`.
- `yas-tax-staging` đã Synced/Healthy trong UI.

Observability:

- Prometheus hoạt động.
- Grafana hoạt động.
- Loki nhận log.
- Tempo nhận trace test từ telemetrygen.
- OpenTelemetry Collector hoạt động.
- Promtail hoạt động trên `yas-worker`.

