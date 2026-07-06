# Jenkins Job Spec - `developer_build` và cleanup

## Mục tiêu

`developer_build` cho developer chọn branch/tag từng service để deploy preview. Service không nhập branch riêng dùng image tag `main`.

Ví dụ:

- `TAX_BRANCH=dev_tax_service`
- Các service khác `main`
- Kết quả:
  - `tax`: `docker.io/<dockerhub-user>/yas-tax:<commit-sha-cua-dev_tax_service>`
  - service khác: `docker.io/<dockerhub-user>/yas-<service>:main`

## Parameters

| Parameter | Default | Ghi chú |
|---|---|---|
| `ENV_NAME` | `demo` | Namespace là `preview-<ENV_NAME>` |
| `PRODUCT_BRANCH` | `main` | |
| `CART_BRANCH` | `main` | |
| `ORDER_BRANCH` | `main` | |
| `CUSTOMER_BRANCH` | `main` | |
| `INVENTORY_BRANCH` | `main` | |
| `TAX_BRANCH` | `main` | |
| `MEDIA_BRANCH` | `main` | |
| `SEARCH_BRANCH` | `main` | |
| `STOREFRONT_BFF_BRANCH` | `main` | |
| `STOREFRONT_UI_BRANCH` | `main` | folder repo là `storefront` |
| `BACKOFFICE_BFF_BRANCH` | `main` | |
| `BACKOFFICE_UI_BRANCH` | `main` | folder repo là `backoffice` |
| `SWAGGER_UI_BRANCH` | `main` | thường dùng image public/default |

## Quy tắc resolve tag

```bash
if [ "$BRANCH" = "main" ]; then
  TAG="main"
else
  git fetch origin "$BRANCH"
  TAG="$(git rev-parse --short=12 "origin/$BRANCH")"
fi
```

Điều kiện quan trọng: CI branch tương ứng phải chạy trước và đã push image tag commit SHA lên Docker Hub.

## Helm deploy sketch

```bash
export KUBECONFIG="$KUBECONFIG_FILE"
NAMESPACE="preview-${ENV_NAME}"
DOCKER_USER="<dockerhub-user>"

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

deploy_backend() {
  service="$1"
  chart="$2"
  tag="$3"

  helm dependency build "k8s/charts/$chart"
  helm upgrade --install "$service" "k8s/charts/$chart" \
    -n "$NAMESPACE" --create-namespace \
    --set backend.image.repository="docker.io/$DOCKER_USER/yas-$service" \
    --set backend.image.tag="$tag"
}

deploy_ui() {
  service="$1"
  chart="$2"
  tag="$3"
  image="$4"

  helm dependency build "k8s/charts/$chart"
  helm upgrade --install "$service" "k8s/charts/$chart" \
    -n "$NAMESPACE" --create-namespace \
    --set ui.image.repository="docker.io/$DOCKER_USER/$image" \
    --set ui.image.tag="$tag"
}

deploy_backend tax tax "$TAX_TAG"
deploy_backend product product "$PRODUCT_TAG"
deploy_backend cart cart "$CART_TAG"
deploy_backend order order "$ORDER_TAG"
deploy_backend customer customer "$CUSTOMER_TAG"
deploy_backend inventory inventory "$INVENTORY_TAG"
deploy_backend media media "$MEDIA_TAG"
deploy_backend search search "$SEARCH_TAG"
deploy_backend storefront-bff storefront-bff "$STOREFRONT_BFF_TAG"
deploy_backend backoffice-bff backoffice-bff "$BACKOFFICE_BFF_TAG"
deploy_ui storefront-ui storefront-ui "$STOREFRONT_UI_TAG" "yas-storefront"
deploy_ui backoffice-ui backoffice-ui "$BACKOFFICE_UI_TAG" "yas-backoffice"
```

## NodePort expose

Nếu có ingress controller, có thể expose ingress qua NodePort. Cách demo nhanh là tạo Service NodePort trỏ tới UI/gateway phù hợp.

Ví dụ service NodePort cho `storefront-ui`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: yas-preview-nodeport
  namespace: preview-demo
spec:
  type: NodePort
  selector:
    app.kubernetes.io/name: storefront-ui
  ports:
    - name: http
      port: 80
      targetPort: 3000
      nodePort: 30080
```

Apply bằng Jenkins:

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
    app.kubernetes.io/name: storefront-ui
  ports:
    - name: http
      port: 80
      targetPort: 3000
      nodePort: 30080
EOF
```

Nếu selector chart khác, kiểm tra bằng:

```bash
kubectl get pod -n "$NAMESPACE" --show-labels
```

## Cleanup job

Tên job: `developer_cleanup`

Parameters:

| Parameter | Default | Ghi chú |
|---|---|---|
| `ENV_NAME` | `demo` | Xóa namespace `preview-<ENV_NAME>` |
| `CONFIRM_DELETE` | | Phải nhập `DELETE` |

Pipeline shell:

```bash
test "$CONFIRM_DELETE" = "DELETE"
export KUBECONFIG="$KUBECONFIG_FILE"
kubectl delete namespace "preview-$ENV_NAME" --ignore-not-found=true
```

## Evidence cần chụp

- Màn hình parameters của `developer_build`.
- Console log resolve branch -> commit SHA.
- Console log `helm upgrade --install`.
- `kubectl get pods -n preview-<env>`.
- Image của deployment service sửa.
- URL NodePort chạy được.
- Cleanup job và namespace biến mất.
