# GCP K3s Runbook

## Mục tiêu

Dựng Kubernetes cluster theo yêu cầu Project 2:

- 1 master/control-plane: `yas-master`.
- 1 worker: `yas-worker`.
- Ưu tiên K3s trên Google Cloud Compute Engine.
- Jenkins cũ kết nối bằng kubeconfig/service account.

## VM đề xuất

| VM | Vai trò | Gợi ý cấu hình |
|---|---|---|
| `yas-master` | K3s server/control-plane, Argo CD/Istio control plane nếu làm nâng cao | 2 vCPU, 4-8GB RAM |
| `yas-worker` | Workload YAS | 4 vCPU, 8-16GB RAM nếu ngân sách cho phép |

Nếu tài nguyên yếu, giảm scope service demo trước, không cố chạy full observability.

## Firewall tối thiểu

- SSH `tcp/22`: chỉ IP nhóm.
- K3s API `tcp/6443`: Jenkins IP và IP quản trị.
- K3s node internal: cho phép private network giữa `yas-master` và `yas-worker`.
- NodePort demo `tcp/30080`: IP nhóm/giảng viên.

Không mở toàn bộ internet nếu không cần.

## Cài K3s master

Trên `yas-master`:

```bash
curl -sfL https://get.k3s.io | sh -s - server \
  --write-kubeconfig-mode 644 \
  --node-name yas-master

sudo systemctl status k3s
sudo cat /var/lib/rancher/k3s/server/node-token
kubectl get nodes -o wide
```

Lấy token để join worker:

```bash
sudo cat /var/lib/rancher/k3s/server/node-token
```

## Join worker

Trên `yas-worker`:

```bash
export K3S_URL=https://<MASTER_PRIVATE_IP>:6443
export K3S_TOKEN=<NODE_TOKEN_FROM_MASTER>

curl -sfL https://get.k3s.io | sh -s - agent \
  --server "$K3S_URL" \
  --token "$K3S_TOKEN" \
  --node-name yas-worker

sudo systemctl status k3s-agent
```

Kiểm tra trên master:

```bash
kubectl get nodes -o wide
```

## Kubeconfig cho Jenkins

Trên master:

```bash
sudo cat /etc/rancher/k3s/k3s.yaml
```

Đổi server từ `https://127.0.0.1:6443` thành:

```text
https://<MASTER_EXTERNAL_OR_PRIVATE_IP_ACCESSIBLE_FROM_JENKINS>:6443
```

Lưu kubeconfig này vào Jenkins Credentials dạng Secret file, ví dụ `kubeconfig-yas-k3s`.

Test trong Jenkins:

```bash
export KUBECONFIG="$KUBECONFIG_FILE"
kubectl get nodes -o wide
kubectl auth can-i get pods --all-namespaces
```

## Service account tối thiểu cho Jenkins

Nếu không dùng admin kubeconfig, tạo service account riêng:

```bash
kubectl create namespace jenkins-cd
kubectl create serviceaccount jenkins-deployer -n jenkins-cd
kubectl create clusterrolebinding jenkins-deployer-admin \
  --clusterrole=cluster-admin \
  --serviceaccount=jenkins-cd:jenkins-deployer
```

Với đồ án 5 ngày, có thể dùng cluster-admin để giảm rủi ro demo, nhưng báo cáo nên ghi đây là demo permission và production cần least privilege.

## Deploy dependency/YAS

Repo có script gốc:

```bash
cd k8s/deploy
./setup-keycloak.sh
./setup-redis.sh
./setup-cluster.sh
./deploy-yas-configuration.sh
./deploy-yas-applications.sh
```

Lưu ý script gốc viết cho local/minikube và cần nhiều tài nguyên. Nếu K3s yếu, triển khai tối thiểu các dependency/service cần demo trước.

## Hosts file cho preview

Trên máy developer/giảng viên:

```text
<WORKER_EXTERNAL_IP> yas-preview.local
```

Truy cập:

```text
http://yas-preview.local:30080
```

## Kiểm tra nhanh

```bash
kubectl get nodes -o wide
kubectl get pods -A
kubectl get svc -A
kubectl get events -A --sort-by=.lastTimestamp | tail -50
```

Nếu pod Pending:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl top nodes
kubectl top pods -A
```

Nếu ImagePullBackOff:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl get secret -n <namespace>
docker pull docker.io/<dockerhub-user>/<image>:<tag>
```
