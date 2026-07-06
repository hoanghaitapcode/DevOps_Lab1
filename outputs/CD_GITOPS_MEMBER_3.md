# Thành viên 3 - Jenkins CD & GitOps Lead

File này là checklist làm việc chính của bạn. Mục tiêu của bạn là tạo job `developer_build` và `developer_cleanup` trên Jenkins cũ để deploy preview lên K3s.

## Kết quả phải bàn giao

Cuối đồ án, bạn cần chứng minh:

- Có Jenkins job `developer_build`.
- Job nhận parameter branch/tag từng service.
- Service để `main` thì deploy image tag `main`.
- Service nhập branch riêng thì deploy image tag commit SHA cuối branch đó.
- Preview chạy trong namespace riêng, ví dụ `preview-demo`.
- Developer truy cập qua NodePort/domain local:

```text
http://yas-preview.local:30080
```

- Có Jenkins job `developer_cleanup` xóa preview.
- Nếu kịp nâng cao: Argo CD deploy `dev` và `staging`.

## Nguyên tắc làm

- Jenkins cũ là trung tâm CD.
- Không tạo GitHub Actions mới cho deploy.
- Helm chart hiện có trong `k8s/charts/*` là nền chính.
- Không hardcode kubeconfig/token vào repo.
- Không deploy full 14 service nếu cluster không chịu nổi; ưu tiên flow demo tối thiểu.

## Ngày 1 - Hiểu chart và cách override image

Repo hiện có chart theo service:

```text
k8s/charts/product
k8s/charts/cart
k8s/charts/order
k8s/charts/customer
k8s/charts/inventory
k8s/charts/tax
k8s/charts/media
k8s/charts/search
k8s/charts/storefront-bff
k8s/charts/storefront-ui
k8s/charts/backoffice-bff
k8s/charts/backoffice-ui
k8s/charts/swagger-ui
```

Backend chart override kiểu:

```bash
helm upgrade --install tax k8s/charts/tax \
  -n preview-demo --create-namespace \
  --set backend.image.repository=docker.io/<dockerhub-user>/yas-tax \
  --set backend.image.tag=<tag>
```

UI chart override kiểu:

```bash
helm upgrade --install storefront-ui k8s/charts/storefront-ui \
  -n preview-demo --create-namespace \
  --set ui.image.repository=docker.io/<dockerhub-user>/yas-storefront \
  --set ui.image.tag=<tag>
```

Done ngày 1 khi:

- Bạn deploy thử được 1 service bằng Helm vào namespace test.
- Bạn biết backend dùng `backend.image.*`, UI dùng `ui.image.*`.

## Ngày 2 - Tạo job `developer_build`

### 1. Parameters job

Tạo Jenkins Pipeline job tên:

```text
developer_build
```

Parameters:

```text
ENV_NAME=demo
PRODUCT_BRANCH=main
CART_BRANCH=main
ORDER_BRANCH=main
CUSTOMER_BRANCH=main
INVENTORY_BRANCH=main
TAX_BRANCH=main
MEDIA_BRANCH=main
SEARCH_BRANCH=main
STOREFRONT_BFF_BRANCH=main
STOREFRONT_UI_BRANCH=main
BACKOFFICE_BFF_BRANCH=main
BACKOFFICE_UI_BRANCH=main
SWAGGER_UI_BRANCH=main
```

### 2. Resolve branch thành tag

Logic bắt buộc:

```bash
resolve_tag() {
  branch="$1"
  if [ "$branch" = "main" ]; then
    echo "main"
  else
    git fetch origin "$branch"
    git rev-parse --short=12 "origin/$branch"
  fi
}
```

Điều kiện: CI của thành viên 2 phải đã push image tag commit SHA đó.

### 3. Jenkins shell skeleton

Trong Jenkins job, dùng kubeconfig credential dạng secret file, ví dụ `kubeconfig-yas-k3s`.

```bash
set -euo pipefail

export KUBECONFIG="$KUBECONFIG_FILE"
DOCKER_USER="<dockerhub-user>"
NAMESPACE="preview-${ENV_NAME}"

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

resolve_tag() {
  branch="$1"
  if [ "$branch" = "main" ]; then
    echo "main"
  else
    git fetch origin "$branch"
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
```

### 4. Deploy functions

```bash
deploy_backend() {
  release="$1"
  chart="$2"
  image="$3"
  tag="$4"

  helm dependency build "k8s/charts/$chart"
  helm upgrade --install "$release" "k8s/charts/$chart" \
    -n "$NAMESPACE" --create-namespace \
    --set backend.image.repository="docker.io/$DOCKER_USER/$image" \
    --set backend.image.tag="$tag"
}

deploy_ui() {
  release="$1"
  chart="$2"
  image="$3"
  tag="$4"

  helm dependency build "k8s/charts/$chart"
  helm upgrade --install "$release" "k8s/charts/$chart" \
    -n "$NAMESPACE" --create-namespace \
    --set ui.image.repository="docker.io/$DOCKER_USER/$image" \
    --set ui.image.tag="$tag"
}
```

### 5. Deploy service tối thiểu trước

Để chắc điểm, deploy flow tối thiểu trước:

```bash
deploy_backend product product yas-product "$PRODUCT_TAG"
deploy_backend cart cart yas-cart "$CART_TAG"
deploy_backend order order yas-order "$ORDER_TAG"
deploy_backend customer customer yas-customer "$CUSTOMER_TAG"
deploy_backend inventory inventory yas-inventory "$INVENTORY_TAG"
deploy_backend tax tax yas-tax "$TAX_TAG"
deploy_backend storefront-bff storefront-bff yas-storefront-bff "$STOREFRONT_BFF_TAG"
deploy_ui storefront-ui storefront-ui yas-storefront "$STOREFRONT_UI_TAG"
```

Nếu còn tài nguyên:

```bash
deploy_backend media media yas-media "$MEDIA_TAG"
deploy_backend search search yas-search "$SEARCH_TAG"
deploy_backend backoffice-bff backoffice-bff yas-backoffice-bff "$BACKOFFICE_BFF_TAG"
deploy_ui backoffice-ui backoffice-ui yas-backoffice "$BACKOFFICE_UI_TAG"
```

Done ngày 2 khi:

- Job `developer_build` chạy tới bước Helm.
- Namespace `preview-demo` được tạo.
- Có ít nhất 1 service deploy được.

## Ngày 3 - NodePort và cleanup

### 1. Expose NodePort

Tạo service NodePort. Selector có thể cần chỉnh theo label thực tế.

Kiểm tra label:

```bash
kubectl get pods -n "$NAMESPACE" --show-labels
```

Apply NodePort:

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

Nếu endpoint rỗng:

```bash
kubectl get endpoints yas-preview-nodeport -n "$NAMESPACE"
kubectl get pods -n "$NAMESPACE" --show-labels
```

Sửa selector theo label thật của pod.

### 2. Test image tag đúng

Ví dụ với tax:

```bash
kubectl get deployment tax -n preview-demo \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

Kỳ vọng:

```text
docker.io/<dockerhub-user>/yas-tax:<commit-sha>
```

### 3. Cleanup job

Tạo Jenkins Pipeline job:

```text
developer_cleanup
```

Parameters:

```text
ENV_NAME=demo
CONFIRM_DELETE=DELETE
```

Shell:

```bash
set -euo pipefail
test "$CONFIRM_DELETE" = "DELETE"
export KUBECONFIG="$KUBECONFIG_FILE"
kubectl delete namespace "preview-$ENV_NAME" --ignore-not-found=true
```

Done ngày 3 khi:

- `developer_build` deploy được preview.
- `http://yas-preview.local:30080` truy cập được.
- `developer_cleanup` xóa được namespace.

## Ngày 4 - Argo CD nâng cao

Chỉ làm nếu bắt buộc đã xong.

Flow cần báo cáo:

- Jenkins vẫn build/test/scan/push image.
- Argo CD chỉ sync manifest/Helm values.
- `main` deploy `dev`.
- Git tag `v1.2.3` deploy `staging`.

Gợi ý app:

```text
argocd app yas-dev      -> namespace dev
argocd app yas-staging  -> namespace staging
```

Nếu không kịp staging, giữ `dev` Healthy/Synced cũng có điểm nâng cao.

## Ngày 5 - Evidence của bạn

Chụp:

- Jenkins job `developer_build` parameters.
- Console log resolve branch -> commit SHA.
- Console log `helm upgrade --install`.
- `kubectl get all -n preview-demo`.
- `kubectl get svc -n preview-demo`.
- Deployment image tag commit SHA.
- Browser/curl NodePort.
- Jenkins job `developer_cleanup`.
- Namespace đã xóa.
- Argo CD Healthy/Synced nếu có.

## Debug nhanh

Helm dependency lỗi:

```bash
helm dependency build k8s/charts/tax
helm template tax k8s/charts/tax
```

Pod không Ready:

```bash
kubectl describe pod <pod> -n preview-demo
kubectl logs <pod> -n preview-demo --tail=100
```

ImagePullBackOff:

```bash
kubectl describe pod <pod> -n preview-demo
docker pull docker.io/<dockerhub-user>/yas-tax:<tag>
```

NodePort không vào được:

```bash
kubectl get svc yas-preview-nodeport -n preview-demo
kubectl get endpoints yas-preview-nodeport -n preview-demo
```

## Câu nói trong demo

> Job `developer_build` cho developer chọn branch từng service. Service nào để `main` sẽ dùng image tag `main`; service nào nhập branch riêng sẽ resolve commit SHA cuối branch và deploy đúng image đó. Vì vậy developer có preview riêng mà không cần build/deploy toàn bộ hệ thống.
