# Ngày 1 - Runbook chi tiết cho cả 4 thành viên

Mục tiêu ngày 1: khóa nền cho Project 2. Hết ngày 1 nhóm phải có:

- Biết chắc Jenkins cũ Project 1 còn dùng được hay không.
- Có GCP VM `yas-master` và `yas-worker` hoặc ít nhất đã tạo xong VM.
- K3s bắt đầu chạy, lý tưởng là 2 node Ready.
- Jenkinsfile được audit/sửa các lỗi nhỏ an toàn.
- Biết cách override Helm image tag cho service.
- Có checklist bằng chứng/screenshot để không sót khi nộp.

Nguyên tắc chung:

- Không cài Jenkins mới nếu Jenkins cũ còn truy cập được.
- Không dùng GitHub Actions cho phần CD mới.
- Không commit secret/token/password.
- Không build full 14 service song song.
- Ai làm xong bước nào thì chụp ảnh/log ngay, đừng để cuối ngày mới gom.

## Lịch làm đề xuất trong ngày

| Khung giờ | Cả nhóm cần đạt |
|---|---|
| 0h-0h30 | Chốt thông tin chung: repo, Jenkins URL, Docker Hub user, GCP project, tên VM |
| 0h30-2h30 | TV1 dựng VM/K3s, TV2 audit Jenkinsfile, TV3 audit Helm, TV4 tạo checklist evidence |
| 2h30-4h00 | TV1 join worker, TV2 sửa Jenkinsfile nháp, TV3 deploy thử Helm template, TV4 gom ảnh Project 1 |
| 4h00-5h00 | Sync nhóm: Jenkins còn dùng được chưa, K3s Ready chưa, ngày 2 cần ai làm gì |

## Thông tin chung cần điền trước khi làm

Tạo một tin nhắn nhóm hoặc note chung, điền các dòng này:

```text
GitHub repo:
Jenkins URL:
Jenkins admin/member phụ trách:
Docker Hub username:
GCP project id:
GCP zone:
yas-master external IP:
yas-master private IP:
yas-worker external IP:
yas-worker private IP:
NodePort demo dự kiến: 30080
Preview domain local: yas-preview.local
Postgres runtime namespace: postgres
Postgres service DNS: postgresql.postgres
```

Nếu dòng nào chưa có, để `TBD`. Đừng chặn cả nhóm vì thiếu một dòng.

---

# Cài đặt công cụ trước khi chia việc

Phần này làm trước, hoặc ai thiếu tool nào thì cài tool đó. Nếu một thành viên không cài được vì máy yếu/thiếu quyền admin, người đó vẫn làm phần checklist/report, còn người có tool chạy lệnh giúp.

## Kiểm tra máy đang có gì

Mỗi người chạy:

```bash
git --version
docker --version
kubectl version --client
helm version
gcloud version
java -version
mvn -version
```

Tool nào báo `command not found` thì cài theo phần dưới.

## Cài Git

macOS:

```bash
xcode-select --install
git --version
```

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y git
git --version
```

Windows:

- Cài Git for Windows: `https://git-scm.com/download/win`
- Mở Git Bash.
- Chạy:

```bash
git --version
```

## Cài Google Cloud CLI

TV1 bắt buộc cần `gcloud`. Các bạn khác không bắt buộc, nhưng có thì tốt.

macOS dùng Homebrew:

```bash
brew install --cask google-cloud-sdk
gcloud version
gcloud init
```

Nếu chưa có Homebrew:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates gnupg curl
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list
sudo apt-get update
sudo apt-get install -y google-cloud-cli
gcloud version
gcloud init
```

Windows:

- Tải installer: `https://cloud.google.com/sdk/docs/install`
- Chọn Windows installer.
- Mở Google Cloud SDK Shell.
- Chạy:

```bash
gcloud version
gcloud init
```

Đăng nhập và chọn project:

```bash
gcloud auth login
gcloud config set project <GCP_PROJECT_ID>
gcloud config set compute/zone <ZONE>
```

Ví dụ:

```bash
gcloud config set compute/zone asia-southeast1-b
```

## Cài kubectl

macOS:

```bash
brew install kubectl
kubectl version --client
```

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.30/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.30/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubectl
kubectl version --client
```

Windows:

- Nếu có Chocolatey:

```powershell
choco install kubernetes-cli
kubectl version --client
```

- Hoặc tải binary theo hướng dẫn Kubernetes rồi thêm vào PATH.

## Cài Helm

TV3 bắt buộc cần Helm. TV1 cũng nên có.

macOS:

```bash
brew install helm
helm version
```

Ubuntu:

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

Windows:

```powershell
choco install kubernetes-helm
helm version
```

Nếu không có Chocolatey, tải Helm từ `https://github.com/helm/helm/releases`, giải nén và thêm `helm.exe` vào PATH.

## Cài Docker

TV2 cần Docker trên Jenkins agent là chính. Máy cá nhân có Docker để test image càng tốt.

macOS/Windows:

- Cài Docker Desktop: `https://www.docker.com/products/docker-desktop/`
- Mở Docker Desktop.
- Kiểm tra:

```bash
docker version
docker run hello-world
```

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo docker run hello-world
```

Cho user hiện tại chạy Docker không cần `sudo`:

```bash
sudo usermod -aG docker $USER
```

Đăng xuất/đăng nhập lại rồi kiểm tra:

```bash
docker ps
```

## Cài Java JDK25 và Maven

TV2 cần biết Jenkins dùng Java/Maven gì. Máy cá nhân chỉ cần khi muốn test local.

macOS:

```bash
brew install maven
brew install --cask temurin@25
java -version
mvn -version
```

Nếu máy chưa nhận Java sau khi cài, mở terminal mới rồi kiểm tra lại:

```bash
java -version
```

Ubuntu:

Ubuntu repo mặc định có thể chưa có OpenJDK 25. Cách dễ nhất là dùng SDKMAN:

```bash
sudo apt-get update
sudo apt-get install -y zip unzip curl maven
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem
java -version
mvn -version
```

Windows:

- Cài Temurin JDK 25: `https://adoptium.net/`
- Cài Maven: `https://maven.apache.org/download.cgi`
- Thêm `JAVA_HOME` và Maven `bin` vào PATH.
- Kiểm tra:

```powershell
java -version
mvn -version
```

## Cài yq nếu cần sửa YAML nhanh

Không bắt buộc ngày 1, nhưng hữu ích cho TV3.

macOS:

```bash
brew install yq
yq --version
```

Ubuntu:

```bash
sudo wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/local/bin/yq
sudo chmod +x /usr/local/bin/yq
yq --version
```

## Clone repo nếu máy chưa có

```bash
git clone https://github.com/hoanghaitapcode/DevOps_Lab1.git
cd DevOps_Lab1
git status
```

Nếu đã có repo:

```bash
cd /path/to/DevOps_Lab1
git status
git remote -v
```

Không chạy `git reset --hard` nếu trong repo đang có file người khác sửa.

## Đăng nhập Docker Hub trên máy cá nhân nếu cần test

```bash
docker login
```

Nhập Docker Hub username và access token/password. Không lưu token vào file trong repo.

## Cài tool trên Jenkins agent

TV2 cần kiểm tra Jenkins agent, không phải chỉ máy cá nhân.

Trong Jenkins tạo job test hoặc dùng node shell, chạy:

```bash
java -version
mvn -version
git --version
docker version
kubectl version --client
helm version
```

Nếu Jenkins agent thiếu Docker:

- Cài Docker trên máy Jenkins agent nếu có quyền.
- Hoặc dùng một Jenkins agent/node khác có Docker.
- Không chuyển sang GitHub Actions để né lỗi này.

Nếu Jenkins agent thiếu `kubectl`/`helm`, ngày 1 chỉ ghi blocker. Ngày 2 cài thêm trên Jenkins agent hoặc dùng container/tool installation.

---

# Thành viên 1 - Infrastructure Lead

Nhiệm vụ ngày 1: tạo VM Google Cloud và dựng K3s 1 master + 1 worker.

## Bước 1. Xác nhận GCP project

Trên máy có `gcloud`, chạy:

```bash
gcloud auth list
gcloud config list project
```

Nếu chưa đúng project:

```bash
gcloud config set project <GCP_PROJECT_ID>
```

Kiểm tra zone:

```bash
gcloud config set compute/zone <ZONE>
gcloud config list compute/zone
```

Gợi ý zone dùng một nơi cố định, ví dụ:

```bash
gcloud config set compute/zone asia-southeast1-b
```

Kết quả cần chụp:

- Terminal hiển thị đúng project id.

## Bước 2. Tạo VM `yas-master`

Nếu làm bằng Google Cloud Console:

1. Vào Compute Engine.
2. VM instances.
3. Create instance.
4. Name: `yas-master`.
5. Region/Zone: zone nhóm chọn.
6. Machine type: tối thiểu 2 vCPU, 4GB RAM; nếu đủ tiền chọn 2 vCPU, 8GB RAM.
7. Boot disk: Ubuntu 22.04 LTS.
8. Disk size: tối thiểu 30GB.
9. Network tags: `yas-k3s`.
10. Create.

Nếu làm bằng CLI:

```bash
gcloud compute instances create yas-master \
  --machine-type=e2-standard-2 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=30GB \
  --tags=yas-k3s
```

Kết quả cần chụp:

- VM `yas-master` trong GCP Console.

## Bước 3. Tạo VM `yas-worker`

Nếu làm bằng Console:

1. Create instance.
2. Name: `yas-worker`.
3. Machine type: tốt nhất 4 vCPU, 8-16GB RAM; nếu tiết kiệm dùng 2 vCPU, 8GB RAM.
4. Boot disk: Ubuntu 22.04 LTS.
5. Disk size: tối thiểu 40GB nếu có thể.
6. Network tags: `yas-k3s`.
7. Create.

Nếu làm bằng CLI:

```bash
gcloud compute instances create yas-worker \
  --machine-type=e2-standard-4 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=40GB \
  --tags=yas-k3s
```

Nếu quota/tiền không đủ `e2-standard-4`, dùng:

```bash
gcloud compute instances create yas-worker \
  --machine-type=e2-standard-2 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=40GB \
  --tags=yas-k3s
```

Kết quả cần chụp:

- VM `yas-worker` trong GCP Console.

## Bước 4. Mở firewall cho SSH, K3s API, NodePort

Mở SSH nếu chưa có:

```bash
gcloud compute firewall-rules create yas-allow-ssh \
  --allow tcp:22 \
  --source-ranges <YOUR_PUBLIC_IP>/32 \
  --target-tags yas-k3s
```

Nếu không biết public IP, vào trình duyệt tìm “what is my ip”. Nếu đang gấp demo trong mạng thay đổi liên tục, có thể tạm dùng `0.0.0.0/0`, nhưng báo cáo nên nói demo-only.

Mở K3s API cho Jenkins/admin:

```bash
gcloud compute firewall-rules create yas-allow-k3s-api \
  --allow tcp:6443 \
  --source-ranges <JENKINS_PUBLIC_IP_OR_YOUR_PUBLIC_IP>/32 \
  --target-tags yas-k3s
```

Mở NodePort demo:

```bash
gcloud compute firewall-rules create yas-allow-preview-nodeport \
  --allow tcp:30080 \
  --source-ranges <YOUR_PUBLIC_IP>/32 \
  --target-tags yas-k3s
```

Mở internal traffic trong VPC:

```bash
gcloud compute firewall-rules create yas-allow-internal \
  --allow tcp,udp,icmp \
  --source-ranges 10.0.0.0/8 \
  --target-tags yas-k3s
```

Nếu firewall rule đã tồn tại và báo lỗi, bỏ qua, chụp rule đang có.

Kết quả cần chụp:

- Firewall rules có `tcp:6443` và `tcp:30080`.

## Bước 5. Lấy IP của VM

```bash
gcloud compute instances list
```

Điền vào note chung:

```text
yas-master external IP:
yas-master private IP:
yas-worker external IP:
yas-worker private IP:
```

## Bước 6. SSH vào `yas-master`

```bash
gcloud compute ssh yas-master
```

Update package:

```bash
sudo apt-get update
sudo apt-get install -y curl ca-certificates git
```

## Bước 7. Cài kubectl/helm/yq trên `yas-master`

K3s sẽ tự có `kubectl`, nhưng cài Helm/yq để ngày 2-3 deploy chart dễ hơn.

Cài Helm:

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

Cài yq:

```bash
sudo wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/local/bin/yq
sudo chmod +x /usr/local/bin/yq
yq --version
```

Cài Docker trên `yas-master` nếu cần debug/pull image. Không dùng master để build chính nếu Jenkins đã build được.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo docker run hello-world
```

## Bước 8. Cài K3s server trên `yas-master`

Trên `yas-master`, chạy:

```bash
curl -sfL https://get.k3s.io | sh -s - server \
  --write-kubeconfig-mode 644 \
  --node-name yas-master
```

Kiểm tra:

```bash
sudo systemctl status k3s --no-pager
kubectl get nodes -o wide
```

Kỳ vọng:

```text
yas-master   Ready
```

Nếu `kubectl` lỗi permission:

```bash
sudo kubectl get nodes -o wide
```

## Bước 9. Lấy token join worker

Trên `yas-master`:

```bash
sudo cat /var/lib/rancher/k3s/server/node-token
```

Copy token vào note riêng của TV1, không gửi công khai nếu không cần.

## Bước 10. SSH vào `yas-worker`

Mở terminal khác:

```bash
gcloud compute ssh yas-worker
```

Update package:

```bash
sudo apt-get update
sudo apt-get install -y curl ca-certificates git
```

## Bước 11. Cài Docker trên `yas-worker`

Worker là nơi chạy workload. K3s dùng containerd, không bắt buộc Docker để chạy pod, nhưng cài Docker giúp debug image pull khi cần.

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo docker run hello-world
```

## Bước 12. Join `yas-worker` vào cluster

Trên `yas-worker`:

```bash
export K3S_URL=https://<MASTER_PRIVATE_IP>:6443
export K3S_TOKEN=<NODE_TOKEN_FROM_MASTER>

curl -sfL https://get.k3s.io | sh -s - agent \
  --server "$K3S_URL" \
  --token "$K3S_TOKEN" \
  --node-name yas-worker
```

Kiểm tra agent:

```bash
sudo systemctl status k3s-agent --no-pager
```

Quay lại `yas-master`:

```bash
kubectl get nodes -o wide
```

Kỳ vọng:

```text
yas-master   Ready
yas-worker   Ready
```

## Bước 13. Nếu worker chưa Ready

Trên `yas-worker`:

```bash
sudo journalctl -u k3s-agent -n 100 --no-pager
```

Trên `yas-master`:

```bash
kubectl describe node yas-worker
```

Các lỗi hay gặp:

- Sai `MASTER_PRIVATE_IP`.
- Firewall internal chưa mở.
- Sai node token.
- VM không cùng VPC/zone network.

## Done của TV1 trong ngày 1

TV1 được coi là xong ngày 1 khi có:

- Ảnh GCP VM list có `yas-master`, `yas-worker`.
- Ảnh firewall rule.
- Ảnh terminal:

```bash
kubectl get nodes -o wide
```

Lý tưởng thấy 2 node Ready. Nếu chỉ master Ready, ghi rõ worker đang lỗi gì và log lỗi.

---

# Thành viên 2 - Jenkins CI & Container Lead

Nhiệm vụ ngày 1: audit Jenkins cũ và chuẩn bị Jenkinsfile cho Docker build/push ngày 2.

## Bước 1. Xác nhận Jenkins cũ còn truy cập được

Mở Jenkins Project 1 cũ bằng trình duyệt.

Cần kiểm tra:

- Đăng nhập được không.
- Có job Multibranch Pipeline không.
- Job có scan branch không.
- Job có build gần đây không.
- Jenkins agent/node có Docker không.

Chụp ảnh:

- Trang Jenkins dashboard/job.
- Trang branch list của Multibranch Pipeline.

Nếu Jenkins không truy cập được:

- Báo ngay cho nhóm.
- Ghi lỗi cụ thể: không mở URL, quên password, server down, job mất, agent offline.
- Chưa vội đề xuất Jenkins mới cho đến khi xác nhận không phục hồi được.

## Bước 2. Kiểm tra Jenkins tools

Trong Jenkins:

1. Manage Jenkins.
2. Tools.
3. Kiểm tra tên Maven tool.
4. Kiểm tra tên JDK tool.

Jenkinsfile hiện đang dùng và nhóm thống nhất giữ:

```groovy
maven 'Maven'
jdk   'JDK25'
```

Ghi vào note:

```text
Maven tool name:
JDK tool name:
Jenkins has JDK25? yes/no
Jenkins has Docker CLI? yes/no
```

Nếu có quyền chạy command trên agent, tạo job tạm hoặc dùng pipeline replay:

```bash
java -version
mvn -version
docker version
git --version
```

Chụp ảnh console output.

## Bước 3. Đọc Jenkinsfile hiện tại

Trong repo local:

```bash
sed -n '1,260p' Jenkinsfile
```

Bạn cần xác nhận có các stage:

- `Detect Changed Services`
- `Gitleaks Scan`
- `Build Common Library`
- `Test`
- `Build`
- `SonarQube Analysis`
- `Snyk Security Scan`

Nếu có đủ, ghi:

```text
Jenkinsfile Project 1 has CI stages: yes
Change detection: yes
Coverage gate 70%: yes
Docker build/push: not yet
```

## Bước 4. Sửa lỗi JUnit glob an toàn

Mở `Jenkinsfile`, tìm đoạn:

```groovy
testResults: '*/target/surefire-reports/TEST-.xml, */target/failsafe-reports/TEST-.xml',
```

Đổi thành:

```groovy
testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
```

Đây là sửa an toàn, không đổi logic pipeline.

## Bước 5. Kiểm tra JDK25, không đổi xuống JDK21

Nhóm đã thống nhất dùng JDK25. Vì vậy giữ nguyên:

```groovy
jdk   'JDK25'
```

Việc cần làm:

- Vào Jenkins `Manage Jenkins` -> `Tools`.
- Kiểm tra JDK tool tên đúng `JDK25`.
- Nếu thiếu, báo người quản trị Jenkins cấu hình thêm JDK25.
- Không đổi Jenkinsfile về JDK21.

Nếu giảng viên hỏi vì sao không dùng Java 21, trả lời ngắn: nhóm kế thừa Jenkins Project 1 và fork YAS hiện tại đang chạy ổn với JDK25, nên Project 2 giữ nguyên toolchain để tránh rủi ro trong 5 ngày.

## Bước 6. Kiểm tra danh sách service

Trong Jenkinsfile, tìm:

```groovy
JAVA_SERVICES
```

Đảm bảo có các service demo:

```text
cart
customer
inventory
media
order
product
search
storefront-bff
backoffice-bff
tax
sampledata
```

Nếu thiếu service quan trọng, thêm vào chuỗi, không thêm UI `storefront`/`backoffice` vào Java list nếu chưa xử lý Next.js riêng.

## Bước 7. Chuẩn bị map image cho ngày 2

Tạo note riêng:

```text
product -> docker.io/<dockerhub-user>/yas-product
cart -> docker.io/<dockerhub-user>/yas-cart
order -> docker.io/<dockerhub-user>/yas-order
customer -> docker.io/<dockerhub-user>/yas-customer
inventory -> docker.io/<dockerhub-user>/yas-inventory
tax -> docker.io/<dockerhub-user>/yas-tax
media -> docker.io/<dockerhub-user>/yas-media
search -> docker.io/<dockerhub-user>/yas-search
storefront-bff -> docker.io/<dockerhub-user>/yas-storefront-bff
backoffice-bff -> docker.io/<dockerhub-user>/yas-backoffice-bff
sampledata -> docker.io/<dockerhub-user>/yas-sampledata
```

Ngày 2 mới thêm Docker build/push stage.

## Bước 8. Commit hoặc để thay đổi local

Kiểm tra thay đổi:

```bash
git diff -- Jenkinsfile
```

Nếu nhóm muốn commit ngay:

```bash
git status --short
git add Jenkinsfile
git commit -m "ci: prepare Jenkins pipeline for project 2"
```

Nếu chưa chắc, không commit, chỉ gửi diff cho nhóm.

## Done của TV2 trong ngày 1

TV2 được coi là xong ngày 1 khi có:

- Ảnh Jenkins cũ truy cập được.
- Ảnh Multibranch Pipeline.
- Note Jenkins tool Maven/JDK/Docker.
- Jenkinsfile đã sửa JUnit glob.
- Jenkins xác nhận có tool `JDK25` hoặc đã ghi blocker nếu thiếu.
- Note image naming cho ngày 2.

---

# Thành viên 3 - Jenkins CD & GitOps Lead

Nhiệm vụ ngày 1: hiểu Helm chart hiện có và chuẩn bị skeleton cho job `developer_build`.

## Bước 1. Kiểm tra chart có sẵn

Trong repo local:

```bash
find k8s/charts -maxdepth 2 -name Chart.yaml | sort
```

Kỳ vọng thấy chart cho:

```text
product
cart
order
customer
inventory
tax
media
search
storefront-bff
storefront-ui
backoffice-bff
backoffice-ui
swagger-ui
```

Nếu thiếu chart nào, ghi lại.

## Bước 2. Xác nhận backend override image

Đọc chart tax:

```bash
sed -n '1,120p' k8s/charts/tax/values.yaml
sed -n '1,140p' k8s/charts/backend/templates/deployment.yaml
```

Cần thấy:

```yaml
backend:
  image:
    repository: ...
    tag: ...
```

và deployment dùng:

```yaml
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
```

Ghi kết luận:

```text
Backend services override bằng:
--set backend.image.repository=...
--set backend.image.tag=...
```

## Bước 3. Xác nhận UI override image

Đọc chart storefront-ui:

```bash
sed -n '1,120p' k8s/charts/storefront-ui/values.yaml
sed -n '1,140p' k8s/charts/ui/templates/deployment.yaml
```

Ghi kết luận:

```text
UI services override bằng:
--set ui.image.repository=...
--set ui.image.tag=...
```

## Bước 4. Test Helm template local

Không cần cluster vẫn test được render:

```bash
helm dependency build k8s/charts/tax
helm template tax k8s/charts/tax \
  --set backend.image.repository=docker.io/<dockerhub-user>/yas-tax \
  --set backend.image.tag=main
```

Nếu máy chưa có Helm:

- Cài Helm, hoặc nhờ TV1/TV2 chạy giúp.
- Ghi rõ “máy TV3 chưa có Helm” nếu chưa làm được.

Kỳ vọng output có image:

```text
docker.io/<dockerhub-user>/yas-tax:main
```

Kiểm tra nhanh:

```bash
helm template tax k8s/charts/tax \
  --set backend.image.repository=docker.io/<dockerhub-user>/yas-tax \
  --set backend.image.tag=main | grep "image:"
```

## Bước 5. Viết danh sách parameter cho `developer_build`

Tạo note:

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

## Bước 6. Viết function resolve tag

Tạo note dùng cho Jenkins job ngày 2:

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

Quy tắc:

- `main` -> tag `main`.
- Branch khác -> tag commit SHA.
- CI của TV2 phải push image tag đó trước.

## Bước 7. Chuẩn bị deploy command mẫu

Backend:

```bash
helm dependency build k8s/charts/tax
helm upgrade --install tax k8s/charts/tax \
  -n preview-demo --create-namespace \
  --set backend.image.repository=docker.io/<dockerhub-user>/yas-tax \
  --set backend.image.tag=<commit-sha>
```

UI:

```bash
helm dependency build k8s/charts/storefront-ui
helm upgrade --install storefront-ui k8s/charts/storefront-ui \
  -n preview-demo --create-namespace \
  --set ui.image.repository=docker.io/<dockerhub-user>/yas-storefront \
  --set ui.image.tag=main
```

## Bước 8. Chuẩn bị NodePort YAML

Tạo note. Ngày 1 chỉ chuẩn bị để hiểu selector/port, chưa cần apply tay. Bản chính thức nên được nhúng vào Jenkins job `developer_build` để mỗi lần developer deploy preview thì NodePort tự có lại.

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

Ngày 3 sẽ kiểm tra selector thật bằng:

```bash
kubectl get pods -n preview-demo --show-labels
```

Nếu demo tối thiểu chỉ deploy `tax`, NodePort sẽ trỏ vào label của `tax`:

```yaml
selector:
  app.kubernetes.io/name: tax
  app.kubernetes.io/instance: tax
```

Ghi chú quan trọng: có NodePort không có nghĩa là `curl` sẽ chạy ngay. `curl` chỉ chạy khi pod phía sau service đã `Ready` và service có endpoint. Với `tax`, pod cần thêm:

```text
yas-configuration-configmap
yas-postgresql-credentials-secret
postgresql.postgres
```

Vì vậy từ ngày 3 trở đi, `developer_build` nên tự tạo NodePort, tự cài `yas-configuration`, và tự đảm bảo Postgres runtime tối thiểu đang tồn tại.

## Bước 9. Ghi thiết kế Postgres runtime tối thiểu

Không dùng chart Postgres phức tạp ngay ngày 1. Chỉ cần thống nhất dependency runtime cho demo:

```text
Namespace: postgres
Service name: postgresql
DNS mà tax dùng: postgresql.postgres
Database demo: tax
Username demo: yasadminuser
Password demo: lưu trong Secret, không commit vào repo
```

Nguyên tắc cleanup:

- `developer_cleanup` chỉ xóa namespace preview, ví dụ `preview-demo`.
- Không xóa namespace `postgres` trong cleanup vì đây là dependency dùng chung.
- Nếu muốn dọn Postgres sau demo, tạo lệnh riêng và hỏi cả nhóm trước.

## Done của TV3 trong ngày 1

TV3 được coi là xong ngày 1 khi có:

- Danh sách chart hiện có.
- Kết luận backend dùng `backend.image.repository/tag`.
- Kết luận UI dùng `ui.image.repository/tag`.
- `helm template` tax ra đúng image override, nếu có Helm.
- Note parameter `developer_build`.
- Note command deploy mẫu.
- NodePort YAML nháp.
- Note Postgres runtime tối thiểu.

---

# Thành viên 4 - Service Mesh & QA/Report Lead

Nhiệm vụ ngày 1: chuẩn bị checklist evidence, gom phần Project 1, và tạo khung report/demo để nhóm không quên bằng chứng.

## Bước 1. Tạo cấu trúc evidence

Trong máy cá nhân hoặc thư mục dùng chung, tạo:

```text
evidence/
  01-lab1-ci/
  02-gcp-k3s/
  03-jenkins-ci-docker/
  04-developer-build/
  05-preview-nodeport/
  06-cleanup/
  07-argocd/
  08-istio-kiali/
```

Nếu muốn tạo trong repo, chỉ đưa ảnh đã che secret.

## Bước 2. Tạo bảng ai chụp ảnh gì

Tạo note:

```text
TV1:
- GCP VM list
- Firewall rules
- kubectl get nodes -o wide
- Jenkins kubectl access nếu có

TV2:
- Jenkins Multibranch
- Jenkins changed service detection
- Test result
- Coverage
- Gitleaks
- Sonar
- Snyk
- Docker build/push
- Docker Hub tags

TV3:
- developer_build parameters
- developer_build console
- helm list
- kubectl get pods preview
- deployment image tag
- NodePort service
- cleanup job

TV4:
- report screenshots collected
- test plan
- Kiali/Istio if any
- final demo script
```

## Bước 3. Gom bằng chứng Project 1

Mở GitHub/Jenkins/Sonar/Snyk, yêu cầu nhóm cung cấp hoặc tự chụp:

Project 1 bắt buộc:

- Link GitHub repo.
- Có PR đang open.
- Branch protection `main`.
- Yêu cầu 2 reviewers.
- Required CI pass.
- Jenkins job screenshot.
- Jenkins branch scan screenshot.
- Test result.
- Coverage report.
- Gitleaks.
- SonarCloud/SonarQube.
- Snyk.

Nếu chưa có ảnh nào, ghi trạng thái:

```text
Branch protection screenshot: missing
Open PR screenshot: missing
Jenkins branch scan: available/missing
...
```

Mục tiêu ngày 1 không nhất thiết đủ hết ảnh, nhưng phải biết thiếu ảnh nào.

## Bước 4. Tạo test plan bắt buộc Project 2

Tạo file nháp hoặc note:

```text
Test case 1: Jenkins cũ còn dùng được
Steps:
1. Mở Jenkins Multibranch job.
2. Chạy scan/build branch.
Expected:
- Jenkins thấy branch.
- Pipeline chạy từ Jenkinsfile.

Test case 2: Build service thay đổi
Steps:
1. Push branch dev_tax_service.
2. Jenkins chạy pipeline.
Expected:
- Detect service tax.
- Không build toàn bộ service.

Test case 3: Docker image commit SHA
Steps:
1. Lấy git rev-parse --short=12 origin/dev_tax_service.
2. Kiểm tra Docker Hub.
Expected:
- Có yas-tax:<sha>.

Test case 4: developer_build
Steps:
1. Chạy job với TAX_BRANCH=dev_tax_service.
2. Service khác để main.
Expected:
- tax dùng image <sha>.
- service khác dùng main.

Test case 5: Preview NodePort
Steps:
1. Thêm hosts.
2. Mở http://yas-preview.local:30080.
Expected:
- Truy cập được.

Test case 6: Cleanup
Steps:
1. Chạy developer_cleanup.
2. kubectl get ns preview-demo.
Expected:
- Namespace bị xóa.
```

## Bước 5. Tạo khung report

Tạo outline:

```text
1. Tổng quan
2. Kế thừa Project 1
3. Audit Project 1
4. Kiến trúc Project 2
5. CI flow trên Jenkins cũ
6. Docker image tag strategy
7. K3s trên Google Cloud
8. Jenkins developer_build
9. Cleanup preview
10. Argo CD nâng cao nếu có
11. Istio/Kiali nâng cao nếu có
12. Test plan và evidence
13. Kết luận
```

## Bước 6. Viết đoạn giải thích quan trọng để dùng lại trong report

Copy đoạn này vào report:

```text
Project 2 không thiết kế lại CI/CD từ đầu mà kế thừa Jenkins CI đã xây dựng ở Project 1. Jenkins cũ tiếp tục đảm nhiệm branch scanning, test, build, coverage, security scan, Docker build/push và điều phối deploy. Google Cloud chỉ được dùng để dựng K3s cluster và chạy workload YAS. Jenkins kết nối vào cluster bằng kubeconfig hoặc Kubernetes service account. Nhóm không dùng GitHub Actions cho phần CD mới và không cài Jenkins mới nếu Jenkins Project 1 còn sử dụng được.
```

## Bước 7. Chuẩn bị checklist ngày 2

Cuối ngày 1, hỏi từng người:

TV1:

```text
VM tạo xong chưa?
K3s master Ready chưa?
Worker join chưa?
IP worker là gì?
```

TV2:

```text
Jenkins cũ truy cập được không?
Jenkins có Docker không?
Jenkinsfile sửa gì rồi?
Docker Hub username là gì?
```

TV3:

```text
Helm template override image được chưa?
developer_build parameters đã chốt chưa?
NodePort YAML nháp có chưa?
```

TV4:

```text
Project 1 còn thiếu screenshot nào?
Report outline có chưa?
Test plan có chưa?
```

## Done của TV4 trong ngày 1

TV4 được coi là xong ngày 1 khi có:

- Thư mục evidence hoặc checklist evidence.
- Danh sách ảnh Project 1 còn thiếu.
- Test plan bắt buộc Project 2.
- Report outline.
- Đoạn mô tả kế thừa Jenkins cũ.

---

# Sync cuối ngày 1

Cả nhóm họp 15 phút và điền bảng:

| Câu hỏi | Trả lời |
|---|---|
| Jenkins cũ còn truy cập được không? | yes/no |
| Jenkins có Docker không? | yes/no |
| GCP VM tạo xong chưa? | yes/no |
| K3s master Ready chưa? | yes/no |
| K3s worker Ready chưa? | yes/no |
| Docker Hub username là gì? | |
| Service demo chính là gì? | tax |
| Branch demo ngày 2 là gì? | dev_tax_service |
| NodePort demo là gì? | 30080 |
| Preview domain là gì? | yas-preview.local |

## Quyết định cuối ngày

Nếu Jenkins cũ dùng được:

```text
Ngày 2 tiếp tục nâng Jenkinsfile Docker build/push và Jenkins kết nối K3s.
```

Nếu Jenkins cũ không truy cập được:

```text
Ngày 2 ưu tiên phục hồi Jenkins cũ. Chỉ xem Jenkins mới là phương án dự phòng nếu Jenkins cũ không thể phục hồi/kết nối K3s.
```

Nếu K3s chưa xong:

```text
Ngày 2 TV1 tiếp tục fix K3s, TV2 vẫn làm Docker build/push, TV3 vẫn chuẩn bị developer_build script, TV4 gom evidence Project 1.
```

## Checklist nộp cuối ngày 1 cho trưởng nhóm

Mỗi thành viên gửi vào nhóm:

TV1 gửi:

```text
[TV1 Day1]
VM master:
VM worker:
kubectl get nodes result:
Blocker:
Screenshot links:
```

TV2 gửi:

```text
[TV2 Day1]
Jenkins access: yes/no
Jenkins Docker: yes/no
JDK tool:
Maven tool:
Jenkinsfile changes:
Blocker:
Screenshot links:
```

TV3 gửi:

```text
[TV3 Day1]
Helm charts checked:
Backend override:
UI override:
Helm template result:
developer_build params ready: yes/no
Blocker:
Screenshot/log links:
```

TV4 gửi:

```text
[TV4 Day1]
Evidence folders/checklist ready: yes/no
Project 1 missing screenshots:
Test plan ready: yes/no
Report outline ready: yes/no
Blocker:
```
