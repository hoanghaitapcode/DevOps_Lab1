# Thành viên 1 - Infrastructure Lead

File này là checklist làm việc chính của bạn. Mục tiêu của bạn là dựng được nền hạ tầng để Jenkins cũ deploy YAS lên Kubernetes trên Google Cloud.

## Kết quả phải bàn giao

Cuối đồ án, bạn cần có:

- Google Cloud có 2 VM:
  - `yas-master`: K3s server/control-plane.
  - `yas-worker`: K3s worker chạy workload YAS.
- Cluster có đúng mô hình tối thiểu:

```bash
kubectl get nodes -o wide
```

Kỳ vọng thấy 2 node Ready.

- Jenkins cũ kết nối được cluster bằng kubeconfig hoặc service account.
- Firewall mở đủ cho demo:
  - SSH.
  - K3s API `6443` cho Jenkins/admin.
  - NodePort demo, ví dụ `30080`.
- Có Worker external IP để nhóm thêm hosts:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

- Screenshot/log không lộ secret.

## Nguyên tắc làm

- Không cài Jenkins mới trên `yas-master` nếu Jenkins Project 1 còn dùng được.
- Google Cloud chỉ chạy K3s và workload YAS.
- Jenkins cũ chỉ điều phối build/deploy từ bên ngoài.
- Không commit kubeconfig, token, password vào repo.
- Nếu thiếu tài nguyên, ưu tiên chạy scope demo tối thiểu, không cố full observability.

## Ngày 1 - Tạo VM và dựng K3s

### 1. Tạo VM

Gợi ý cấu hình:

| VM | Vai trò | Cấu hình nên dùng |
|---|---|---|
| `yas-master` | control-plane | 2 vCPU, 4-8GB RAM |
| `yas-worker` | workload | 4 vCPU, 8-16GB RAM nếu đủ ngân sách |

Ghi lại:

```text
MASTER_PRIVATE_IP=
MASTER_EXTERNAL_IP=
WORKER_PRIVATE_IP=
WORKER_EXTERNAL_IP=
ZONE=
```

### 2. Firewall tối thiểu

Mở:

- `tcp/22`: SSH, nên giới hạn IP nhóm.
- `tcp/6443`: Jenkins/admin gọi Kubernetes API.
- Internal traffic giữa 2 VM trong cùng VPC.
- `tcp/30080`: NodePort preview.

Không mở toàn bộ `30000-32767` nếu chỉ demo 1 NodePort.

### 3. Cài K3s server trên `yas-master`

SSH vào `yas-master`:

```bash
curl -sfL https://get.k3s.io | sh -s - server \
  --write-kubeconfig-mode 644 \
  --node-name yas-master
```

Kiểm tra:

```bash
sudo systemctl status k3s
kubectl get nodes -o wide
```

Lấy token join worker:

```bash
sudo cat /var/lib/rancher/k3s/server/node-token
```

### 4. Join `yas-worker`

SSH vào `yas-worker`:

```bash
export K3S_URL=https://<MASTER_PRIVATE_IP>:6443
export K3S_TOKEN=<NODE_TOKEN_FROM_MASTER>

curl -sfL https://get.k3s.io | sh -s - agent \
  --server "$K3S_URL" \
  --token "$K3S_TOKEN" \
  --node-name yas-worker
```

Quay lại `yas-master`:

```bash
kubectl get nodes -o wide
```

Done ngày 1 khi:

- 2 node Ready.
- Có screenshot `kubectl get nodes -o wide`.
- Có ghi lại external IP worker.

## Ngày 2 - Cho Jenkins cũ truy cập cluster

### 1. Chuẩn bị kubeconfig

Trên `yas-master`:

```bash
sudo cat /etc/rancher/k3s/k3s.yaml
```

Copy nội dung ra file tạm trên máy quản trị, đổi:

```yaml
server: https://127.0.0.1:6443
```

thành:

```yaml
server: https://<MASTER_EXTERNAL_IP_OR_PRIVATE_IP_JENKINS_ACCESSIBLE>:6443
```

Không commit file này vào repo.

### 2. Lưu kubeconfig vào Jenkins

Trong Jenkins cũ:

- Manage Jenkins.
- Credentials.
- Add Credentials.
- Kind: Secret file.
- ID gợi ý: `kubeconfig-yas-k3s`.
- Upload kubeconfig.

### 3. Test từ Jenkins

Nhờ thành viên 2 hoặc 3 tạo job test ngắn:

```bash
export KUBECONFIG="$KUBECONFIG_FILE"
kubectl get nodes -o wide
kubectl auth can-i get pods --all-namespaces
```

Nếu dùng service account riêng:

```bash
kubectl create namespace jenkins-cd
kubectl create serviceaccount jenkins-deployer -n jenkins-cd
kubectl create clusterrolebinding jenkins-deployer-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=jenkins-cd:jenkins-deployer
```

Ghi chú báo cáo: quyền cluster-admin chỉ dùng cho demo đồ án; production cần least privilege.

Done ngày 2 khi:

- Jenkins chạy được `kubectl get nodes`.
- Có screenshot Jenkins log, nhưng che kubeconfig/token.

## Ngày 3 - Hỗ trợ preview NodePort

### 1. Kiểm tra namespace preview

Khi thành viên 3 deploy:

```bash
kubectl get ns | grep preview
kubectl get all -n preview-demo
```

### 2. Kiểm tra NodePort

```bash
kubectl get svc -n preview-demo
```

Nếu NodePort là `30080`, trên máy demo thêm hosts:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

Test:

```bash
curl -I http://yas-preview.local:30080
```

Nếu curl không được:

- Kiểm tra firewall GCP mở `tcp/30080`.
- Kiểm tra service có đúng NodePort.
- Kiểm tra pod Ready.

```bash
kubectl get pods -n preview-demo -o wide
kubectl describe svc yas-preview-nodeport -n preview-demo
kubectl describe pod <pod-name> -n preview-demo
```

Done ngày 3 khi:

- Preview truy cập được từ ngoài qua NodePort.
- Có screenshot URL hoặc curl.

## Ngày 4 - Argo CD/Istio nếu kịp

Chỉ làm nếu phần bắt buộc đã chạy.

### Argo CD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd
```

Port-forward để thành viên 3 cấu hình:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Lấy password admin:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath='{.data.password}' | base64 --decode
```

Không chụp lộ password.

### Istio/Kiali

Chỉ cài nếu cluster còn RAM/CPU. Nếu pod bắt đầu Pending nhiều, dừng nâng cao và giữ demo bắt buộc ổn định.

Done ngày 4 khi:

- Nếu làm Argo: `argocd` pods Running.
- Nếu làm Istio: `istio-system` pods Running.

## Ngày 5 - Evidence của bạn

Chuẩn bị các ảnh/log:

- GCP VM list có `yas-master`, `yas-worker`.
- Firewall rules liên quan SSH, 6443, 30080.
- `kubectl get nodes -o wide`.
- `kubectl get pods -A`.
- Jenkins log chạy được `kubectl get nodes`.
- Preview NodePort service.
- URL/curl preview.

## Debug nhanh

Node chưa Ready:

```bash
kubectl describe node yas-worker
sudo systemctl status k3s-agent
sudo journalctl -u k3s-agent -n 100 --no-pager
```

Pod Pending:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl top nodes
kubectl top pods -A
```

ImagePullBackOff:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl get secret -n <namespace>
```

Không truy cập NodePort:

```bash
kubectl get svc -n preview-demo
kubectl get endpoints -n preview-demo
kubectl get pods -n preview-demo --show-labels
```

## Câu nói trong demo

> Nhóm giữ Jenkins cũ từ Project 1 làm CI/CD. Google Cloud chỉ dùng để chạy K3s cluster và workload YAS. Jenkins kết nối vào cluster bằng kubeconfig/service account, không chạy full YAS stack trên Jenkins.
