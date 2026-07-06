# Ngày 3 - Runbook chi tiết cho cả 4 thành viên

Mục tiêu ngày 3: hoàn thành phần CD preview bắt buộc. Hết ngày 3 nhóm phải có:

- Jenkins job `developer_build` chạy được.
- Job nhận branch từng service.
- Service để `main` dùng image tag `main`.
- Service nhập branch riêng dùng image tag commit SHA cuối branch đó.
- Deploy preview vào namespace `preview-<ENV_NAME>`.
- Expose được preview bằng NodePort, ví dụ `30080`.
- Jenkins job `developer_cleanup` xóa được preview.
- Có evidence đủ để demo phần bắt buộc.

Nguyên tắc chung:

- Không làm Argo CD/Istio nếu `developer_build` và cleanup chưa xong.
- Không deploy full tất cả service nếu cluster thiếu tài nguyên.
- Ưu tiên demo tối thiểu: `tax` trước, sau đó mở rộng service.
- Tắt `ServiceMonitor` khi cluster chưa cài Prometheus Operator CRD.
- Không hardcode kubeconfig/token/password trong repo.

## Lịch làm đề xuất trong ngày

| Khung giờ | Cả nhóm cần đạt |
|---|---|
| 0h-0h30 | Sync ngày 2: Docker image, kubeconfig, Helm blocker |
| 0h30-2h00 | TV3 hoàn thiện `developer_build`; TV1 fix K3s/CRD/firewall; TV2 bảo đảm image `main`/SHA; TV4 chuẩn bị evidence |
| 2h00-3h30 | Chạy `developer_build` với `TAX_BRANCH=dev_tax_service` |
| 3h30-4h30 | Kiểm tra NodePort tự tạo trong `developer_build` và tạo cleanup job |
| 4h30-5h00 | Chụp bằng chứng, ghi blocker, chốt scope ngày 4 |

## Thông tin chung cần điền

```text
Docker Hub username: doubleho
Demo service: tax
Demo branch: dev_tax_service
Demo branch commit SHA:
Default tag: main
Preview env name: demo
Preview namespace: preview-demo
Worker external IP:
Preview domain: yas-preview.local
NodePort: 30080
Kubeconfig credential ID: kubeconfig-yas-k3s
```

---

# Thành viên 1 - Infrastructure Lead

Nhiệm vụ ngày 3: bảo đảm K3s chạy ổn, Jenkins deploy được, firewall NodePort mở, và hỗ trợ lỗi Helm/Kubernetes.

## Mục đích của TV1 trong ngày 3

TV1 không phải người chính viết Jenkins job `developer_build`. Người viết job là TV3. Vai trò của TV1 là giữ cho “đường hạ tầng” thông suốt để TV3 deploy được.

Nói dễ hiểu, TV3 bấm Jenkins deploy, còn TV1 đảm bảo các điều kiện bên dưới không bị gãy:

```text
Jenkins
  -> kubeconfig kết nối được K3s API
  -> K3s master/worker Ready
  -> Helm manifest apply được
  -> Pod được schedule lên worker
  -> Service/NodePort mở ra ngoài được
  -> Nếu lỗi Kubernetes thì đọc describe/events để chỉ ra nguyên nhân
```

TV1 cần trả lời được 5 câu hỏi:

1. Cluster K3s có sống không?
2. Jenkins có gọi được `kubectl` vào cluster không?
3. Firewall GCP đã mở NodePort `30080` chưa?
4. Lỗi deploy là lỗi hạ tầng, lỗi chart, lỗi image pull, hay lỗi thiếu runtime dependency?
5. Từ ngoài có chạm được Worker NodePort không?

Nếu TV3 deploy fail, TV1 là người chạy các lệnh `kubectl describe`, `kubectl get events`, kiểm tra firewall, kiểm tra node, rồi nói rõ lỗi nằm ở đâu. TV1 không cần sửa code Java, không cần sửa Jenkinsfile CI.

## Output TV1 cần đưa cho nhóm cuối ngày 3

- Ảnh `kubectl get nodes -o wide`.
- Ảnh Jenkins hoặc terminal chạy được `kubectl get nodes`.
- Ảnh firewall GCP có rule mở `tcp:30080`.
- Ảnh `kubectl get svc -n preview-demo` có NodePort nếu TV3 đã tạo.
- Nếu pod lỗi, gửi đúng nguyên nhân, ví dụ:
  - `ImagePullBackOff`: sai image/tag/private registry/network.
  - `FailedMount configmap not found`: thiếu `yas-configuration-configmap`.
  - `FailedMount secret not found`: thiếu `yas-postgresql-credentials-secret`.
  - `Pending`: node thiếu CPU/RAM hoặc scheduling issue.
  - `CrashLoopBackOff`: app start rồi crash, xem `kubectl logs`.

## Bước 1. Kiểm tra cluster đầu ngày

SSH vào `yas-master`:

```bash
gcloud compute ssh yas-master
```

Chạy:

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl get events -A --sort-by=.lastTimestamp | tail -50
```

Kỳ vọng:

- `yas-master` Ready.
- `yas-worker` Ready.
- Không có nhiều pod hệ thống CrashLoopBackOff.

Nếu worker NotReady, dừng các phần nâng cao và fix worker trước.

## Bước 2. Kiểm tra Jenkins còn truy cập K3s được

Nhờ TV3 chạy job `developer_build` skeleton hoặc job test:

```bash
kubectl get nodes -o wide
```

Nếu Jenkins lỗi TLS:

- Kiểm tra kubeconfig đang trỏ đúng IP.
- Nếu dùng public IP, K3s phải có `--tls-san <MASTER_EXTERNAL_IP>`.

Trên master, kiểm tra service:

```bash
sudo systemctl cat k3s | grep -A20 ExecStart
```

Phải thấy:

```text
--tls-san
<MASTER_EXTERNAL_IP>
```

Nếu vừa thêm `--tls-san`, restart:

```bash
sudo systemctl daemon-reload
sudo systemctl restart k3s
sudo systemctl status k3s --no-pager
```

Nếu vẫn lỗi cert, rotate:

```bash
sudo systemctl stop k3s
sudo k3s certificate rotate
sudo systemctl start k3s
```

## Bước 3. Mở firewall NodePort

NodePort demo dự kiến:

```text
30080
```

Kiểm tra firewall rule:

```bash
gcloud compute firewall-rules list --filter="name~yas"
```

Nếu chưa mở:

```bash
gcloud compute firewall-rules create yas-allow-preview-nodeport \
  --allow tcp:30080 \
  --source-ranges <YOUR_PUBLIC_IP_OR_DEMO_IP>/32 \
  --target-tags yas-k3s
```

Nếu cần demo nhanh cho nhiều mạng, tạm dùng:

```bash
gcloud compute firewall-rules update yas-allow-preview-nodeport \
  --source-ranges 0.0.0.0/0
```

Ghi trong report: mở rộng chỉ để demo, production cần giới hạn IP.

## Bước 4. Xử lý lỗi ServiceMonitor

Nếu TV3 gặp lỗi:

```text
no matches for kind "ServiceMonitor" in version "monitoring.coreos.com/v1"
```

Không cài Prometheus Operator lúc này. Cách nhanh là tắt ServiceMonitor khi Helm deploy:

```bash
--set backend.serviceMonitor.enabled=false
```

Test render:

```bash
helm template tax k8s/charts/tax \
  -n preview-demo \
  --set backend.image.repository=docker.io/doubleho/yas-tax \
  --set backend.image.tag=main \
  --set backend.serviceMonitor.enabled=false | grep -n "ServiceMonitor" || true
```

Kỳ vọng không còn output `ServiceMonitor`.

## Bước 5. Hỗ trợ thiếu ConfigMap/Secret/Postgres

Nếu pod tax báo thiếu:

```text
yas-configuration-configmap
yas-postgresql-credentials-secret
```

Ngày 3 có 2 hướng:

Hướng nhanh để chứng minh deploy image:

- Chấp nhận pod chưa Ready, nhưng deployment image đúng.
- Vẫn hoàn thành `developer_build` logic.

Hướng tốt hơn:

- Deploy chart cấu hình YAS nếu đủ thời gian:

```bash
helm dependency build k8s/charts/yas-configuration
helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n preview-demo --create-namespace
```

Nếu chart yêu cầu secret phức tạp, không sa lầy. Ưu tiên demo CD flow.

Nếu log app báo:

```text
UnknownHostException: postgresql.postgres
```

thì không phải lỗi image/tag/NodePort. Nghĩa là trong cluster chưa có service Postgres tên `postgresql` ở namespace `postgres`. Bản `developer_build` hoàn chỉnh ở phần TV3 bên dưới đã có đoạn bootstrap Postgres tối thiểu. Nếu job chưa có đoạn đó, báo TV3 cập nhật job.

## Bước 6. Kiểm tra NodePort sau khi TV3 tạo

Mục đích bước này: xác nhận TV3 đã tạo service kiểu `NodePort` thật sự. Chart `tax` mặc định chỉ tạo `ClusterIP`, nên nếu chỉ thấy service `tax` là `ClusterIP` thì chưa expose ra ngoài.

```bash
kubectl get svc -n preview-demo
kubectl get endpoints -n preview-demo
kubectl get pods -n preview-demo -o wide
```

Kết quả chưa đủ:

```text
tax   ClusterIP   ...   80/TCP,8090/TCP
```

Kết quả cần có thêm:

```text
yas-preview-nodeport   NodePort   ...   80:30080/TCP
```

Nếu chưa có NodePort, báo TV3 tạo service NodePort. TV1 có thể tự tạo tạm để unblock demo:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: preview-demo
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

Sau đó kiểm tra:

```bash
kubectl get svc yas-preview-nodeport -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
```

Nếu endpoints rỗng, thường là pod chưa Ready hoặc selector không khớp. Kiểm tra label thật:

```bash
kubectl get pods -n preview-demo --show-labels
```

Nếu pod đang `ImagePullBackOff`, NodePort tạo được nhưng chưa route được traffic. Lúc đó TV1 cần chạy:

```bash
kubectl describe pod -n preview-demo -l app.kubernetes.io/name=tax
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Từ máy ngoài:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080
```

Nếu dùng hosts file:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

Test:

```bash
curl -I http://yas-preview.local:30080
```

## Done của TV1 trong ngày 3

TV1 xong khi có:

- K3s 2 node Ready.
- Jenkins vẫn kết nối K3s được.
- Firewall `30080` mở.
- Hỗ trợ fix/tắt ServiceMonitor.
- Có screenshot `kubectl get nodes`, `kubectl get svc -n preview-demo`, firewall.

---

# Thành viên 2 - Jenkins CI & Container Lead

Nhiệm vụ ngày 3: đảm bảo image tag có đủ cho `developer_build`, nhất là tag `main` và tag commit SHA branch demo.

## Bước 1. Kiểm tra Docker Hub image branch demo

Trên máy local:

```bash
git fetch origin dev_tax_service
TAX_SHA="$(git rev-parse --short=12 origin/dev_tax_service)"
echo "$TAX_SHA"
docker pull docker.io/doubleho/yas-tax:$TAX_SHA
docker pull docker.io/doubleho/yas-tax:dev_tax_service
```

Kỳ vọng pull được cả 2.

Nếu chỉ có branch alias mà chưa có SHA, Jenkins stage Docker build/push sai. Phải có tag commit SHA.

## Bước 2. Kiểm tra tag `main`

Project 2 yêu cầu service không nhập branch riêng dùng tag mặc định `main` hoặc `latest`.

Kiểm tra:

```bash
docker pull docker.io/doubleho/yas-tax:main
```

Nếu chưa có tag `main`, cần build branch main hoặc tag thủ công tạm cho demo.

Cách đúng: chạy Jenkins main sau khi Docker stage đã merge vào main.

Cách tạm nếu gấp:

```bash
docker pull docker.io/doubleho/yas-tax:$TAX_SHA
docker tag docker.io/doubleho/yas-tax:$TAX_SHA docker.io/doubleho/yas-tax:main
docker push docker.io/doubleho/yas-tax:main
```

Ghi rõ đây là bootstrap default image ban đầu. Sau đó Jenkins main sẽ quản lý tag `main`.

## Bước 3. Build/push default `main` cho các service demo tối thiểu

Nếu ngày 3 chỉ demo `tax`, chỉ cần `yas-tax:main` và `yas-tax:<sha>`.

Nếu demo storefront flow, cần chuẩn bị thêm:

```text
yas-product:main
yas-cart:main
yas-order:main
yas-customer:main
yas-inventory:main
yas-tax:main
yas-storefront-bff:main
yas-storefront:main
```

Không build song song. Làm từng service.

## Bước 4. Hỗ trợ TV3 resolve tag

TV3 sẽ dùng:

```bash
git rev-parse --short=12 origin/dev_tax_service
```

Bạn phải đảm bảo tag Docker Hub khớp chính xác với output này.

Gửi cho nhóm:

```text
TAX_BRANCH=dev_tax_service
TAX_SHA=<sha>
Image=docker.io/doubleho/yas-tax:<sha>
Branch alias=docker.io/doubleho/yas-tax:dev_tax_service
Default=docker.io/doubleho/yas-tax:main
```

## Bước 5. Nếu branchAlias bị sai tên

Trong Docker stage đang có:

```groovy
def branchAlias = env.BRANCH_NAME.replaceAll('[^A-Za-z0-9_.-]', '-')
```

Nếu branch là:

```text
dev_tax_service
```

Tag sẽ là:

```text
dev_tax_service
```

Nếu branch là:

```text
feature/dev-tax-service
```

Tag sẽ là:

```text
feature-dev-tax-service
```

Đây là đúng vì Docker tag không nên có `/`.

## Done của TV2 trong ngày 3

TV2 xong khi có:

- Docker Hub có `yas-tax:<commit-sha>`.
- Docker Hub có `yas-tax:dev_tax_service`.
- Docker Hub có `yas-tax:main`.
- Gửi SHA/tag cho TV3 và TV4.
- Chụp Docker Hub tags.

---

# Thành viên 3 - Jenkins CD & GitOps Lead

Nhiệm vụ ngày 3: hoàn thiện Jenkins job `developer_build`, deploy preview bằng Helm, tạo NodePort, tạo cleanup job.

## Bước 1. Tạo hoặc mở Jenkins job `developer_build`

Trong Jenkins:

1. New Item nếu chưa có.
2. Name:

```text
developer_build
```

3. Type:

```text
Pipeline
```

4. Pipeline script: paste pipeline bên dưới.

## Bước 2. Pipeline `developer_build` bản hoàn chỉnh cho demo `tax`

Thay `DOCKERHUB_USER = 'doubleho'` nếu Docker Hub user khác.

Lưu ý thực tế:

- Job `developer_build` dùng Jenkins `Pipeline Script`, không phải Multibranch Pipeline.
- Vì vậy không dùng `checkout scm`; dùng `git branch: 'main', url: ...`.
- Jenkins có thể chạy shell bằng `/bin/sh`, nên dùng `set -eu` thay vì `set -euo pipefail`.

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'ENV_NAME', defaultValue: 'demo', description: 'Preview environment name')
        string(name: 'PRODUCT_BRANCH', defaultValue: 'main')
        string(name: 'CART_BRANCH', defaultValue: 'main')
        string(name: 'ORDER_BRANCH', defaultValue: 'main')
        string(name: 'CUSTOMER_BRANCH', defaultValue: 'main')
        string(name: 'INVENTORY_BRANCH', defaultValue: 'main')
        string(name: 'TAX_BRANCH', defaultValue: 'main')
        string(name: 'MEDIA_BRANCH', defaultValue: 'main')
        string(name: 'SEARCH_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_BFF_BRANCH', defaultValue: 'main')
        string(name: 'STOREFRONT_UI_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_BFF_BRANCH', defaultValue: 'main')
        string(name: 'BACKOFFICE_UI_BRANCH', defaultValue: 'main')
    }

    environment {
        DOCKERHUB_USER = 'doubleho'
        REPO_URL = 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: "${REPO_URL}"
            }
        }

        stage('Deploy Preview') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-yas-k3s', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$KUBECONFIG_FILE"
                        NAMESPACE="preview-${ENV_NAME}"

                        resolve_tag() {
                          branch="$1"
                          if [ "$branch" = "main" ]; then
                            echo "main"
                          else
                            git fetch origin "$branch"
                            git rev-parse --short=12 "origin/$branch"
                          fi
                        }

                        TAX_TAG="$(resolve_tag "$TAX_BRANCH")"
                        PRODUCT_TAG="$(resolve_tag "$PRODUCT_BRANCH")"
                        CART_TAG="$(resolve_tag "$CART_BRANCH")"
                        ORDER_TAG="$(resolve_tag "$ORDER_BRANCH")"
                        CUSTOMER_TAG="$(resolve_tag "$CUSTOMER_BRANCH")"
                        INVENTORY_TAG="$(resolve_tag "$INVENTORY_BRANCH")"
                        MEDIA_TAG="$(resolve_tag "$MEDIA_BRANCH")"
                        SEARCH_TAG="$(resolve_tag "$SEARCH_BRANCH")"
                        STOREFRONT_BFF_TAG="$(resolve_tag "$STOREFRONT_BFF_BRANCH")"
                        STOREFRONT_UI_TAG="$(resolve_tag "$STOREFRONT_UI_BRANCH")"
                        BACKOFFICE_BFF_TAG="$(resolve_tag "$BACKOFFICE_BFF_BRANCH")"
                        BACKOFFICE_UI_TAG="$(resolve_tag "$BACKOFFICE_UI_BRANCH")"

                        echo "Namespace: $NAMESPACE"
                        echo "TAX_TAG=$TAX_TAG"

                        kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

                        bootstrap_postgres() {
                          echo "Ensuring shared Postgres runtime exists at postgresql.postgres"
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
                          kubectl get svc,pods -n postgres
                        }

                        bootstrap_yas_configuration() {
                          echo "Ensuring yas-configuration exists in $NAMESPACE"

                          # Nếu trước đó từng tạo secret tay, Helm sẽ bị conflict ownership.
                          # Xóa secret preview namespace để chart yas-configuration quản lý lại.
                          kubectl delete secret yas-postgresql-credentials-secret -n "$NAMESPACE" --ignore-not-found=true

                          helm dependency build k8s/charts/yas-configuration || true
                          helm upgrade --install yas-configuration k8s/charts/yas-configuration \
                            -n "$NAMESPACE" --create-namespace
                        }

                        deploy_backend() {
                          release="$1"
                          chart="$2"
                          image="$3"
                          tag="$4"

                          echo "Deploying $release image=docker.io/$DOCKERHUB_USER/$image:$tag"
                          helm dependency build "k8s/charts/$chart"
                          helm upgrade --install "$release" "k8s/charts/$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set backend.image.repository="docker.io/$DOCKERHUB_USER/$image" \
                            --set backend.image.tag="$tag" \
                            --set backend.serviceMonitor.enabled=false
                        }

                        deploy_ui() {
                          release="$1"
                          chart="$2"
                          image="$3"
                          tag="$4"

                          echo "Deploying $release image=docker.io/$DOCKERHUB_USER/$image:$tag"
                          helm dependency build "k8s/charts/$chart"
                          helm upgrade --install "$release" "k8s/charts/$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set ui.image.repository="docker.io/$DOCKERHUB_USER/$image" \
                            --set ui.image.tag="$tag"
                        }

                        bootstrap_postgres
                        bootstrap_yas_configuration

                        # Bắt đầu tối thiểu với tax để chứng minh branch -> commit SHA -> deploy.
                        deploy_backend tax tax yas-tax "$TAX_TAG"

                        # Mở dần các service dưới đây khi image main đã có và cluster chịu được.
                        # deploy_backend product product yas-product "$PRODUCT_TAG"
                        # deploy_backend cart cart yas-cart "$CART_TAG"
                        # deploy_backend order order yas-order "$ORDER_TAG"
                        # deploy_backend customer customer yas-customer "$CUSTOMER_TAG"
                        # deploy_backend inventory inventory yas-inventory "$INVENTORY_TAG"
                        # deploy_backend media media yas-media "$MEDIA_TAG"
                        # deploy_backend search search yas-search "$SEARCH_TAG"
                        # deploy_backend storefront-bff storefront-bff yas-storefront-bff "$STOREFRONT_BFF_TAG"
                        # deploy_ui storefront-ui storefront-ui yas-storefront "$STOREFRONT_UI_TAG"

                        kubectl get all -n "$NAMESPACE"
                        kubectl get deployment tax -n "$NAMESPACE" \
                          -o jsonpath='{.spec.template.spec.containers[0].image}{"\\n"}'

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

                        kubectl get svc -n "$NAMESPACE"
                    '''
                }
            }
        }
    }
}
```

## Bước 3. Chạy `developer_build`

Build with Parameters:

```text
ENV_NAME=demo
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=main
CART_BRANCH=main
...
```

Kỳ vọng console:

```text
TAX_TAG=<commit-sha>
Deploying tax image=docker.io/doubleho/yas-tax:<commit-sha>
```

Kiểm tra:

```bash
kubectl get all -n preview-demo
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

## Bước 4. Kiểm tra NodePort

Nếu đã dùng pipeline ở Bước 2 bản mới, NodePort đã được tạo tự động ở cuối job. Bước này chỉ kiểm tra. Chỉ tạo thủ công khi job chưa có đoạn tạo `yas-preview-nodeport`.

Nếu chỉ deploy `tax`, NodePort có thể trỏ vào `tax` để chứng minh service expose. Nếu deploy UI thì trỏ `storefront-ui`.

Lưu ý rất quan trọng: tạo NodePort **không đồng nghĩa curl sẽ thành công ngay**. NodePort chỉ tạo đường vào từ ngoài cluster. Curl chỉ thành công khi pod backend phía sau service đã `Ready` và service có endpoint.

Với `tax`, nếu chưa có PostgreSQL runtime dependency thì app sẽ chưa Ready hoặc CrashLoop vì lỗi:

```text
UnknownHostException: postgresql.postgres
```

Khi đó:

```bash
kubectl get svc -n preview-demo
```

có thể đã thấy:

```text
yas-preview-nodeport   NodePort   ...   80:30080/TCP
```

nhưng:

```bash
kubectl get endpoints yas-preview-nodeport -n preview-demo
```

sẽ rỗng, và lệnh này sẽ fail:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080/tax/actuator/health
```

Muốn curl thành công thì pod `tax` phải thành `1/1 Running`. Nếu đã dùng pipeline hoàn chỉnh ở Bước 2, job đã tự bootstrap Postgres và `yas-configuration`; nếu vẫn chưa Ready thì xem log/describe để tìm lỗi app.

Trường hợp tối thiểu trỏ vào `tax`:

```bash
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: preview-demo
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

Nếu endpoint rỗng:

```bash
kubectl get pods -n preview-demo --show-labels
kubectl get endpoints yas-preview-nodeport -n preview-demo
```

Sửa selector theo label thật.

Test:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080/tax/actuator/health
```

Nếu dùng hosts:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

Test:

```bash
curl -I http://yas-preview.local:30080/tax/actuator/health
```

Nếu service chưa Ready vì thiếu DB/config/Postgres, curl có thể fail. Đây là bình thường. NodePort service vẫn là evidence phụ rằng đường expose đã được tạo. Evidence chính ngày 3 là `developer_build` resolve branch đúng commit SHA và deployment image tag đúng.

## Bước 5. Tạo Jenkins job `developer_cleanup`

New Item:

```text
developer_cleanup
```

Type:

```text
Pipeline
```

Pipeline script:

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

Kỳ vọng NotFound hoặc không còn namespace.

## Done của TV3 trong ngày 3

TV3 xong khi có:

- `developer_build` chạy được với `TAX_BRANCH=dev_tax_service`.
- Namespace `preview-demo` được tạo.
- Deployment `tax` dùng image `docker.io/doubleho/yas-tax:<commit-sha>`.
- ServiceMonitor đã tắt khi deploy.
- Có NodePort service.
- `developer_cleanup` xóa được namespace.

## Lỗi thực tế đã gặp và cách sửa

### Lỗi 1: `checkout scm` không chạy trong Pipeline Script

Log:

```text
ERROR: 'checkout scm' is only available when using "Multibranch Pipeline" or "Pipeline script from SCM"
```

Cách sửa:

```groovy
stage('Checkout') {
    steps {
        git branch: 'main', url: 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
    }
}
```

### Lỗi 2: `set -euo pipefail` không chạy với `/bin/sh`

Log:

```text
set: Illegal option -o pipefail
```

Cách sửa trong Jenkins `sh` block:

```bash
set -eu
```

### Lỗi 3: `ServiceMonitor` CRD không có

Log:

```text
no matches for kind "ServiceMonitor" in version "monitoring.coreos.com/v1"
```

Cách sửa khi deploy chart backend:

```bash
--set backend.serviceMonitor.enabled=false
```

### Lỗi 4: Chỉ có `ClusterIP`, chưa có NodePort

Chart `tax` mặc định tạo service `tax` kiểu `ClusterIP`. Muốn truy cập ngoài cluster cần tạo thêm:

```text
yas-preview-nodeport   NodePort   80:30080/TCP
```

Pipeline `developer_build` bản mới đã có đoạn `kubectl apply` tạo service này.

### Lỗi 5: Pod chưa Ready do thiếu runtime dependency

Nếu log báo:

```text
UnknownHostException: postgresql.postgres
```

thì deploy image đã đúng, nhưng thiếu PostgreSQL runtime. Xem `DAY4_ALL_MEMBERS_RUNBOOK.md` để tạo PostgreSQL đơn giản và `yas-configuration`.

---

# Thành viên 4 - Service Mesh & QA/Report Lead

Nhiệm vụ ngày 3: chụp evidence phần bắt buộc và cập nhật test plan/demo script.

## Bước 1. Chuẩn bị folder evidence ngày 3

```text
evidence/
  06-developer-build/
  07-preview-nodeport/
  08-cleanup/
```

## Bước 2. Chụp Jenkins job `developer_build`

Ảnh cần:

- Job tên `developer_build`.
- Màn hình parameters.
- Console log có:

```text
TAX_BRANCH=dev_tax_service
TAX_TAG=<commit-sha>
Deploying tax image=docker.io/doubleho/yas-tax:<commit-sha>
helm upgrade --install tax
```

## Bước 3. Chụp Kubernetes state

Chạy hoặc nhờ TV3 chạy:

```bash
kubectl get ns preview-demo
kubectl get all -n preview-demo
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Ảnh quan trọng nhất:

```text
docker.io/doubleho/yas-tax:<commit-sha>
```

## Bước 4. Chụp Docker Hub đối chiếu

Chụp tag:

```text
doubleho/yas-tax:<commit-sha>
doubleho/yas-tax:dev_tax_service
doubleho/yas-tax:main
```

Ghi rõ:

- Commit SHA tag là bắt buộc.
- Branch alias là phụ.
- Cả hai có thể trỏ cùng digest, điều đó đúng.

## Bước 5. Chụp NodePort

Chạy:

```bash
kubectl get svc -n preview-demo
kubectl get endpoints -n preview-demo
curl -I http://yas-preview.local:30080/tax/actuator/health
```

Nếu curl fail do app thiếu DB/config, ghi:

```text
NodePort service đã tạo. Pod/app chưa Ready do thiếu dependency/config, sẽ hoàn thiện ở bước deploy full YAS. Phần CD image tag và Kubernetes deployment đã chứng minh đúng.
```

Nếu curl OK, chụp output HTTP.

## Bước 6. Chụp cleanup

Trước cleanup:

```bash
kubectl get ns preview-demo
```

Chạy Jenkins `developer_cleanup`.

Sau cleanup:

```bash
kubectl get ns preview-demo
```

Chụp console Jenkins cleanup và output NotFound.

## Bước 7. Cập nhật test plan ngày 3

Điền:

```text
Test case: developer_build resolves branch to commit SHA
Input:
- ENV_NAME=demo
- TAX_BRANCH=dev_tax_service
Expected:
- TAX_TAG=<commit-sha of origin/dev_tax_service>
- Deployment image uses docker.io/doubleho/yas-tax:<commit-sha>
Actual:
Status: PASS/FAIL
Evidence:

Test case: preview namespace
Expected:
- preview-demo namespace created
Actual:
Status:
Evidence:

Test case: cleanup
Expected:
- preview-demo namespace deleted
Actual:
Status:
Evidence:
```

## Bước 8. Câu giải thích cho report

Copy đoạn này:

```text
Jenkins job developer_build là CD job thủ công cho developer. Job nhận branch của từng service. Nếu parameter là main, job deploy image tag main. Nếu parameter là branch khác, job fetch branch đó và resolve commit SHA bằng git rev-parse --short=12 origin/<branch>, sau đó deploy image Docker Hub có tag commit SHA tương ứng. Trong demo, TAX_BRANCH=dev_tax_service được resolve thành <commit-sha> và deployment tax trong namespace preview-demo dùng image docker.io/doubleho/yas-tax:<commit-sha>. Job developer_cleanup xóa namespace preview-demo sau khi test xong.
```

## Done của TV4 trong ngày 3

TV4 xong khi có:

- Evidence `developer_build`.
- Evidence image tag commit SHA trong Kubernetes deployment.
- Evidence Docker Hub tag.
- Evidence NodePort service.
- Evidence cleanup.
- Test plan ngày 3 PASS/FAIL.

---

# Sync cuối ngày 3

Điền bảng:

| Câu hỏi | Trả lời |
|---|---|
| `developer_build` đã tạo chưa? | yes/no |
| `developer_build` chạy với `TAX_BRANCH=dev_tax_service` chưa? | yes/no |
| Commit SHA là gì? | |
| Deployment image là gì? | |
| Namespace preview là gì? | preview-demo |
| NodePort service tạo chưa? | yes/no |
| URL test là gì? | http://yas-preview.local:30080 |
| `developer_cleanup` xóa namespace được chưa? | yes/no |
| Pod Ready chưa? | yes/no |
| Blocker lớn nhất? | |

## Quyết định cuối ngày

Nếu `developer_build` và cleanup đã xong:

```text
Ngày 4 làm Argo CD/Istio hoặc mở rộng full service demo.
```

Nếu `developer_build` chưa xong:

```text
Ngày 4 không làm nâng cao. Tiếp tục fix developer_build trước.
```

Nếu pod chưa Ready nhưng image deploy đúng:

```text
Ngày 4 xử lý dependency/config để demo app chạy được, nhưng phần CD image tag đã có evidence.
```

## Checklist nộp cuối ngày 3 cho trưởng nhóm

TV1 gửi:

```text
[TV1 Day3]
K3s nodes:
Firewall NodePort:
ServiceMonitor issue fixed by:
Blocker:
Screenshot/log:
```

TV2 gửi:

```text
[TV2 Day3]
Tax SHA:
Docker tags available:
- yas-tax:<sha>
- yas-tax:dev_tax_service
- yas-tax:main
Blocker:
Screenshot/log:
```

TV3 gửi:

```text
[TV3 Day3]
developer_build status:
Namespace:
Deployment image:
NodePort:
developer_cleanup status:
Blocker:
Screenshot/log:
```

TV4 gửi:

```text
[TV4 Day3]
Evidence collected:
PASS test cases:
FAIL/blocker cases:
Report updated: yes/no
```
