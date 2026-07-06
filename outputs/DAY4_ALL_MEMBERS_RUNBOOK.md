# Ngày 4 - Runbook chi tiết cho cả 4 thành viên

Mục tiêu ngày 4: củng cố demo bắt buộc và bắt đầu phần nâng cao nếu phần bắt buộc đã có evidence.

Tình trạng cuối ngày 3 của nhóm:

- Jenkins `developer_build` đã chạy SUCCESS.
- `TAX_BRANCH=dev_tax_service` đã resolve thành `af2dbde4f1a2`.
- Deployment `tax` dùng đúng image:

```text
docker.io/doubleho/yas-tax:af2dbde4f1a2
```

- NodePort đã tạo:

```text
yas-preview-nodeport   NodePort   ...   80:30080/TCP
```

- Pod `tax` ban đầu chưa Ready vì thiếu runtime dependency PostgreSQL:

```text
UnknownHostException: postgresql.postgres
```

Điều này có nghĩa: **CD image selection đã đúng**, nhưng app runtime lúc đó chưa đủ dependency để health OK.

Sau khi tạo `yas-configuration` và PostgreSQL service đơn giản, `tax` có thể chạy Ready. Ngày 4 cần ưu tiên ghi lại đúng flow đã sửa để nhóm khác làm lại không vấp.

## Quyết định đầu ngày 4

Trước khi chia việc, cả nhóm chọn một trong hai nhánh:

```text
Nhánh A - Bắt buộc chưa sạch:
Tập trung làm app Ready hơn, cleanup job, evidence, report. Chưa làm Argo/Istio.

Nhánh B - Bắt buộc đủ evidence:
Giữ nguyên evidence bắt buộc, làm nâng cao Argo CD hoặc Istio/Kiali ở mức tối thiểu.
```

Khuyến nghị của mình: làm theo thứ tự này:

1. Hoàn thành `developer_cleanup`.
2. Cố gắng deploy PostgreSQL để `tax` bớt CrashLoop.
3. Nếu còn thời gian, làm Argo CD dev.
4. Istio/Kiali chỉ làm khi cluster còn khỏe.

## Lịch làm đề xuất trong ngày

| Khung giờ | Cả nhóm cần đạt |
|---|---|
| 0h-0h30 | Sync evidence ngày 3, xác định blocker còn lại |
| 0h30-1h30 | TV3 hoàn thiện cleanup job, TV4 chụp evidence bắt buộc |
| 1h30-3h00 | TV1 + TV3 deploy PostgreSQL/runtime dependency tối thiểu |
| 3h00-4h30 | Nếu bắt buộc ổn: TV3 làm Argo CD dev hoặc TV4 làm Istio tối thiểu |
| 4h30-5h00 | Chốt ảnh/log, cập nhật report/demo script |

---

# Thành viên 1 - Infrastructure Lead

Nhiệm vụ ngày 4: giữ cluster ổn định, hỗ trợ deploy PostgreSQL/runtime dependency, và chỉ cài Argo/Istio nếu cluster còn tài nguyên.

## Mục đích của TV1 ngày 4

TV1 không cần viết Jenkins pipeline. TV1 cần giúp nhóm trả lời:

```text
Vì sao app chưa Ready?
Cluster có đủ tài nguyên chạy dependency không?
Có service postgresql.postgres chưa?
NodePort/firewall còn hoạt động không?
Nếu cài Argo/Istio thì cluster có chịu được không?
```

## Bước 1. Kiểm tra cluster và tài nguyên

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl top nodes || true
kubectl top pods -A || true
```

Nếu `kubectl top` không có Metrics Server, bỏ qua.

Kiểm tra pod đang lỗi:

```bash
kubectl get pods -n preview-demo -o wide
kubectl logs -n preview-demo deploy/tax --tail=80 || true
kubectl describe pod -n preview-demo -l app.kubernetes.io/name=tax
```

Kỳ vọng lỗi hiện tại:

```text
UnknownHostException: postgresql.postgres
```

## Bước 2. Kiểm tra namespace/service Postgres

```bash
kubectl get ns
kubectl get svc -n postgres
kubectl get pods -n postgres
```

Nếu báo:

```text
Error from server (NotFound): namespaces "postgres" not found
```

thì chưa có Postgres.

## Bước 3. Không ưu tiên Postgres chart operator của repo

Chart sau có trong repo:

```text
k8s/deploy/postgres/postgresql
```

Nhưng thực tế chart này có 2 vấn đề:

- Template lỗi ở `recommendation`/`webhook` do viết `{ { .Values.username } }`.
- Chart dùng `apiVersion: acid.zalan.do/v1`, cần Zalando Postgres Operator CRD.

Vì vậy trong demo gấp, không ưu tiên dùng chart này. Dùng PostgreSQL YAML đơn giản ở bước 4 để tạo đúng DNS mà app cần:

```text
postgresql.postgres
```

## Bước 4. Deploy PostgreSQL đơn giản để tax chạy được

Tạo namespace:

```bash
kubectl create namespace postgres --dry-run=client -o yaml | kubectl apply -f -
```

Tạo PostgreSQL Deployment + Service:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
  namespace: postgres
type: Opaque
stringData:
  POSTGRES_USER: yasadminuser
  POSTGRES_PASSWORD: admin
  POSTGRES_DB: tax
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql
  namespace: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:15
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: postgresql-secret
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: postgres
spec:
  selector:
    app: postgresql
  ports:
    - name: postgres
      port: 5432
      targetPort: 5432
EOF
```

Kiểm tra:

```bash
kubectl get pods -n postgres
kubectl get svc -n postgres
```

Cần thấy service:

```text
postgresql
```

DNS mà app cần là:

```text
postgresql.postgres
```

## Bước 5. Cài `yas-configuration` vào preview namespace

Sau cleanup, namespace `preview-demo` bị xóa nên config/secret cũng mất. Chạy lại `developer_build` trước để tạo namespace/deployment, rồi cài config.

Nếu từng tạo tay secret `yas-postgresql-credentials-secret`, Helm có thể fail ownership. Xóa secret tay trước:

```bash
kubectl delete secret yas-postgresql-credentials-secret -n preview-demo --ignore-not-found=true
```

Deploy config:

```bash
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n preview-demo --create-namespace
```

Kiểm tra:

```bash
kubectl get configmap -n preview-demo
kubectl get secret -n preview-demo
```

Cần thấy:

```text
yas-configuration-configmap
yas-postgresql-credentials-secret
```

Nếu `helm dependency build k8s/charts/yas-configuration` báo thiếu repo Stakater, có thể thêm:

```bash
helm repo add stakater https://stakater.github.io/stakater-charts
helm repo update
```

Nhưng nếu `helm upgrade --install yas-configuration ...` vẫn deployed OK thì không cần sa lầy vào warning dependency.

## Bước 6. Restart tax sau khi có Postgres và config

Sau khi có:

```text
postgresql.postgres
yas-configuration-configmap
yas-postgresql-credentials-secret
```

restart:

```bash
kubectl rollout restart deployment tax -n preview-demo
kubectl rollout status deployment tax -n preview-demo --timeout=180s || true
kubectl get pods -n preview-demo
kubectl logs -n preview-demo deploy/tax --tail=100 || true
```

Nếu app chạy đúng, log sẽ có:

```text
HikariPool-1 - Start completed.
Database JDBC URL [jdbc:postgresql://postgresql.postgres:5432/tax]
Liquibase ... Database is up to date
Started TaxApplication
```

Và pod:

```bash
kubectl get pods -n preview-demo
```

cần thấy:

```text
tax-...   1/1   Running
```

## Bước 7. Kiểm tra NodePort/firewall lại

```bash
kubectl get svc -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
gcloud compute firewall-rules list --filter="name~yas"
gcloud compute instances list
```

Nếu endpoints có IP, test:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080/tax/actuator/health
```

Nếu endpoints rỗng, đừng debug firewall trước. Ghi nguyên nhân là service chưa có backend Ready. Với `tax`, nguyên nhân thường là chưa tạo PostgreSQL/config nên pod chưa Ready.

Chỉ kỳ vọng curl port `30080` thành công khi các điều kiện này cùng đúng:

```text
1. Service yas-preview-nodeport tồn tại kiểu NodePort.
2. Firewall GCP mở tcp:30080 tới worker.
3. Pod tax 1/1 Running và Ready.
4. Endpoints yas-preview-nodeport có IP pod.
5. Đang curl đúng external IP của yas-worker.
```

Kiểm tra đầy đủ:

```bash
kubectl get pods -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
kubectl get svc yas-preview-nodeport -n preview-demo
gcloud compute instances list
```

## Bước 8. Cài Argo CD nếu bắt buộc đã ổn

Chỉ làm nếu:

- `developer_build` evidence đã có.
- `developer_cleanup` đã có hoặc TV3 đang làm.
- Cluster không quá tải.

Cài:

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd
```

Đây cần network. Nếu cluster/VM không truy cập được internet hoặc lệnh fail, ghi blocker.

Port-forward:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Không chụp lộ password.

## Done của TV1 ngày 4

TV1 xong khi có:

- Ảnh/log cluster ổn.
- Ảnh/log Postgres service nếu deploy được.
- Ảnh/log lỗi rõ nếu Postgres không deploy được.
- Ảnh/log NodePort/firewall.
- Nếu làm Argo: `kubectl get pods -n argocd`.

## Lỗi thực tế đã gặp và cách sửa

### Lỗi Postgres chart YAML parse

Log:

```text
YAML parse error on postgres/templates/postgresql.yaml
invalid map key: map[interface {}]interface {}{".Values.username":interface {}(nil)}
```

Nguyên nhân: chart có dòng sai:

```yaml
recommendation: { { .Values.username } }
webhook: { { .Values.username } }
```

Ngoài ra chart dùng CRD của Zalando Postgres Operator. Với demo gấp, dùng PostgreSQL YAML đơn giản ở Bước 4.

### Lỗi `yas-configuration` conflict secret

Log:

```text
Secret "yas-postgresql-credentials-secret" ... exists and cannot be imported into the current release
```

Nguyên nhân: đã tạo secret bằng `kubectl create secret`, sau đó Helm chart muốn tạo secret cùng tên.

Cách sửa:

```bash
kubectl delete secret yas-postgresql-credentials-secret -n preview-demo --ignore-not-found=true
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n preview-demo --create-namespace
```

### Lỗi Docker pull trên Mac ARM

Log:

```text
no matching manifest for linux/arm64/v8
```

Image build cho GCP worker `linux/amd64`, đúng với cluster. Trên Mac ARM kiểm tra bằng:

```bash
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:af2dbde4f1a2
```

---

# Thành viên 2 - Jenkins CI & Container Lead

Nhiệm vụ ngày 4: ổn định Docker image tags, chuẩn bị default `main` image cho service demo, và hỗ trợ release tag nếu làm Argo/staging.

## Mục đích của TV2 ngày 4

TV2 đảm bảo mọi image mà CD/GitOps gọi tới đều tồn tại trên Docker Hub.

Ngày 3 đã có:

```text
doubleho/yas-tax:af2dbde4f1a2
doubleho/yas-tax:dev_tax_service
doubleho/yas-tax:main
```

Ngày 4 nếu mở rộng demo, phải có thêm image `main` cho service khác.

## Bước 1. Kiểm tra tag tax

Trên Mac ARM:

```bash
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:af2dbde4f1a2
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:dev_tax_service
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:main
```

Nếu pull OK, chụp hoặc ghi log.

## Bước 2. Không cố build tất cả service nếu chưa cần

Chỉ build thêm khi TV3 thật sự deploy thêm service.

Ưu tiên nếu cần demo storefront flow:

```text
product
cart
order
customer
inventory
tax
storefront-bff
storefront
```

Nhưng nếu ngày 4 còn ít thời gian, chỉ giữ `tax`.

## Bước 3. Bootstrap `main` image thủ công cho service cần demo

Nếu Jenkins main chưa build được service đó, có thể build thủ công tuần tự.

Ví dụ `product`:

```bash
docker build --platform linux/amd64 -t docker.io/doubleho/yas-product:main ./product
docker push docker.io/doubleho/yas-product:main
```

Ví dụ `cart`:

```bash
docker build --platform linux/amd64 -t docker.io/doubleho/yas-cart:main ./cart
docker push docker.io/doubleho/yas-cart:main
```

BFF:

```bash
docker build --platform linux/amd64 -t docker.io/doubleho/yas-storefront-bff:main ./storefront-bff
docker push docker.io/doubleho/yas-storefront-bff:main
```

UI:

```bash
docker build --platform linux/amd64 -t docker.io/doubleho/yas-storefront:main ./storefront
docker push docker.io/doubleho/yas-storefront:main
```

Không chạy loop build nhiều service nếu máy/Jenkins yếu. Làm từng service, service nào xong ghi lại.

## Bước 4. Nếu muốn làm đúng bằng Jenkins main

Sau khi Jenkinsfile Docker stage đã merge vào main, chạy Jenkins branch main.

Nhưng Jenkins chỉ build service changed. Nếu main không có thay đổi ở service, nó có thể skip.

Cách không nên làm:

```text
sửa linh tinh tất cả service để ép Jenkins build hết
```

Cách tốt hơn nếu còn thời gian:

- Tạo Jenkins job riêng `bootstrap_main_images`.
- Build/push tuần tự danh sách service.

Nhưng ngày 4 không bắt buộc nếu tax demo đã đủ.

## Bước 5. Chuẩn bị release tag nếu làm staging nâng cao

Nếu nhóm làm Argo CD staging:

```bash
git checkout main
git pull origin main
git tag v1.0.0-demo
git push origin v1.0.0-demo
```

Image release cần có:

```text
docker.io/doubleho/yas-tax:v1.0.0-demo
```

Tạo tạm từ SHA nếu cần:

```bash
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:af2dbde4f1a2
docker tag docker.io/doubleho/yas-tax:af2dbde4f1a2 docker.io/doubleho/yas-tax:v1.0.0-demo
docker push docker.io/doubleho/yas-tax:v1.0.0-demo
```

## Done của TV2 ngày 4

TV2 xong khi có:

- Docker Hub tag tax đầy đủ.
- Nếu mở rộng service: có tag `main` cho service được deploy.
- Screenshot Docker Hub tags.
- Ghi rõ image nào là bootstrap thủ công, image nào do Jenkins build.

---

# Thành viên 3 - Jenkins CD & GitOps Lead

Nhiệm vụ ngày 4: hoàn thiện cleanup job, tinh chỉnh `developer_build`, và nếu đủ thời gian tạo Argo CD app dev.

## Bước 1. Tạo Jenkins job `developer_cleanup`

Nếu chưa tạo, vào Jenkins:

1. New Item.
2. Name:

```text
developer_cleanup
```

3. Type: Pipeline.
4. Pipeline Script:

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'ENV_NAME', defaultValue: 'demo')
        string(name: 'CONFIRM_DELETE', defaultValue: '')
    }

    stages {
        stage('Cleanup Preview') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-yas-k3s', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        set -eu
                        test "$CONFIRM_DELETE" = "DELETE"
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        kubectl delete namespace "preview-$ENV_NAME" --ignore-not-found=true
                    '''
                }
            }
        }
    }
}
```

Run:

```text
ENV_NAME=demo
CONFIRM_DELETE=DELETE
```

Kiểm tra:

```bash
kubectl get ns preview-demo
```

Nếu namespace không còn, cleanup PASS.

## Bước 2. Vì cần evidence, tạo lại preview sau cleanup

Sau khi chứng minh cleanup xong, chạy lại `developer_build`:

```text
ENV_NAME=demo
TAX_BRANCH=dev_tax_service
```

Kiểm tra:

```bash
kubectl get ns preview-demo
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get svc -n preview-demo
```

## Bước 3. Đưa Postgres, config và NodePort vào `developer_build`

Sau các lỗi thực tế ngày 3, job `developer_build` không nên chỉ Helm deploy tax. Job nên tự đảm bảo đủ dependency tối thiểu:

- Namespace `postgres` có Deployment/Service `postgresql`.
- Preview namespace có `yas-configuration`.
- Preview namespace có NodePort `yas-preview-nodeport`.

Thêm function Postgres vào trước đoạn deploy tax:

```bash
bootstrap_postgres() {
  kubectl create namespace postgres --dry-run=client -o yaml | kubectl apply -f -

  cat <<'EOF' | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
  namespace: postgres
type: Opaque
stringData:
  POSTGRES_USER: yasadminuser
  POSTGRES_PASSWORD: admin
  POSTGRES_DB: tax
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql
  namespace: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:15
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: postgresql-secret
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: postgres
spec:
  selector:
    app: postgresql
  ports:
    - name: postgres
      port: 5432
      targetPort: 5432
EOF

  kubectl rollout status deployment/postgresql -n postgres --timeout=180s || true
}
```

Thêm `yas-configuration` trước deploy tax:

```bash
bootstrap_yas_configuration() {
  kubectl delete secret yas-postgresql-credentials-secret -n "$NAMESPACE" --ignore-not-found=true
  helm dependency build k8s/charts/yas-configuration || true
  helm upgrade --install yas-configuration k8s/charts/yas-configuration \
    -n "$NAMESPACE" --create-namespace
}
```

Gọi theo thứ tự:

```bash
bootstrap_postgres
bootstrap_yas_configuration
deploy_backend tax tax yas-tax "$TAX_TAG"
```

Thêm NodePort ở cuối stage deploy:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: $NAMESPACE
spec:
  type: NodePort
  selector:
    app.kubernetes.io/name: tax
    app.kubernetes.io/instance: tax
  ports:
    - name: http
      port: 80
      targetPort: 80
      nodePort: 30080
EOF
```

Sau đó Jenkins tự tạo dependency và NodePort mỗi lần deploy.

## Bước 4. Cleanup không xóa Postgres

`developer_cleanup` chỉ xóa preview namespace:

```bash
kubectl delete namespace "preview-${ENV_NAME}" --ignore-not-found=true
```

Không đưa lệnh này vào cleanup mặc định:

```bash
kubectl delete namespace postgres
```

Lý do: Postgres là dependency runtime dùng chung để preview chạy được. Nếu cleanup xóa luôn Postgres, lần demo sau sẽ lại lỗi `UnknownHostException: postgresql.postgres` hoặc app không connect DB.

## Bước 5. Tạo Argo CD app dev nếu đủ thời gian

Chỉ làm nếu:

- cleanup đã PASS,
- developer_build evidence đã có,
- TV1 đã cài Argo CD.

Tối thiểu tạo app cho `tax` hoặc chart YAS:

```bash
kubectl create namespace dev --dry-run=client -o yaml | kubectl apply -f -
```

Nếu dùng Argo CD CLI chưa có, có thể apply Application YAML:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: yas-tax-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/hoanghaitapcode/DevOps_Lab1.git
    targetRevision: main
    path: k8s/charts/tax
    helm:
      parameters:
        - name: backend.image.repository
          value: docker.io/doubleho/yas-tax
        - name: backend.image.tag
          value: main
        - name: backend.serviceMonitor.enabled
          value: "false"
  destination:
    server: https://kubernetes.default.svc
    namespace: dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

Apply:

```bash
cat > /tmp/yas-tax-dev-app.yaml <<'EOF'
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: yas-tax-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/hoanghaitapcode/DevOps_Lab1.git
    targetRevision: main
    path: k8s/charts/tax
    helm:
      parameters:
        - name: backend.image.repository
          value: docker.io/doubleho/yas-tax
        - name: backend.image.tag
          value: main
        - name: backend.serviceMonitor.enabled
          value: "false"
  destination:
    server: https://kubernetes.default.svc
    namespace: dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF

kubectl apply -f /tmp/yas-tax-dev-app.yaml
kubectl get applications -n argocd
kubectl describe application yas-tax-dev -n argocd
```

Nếu Argo app fail vì dependency/chart, ghi evidence. Không phá preview.

## Done của TV3 ngày 4

TV3 xong khi có:

- `developer_cleanup` PASS.
- Preview tạo lại được sau cleanup.
- NodePort được tạo bởi Jenkins hoặc có command rõ.
- Nếu làm Argo: Application `yas-tax-dev` được tạo.

---

# Thành viên 4 - Service Mesh & QA/Report Lead

Nhiệm vụ ngày 4: gom evidence bắt buộc, cập nhật report, và nếu có thời gian làm Istio/Kiali tối thiểu.

## Bước 1. Gom evidence bắt buộc trước

Bạn cần chắc chắn có ảnh/log:

```text
Jenkins developer_build SUCCESS
TAX_TAG=af2dbde4f1a2
Deploying tax image=docker.io/doubleho/yas-tax:af2dbde4f1a2
kubectl deployment image đúng commit SHA
Docker Hub tags: main, dev_tax_service, af2dbde4f1a2
NodePort service 80:30080
developer_cleanup SUCCESS
namespace preview-demo deleted
```

Nếu thiếu ảnh nào, yêu cầu đúng người chụp ngay.

## Bước 2. Viết giải thích pod CrashLoop

Dùng đoạn này trong report:

```text
Preview deployment đã dùng đúng image tag commit SHA và K3s pull image thành công. Container tax start nhưng chưa Ready vì ứng dụng cần PostgreSQL runtime dependency tại DNS postgresql.postgres. Khi dependency này chưa được triển khai, Spring Boot/Liquibase không kết nối được database và container restart. Đây là lỗi dependency runtime của YAS, không phải lỗi Jenkins build image, Docker Hub push, hay Helm deploy image tag.
```

## Bước 3. Cập nhật test plan

```text
Test case: developer_build branch override
Input: TAX_BRANCH=dev_tax_service
Expected: tax image tag = commit SHA af2dbde4f1a2
Actual: docker.io/doubleho/yas-tax:af2dbde4f1a2
Status: PASS

Test case: default main tag
Input: TAX_BRANCH=main
Expected: tax image tag = main
Actual: docker.io/doubleho/yas-tax:main
Status: PASS

Test case: NodePort
Expected: service yas-preview-nodeport exposes 30080
Actual: NodePort created. Endpoint empty until tax app Ready.
Status: PARTIAL PASS

Test case: cleanup
Expected: preview-demo deleted
Actual:
Status:
```

## Bước 4. Làm Istio/Kiali tối thiểu nếu còn thời gian

Chỉ làm nếu bắt buộc đã đủ evidence. Nếu chưa có cleanup evidence, bỏ qua Istio.

Kiểm tra Istio đã cài chưa:

```bash
kubectl get pods -n istio-system
```

Nếu chưa cài, không cố nếu chỉ còn ít thời gian. Istio khá nặng.

Nếu đã cài, enable injection cho namespace `preview-demo`:

```bash
kubectl label namespace preview-demo istio-injection=enabled --overwrite
kubectl rollout restart deployment tax -n preview-demo
```

Tạo mTLS manifest:

```bash
cat > /tmp/mtls-strict.yaml <<'EOF'
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default
  namespace: preview-demo
spec:
  mtls:
    mode: STRICT
EOF

kubectl apply -f /tmp/mtls-strict.yaml
kubectl get peerauthentication -n preview-demo
```

Tạo retry VirtualService:

```bash
cat > /tmp/tax-retry.yaml <<'EOF'
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: tax-retry
  namespace: preview-demo
spec:
  hosts:
    - tax
  http:
    - route:
        - destination:
            host: tax
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: 5xx
EOF

kubectl apply -f /tmp/tax-retry.yaml
kubectl get virtualservice -n preview-demo
```

Chụp Kiali nếu có:

```bash
kubectl get svc -n istio-system | grep kiali
```

Port-forward:

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

Mở:

```text
http://localhost:20001
```

## Bước 5. Nếu không làm Istio, ghi scope rõ ràng

Ghi trong report:

```text
Nhóm ưu tiên hoàn thành phần bắt buộc gồm Jenkins CI/CD, Docker Hub image tag commit SHA, K3s preview deployment, NodePort và cleanup. Istio/Kiali được chuẩn bị trong kế hoạch nâng cao nhưng chỉ triển khai khi cluster còn đủ tài nguyên và phần bắt buộc đã ổn định.
```

## Done của TV4 ngày 4

TV4 xong khi có:

- Evidence bắt buộc đủ.
- Test plan cập nhật.
- Giải thích CrashLoop do thiếu Postgres.
- Report có phần cleanup.
- Nếu có nâng cao: ảnh Argo hoặc Istio/Kiali.

---

# Sync cuối ngày 4

Điền bảng:

| Câu hỏi | Trả lời |
|---|---|
| `developer_build` evidence đủ chưa? | yes/no |
| `developer_cleanup` evidence đủ chưa? | yes/no |
| Docker Hub tag evidence đủ chưa? | yes/no |
| NodePort evidence đủ chưa? | yes/no |
| Postgres deploy được chưa? | yes/no |
| Tax pod Ready chưa? | yes/no |
| Argo CD có làm không? | yes/no |
| Istio/Kiali có làm không? | yes/no |
| Blocker còn lại cho ngày 5? | |

## Quyết định cuối ngày

Nếu bắt buộc đủ:

```text
Ngày 5 chỉ polish report, demo script, screenshot, và rehearsal.
```

Nếu cleanup chưa đủ:

```text
Ngày 5 đầu giờ làm cleanup evidence trước.
```

Nếu app chưa Ready:

```text
Không để app Ready chặn nộp. Ghi rõ dependency Postgres và demo CD image tag.
```

Nếu Argo/Istio chưa làm:

```text
Chỉ đưa vào phần kế hoạch nâng cao nếu không có evidence thực tế.
```

## Checklist nộp cuối ngày 4 cho trưởng nhóm

TV1 gửi:

```text
[TV1 Day4]
Cluster:
Postgres:
NodePort/firewall:
Argo/Istio install if any:
Blocker:
Screenshot/log:
```

TV2 gửi:

```text
[TV2 Day4]
Docker tags verified:
Additional main images:
Release tag if any:
Blocker:
Screenshot/log:
```

TV3 gửi:

```text
[TV3 Day4]
developer_cleanup:
developer_build after cleanup:
NodePort automation:
Argo app if any:
Blocker:
Screenshot/log:
```

TV4 gửi:

```text
[TV4 Day4]
Evidence complete:
Report sections updated:
Test plan status:
Missing screenshots:
Blocker:
```
