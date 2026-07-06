# Chặng 4 chi tiết - Nâng `developer_build` thành preview e-commerce

Mục tiêu của chặng này: biến Jenkins job `developer_build` từ chỗ chỉ deploy được `tax` thành job deploy được một môi trường preview gần giống e-commerce thật.

Luồng đúng cần đạt:

```text
Developer sửa 1 hoặc nhiều service trên branch riêng
        |
Jenkins CI build/push image commit SHA cho service đó
        |
Developer vào job developer_build
        |
Nhập branch cho service đang sửa, service còn lại để main
        |
Jenkins deploy namespace preview-demo lên K3s
        |
Service sửa dùng image commit SHA
Service không sửa dùng image main
        |
NodePort trỏ vào storefront-ui để test từ browser
```

Ví dụ demo đẹp:

```text
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=dev_product_service
STOREFRONT_UI_BRANCH=dev_storefront_ui
Các branch còn lại=main
DEPLOY_MODE=core
```

Kết quả mong muốn:

```text
tax image            = docker.io/doubleho/yas-tax:<sha của dev_tax_service>
product image        = docker.io/doubleho/yas-product:<sha của dev_product_service>
storefront-ui image  = docker.io/doubleho/yas-storefront:<sha của dev_storefront_ui>
cart/order/customer/inventory/storefront-bff = tag main
```

---

# Bước 0 - Hiểu rõ chặng 4 làm gì

`developer_build` không build Docker image. Image phải được Jenkins CI build/push trước đó rồi.

`developer_build` chỉ làm 5 việc:

1. Nhận parameter branch từng service.
2. Resolve branch thành tag image:
   - `main` -> tag `main`
   - branch khác -> commit SHA cuối branch
3. Bootstrap runtime dependency tối thiểu:
   - Postgres service `postgresql.postgres`
   - Database cho các service core
   - `yas-configuration` trong namespace preview
4. Helm deploy service lên K3s.
5. Tạo NodePort để developer test.

Nếu Docker Hub thiếu image tag, `developer_build` không tự sửa được. Phải quay lại Jenkins CI/build image.

---

# Bước 1 - Điều kiện trước khi làm

Trước khi sửa job, kiểm tra các thứ này.

## 1.1. K3s còn chạy

```bash
kubectl get nodes -o wide
kubectl get pods -n argocd || true
kubectl get pods -n postgres || true
```

Kỳ vọng:

```text
yas-master Ready
yas-worker Ready
```

## 1.2. Jenkins còn dùng được kubeconfig

Trong Jenkins job cũ `developer_build`, console phải từng chạy được:

```bash
kubectl get nodes -o wide
```

Credential ID cần có:

```text
kubeconfig-yas-k3s
```

## 1.3. Docker Hub đã có tag `main` cho core service

Core service cần có tag `main`:

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

Kiểm tra nhanh trên máy local:

```bash
docker pull --platform linux/amd64 docker.io/doubleho/yas-product:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-cart:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-order:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-customer:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-inventory:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-storefront-bff:main
docker pull --platform linux/amd64 docker.io/doubleho/yas-storefront:main
```

Nếu máy Mac ARM báo:

```text
no matching manifest for linux/arm64
```

thì thêm `--platform linux/amd64`, như trên. GCP worker thường là `linux/amd64`, nên image dùng được.

## 1.4. Docker Hub đã có tag branch override

Ví dụ:

```bash
git fetch origin dev_tax_service dev_product_service dev_storefront_ui

TAX_SHA="$(git rev-parse --short=12 origin/dev_tax_service)"
PRODUCT_SHA="$(git rev-parse --short=12 origin/dev_product_service)"
STOREFRONT_SHA="$(git rev-parse --short=12 origin/dev_storefront_ui)"

echo "$TAX_SHA"
echo "$PRODUCT_SHA"
echo "$STOREFRONT_SHA"

docker pull --platform linux/amd64 docker.io/doubleho/yas-tax:$TAX_SHA
docker pull --platform linux/amd64 docker.io/doubleho/yas-product:$PRODUCT_SHA
docker pull --platform linux/amd64 docker.io/doubleho/yas-storefront:$STOREFRONT_SHA
```

Nếu pull không được, quay lại Jenkins CI build branch đó trước.

---

# Bước 2 - Tạo job test trước, đừng phá job chính

Trong Jenkins:

1. New Item.
2. Tên:

```text
developer_build_v2_test
```

3. Chọn `Pipeline`.
4. OK.
5. Trong phần Pipeline:
   - Definition: `Pipeline script`.
   - Use Groovy Sandbox: bật.
6. Dán pipeline ở Bước 3.
7. Save.

Chỉ khi `developer_build_v2_test` chạy ổn mới copy script sang job chính:

```text
developer_build
```

---

# Bước 3 - Pipeline hoàn chỉnh cho `developer_build_v2_test`

Thay toàn bộ script bằng bản này:

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'ENV_NAME', defaultValue: 'demo', description: 'Preview environment name')
        string(name: 'DEPLOY_MODE', defaultValue: 'core', description: 'tax-only, core, or full')

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
        string(name: 'SWAGGER_UI_BRANCH', defaultValue: 'main')
    }

    environment {
        DOCKERHUB_USER = 'doubleho'
        REPO_URL = 'https://github.com/hoanghaitapcode/DevOps_Lab1.git'
        NODE_PORT = '30080'
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

                        echo "Preview namespace: $NAMESPACE"
                        echo "Deploy mode: $DEPLOY_MODE"

                        resolve_tag() {
                          branch="$1"
                          if [ "$branch" = "main" ]; then
                            echo "main"
                          else
                            git fetch origin "$branch:refs/remotes/origin/$branch"
                            git rev-parse --short=12 "origin/$branch"
                          fi
                        }

                        PRODUCT_TAG="$(resolve_tag "$PRODUCT_BRANCH")"
                        CART_TAG="$(resolve_tag "$CART_BRANCH")"
                        ORDER_TAG="$(resolve_tag "$ORDER_BRANCH")"
                        CUSTOMER_TAG="$(resolve_tag "$CUSTOMER_BRANCH")"
                        INVENTORY_TAG="$(resolve_tag "$INVENTORY_BRANCH")"
                        TAX_TAG="$(resolve_tag "$TAX_BRANCH")"
                        MEDIA_TAG="$(resolve_tag "$MEDIA_BRANCH")"
                        SEARCH_TAG="$(resolve_tag "$SEARCH_BRANCH")"
                        STOREFRONT_BFF_TAG="$(resolve_tag "$STOREFRONT_BFF_BRANCH")"
                        STOREFRONT_UI_TAG="$(resolve_tag "$STOREFRONT_UI_BRANCH")"
                        BACKOFFICE_BFF_TAG="$(resolve_tag "$BACKOFFICE_BFF_BRANCH")"
                        BACKOFFICE_UI_TAG="$(resolve_tag "$BACKOFFICE_UI_BRANCH")"
                        SWAGGER_UI_TAG="$(resolve_tag "$SWAGGER_UI_BRANCH")"

                        echo "Resolved image tags:"
                        echo "  product=$PRODUCT_TAG"
                        echo "  cart=$CART_TAG"
                        echo "  order=$ORDER_TAG"
                        echo "  customer=$CUSTOMER_TAG"
                        echo "  inventory=$INVENTORY_TAG"
                        echo "  tax=$TAX_TAG"
                        echo "  storefront-bff=$STOREFRONT_BFF_TAG"
                        echo "  storefront-ui=$STOREFRONT_UI_TAG"

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
  POSTGRES_DB: postgres
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

                          for db in product cart order customer inventory tax media search; do
                            echo "Ensuring database $db exists"
                            kubectl exec -n postgres deploy/postgresql -- sh -c "export PGPASSWORD=admin; psql -U yasadminuser -d postgres -tc \\"SELECT 1 FROM pg_database WHERE datname = '$db'\\" | grep -q 1 || psql -U yasadminuser -d postgres -c \\"CREATE DATABASE $db OWNER yasadminuser;\\"" || true
                          done

                          kubectl get svc,pods -n postgres
                        }

                        bootstrap_yas_configuration() {
                          echo "Ensuring yas-configuration exists in $NAMESPACE"

                          # Xóa secret tạo tay nếu có để tránh Helm ownership conflict.
                          kubectl delete secret yas-postgresql-credentials-secret -n "$NAMESPACE" --ignore-not-found=true

                          helm dependency build k8s/charts/yas-configuration || true
                          helm upgrade --install yas-configuration k8s/charts/yas-configuration \
                            -n "$NAMESPACE" --create-namespace

                          kubectl get configmap -n "$NAMESPACE" | grep yas-configuration || true
                          kubectl get secret -n "$NAMESPACE" | grep yas-postgresql || true
                        }

                        deploy_backend() {
                          release="$1"
                          chart="$2"
                          image="$3"
                          tag="$4"

                          echo "Deploying backend $release image=docker.io/$DOCKERHUB_USER/$image:$tag"

                          helm dependency build "k8s/charts/$chart" || true
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

                          echo "Deploying UI $release image=docker.io/$DOCKERHUB_USER/$image:$tag"

                          helm dependency build "k8s/charts/$chart" || true
                          helm upgrade --install "$release" "k8s/charts/$chart" \
                            -n "$NAMESPACE" --create-namespace \
                            --set ui.image.repository="docker.io/$DOCKERHUB_USER/$image" \
                            --set ui.image.tag="$tag"
                        }

                        deploy_swagger() {
                          echo "Deploying swagger-ui"

                          helm dependency build k8s/charts/swagger-ui || true
                          helm upgrade --install swagger-ui k8s/charts/swagger-ui \
                            -n "$NAMESPACE" --create-namespace \
                            --set image.repository=swaggerapi/swagger-ui \
                            --set image.tag=v4.16.0
                        }

                        create_nodeport() {
                          target_name="$1"
                          target_port="$2"

                          echo "Creating NodePort $NODE_PORT -> $target_name:$target_port"

                          cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: $NAMESPACE
spec:
  type: NodePort
  selector:
    app.kubernetes.io/name: $target_name
    app.kubernetes.io/instance: $target_name
  ports:
    - name: http
      port: 80
      targetPort: $target_port
      nodePort: $NODE_PORT
EOF
                        }

                        bootstrap_postgres
                        bootstrap_yas_configuration

                        if [ "$DEPLOY_MODE" = "tax-only" ]; then
                          deploy_backend tax tax yas-tax "$TAX_TAG"
                          create_nodeport tax 80
                        elif [ "$DEPLOY_MODE" = "core" ]; then
                          deploy_backend product product yas-product "$PRODUCT_TAG"
                          deploy_backend cart cart yas-cart "$CART_TAG"
                          deploy_backend customer customer yas-customer "$CUSTOMER_TAG"
                          deploy_backend inventory inventory yas-inventory "$INVENTORY_TAG"
                          deploy_backend tax tax yas-tax "$TAX_TAG"
                          deploy_backend order order yas-order "$ORDER_TAG"
                          deploy_backend storefront-bff storefront-bff yas-storefront-bff "$STOREFRONT_BFF_TAG"
                          deploy_ui storefront-ui storefront-ui yas-storefront "$STOREFRONT_UI_TAG"
                          create_nodeport storefront-ui 3000
                        elif [ "$DEPLOY_MODE" = "full" ]; then
                          deploy_backend product product yas-product "$PRODUCT_TAG"
                          deploy_backend cart cart yas-cart "$CART_TAG"
                          deploy_backend customer customer yas-customer "$CUSTOMER_TAG"
                          deploy_backend inventory inventory yas-inventory "$INVENTORY_TAG"
                          deploy_backend tax tax yas-tax "$TAX_TAG"
                          deploy_backend order order yas-order "$ORDER_TAG"
                          deploy_backend media media yas-media "$MEDIA_TAG"
                          deploy_backend search search yas-search "$SEARCH_TAG"
                          deploy_backend storefront-bff storefront-bff yas-storefront-bff "$STOREFRONT_BFF_TAG"
                          deploy_ui storefront-ui storefront-ui yas-storefront "$STOREFRONT_UI_TAG"
                          deploy_backend backoffice-bff backoffice-bff yas-backoffice-bff "$BACKOFFICE_BFF_TAG"
                          deploy_ui backoffice-ui backoffice-ui yas-backoffice "$BACKOFFICE_UI_TAG"
                          deploy_backend sampledata sampledata yas-sampledata main
                          deploy_swagger
                          create_nodeport storefront-ui 3000
                        else
                          echo "Unsupported DEPLOY_MODE=$DEPLOY_MODE. Use tax-only, core, or full."
                          exit 1
                        fi

                        echo "Current resources in $NAMESPACE"
                        kubectl get all -n "$NAMESPACE"
                        kubectl get svc -n "$NAMESPACE"
                        kubectl get endpoints yas-preview-nodeport -n "$NAMESPACE" || true

                        echo "Deployment images:"
                        for deploy in product cart order customer inventory tax storefront-bff storefront-ui media search backoffice-bff backoffice-ui sampledata; do
                          if kubectl get deployment "$deploy" -n "$NAMESPACE" >/dev/null 2>&1; then
                            image="$(kubectl get deployment "$deploy" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].image}')"
                            echo "$deploy -> $image"
                          fi
                        done
                    '''
                }
            }
        }
    }
}
```

---

# Bước 4 - Chạy thử lần 1: `tax-only`

Mục tiêu: kiểm tra job mới không hỏng những gì đã làm được.

Trong Jenkins `developer_build_v2_test`:

```text
Build with Parameters
```

Nhập:

```text
ENV_NAME=demo
DEPLOY_MODE=tax-only
TAX_BRANCH=dev_tax_service
Các branch khác=main
```

Kỳ vọng console có:

```text
Deploy mode: tax-only
tax=<commit-sha>
Deploying backend tax image=docker.io/doubleho/yas-tax:<commit-sha>
Creating NodePort 30080 -> tax:80
```

Kiểm tra local:

```bash
kubectl get pods -n preview-demo
kubectl get svc -n preview-demo
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Nếu bước này fail thì chưa chạy `core`. Fix `tax-only` trước.

---

# Bước 5 - Chạy thử lần 2: `core` toàn bộ dùng `main`

Mục tiêu: xem core flow có deploy được với image ổn định `main`.

Trong Jenkins:

```text
ENV_NAME=demo
DEPLOY_MODE=core
PRODUCT_BRANCH=main
CART_BRANCH=main
ORDER_BRANCH=main
CUSTOMER_BRANCH=main
INVENTORY_BRANCH=main
TAX_BRANCH=main
STOREFRONT_BFF_BRANCH=main
STOREFRONT_UI_BRANCH=main
Các branch khác=main
```

Kỳ vọng console:

```text
Deploying backend product image=docker.io/doubleho/yas-product:main
Deploying backend cart image=docker.io/doubleho/yas-cart:main
Deploying backend order image=docker.io/doubleho/yas-order:main
Deploying backend customer image=docker.io/doubleho/yas-customer:main
Deploying backend inventory image=docker.io/doubleho/yas-inventory:main
Deploying backend tax image=docker.io/doubleho/yas-tax:main
Deploying backend storefront-bff image=docker.io/doubleho/yas-storefront-bff:main
Deploying UI storefront-ui image=docker.io/doubleho/yas-storefront:main
Creating NodePort 30080 -> storefront-ui:3000
```

Kiểm tra:

```bash
kubectl get pods -n preview-demo
kubectl get svc -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
kubectl get deployment storefront-ui -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Nếu `storefront-ui` ImagePullBackOff:

```bash
kubectl describe pod -n preview-demo -l app.kubernetes.io/name=storefront-ui
docker pull --platform linux/amd64 docker.io/doubleho/yas-storefront:main
```

Nếu backend CrashLoop:

```bash
kubectl logs -n preview-demo deploy/<service-name> --tail=120
kubectl describe pod -n preview-demo -l app.kubernetes.io/name=<service-name>
```

Lỗi thường gặp:

```text
UnknownHostException: postgresql.postgres
```

Nghĩa là Postgres service chưa chạy hoặc DNS chưa có.

```text
database "<service>" does not exist
```

Nghĩa là cần tạo database cho service đó. Pipeline ở trên đã cố tạo DB `product cart order customer inventory tax media search`; nếu vẫn lỗi, chạy lại job hoặc kiểm tra Postgres log.

```text
secret/configmap not found
```

Nghĩa là `yas-configuration` chưa cài đúng namespace `preview-demo`.

---

# Bước 6 - Chạy thử lần 3: `core` với branch override

Mục tiêu: chứng minh đúng yêu cầu đề.

Trong Jenkins:

```text
ENV_NAME=demo
DEPLOY_MODE=core
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=dev_product_service
STOREFRONT_UI_BRANCH=dev_storefront_ui
CART_BRANCH=main
ORDER_BRANCH=main
CUSTOMER_BRANCH=main
INVENTORY_BRANCH=main
STOREFRONT_BFF_BRANCH=main
Các branch khác=main
```

Kỳ vọng console:

```text
tax=<sha của dev_tax_service>
product=<sha của dev_product_service>
storefront-ui=<sha của dev_storefront_ui>
cart=main
order=main
customer=main
inventory=main
storefront-bff=main
```

Kiểm tra image thực tế:

```bash
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'

kubectl get deployment product -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'

kubectl get deployment storefront-ui -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'

kubectl get deployment cart -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
tax            -> docker.io/doubleho/yas-tax:<sha>
product        -> docker.io/doubleho/yas-product:<sha>
storefront-ui  -> docker.io/doubleho/yas-storefront:<sha>
cart           -> docker.io/doubleho/yas-cart:main
```

Đây là evidence quan trọng nhất của chặng 4.

---

# Bước 7 - Test NodePort

Lấy Worker external IP:

```bash
kubectl get nodes -o wide
```

Hoặc từ GCP:

```bash
gcloud compute instances list
```

Test:

```bash
curl -I http://<WORKER_EXTERNAL_IP>:30080
```

Nếu dùng hosts file, thêm:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
<WORKER_EXTERNAL_IP> storefront.yas.local.com
<WORKER_EXTERNAL_IP> api.yas.local.com
<WORKER_EXTERNAL_IP> backoffice.yas.local.com
```

Test:

```bash
curl -I http://yas-preview.local:30080
```

Nếu curl fail:

```bash
kubectl get svc yas-preview-nodeport -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
kubectl get pods -n preview-demo
gcloud compute firewall-rules list --filter="name~yas"
```

Ý nghĩa:

- Có Service NodePort nhưng endpoints rỗng: pod phía sau chưa Ready hoặc selector sai.
- Có endpoints nhưng curl fail: firewall GCP hoặc Worker IP sai.
- Pod `storefront-ui` chưa Ready: xem log UI.

---

# Bước 8 - Khi test ổn, copy sang job chính

Khi `developer_build_v2_test` đã pass:

1. Mở `developer_build_v2_test`.
2. Copy toàn bộ Pipeline script.
3. Mở job chính `developer_build`.
4. Backup script cũ ra file evidence.
5. Paste script mới vào `developer_build`.
6. Save.
7. Chạy lại `developer_build` với cùng parameter.

Không xóa job test ngay. Giữ lại để rollback nếu cần.

---

# Bước 9 - Kiểm tra cleanup không xóa Postgres

Chạy Jenkins job:

```text
developer_cleanup
```

Parameter:

```text
ENV_NAME=demo
CONFIRM_DELETE=DELETE
```

Kiểm tra:

```bash
kubectl get ns preview-demo
kubectl get ns postgres
kubectl get pods -n postgres
```

Kỳ vọng:

```text
preview-demo bị xóa
postgres vẫn còn
```

Nếu cleanup xóa luôn `postgres`, sửa ngay. Cleanup chỉ được xóa preview namespace.

---

# Bước 10 - Evidence cần chụp

Chụp các ảnh/log sau:

1. Jenkins `developer_build` parameters:

```text
DEPLOY_MODE=core
TAX_BRANCH=dev_tax_service
PRODUCT_BRANCH=dev_product_service
STOREFRONT_UI_BRANCH=dev_storefront_ui
```

2. Jenkins console có:

```text
Resolved image tags
Deploying backend product image=...
Deploying backend tax image=...
Deploying UI storefront-ui image=...
```

3. Kubernetes image check:

```bash
kubectl get deployment tax -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment product -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment storefront-ui -n preview-demo -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

4. NodePort:

```bash
kubectl get svc -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
```

5. Cleanup:

```bash
kubectl get ns preview-demo
kubectl get ns postgres
```

---

# Cách giải thích với giảng viên

Nói ngắn gọn:

```text
developer_build tạo preview environment cho developer. Job nhận branch từng service. Service nào để main thì dùng image main ổn định. Service nào nhập branch riêng thì Jenkins resolve branch đó thành commit SHA và deploy đúng image SHA. Nhờ vậy developer có thể test service đang sửa trong toàn bộ core e-commerce flow thay vì chỉ test một pod riêng lẻ.
```

Nếu hỏi vì sao phải có image `main`:

```text
Vì preview cần chạy các service còn lại ở bản ổn định. Ví dụ developer sửa tax, thì tax dùng image commit SHA của branch, còn product/cart/order/customer/inventory/storefront dùng image main.
```

Nếu hỏi vì sao NodePort trỏ UI:

```text
Vì mục tiêu preview là để developer/giảng viên test e-commerce flow qua giao diện storefront. Khi debug backend riêng có thể tạm trỏ NodePort vào tax, nhưng final demo trỏ vào storefront-ui.
```

Nếu hỏi vì sao Postgres không bị cleanup:

```text
Postgres là dependency runtime dùng chung. Cleanup chỉ xóa preview namespace để dọn môi trường developer, không xóa dependency chung của cluster.
```

---

# Checklist pass chặng 4

| Hạng mục | Trạng thái |
|---|---|
| `developer_build_v2_test` tạo xong | yes/no |
| `tax-only` chạy được | yes/no |
| `core` với toàn bộ `main` deploy được | yes/no |
| `core` với branch override deploy được | yes/no |
| `tax` dùng SHA branch | yes/no |
| `product` dùng SHA branch | yes/no |
| `storefront-ui` dùng SHA branch | yes/no |
| service còn lại dùng `main` | yes/no |
| NodePort trỏ `storefront-ui` | yes/no |
| cleanup xóa `preview-demo` | yes/no |
| cleanup không xóa `postgres` | yes/no |
