# Phân công 2 người cho phần còn lại Project 2

Ngày cập nhật: 2026-07-07

## Mục tiêu của tài liệu

Tài liệu này dành cho 2 người cùng chạy phần còn lại của Project 2. Người đọc chỉ cần làm theo từng bước, chụp bằng chứng theo checklist, không cần tự thiết kế lại.

Nguyên tắc giữ nguyên:

```text
Jenkins là CI/CD chính.
Không thêm GitHub Actions cho phần mới.
Tái sử dụng Jenkins cũ Project 1.
Argo CD chỉ dùng cho dev/staging GitOps nâng cao.
developer_build là Jenkins job riêng cho preview.
cleanup là Jenkins job riêng để xóa preview.
```

## Trạng thái hiện tại

### Đã làm được

| Hạng mục | Trạng thái | Bằng chứng/ghi chú |
|---|---|---|
| K3s cluster 1 master + 1 worker | Đã có | `yas-master Ready`, `yas-worker Ready`. |
| Namespace `argocd`, `dev`, `postgres` | Đã có | `kubectl get ns`. |
| PostgreSQL | Đã chạy | Pod `postgresql` Running; instance này do nhóm tạo sẵn/thủ công, không khẳng định được tạo từ `k8s/deploy/postgres`. |
| Argo CD dev apps | Đã apply | Nhiều app `Synced/Healthy`; `yas-tax-dev` Healthy. |
| GitOps repo | Đã có | `https://github.com/DoubleHo05/yas-deployment.git`. |
| Argo CD auto sync dev | Đã test | Đổi `envs/dev/tax-values.yaml` thì deployment `tax` đổi image. |
| Tax service trong dev | Đã chạy | `tax` deployment `1/1`, image `docker.io/doubleho/yas-tax:bb479177d6d0` ở thời điểm kiểm tra. |
| Jenkinsfile mới | Đã merge vào main theo user | Có Docker push và Update GitOps stage. |
| Lỗi GitOps stage `git diff --cached --quiet` | Đã sửa trên branch test | Commit fix `ci: fix gitops change detection`; cần đảm bảo đã merge main và Jenkins main pass. |
| `developer_build` job | User báo đã làm | Cần chụp bằng chứng parameter/deploy/image/NodePort. |
| Kubeconfig | Đã lấy được | Không commit kubeconfig vào repo. |

### Chưa thấy/chưa chốt bằng chứng cuối

| Hạng mục | Việc còn thiếu |
|---|---|
| Jenkins main full success sau fix GitOps | Cần screenshot/log `Finished: SUCCESS`, Docker push, GitOps commit/push. |
| GitOps commit do Jenkins tạo | Cần commit trong `DoubleHo05/yas-deployment` có message `chore(gitops): update dev image tags...`. |
| Argo staging | Chưa test release tag `vX.Y.Z`. |
| Cleanup job evidence | Nếu đã có job, cần chạy demo và chụp log xóa namespace preview. |
| Dependency evidence | Cần chụp service nền tảng đã chạy hoặc ghi rõ scope tax không cần Kafka/Elasticsearch/Keycloak runtime. |
| Service mesh | Chưa làm | Đã có runbook/manifest `outputs/service-mesh`; Người 2 làm sau khi CD bắt buộc ổn để lấy 2 điểm nâng cao. |
| Báo cáo `.docx` | Chưa tổng hợp ảnh/log cuối. |

## Phân công 2 người

Chia theo hướng ít phụ thuộc nhau nhất:

| Người | Trục việc | Không phụ thuộc nhiều vào | Output cuối cùng |
|---|---|---|---|
| Người 1 | Jenkins, Docker Hub, GitOps dev/staging | Không cần chỉnh cluster nhiều, chỉ cần Jenkins và GitHub/Docker Hub. | Jenkins main success, Docker Hub tag, GitOps commit, release tag staging nếu làm. |
| Người 2 | Kubernetes, Argo CD, preview, cleanup, evidence/report | Không cần sửa Jenkinsfile chính, chủ yếu kiểm tra cluster/job đã có. | Argo/K8s evidence, preview evidence, cleanup evidence, dependency evidence, report asset list. |

Hai người có thể làm song song:

```text
Người 1 chạy Jenkins main/release.
Người 2 mở sẵn kubectl/Argo/DockerHub/GitOps để quan sát và chụp bằng chứng.
```

## Người 1: Jenkins, Docker Hub, GitOps

### Mục tiêu

Chứng minh Jenkins làm được:

```text
Branch developer -> CI -> Docker Hub SHA/branch tag
Merge main -> Docker Hub SHA/main tag -> update GitOps dev
Release tag -> Docker Hub vX.Y.Z -> update GitOps staging
```

### Bước 1. Kiểm tra main đã có Jenkinsfile fix

Trên source repo:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
git switch main
git pull origin main
grep -n "returnStatus: true" Jenkinsfile
grep -n "GITOPS_REPO_URL" Jenkinsfile
grep -n "Update GitOps Manifests" Jenkinsfile
```

Đạt nếu có:

```text
returnStatus: true
GITOPS_REPO_URL = 'https://github.com/DoubleHo05/yas-deployment.git'
stage('Update GitOps Manifests')
```

Nếu chưa có `returnStatus: true`, merge branch fix trước khi test.

### Bước 2. Kiểm tra Jenkins credentials

Vào Jenkins:

```text
Manage Jenkins -> Credentials -> Global
```

Phải có:

| Credential ID | Bắt buộc | Mục đích |
|---|---|---|
| `dockerhub-credentials` | Có | Push Docker Hub. |
| `github-push-token` | Có | Push GitOps repo `DoubleHo05/yas-deployment`. |
| `snyk-token` | Có nếu stage Snyk chạy | Snyk scan. |
| `SonarCloud` | Có nếu Quality Gate chạy | SonarCloud. |

Không mở/chụp giá trị secret.

### Bước 3. Test Jenkins main -> GitOps dev

Nếu vừa merge branch test vào main, mở Jenkins job `main`.

Cần thấy các stage:

```text
Detect Changed Services
Gitleaks Scan
Build Common Library
Test
Build
Docker Build and Push
SonarQube Analysis
Quality Gate
Snyk Security Scan
Update GitOps Manifests
```

Trong log phải có:

```text
Services cần build: tax
docker push docker.io/doubleho/yas-tax:<commit-sha>
docker push docker.io/doubleho/yas-tax:main
Updating envs/dev/tax-values.yaml -> tag <commit-sha>
git commit -m 'chore(gitops): update dev image tags to <commit-sha> [skip ci]'
git push origin HEAD:main
Finished: SUCCESS
```

Nếu Jenkins báo:

```text
git diff --cached --quiet
ERROR: script returned exit code 1
```

thì main chưa có fix `returnStatus: true`.

### Bước 4. Kiểm tra Docker Hub

Mở Docker Hub hoặc dùng local:

```bash
docker pull docker.io/doubleho/yas-tax:<commit-sha>
docker pull docker.io/doubleho/yas-tax:main
```

Chụp Docker Hub có tag:

```text
<commit-sha>
main
branch tag nếu có
```

### Bước 5. Kiểm tra GitOps repo đã được Jenkins push

```bash
cd /tmp/yas-deployment
git pull origin main
git log --oneline -5
sed -n '1,40p' envs/dev/tax-values.yaml
```

Đạt nếu log có commit mới do Jenkins:

```text
chore(gitops): update dev image tags to <commit-sha> [skip ci]
```

và file có:

```yaml
backend:
  image:
    repository: docker.io/doubleho/yas-tax
    tag: <commit-sha>
```

### Bước 6. Test release tag staging

Chỉ làm sau khi main -> dev đã chụp bằng chứng xong.

Kiểm tra Jenkins multibranch có scan tag:

```text
Jenkins -> YAS multibranch -> Configure
Branch Sources -> Behaviors -> Discover tags
Save -> Scan Multibranch Pipeline Now
```

Tạo tag:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
git switch main
git pull origin main
git tag v1.0.0
git push origin v1.0.0
```

Kỳ vọng Jenkins tag job:

```text
BRANCH_NAME hoặc TAG_NAME = v1.0.0
docker push docker.io/doubleho/yas-tax:v1.0.0
Updating envs/staging/tax-values.yaml -> tag v1.0.0
git push origin HEAD:main
```

Nếu tag không chạy, chụp cấu hình Jenkins chưa bật `Discover tags`, bật rồi scan lại.

### Bước 7. Bằng chứng Người 1 phải nộp

| Bằng chứng | Cách lấy |
|---|---|
| Jenkins main success | Screenshot build `main` `Finished: SUCCESS`. |
| Detect service | Screenshot log `Services cần build: tax`. |
| Docker push SHA/main | Screenshot log `docker push`. |
| GitOps update | Screenshot log `Updating envs/dev/tax-values.yaml`. |
| GitOps commit | Screenshot GitHub repo `DoubleHo05/yas-deployment` commit Jenkins. |
| Docker Hub tag | Screenshot Docker Hub `doubleho/yas-tax`. |
| Release tag staging nếu làm | Screenshot Jenkins tag job + GitOps staging values. |

## Người 2: Kubernetes, Argo CD, preview, cleanup, report evidence

### Mục tiêu

Chứng minh cluster nhận được deployment từ GitOps và preview job hoạt động:

```text
Argo dev/staging sync đúng image
developer_build deploy preview đúng tag
NodePort/domain test được
cleanup xóa preview
dependency/config có bằng chứng
```

### Bước 1. Kiểm tra cluster và namespace

```bash
kubectl get nodes -o wide
kubectl get ns
```

Chụp kết quả có:

```text
yas-master Ready
yas-worker Ready
argocd
dev
postgres
preview namespace nếu đang test
```

### Bước 2. Kiểm tra dependency nền tảng

Chạy:

```bash
kubectl get pods -A | grep -E "postgres|kafka|strimzi|elastic|keycloak|redis|argocd"
kubectl get svc -A | grep -E "postgres|kafka|elastic|keycloak|redis"
kubectl get cm -n dev
kubectl get secret -n dev
```

Diễn giải khi báo cáo:

```text
PostgreSQL đang chạy thật trong namespace postgres.
PostgreSQL này là instance nhóm đã tạo sẵn/thủ công, không phải bằng chứng rằng chart `k8s/deploy/postgres` đã được dùng.
YAS config/secret trong dev đã được tạo bởi yas-configuration.
Tax service demo chính chỉ cần PostgreSQL/config nên có thể Healthy.
Một số service khác như search/storefront-bff/backoffice-bff có thể Degraded nếu Kafka/Elasticsearch/Keycloak chưa chạy đầy đủ.
```

Nếu muốn full YAS, thứ tự triển khai phải là:

```text
PostgreSQL -> Kafka/Strimzi -> Elasticsearch -> Keycloak/Redis nếu cần -> yas-configuration -> application services
```

Nếu không đủ thời gian, demo tax là hợp lý.

### Bước 2.1. Deploy dependency còn thiếu nếu muốn full YAS

Người 2 phụ trách phần này. Không bắt buộc phải làm hết nếu mục tiêu chính là chứng minh CD flow bằng tax service, nhưng phải biết dependency nào đang thiếu để giải thích khi service khác Degraded.

Nguyên tắc:

```text
Không chạy nguyên k8s/deploy/setup-cluster.sh trên cluster yếu.
Script đó cài cả observability, trong khi Project 2 không bắt buộc Grafana/Prometheus.
Ưu tiên cài từng dependency cần cho demo.
```

Mức ưu tiên:

| Dependency | Khi nào cần | Trạng thái lần kiểm tra gần nhất | Ưu tiên |
|---|---|---|---|
| PostgreSQL | Hầu hết backend service, tax demo | Đã Running, tạo sẵn/thủ công | Bắt buộc có bằng chứng, không deploy lại nếu đang ổn. |
| yas-configuration | Config/secret cho app | `yas-configuration-dev` Healthy | Bắt buộc có bằng chứng. |
| Kafka/Strimzi | Search/event/debezium flow | Chưa thấy pod thật | Làm nếu demo search/full YAS. |
| Elasticsearch | Search service | Chưa thấy pod thật | Làm nếu demo search. |
| Redis | BFF/cache/session tùy config | Chưa thấy pod thật | Làm nếu demo BFF/UI. |
| Keycloak | Login/UI/BFF | Chưa thấy pod thật | Làm nếu demo UI/login. |

#### PostgreSQL hiện tại

Không cài lại PostgreSQL nếu pod hiện tại vẫn `Running`. Nhiệm vụ của Người 2 là chụp bằng chứng và ghi đúng nguồn gốc:

```bash
kubectl get pods -n postgres
kubectl get svc -n postgres
kubectl get secret -n dev yas-postgresql-credentials-secret
```

Trong báo cáo ghi:

```text
PostgreSQL đã được nhóm tạo sẵn trong Kubernetes cluster ở namespace postgres.
Các service YAS lấy thông tin kết nối qua config/secret trong namespace dev.
Chart `k8s/deploy/postgres` chỉ là phương án tái dựng nếu cluster mới bị mất database, không phải nguồn tạo instance hiện tại.
```

Nếu dựng cluster mới từ đầu và cần tạo lại PostgreSQL bằng chart trong repo thì dùng phần tham khảo sau:

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

`<POSTGRES_USER>` và `<POSTGRES_PASSWORD>` phải lấy theo credential nhóm đang dùng. Không ghi secret thật vào report hoặc commit vào repo.

Nếu dùng PostgreSQL hiện tại, chụp:

```text
postgresql pod Running
postgresql service ClusterIP
yas-postgresql-credentials-secret tồn tại trong dev
```

#### Kafka/Strimzi nếu demo search/event

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

Chụp:

```text
kafka operator Running
kafka cluster Ready nếu có
```

#### Elasticsearch nếu demo search

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

Chụp:

```text
elastic-operator Running
elasticsearch pod Running hoặc Elasticsearch health green/yellow
```

#### Redis nếu demo BFF/cache

```bash
helm upgrade --install redis \
  --set auth.password=redis \
  oci://registry-1.docker.io/bitnamicharts/redis \
  -n redis --create-namespace

kubectl get pods -n redis
kubectl get svc -n redis
```

Chụp Redis master pod `Running`.

#### Keycloak nếu demo UI/login

Keycloak cần PostgreSQL chạy trước. Repo đã có script:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
./setup-keycloak.sh

kubectl get pods -n keycloak
kubectl get keycloaks -n keycloak
kubectl get svc -n keycloak
```

Nếu script lỗi vì namespace đã tồn tại:

```bash
kubectl get ns keycloak
kubectl get pods -n keycloak
```

Sau đó báo lại lỗi cụ thể, không xóa namespace nếu chưa backup.

#### Quyết định scope dependency

Nếu cluster thiếu RAM/CPU, chọn scope này:

```text
Scope chắc điểm: PostgreSQL + yas-configuration + tax Healthy + Jenkins/Argo flow.
Scope thêm nếu còn thời gian: Kafka + Elasticsearch để cứu search.
Scope thêm nữa: Keycloak + Redis để demo UI/BFF/login.
```

### Bước 3. Theo dõi Argo dev sau Jenkins main

Khi Người 1 báo Jenkins đã push GitOps:

```bash
kubectl get applications -n argocd
kubectl get application yas-tax-dev -n argocd
kubectl get deploy tax -n dev
kubectl get pods -n dev -l app.kubernetes.io/name=tax -o wide
kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
```

Đạt nếu:

```text
yas-tax-dev Synced Healthy
tax deployment READY 1/1
image = docker.io/doubleho/yas-tax:<commit-sha Jenkins>
```

Nếu Argo chưa sync sau vài phút:

```bash
kubectl annotate application yas-tax-dev -n argocd argocd.argoproj.io/refresh=hard --overwrite
```

Khi demo nói:

```text
Argo CD có automated sync; refresh chỉ để demo không phải chờ polling.
```

### Bước 4. Apply/kiểm tra Argo staging

Trước khi release tag, kiểm tra:

```bash
kubectl get applications -n argocd | grep staging
kubectl get ns staging
```

Nếu chưa có staging app:

```bash
cd /tmp/yas-deployment
kubectl apply -f argocd/root-staging.yaml
kubectl get applications -n argocd | grep staging
```

Sau khi Người 1 tạo tag `v1.0.0`:

```bash
kubectl get application yas-tax-staging -n argocd
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get pods -n staging -l app.kubernetes.io/name=tax -o wide
```

Đạt nếu:

```text
docker.io/doubleho/yas-tax:v1.0.0
yas-tax-staging Synced/Healthy
```

### Bước 5. Chạy/chụp `developer_build`

Nếu job đã làm rồi, không viết lại. Chỉ cần chạy demo và chụp.

Input mẫu:

```text
NAMESPACE = preview-tax
TAX_TAG hoặc TAX_BRANCH = <branch-tag/SHA đã có trên Docker Hub>
Các service khác = main hoặc bỏ trống để default main/latest
```

Sau job:

```bash
kubectl get ns preview-tax
kubectl get deploy -n preview-tax
kubectl get svc -n preview-tax
kubectl get deploy tax -n preview-tax -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
```

Đạt nếu:

```text
tax dùng đúng tag developer nhập
service khác dùng default main/latest
có NodePort để developer test
```

### Bước 6. Kiểm tra NodePort/domain preview

```bash
kubectl get svc -n preview-tax
kubectl get nodes -o wide
```

Ghi vào báo cáo:

```text
Developer thêm vào hosts:
<WORKER_EXTERNAL_IP> preview-tax.yas.local

Truy cập:
http://preview-tax.yas.local:<nodePort>
```

Nếu không có external IP trên node, dùng external IP của GCP VM worker hoặc port-forward khi demo.

### Bước 7. Chạy cleanup job

Chạy Jenkins cleanup job với:

```text
NAMESPACE = preview-tax
CONFIRM_DELETE = DELETE
```

Sau đó:

```bash
kubectl get ns preview-tax
```

Đạt nếu:

```text
Error from server (NotFound): namespaces "preview-tax" not found
```

hoặc namespace biến mất.

### Bước 8. Service mesh Istio/Kiali nâng cao

Người 2 làm chính phần này, Người 1 hỗ trợ chụp log và kiểm tra cluster không bị ảnh hưởng. Đây là phần nâng cao 2 điểm của Project 2, gồm:

```text
mTLS STRICT
AuthorizationPolicy allow/deny
VirtualService retry
Kiali topology
curl test evidence
README hướng dẫn triển khai
```

Không bật Istio lên namespace `dev` đang dùng demo Argo CD. Dùng namespace riêng `mesh-demo` để giảm rủi ro.

Manifest/runbook đã tạo sẵn:

```text
outputs/service-mesh/01-mesh-demo-namespace.yaml
outputs/service-mesh/02-mtls-strict.yaml
outputs/service-mesh/03-tax-retry-virtualservice.yaml
outputs/service-mesh/04-authz-default-deny.yaml
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
outputs/service-mesh/README.md
```

#### 8.1. Cài Istio/Kiali

Nếu chưa có `istioctl`:

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH="$PWD/bin:$PATH"
istioctl version
```

Cài Istio/Kiali:

```bash
istioctl install --set profile=demo -y
kubectl get pods -n istio-system

kubectl apply -f samples/addons/kiali.yaml
kubectl rollout status deployment/kiali -n istio-system --timeout=180s || true
kubectl get pods -n istio-system
```

Chụp `kubectl get pods -n istio-system`.

#### 8.2. Tạo namespace mesh-demo và deploy service mẫu

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
kubectl apply -f outputs/service-mesh/01-mesh-demo-namespace.yaml
kubectl get ns mesh-demo --show-labels

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

#### 8.3. Apply mTLS, retry, AuthorizationPolicy

```bash
kubectl apply -f outputs/service-mesh/02-mtls-strict.yaml
kubectl apply -f outputs/service-mesh/03-tax-retry-virtualservice.yaml
kubectl apply -f outputs/service-mesh/04-authz-default-deny.yaml
kubectl apply -f outputs/service-mesh/05-authz-allow-order-to-tax.yaml

kubectl get peerauthentication -n mesh-demo
kubectl get virtualservice -n mesh-demo
kubectl get authorizationpolicy -n mesh-demo
```

Chụp:

```bash
kubectl get peerauthentication -n mesh-demo -o yaml
kubectl get virtualservice -n mesh-demo -o yaml
kubectl get authorizationpolicy -n mesh-demo -o yaml
```

#### 8.4. Test allow/deny bằng curl

```bash
kubectl run curl-order -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl-order","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'

kubectl run curl-deny -n mesh-demo \
  --image=curlimages/curl:8.8.0 \
  --restart=Never \
  --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl-deny","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'

kubectl get pods -n mesh-demo
```

Allow case:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- \
  curl -i http://tax:8090/actuator/health || true
```

Deny case:

```bash
kubectl exec -n mesh-demo curl-deny -c curl-deny -- \
  curl -i http://tax:8090/actuator/health || true
```

Kỳ vọng:

```text
curl-order đi qua được tới service tax.
curl-deny nhận 403 hoặc RBAC access denied.
```

Nếu path health khác, thử:

```bash
kubectl exec -n mesh-demo curl-order -c curl-order -- curl -i http://tax:8090/actuator/health/readiness || true
kubectl exec -n mesh-demo curl-deny -c curl-deny -- curl -i http://tax:8090/actuator/health/readiness || true
```

#### 8.5. Chụp Kiali topology

Mở Kiali:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở browser:

```text
http://localhost:20001
```

Tạo traffic trước khi chụp:

```bash
for i in $(seq 1 20); do
  kubectl exec -n mesh-demo curl-order -c curl-order -- curl -s http://tax:8090/actuator/health >/dev/null || true
done
```

Chụp graph namespace `mesh-demo`.

#### 8.6. Nếu service mesh lỗi thì cắt scope thế nào

```text
Ưu tiên giữ bằng chứng CD bắt buộc + Argo CD.
Nếu Istio cài được nhưng app demo chưa Ready, vẫn nộp YAML mTLS/Auth/Retry + Kiali/Istio pods + giải thích chưa đủ traffic.
Nếu cluster thiếu tài nguyên, không bật Istio trên dev/staging.
```

### Bước 9. Bằng chứng Người 2 phải nộp

| Bằng chứng | Cách lấy |
|---|---|
| K3s 1 master + 1 worker | `kubectl get nodes -o wide`. |
| Dependencies/config | `kubectl get pods -A`, `kubectl get cm/secret -n dev`. |
| Argo dev | `kubectl get applications -n argocd`, screenshot UI nếu có. |
| Tax dev image | `kubectl get deploy tax -n dev -o jsonpath=...`. |
| Tax pod Ready | `kubectl get pods -n dev -l app.kubernetes.io/name=tax -o wide`. |
| Argo staging | `kubectl get applications -n argocd | grep staging`. |
| Preview deploy | `kubectl get deploy/svc -n preview-tax`. |
| Preview image tag | jsonpath image trong preview namespace. |
| NodePort/domain | service NodePort + hosts file note. |
| Cleanup | cleanup job log + namespace NotFound. |
| Istio/Kiali | `kubectl get pods -n istio-system`, Kiali topology. |
| mTLS | `kubectl get peerauthentication -n mesh-demo -o yaml`. |
| AuthorizationPolicy | `kubectl get authorizationpolicy -n mesh-demo -o yaml`, curl allow/deny logs. |
| Retry | `kubectl get virtualservice -n mesh-demo -o yaml`. |

## Việc nào còn thiếu để hoàn thành Project 2

| Mức điểm | Việc còn lại | Ai làm | Trạng thái mong muốn |
|---|---|---|---|
| Bắt buộc | Jenkins main success sau fix GitOps | Người 1 | Chụp build success. |
| Bắt buộc | GitOps commit do Jenkins tạo | Người 1 | GitHub commit trong `DoubleHo05/yas-deployment`. |
| Bắt buộc | Argo dev deploy đúng image Jenkins | Người 2 | `tax` image là SHA mới. |
| Bắt buộc | `developer_build` evidence | Người 2 | Preview namespace chạy đúng tag. |
| Bắt buộc | Cleanup evidence | Người 2 | Namespace preview bị xóa. |
| Bắt buộc | NodePort/domain evidence | Người 2 | Có service NodePort + hosts note. |
| Nâng cao | Release tag staging | Người 1 + Người 2 | `v1.0.0` deploy vào staging. |
| Nâng cao | Service mesh Istio/Kiali | Người 2 chính, Người 1 hỗ trợ ảnh/log | mTLS, AuthorizationPolicy, retry, Kiali. |
| Nộp bài | Báo cáo `.docx` | Người 2 gom, Người 1 cung cấp ảnh Jenkins/Docker/GitOps | Có ảnh và giải thích flow. |

## Thứ tự chạy khuyến nghị trong 1 buổi

1. Người 1 xác nhận Jenkins main success và GitOps commit.
2. Người 2 xác nhận Argo dev/tax image đổi đúng.
3. Người 2 chạy/chụp `developer_build`.
4. Người 2 chạy/chụp cleanup.
5. Người 1 tạo release tag `v1.0.0`.
6. Người 2 xác nhận Argo staging.
7. Cả hai gom ảnh vào báo cáo.
8. Nếu nhắm đủ điểm nâng cao, làm Istio/Kiali theo `outputs/service-mesh/README.md`.

## Nếu bị kẹt thì xử lý thế nào

| Lỗi | Người xử lý | Cách xử lý nhanh |
|---|---|---|
| Jenkins checkout GitHub timeout | Người 1 | Rerun; test `curl -I https://github.com`, `git ls-remote`. |
| Jenkins GitOps stage fail ở `git diff --cached --quiet` | Người 1 | Main chưa có fix `returnStatus: true`. |
| Jenkins push GitOps unauthorized | Người 1 | Kiểm tra `github-push-token` quyền push repo `DoubleHo05/yas-deployment`. |
| Argo chưa sync | Người 2 | Chờ polling hoặc hard refresh application. |
| Tax pod restart | Người 2 | Kiểm tra probe delay trong `tax-values.yaml`; logs pod. |
| Full app Degraded | Người 2 | Demo tax trước; ghi rõ Kafka/Elastic/Keycloak chưa là scope bắt buộc nếu không demo service đó. |
| Tag release không chạy Jenkins | Người 1 | Bật `Discover tags`. |
| Preview không expose | Người 2 | Kiểm tra service type NodePort và firewall GCP. |

## Không được làm

- Không thêm GitHub Actions cho CD mới.
- Không commit kubeconfig/token/password.
- Không cài Jenkins mới trên `yas-master` nếu Jenkins cũ dùng được.
- Không cố deploy full observability nếu chưa xong CD bắt buộc.
- Không làm Istio trước khi có bằng chứng Jenkins/GitOps/preview/cleanup.
