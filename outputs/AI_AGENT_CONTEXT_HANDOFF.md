# Context handoff cho AI agent khác

Ngày cập nhật: 2026-07-07

## Nhiệm vụ của AI agent tiếp theo

Hỗ trợ user hoàn thành Project 2 DevOps cho hệ thống YAS. User là executor, cần hướng dẫn cụ thể từng lệnh, từng bước kiểm tra, từng ảnh/log cần chụp. Không chỉ nói ý tưởng.

Luôn trả lời bằng tiếng Việt có dấu.

## Ràng buộc quan trọng

```text
Jenkins là CI/CD chính.
Không đề xuất GitHub Actions cho phần triển khai mới.
Không cài Jenkins mới trên yas-master nếu Jenkins cũ Project 1 vẫn dùng được.
Argo CD chỉ dùng cho phần nâng cao dev/staging GitOps.
developer_build là Jenkins job riêng, không nằm trong Jenkinsfile chính.
cleanup là Jenkins job riêng.
Istio/Kiali chỉ làm sau khi xong bắt buộc.
```

Nếu user hỏi GitHub Actions, chỉ nhắc dưới góc audit repo cũ, không chuyển kiến trúc mới sang GitHub Actions.

## Project là gì

Project 2 yêu cầu xây dựng CD cho YAS:

```text
1 master + 1 worker Kubernetes cluster.
Mỗi branch commit -> Jenkins build/push image tag commit SHA lên Docker Hub.
Jenkins job developer_build cho developer nhập branch/tag từng service để deploy preview.
Preview expose qua NodePort/domain local hosts file.
Jenkins cleanup job xóa preview.
Nâng cao: Argo CD handle dev/staging.
Nâng cao: Istio/Kiali service mesh, mTLS, AuthorizationPolicy, retry, curl test.
```

## Repo và hạ tầng

| Thành phần | Giá trị hiện tại |
|---|---|
| Source repo | `https://github.com/hoanghaitapcode/DevOps_Lab1.git` |
| GitOps repo | `https://github.com/DoubleHo05/yas-deployment.git` |
| Docker Hub namespace | `doubleho` |
| Cluster | Google Cloud Compute Engine + K3s |
| Master node | `yas-master`, internal IP `10.148.0.7` |
| Worker node | `yas-worker`, internal IP `10.148.0.8` |
| Namespace dev | `dev` |
| Namespace Argo | `argocd` |
| Namespace Postgres | `postgres` |

Không commit kubeconfig.

## Kiến trúc đã chốt

### Branch developer

```text
Developer commit branch
-> Jenkins multibranch scan
-> detect service đổi
-> test/build
-> Docker build/push:
   docker.io/doubleho/yas-<service>:<commit-sha>
   docker.io/doubleho/yas-<service>:<branch-name>
-> không update GitOps dev/staging
```

### Merge main

```text
Merge main
-> Jenkins main job
-> test/build
-> push Docker Hub:
   docker.io/doubleho/yas-<service>:<commit-sha>
   docker.io/doubleho/yas-<service>:main
-> Jenkins update DoubleHo05/yas-deployment envs/dev/<service>-values.yaml
-> Argo CD auto sync
-> Kubernetes dev đổi image
```

### Release staging

```text
git tag v1.0.0
git push origin v1.0.0
-> Jenkins tag job
-> push Docker Hub tag v1.0.0
-> update envs/staging/<service>-values.yaml
-> Argo CD sync staging
```

### Preview

```text
developer_build job
-> nhập namespace + branch/tag từng service
-> service nhập riêng dùng tag đó
-> service bỏ trống dùng main/latest
-> deploy namespace preview bằng Helm/Kubernetes
-> expose NodePort
```

Cleanup:

```text
cleanup job
-> nhập namespace preview
-> xóa namespace/resource preview
```

## Trạng thái đã xác nhận trong chat

### Cluster

User đã gửi:

```text
yas-master Ready
yas-worker Ready
namespace argocd/dev/postgres có
```

### Dependencies/config

User đã gửi:

```text
postgresql pod Running
PostgreSQL này do nhóm tạo sẵn/thủ công trong cluster, không phải bằng chứng đã deploy từ chart `k8s/deploy/postgres`.
argocd components đa số Running
yas-configuration-dev Synced Healthy
dev namespace có yas-configuration-configmap
dev namespace có yas-postgresql-credentials-secret
dev namespace có secret cho elasticsearch/keycloak/openai/redis
```

Chưa thấy pod thật cho Kafka, Elasticsearch, Keycloak, Redis của YAS. Vì vậy nếu full app Degraded thì không sa lầy. Tax service là demo chính vì nó đã Running.

### Argo dev

User đã gửi:

```text
yas-tax-dev Synced Healthy
tax deployment 1/1 Available
tax pod 1/1 Running
image: docker.io/doubleho/yas-tax:bb479177d6d0
GitOps envs/dev/tax-values.yaml tag: bb479177d6d0
```

Điều này chứng minh:

```text
GitOps repo đổi tag -> Argo CD sync -> Kubernetes dev đổi image
```

### Jenkins main

Jenkins main đã từng fail ở checkout do GitHub timeout:

```text
Failed to connect to github.com port 443
```

Sau đó user test:

```text
curl -I https://github.com -> HTTP/2 200
```

Nên hướng dẫn rerun Jenkins.

Jenkins main sau đó đi tới GitOps stage nhưng fail:

```text
git diff --cached --quiet
ERROR: script returned exit code 1
```

Nguyên nhân: `git diff --cached --quiet` trả exit code `1` khi có staged changes; Jenkins hiểu là lỗi.

Đã sửa Jenkinsfile:

```groovy
def hasChanges = sh(
    script: 'git diff --cached --quiet',
    returnStatus: true
)

if (hasChanges == 0) {
    echo "No GitOps manifest changes to commit."
} else {
    ...
}
```

Đã push branch `test-tax-main-flow` với:

```text
0a01aa59 ci: fix gitops change detection
a4f7e424 test: retrigger tax gitops flow
```

Cần xác nhận user đã merge PR này vào `main` và Jenkins main đã `Finished: SUCCESS`. Nếu chưa có log success, hướng dẫn user merge/rerun.

### developer_build

User nói:

```text
developer_build job làm rồi
```

Không hướng dẫn tạo lại job nếu user không hỏi. Chỉ yêu cầu chạy/chụp bằng chứng:

```text
parameters
console output
kubectl get deploy/svc -n preview namespace
image tag đúng
NodePort/domain
cleanup
```

## Jenkinsfile hiện tại cần biết

Các biến quan trọng:

```groovy
DOCKERHUB_USER = 'doubleho'
GITOPS_BRANCH = 'main'
GITOPS_REPO_URL = 'https://github.com/DoubleHo05/yas-deployment.git'
GITOPS_REPO_PUSH_PATH = 'github.com/DoubleHo05/yas-deployment.git'
```

Các stage:

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

Credential Jenkins cần:

```text
dockerhub-credentials
github-push-token
snyk-token
SonarCloud
Maven
JDK25
```

Gitleaks/Snyk report-only:

```text
Gitleaks --exit-code=0
Snyk || true
```

Coverage publish-only, không enforce 70%.

## Việc còn lại theo mức ưu tiên

### Ưu tiên 1: Chốt Jenkins main -> dev GitOps

Nếu user chưa có bằng chứng success, hướng dẫn:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
git switch main
git pull origin main
grep -n "returnStatus: true" Jenkinsfile
```

Rerun Jenkins main. Cần log:

```text
Services cần build: tax
docker push docker.io/doubleho/yas-tax:<sha>
docker push docker.io/doubleho/yas-tax:main
Updating envs/dev/tax-values.yaml -> tag <sha>
git commit -m 'chore(gitops): update dev image tags to <sha> [skip ci]'
git push origin HEAD:main
Finished: SUCCESS
```

Kiểm tra:

```bash
cd /tmp/yas-deployment
git pull origin main
git log --oneline -5
sed -n '1,40p' envs/dev/tax-values.yaml

kubectl get deploy tax -n dev -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get pods -n dev -l app.kubernetes.io/name=tax -o wide
kubectl get application yas-tax-dev -n argocd
```

### Ưu tiên 2: Chụp developer_build + cleanup

Nếu job đã có:

```bash
kubectl get ns
kubectl get deploy -n preview-tax
kubectl get svc -n preview-tax
kubectl get deploy tax -n preview-tax -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
```

Sau cleanup:

```bash
kubectl get ns preview-tax
```

### Ưu tiên 3: Argo staging

Không làm trước khi dev evidence xong.

Kiểm tra staging app:

```bash
kubectl get applications -n argocd | grep staging
kubectl get ns staging
```

Nếu thiếu:

```bash
cd /tmp/yas-deployment
kubectl apply -f argocd/root-staging.yaml
```

Tạo release tag:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
git switch main
git pull origin main
git tag v1.0.0
git push origin v1.0.0
```

Nếu Jenkins không chạy tag, bật `Discover tags`.

Kiểm tra staging:

```bash
cd /tmp/yas-deployment
git pull origin main
sed -n '1,40p' envs/staging/tax-values.yaml

kubectl get application yas-tax-staging -n argocd
kubectl get deploy tax -n staging -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
```

### Ưu tiên 4: Service mesh Istio/Kiali nếu muốn đủ 2 điểm nâng cao

Chỉ làm sau khi:

```text
Jenkins main dev flow có bằng chứng
developer_build có bằng chứng
cleanup có bằng chứng
staging có hoặc đã quyết định bỏ
```

Không bật Istio trực tiếp vào namespace `dev` đang demo Argo. Dùng namespace riêng `mesh-demo` để giảm rủi ro.

Đã tạo manifest/runbook tại:

```text
outputs/service-mesh/01-mesh-demo-namespace.yaml
outputs/service-mesh/02-mtls-strict.yaml
outputs/service-mesh/03-tax-retry-virtualservice.yaml
outputs/service-mesh/04-authz-default-deny.yaml
outputs/service-mesh/05-authz-allow-order-to-tax.yaml
outputs/service-mesh/README.md
```

Lệnh chính:

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

Tạo namespace mesh:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1
kubectl apply -f outputs/service-mesh/01-mesh-demo-namespace.yaml
kubectl get ns mesh-demo --show-labels
```

Deploy service mẫu:

```bash
helm dependency build k8s/charts/tax
helm dependency build k8s/charts/order

helm upgrade --install tax k8s/charts/tax -n mesh-demo --create-namespace --set backend.image.repository=docker.io/doubleho/yas-tax --set backend.image.tag=main --set backend.serviceMonitor.enabled=false
helm upgrade --install order k8s/charts/order -n mesh-demo --create-namespace --set backend.image.repository=docker.io/doubleho/yas-order --set backend.image.tag=main --set backend.serviceMonitor.enabled=false

kubectl rollout restart deployment/tax -n mesh-demo
kubectl rollout restart deployment/order -n mesh-demo
kubectl get pod -n mesh-demo -o jsonpath='{range .items[*]}{.metadata.name}{" containers="}{range .spec.containers[*]}{.name}{" "}{end}{"\n"}{end}'
```

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

Curl allow/deny:

```bash
kubectl run curl-order -n mesh-demo --image=curlimages/curl:8.8.0 --restart=Never --overrides='{"spec":{"serviceAccountName":"order","containers":[{"name":"curl-order","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'
kubectl run curl-deny -n mesh-demo --image=curlimages/curl:8.8.0 --restart=Never --overrides='{"spec":{"serviceAccountName":"default","containers":[{"name":"curl-deny","image":"curlimages/curl:8.8.0","command":["sleep","3600"]}]}}'

kubectl exec -n mesh-demo curl-order -c curl-order -- curl -i http://tax:8090/actuator/health || true
kubectl exec -n mesh-demo curl-deny -c curl-deny -- curl -i http://tax:8090/actuator/health || true
```

Kỳ vọng:

```text
curl-order: request tới được tax.
curl-deny: HTTP 403 hoặc RBAC access denied.
```

Kiali:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở `http://localhost:20001`, chọn namespace `mesh-demo`, chụp topology.

Tối thiểu cần tạo/chụp:

```text
PeerAuthentication STRICT mTLS
AuthorizationPolicy allow/deny
VirtualService retry
Kiali topology screenshot
curl allow/deny logs
retry evidence
README deploy steps
```

## Dependency order của YAS

Khi deploy nhiều service hơn tax, thứ tự đúng là:

```text
PostgreSQL
Kafka/Strimzi
Elasticsearch
Keycloak/Redis nếu service cần
yas-configuration
Application services
UI/BFF
sampledata chạy một lần rồi tắt
```

Trạng thái đã thấy từ user:

```text
PostgreSQL namespace postgres có pod postgresql Running.
PostgreSQL này là instance nhóm đã tạo sẵn/thủ công, không kết luận là được tạo từ `k8s/deploy/postgres`.
dev namespace đã có configmap/secret YAS.
yas-configuration-dev Synced Healthy.
Chưa thấy pod thật cho Kafka, Elasticsearch, Keycloak, Redis.
```

Không được tự đề xuất chạy nguyên `k8s/deploy/setup-cluster.sh` như bước mặc định nếu cluster nhỏ, vì script đó cài cả observability stack. Project 2 hiện không bắt buộc Grafana/Prometheus. Nếu cần dependency, hướng dẫn user cài từng phần.

PostgreSQL hiện tại:

```text
Không deploy lại nếu pod postgresql hiện tại vẫn Running.
Khi báo cáo, ghi PostgreSQL đã được nhóm tạo sẵn trong cluster namespace postgres.
Chart `k8s/deploy/postgres` chỉ là phương án tái dựng nếu dựng cluster mới hoặc database bị mất.
```

Lệnh chụp bằng chứng PostgreSQL hiện tại:

```bash
kubectl get pods -n postgres
kubectl get svc -n postgres
kubectl get secret -n dev yas-postgresql-credentials-secret
```

PostgreSQL nếu dựng lại từ đầu bằng chart repo:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator
helm repo update
helm upgrade --install postgres-operator postgres-operator-charts/postgres-operator --create-namespace --namespace postgres
helm upgrade --install postgres ./postgres/postgresql --create-namespace --namespace postgres --set replicas=1 --set username=<POSTGRES_USER> --set password=<POSTGRES_PASSWORD>
kubectl get pods -n postgres
kubectl get svc -n postgres
```

`<POSTGRES_USER>` và `<POSTGRES_PASSWORD>` phải lấy theo credential nhóm đang dùng, không hardcode secret thật vào repo/report.

Kafka/Strimzi nếu demo search/event:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add strimzi https://strimzi.io/charts/
helm repo update
helm upgrade --install kafka-operator strimzi/strimzi-kafka-operator --create-namespace --namespace kafka
helm upgrade --install kafka-cluster ./kafka/kafka-cluster --create-namespace --namespace kafka --set kafka.replicas=1 --set zookeeper.replicas=1 --set postgresql.username=<POSTGRES_USER> --set postgresql.password=<POSTGRES_PASSWORD>
kubectl get pods -n kafka
kubectl get kafka -n kafka
```

Elasticsearch nếu demo search:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
helm repo add elastic https://helm.elastic.co
helm repo update
helm upgrade --install elastic-operator elastic/eck-operator --create-namespace --namespace elasticsearch
helm upgrade --install elasticsearch-cluster ./elasticsearch/elasticsearch-cluster --create-namespace --namespace elasticsearch --set elasticsearch.replicas=1 --set kibana.ingress.hostname=kibana.yas.local
kubectl get pods -n elasticsearch
kubectl get elasticsearch -n elasticsearch
```

Redis nếu demo BFF/cache:

```bash
helm upgrade --install redis --set auth.password=redis oci://registry-1.docker.io/bitnamicharts/redis -n redis --create-namespace
kubectl get pods -n redis
kubectl get svc -n redis
```

Keycloak nếu demo UI/login:

```bash
cd /Users/giabao/Documents/devops/DevOps_Lab1/k8s/deploy
./setup-keycloak.sh
kubectl get pods -n keycloak
kubectl get keycloaks -n keycloak
kubectl get svc -n keycloak
```

Dependency scope nên tư vấn:

```text
Tax demo: PostgreSQL + yas-configuration là đủ.
Search demo: cần Kafka + Elasticsearch.
UI/BFF login demo: cần Keycloak, thường thêm Redis tùy config.
Full YAS: cần đầy đủ Postgres + Kafka + Elasticsearch + Keycloak + Redis.
```

Nếu chỉ demo tax:

```text
PostgreSQL + yas-configuration là đủ.
```

Nếu demo search:

```text
Elasticsearch + Kafka cần ổn.
```

Nếu demo UI/BFF login:

```text
Keycloak cần ổn.
```

## Những câu trả lời nên dùng

Nếu user hỏi “còn gì thiếu?”:

```text
Thiếu bằng chứng cuối: Jenkins main success sau fix, GitOps commit Jenkins, Argo dev image mới, developer_build evidence, cleanup evidence, staging tag nếu làm Argo nâng cao, service mesh Istio/Kiali nếu lấy thêm 2 điểm, report screenshots.
```

Nếu user hỏi “có cần deploy full YAS không?”:

```text
Không nhất thiết cho flow CD bắt buộc. Nên demo tax vì tax Healthy. Full YAS cần Kafka/Elasticsearch/Keycloak/Redis ổn trước.
```

Nếu user hỏi “Argo staging làm sao?”:

```text
Apply root-staging nếu chưa có, bật Jenkins Discover tags, tạo git tag v1.0.0, Jenkins update envs/staging, Argo sync staging.
```

Nếu user hỏi “developer_build đã có rồi sao còn làm?”:

```text
Không làm lại job, chỉ chạy và chụp evidence: parameters, console output, preview deployment image, NodePort/domain, cleanup.
```

## Không được làm

- Không dùng GitHub Actions cho CD mới.
- Không commit kubeconfig/token/password.
- Không nói phải cài Jenkins mới trên `yas-master`.
- Không sa lầy sửa toàn bộ app Degraded nếu tax/dev flow đã đủ demo.
- Không làm Istio trước khi xong bằng chứng CD bắt buộc.
