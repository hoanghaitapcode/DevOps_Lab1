# Script tạo traffic tự động cho Kiali demo (Chạy trên Windows PowerShell)
$stopTime = (Get-Date).AddMinutes(15)
Write-Host "=========================================================="
Write-Host "Đang tạo traffic tự động trong 15 phút để Kiali hiển thị."
Write-Host "Nhấn Ctrl+C để dừng bất kỳ lúc nào."
Write-Host "=========================================================="

while ((Get-Date) -lt $stopTime) {
    # 1. allowed-client gọi tax (Public Endpoint -> HTTP 200 -> Đường màu XANH LÁ)
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -s -o /dev/null http://tax/tax/v3/api-docs
    
    # 2. allowed-client gọi faulty (Kiểm tra retry policy -> HTTP 500)
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -s -o /dev/null http://faulty/status/500
    
    # 3. blocked-client gọi tax (Kiểm tra default-deny -> HTTP 403 -> Đường màu CAM/ĐỎ)
    kubectl exec -n mesh-demo blocked-client -c curl -- curl -s -o /dev/null http://tax/tax/v3/api-docs
    
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 1
}
Write-Host "`nĐã hoàn thành tạo traffic."
