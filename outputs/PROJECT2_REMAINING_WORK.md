# Tổng hợp việc còn lại để hoàn thành Project 2

Ngày cập nhật: 2026-07-07

## Kết luận nhanh

Project 2 hiện đã có nền tảng tốt:

```text
K3s cluster đã chạy.
Argo CD dev đã quản lý app.
Tax service dev đang Healthy.
GitOps repo đã auto sync được.
Jenkinsfile mới đã có Docker push + GitOps update.
developer_build job user báo đã làm.
```

Việc còn lại chủ yếu là:

```text
1. Chốt bằng chứng Jenkins main -> GitOps -> Argo dev sau fix.
2. Chụp bằng chứng developer_build + cleanup.
3. Làm Argo staging bằng release tag.
4. Gom screenshot/log vào báo cáo.
5. Làm Istio/Kiali service mesh để lấy thêm 2 điểm nâng cao.
```

## Checklist yêu cầu Project 2 và trạng thái

| Yêu cầu | Trạng thái | Cần làm tiếp |
|---|---|---|
| K8s cluster 1 master + 1 worker | Đã có | Chụp `kubectl get nodes -o wide`. |
| Có image mặc định `main/latest` cho service | Có một phần | Chụp Docker Hub tag `main`; nếu service nào thiếu thì build/push default. |
| Branch commit build image tag commit SHA | Đã có Jenkinsfile | Chụp Jenkins branch job + Docker Hub SHA tag. |
| Push image lên Docker Hub | Đã có | Chụp Docker Hub và Jenkins log. |
| Jenkins job `developer_build` | User báo đã làm | Chạy lại một case preview và chụp evidence. |
| Developer nhập branch/tag từng service | Đã làm hoặc cần xác nhận | Chụp parameter page. |
| Service không nhập dùng `main/latest` | Cần evidence | Chụp deployment image của ít nhất tax và một service default. |
| NodePort/domain hosts file | Cần evidence | Chụp `kubectl get svc`, ghi hosts mapping. |
| Cleanup job | User nói có hoặc cần xác nhận | Chạy cleanup và chụp namespace biến mất. |
| Dev/staging Jenkins jobs | Có thể bỏ nếu dùng Argo CD nâng cao | Đã chọn Argo CD. |
| Argo CD dev | Đã chạy | Chụp app `yas-tax-dev`, deployment image. |
| Argo CD staging | Chưa chốt | Tạo release tag và test. |
| Service mesh | Chưa làm | Đã có runbook/manifest trong `outputs/service-mesh`; làm sau khi CD bắt buộc ổn để lấy 2 điểm nâng cao. |
| Báo cáo `.docx` | Chưa tổng hợp | Gom ảnh/log theo checklist. |

## Việc còn lại chi tiết

### 1. Chốt Jenkins main -> Argo dev

Mục tiêu:

```text
Merge main -> Jenkins -> Docker Hub -> GitOps repo -> Argo CD -> Kubernetes dev
```

Lệnh kiểm tra sau khi Jenkins main chạy:

```bash
cd /tmp/yas-deployment
git pull origin main
git log --oneline -5
sed -n '1,40p' envs/dev/tax-values.yaml

kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get pods -n dev -l app.kubernetes.io/name=tax -o wide
kubectl get application yas-tax-dev -n argocd
```

Đạt nếu:

```text
GitOps tag = Jenkins commit SHA mới.
Kubernetes tax image = cùng SHA đó.
tax pod 1/1 Running.
yas-tax-dev Synced Healthy.
```

Ảnh cần chụp:

```text
Jenkins main success.
Jenkins Docker push.
Jenkins Update GitOps Manifests.
GitOps repo commit Jenkins.
Argo CD tax app.
kubectl tax deployment image.
```

### 2. Chụp `developer_build`

Mục tiêu:

```text
Developer chọn tag/branch cho tax, service khác default main/latest, Jenkins deploy preview.
```

Case demo nên dùng:

```text
NAMESPACE = preview-tax
TAX_TAG hoặc TAX_BRANCH = tag tax đã có trên Docker Hub
Các service khác = main hoặc bỏ trống
```

Lệnh kiểm tra:

```bash
kubectl get ns preview-tax
kubectl get deploy -n preview-tax
kubectl get svc -n preview-tax
kubectl get deploy tax -n preview-tax -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
```

Đạt nếu:

```text
Namespace preview tồn tại.
Tax deployment dùng tag developer nhập.
Service NodePort tồn tại.
```

Ảnh cần chụp:

```text
Jenkins job parameter page.
Jenkins console output deploy.
kubectl get deploy/svc.
jsonpath image tag.
```

### 3. Chụp cleanup job

Chạy cleanup với namespace đã deploy:

```text
NAMESPACE = preview-tax
CONFIRM_DELETE = DELETE
```

Kiểm tra:

```bash
kubectl get ns preview-tax
```

Đạt nếu namespace không còn.

Ảnh cần chụp:

```text
Cleanup job console output.
kubectl get ns preview-tax trả NotFound hoặc không còn namespace.
```

### 4. Làm Argo staging

Mục tiêu:

```text
Release tag v1.0.0 -> Jenkins build/push image v1.0.0 -> update envs/staging -> Argo sync staging.
```

Kiểm tra staging app:

```bash
kubectl get applications -n argocd | grep staging
kubectl get ns staging
```

Nếu chưa có:

```bash
cd /tmp/yas-deployment
kubectl apply -f argocd/root-staging.yaml
```

Bật Jenkins tag discovery nếu chưa có:

```text
Jenkins multibranch -> Configure -> Branch Sources -> Behaviors -> Discover tags
Scan Multibranch Pipeline Now
```

Tạo tag:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
git switch main
git pull origin main
git tag v1.0.0
git push origin v1.0.0
```

Kiểm tra:

```bash
cd /tmp/yas-deployment
git pull origin main
sed -n '1,40p' envs/staging/tax-values.yaml

kubectl get application yas-tax-staging -n argocd
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get pods -n staging -l app.kubernetes.io/name=tax -o wide
```

Đạt nếu:

```text
envs/staging/tax-values.yaml tag = v1.0.0
deployment staging image = docker.io/doubleho/yas-tax:v1.0.0
```

### 5. Dependency evidence

Do đề bài là YAS microservices, dependency nền tảng cũng phải có trong kế hoạch. Trạng thái lần kiểm tra gần nhất:

```text
Đã thấy PostgreSQL Running trong namespace postgres.
PostgreSQL này nhiều khả năng được tạo bởi Jenkins job `developer_build` bằng manifest inline trong pipeline.
Không kết luận là được deploy từ chart `k8s/deploy/postgres`.
Đã thấy config/secret của YAS trong namespace dev.
Chưa thấy pod thật cho Kafka, Elasticsearch, Keycloak, Redis.
```

Thứ tự nền tảng đúng:

```text
PostgreSQL -> Kafka/Strimzi -> Elasticsearch -> Keycloak/Redis -> yas-configuration -> app services.
```

Không chạy nguyên file `k8s/deploy/setup-cluster.sh` nếu cluster nhỏ, vì script đó cài cả observability như Loki/Tempo/Prometheus/Grafana. Project 2 lần này không bắt buộc observability, nên ưu tiên cài từng dependency cần thiết.

#### 5.1. Kiểm tra dependency hiện có

```bash
kubectl get pods -A | grep -E "postgres|kafka|strimzi|elastic|keycloak|redis|argocd"
kubectl get cm -n dev
kubectl get secret -n dev
kubectl get app yas-configuration-dev -n argocd
```

#### 5.2. PostgreSQL hiện tại

Không deploy lại PostgreSQL nếu pod hiện tại vẫn `Running`, vì đây là database đang được YAS dev/preview trỏ tới.

Nguồn gốc PostgreSQL hiện tại theo Jenkins job `developer_build`:

```text
developer_build tạo namespace postgres.
developer_build apply Secret `postgresql-secret`.
developer_build apply Deployment `postgresql` dùng image `postgres:15`.
developer_build apply Service `postgresql` port 5432.
developer_build đặt POSTGRES_DB=tax khi khởi tạo.
developer_build có đoạn cố gắng tạo thêm database product/cart/order/customer/inventory/tax/media/search/sampledata.
```

Output thực tế hiện mới chứng minh chắc chắn database `tax` tồn tại. Nếu muốn demo nhiều backend service, cần kiểm tra lại Jenkins log đoạn `Creating database ...` hoặc chạy lại câu lệnh `psql -l`.

Chụp bằng chứng hiện tại:

```bash
kubectl get pods -n postgres
kubectl get svc -n postgres
kubectl get secret -n dev yas-postgresql-credentials-secret
```

Khi viết báo cáo, ghi:

```text
PostgreSQL được Jenkins job developer_build tạo trực tiếp trong Kubernetes namespace postgres bằng manifest inline.
Các microservice YAS dùng thông tin kết nối thông qua secret/config trong namespace dev.
Chart `k8s/deploy/postgres` chỉ là phương án tái dựng nếu cần dựng lại từ đầu, không phải nguồn tạo PostgreSQL hiện tại.
```

Chỉ khi PostgreSQL mất hoặc dựng cluster mới, có thể dùng chart trong repo để tái dựng tham khảo:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator
helm repo update

helm upgrade --install postgres-operator postgres-operator-charts/postgres-operator \
  --create-namespace --namespace postgres

helm upgrade --install postgres ./postgres/postgresql \
  --create-namespace --namespace postgres \
  --set replicas=1 \
  --set username=<POSTGRES_USER> \
  --set password=<POSTGRES_PASSWORD>

kubectl get pods -n postgres
kubectl get svc -n postgres
```

Trong đó `<POSTGRES_USER>` và `<POSTGRES_PASSWORD>` phải khớp cấu hình YAS đang dùng. Không hardcode credential vào repo.

#### 5.3. Deploy Kafka/Strimzi nếu demo search/event flow

Chỉ cần làm nếu muốn demo service phụ thuộc Kafka hoặc muốn full YAS ổn hơn:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add strimzi https://strimzi.io/charts/
helm repo update

helm upgrade --install kafka-operator strimzi/strimzi-kafka-operator \
  --create-namespace --namespace kafka

helm upgrade --install kafka-cluster ./kafka/kafka-cluster \
  --create-namespace --namespace kafka \
  --set kafka.replicas=1 \
  --set zookeeper.replicas=1 \
  --set postgresql.username=<POSTGRES_USER> \
  --set postgresql.password=<POSTGRES_PASSWORD>

kubectl get pods -n kafka
kubectl get kafka -n kafka
```

Đạt nếu Kafka operator và Kafka cluster pod `Running/Ready`.

#### 5.4. Deploy Elasticsearch nếu demo search

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add elastic https://helm.elastic.co
helm repo update

helm upgrade --install elastic-operator elastic/eck-operator \
  --create-namespace --namespace elasticsearch

helm upgrade --install elasticsearch-cluster ./elasticsearch/elasticsearch-cluster \
  --create-namespace --namespace elasticsearch \
  --set elasticsearch.replicas=1 \
  --set kibana.ingress.hostname=kibana.yas.local

kubectl get pods -n elasticsearch
kubectl get elasticsearch -n elasticsearch
```

Đạt nếu Elasticsearch resource `green/yellow` hoặc pod Elasticsearch `Running`.

#### 5.5. Deploy Redis nếu demo BFF/session/cache

```bash
helm upgrade --install redis \
  --set auth.password=redis \
  oci://registry-1.docker.io/bitnamicharts/redis \
  -n redis --create-namespace

kubectl get pods -n redis
kubectl get svc -n redis
```

Đạt nếu Redis master pod `Running`.

#### 5.6. Deploy Keycloak nếu demo UI/BFF/login

Keycloak cần PostgreSQL chạy trước:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
./setup-keycloak.sh

kubectl get pods -n keycloak
kubectl get keycloaks -n keycloak
kubectl get svc -n keycloak
```

Nếu script lỗi do namespace đã tồn tại, kiểm tra namespace trước rồi chạy lại phần Helm:

```bash
kubectl get ns keycloak
```

Đạt nếu Keycloak operator và Keycloak instance `Running/Ready`.

#### 5.7. Scope demo nếu không đủ thời gian

Nếu chỉ Postgres chạy thật, ghi rõ:

```text
Ghi rõ demo chính dùng tax nên cần PostgreSQL + config.
Các service search/UI/BFF cần thêm Elasticsearch/Kafka/Keycloak nên không dùng làm demo chính.
```

### 6. Service mesh Istio/Kiali cho 2 điểm nâng cao

Phần này là yêu cầu nâng cao riêng của Project 2, không được bỏ khỏi kế hoạch tổng. Tuy nhiên chỉ triển khai sau khi CD bắt buộc và Argo dev/staging đã có bằng chứng, vì Istio có thể làm cluster 1 master/1 worker nặng hơn.

Đã tạo sẵn deliverable manifest tại:

```text
outputs/service-mesh/01-mesh-demo-namespace.yaml
outputs/service-mesh/02-mtls-strict.yaml
outputs/service-mesh/03-tax-retry-virtualservice.yaml
outputs/service-mesh/04-authz-default-deny.yaml
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
outputs/service-mesh/README.md
```

#### 6.1. Chiến lược triển khai

Không bật Istio trực tiếp lên namespace `dev` đang dùng demo Argo CD. Dùng namespace riêng `mesh-demo` để chứng minh service mesh mà không phá flow chính.

Flow:

```text
Cài Istio -> cài Kiali -> tạo mesh-demo có sidecar injection -> deploy tax/order demo -> bật mTLS STRICT -> tạo retry VirtualService -> tạo AuthorizationPolicy allow/deny -> curl test -> chụp Kiali topology
```

#### 6.2. Cài Istio/Kiali

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH="$PWD/bin:$PATH"
istioctl install --set profile=demo -y
kubectl get pods -n istio-system

kubectl apply -f samples/addons/kiali.yaml
kubectl rollout status deployment/kiali -n istio-system --timeout=180s || true
kubectl get pods -n istio-system
```

#### 6.3. Apply namespace và manifest mesh

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
kubectl apply -f outputs/service-mesh/01-mesh-demo-namespace.yaml
kubectl get ns mesh-demo --show-labels
```

Deploy `tax` và `order` vào `mesh-demo`:

```bash
helm dependency build k8s/charts/tax
helm dependency build k8s/charts/order

helm upgrade --install tax k8s/charts/tax \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-tax \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false

helm upgrade --install order k8s/charts/order \
  -n mesh-demo --create-namespace \
  --set backend.image.repository=docker.io/doubleho/yas-order \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false

kubectl rollout restart deployment/tax -n mesh-demo
kubectl rollout restart deployment/order -n mesh-demo
kubectl get pods -n mesh-demo
```

Kiểm tra sidecar:

```bash
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

Đạt nếu pod có container `istio-proxy`.

Apply policy:

```bash
kubectl apply -f outputs/service-mesh/02-mtls-strict.yaml
kubectl apply -f outputs/service-mesh/03-tax-retry-virtualservice.yaml
kubectl apply -f outputs/service-mesh/04-authz-default-deny.yaml
kubectl apply -f outputs/service-mesh/05-authz-allow-order-to-tax.yaml

kubectl get peerauthentication -n mesh-demo
kubectl get virtualservice -n mesh-demo
kubectl get authorizationpolicy -n mesh-demo
```

#### 6.4. Curl allow/deny evidence

Tạo pod được allow theo service account `order`:

```bash
kubectl run curl-order -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl-order","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

Tạo pod bị deny theo service account `default`:

```bash
kubectl run curl-deny -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl-deny","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
```

Test:

```bash
kubectl get pods -n mesh-demo

kubectl exec -n mesh-demo curl-order -c curl-order -- \
  curl -i http://tax:8090/actuator/health || true

kubectl exec -n mesh-demo curl-deny -c curl-deny -- \
  curl -i http://tax:8090/actuator/health || true
```

Kỳ vọng:

```text
curl-order tới được service tax.
curl-deny nhận HTTP 403 hoặc RBAC access denied.
```

#### 6.5. Kiali topology

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

Tạo traffic trước khi chụp:

```bash
for i in $(seq 1 20); do
  kubectl exec -n mesh-demo curl-order -c curl-order -- curl -s http://tax:8090/actuator/health >/dev/null || true
done
```

Chụp Kiali namespace `mesh-demo`.

#### 6.6. Evidence service mesh phải có

```text
Screenshot Istio pods Running.
Screenshot mesh-demo pod có istio-proxy.
YAML PeerAuthentication STRICT.
YAML AuthorizationPolicy default-deny và allow-order-to-tax.
YAML VirtualService tax-retry.
Log curl allow.
Log curl deny.
Screenshot Kiali topology.
README hướng dẫn triển khai: outputs/service-mesh/README.md.
```

## Bằng chứng cuối cùng cần có

| Nhóm ảnh/log | Bắt buộc |
|---|---|
| GCP/K3s | VM + `kubectl get nodes -o wide`. |
| Jenkins branch CI | Detect service, test/build, Docker push SHA/branch. |
| Docker Hub | Tag SHA, branch, main, release nếu có. |
| Jenkins main CD | Update GitOps log, success. |
| GitOps repo | Commit Jenkins update `envs/dev`. |
| Argo dev | `yas-tax-dev` Synced/Healthy, image SHA mới. |
| developer_build | Parameter, console output, preview deployment. |
| NodePort/domain | `kubectl get svc`, hosts mapping. |
| cleanup | Cleanup log + namespace deleted. |
| Argo staging | Release tag, staging values, staging app/image. |
| Service mesh | Istio pods, mTLS YAML, AuthorizationPolicy YAML, VirtualService retry YAML, curl logs, Kiali topology. |

## Demo script cuối

Nói:

```text
Nhóm dùng Jenkins làm CI/CD chính. Branch developer chạy CI và push image theo commit SHA/branch tag. Khi merge main, Jenkins push image SHA/main, cập nhật GitOps repo DoubleHo05/yas-deployment. Argo CD auto sync namespace dev. developer_build là job riêng để deploy preview theo parameter, cleanup job xóa preview sau test. Release tag v1.0.0 dùng cho staging.
```

Chạy:

```bash
kubectl get nodes -o wide
kubectl get applications -n argocd
kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get pods -n dev -l app.kubernetes.io/name=tax -o wide
```

Mở:

```text
Jenkins main success
Docker Hub yas-tax tags
GitHub DoubleHo05/yas-deployment commit
Argo CD UI
developer_build job
cleanup job
```
