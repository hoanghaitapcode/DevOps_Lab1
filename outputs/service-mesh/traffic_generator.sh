#!/bin/bash
# Script tạo traffic tự động cho Kiali demo (Chạy trên macOS/Linux Bash)
stop_time=$((SECONDS + 900)) # 15 phút
echo "=========================================================="
echo "Đang tạo traffic tự động trong 15 phút để Kiali hiển thị."
echo "Nhấn Ctrl+C để dừng bất kỳ lúc nào."
echo "=========================================================="

while [ $SECONDS -lt $stop_time ]; do
    # 1. allowed-client gọi tax (Public Endpoint -> HTTP 200 -> Đường màu XANH LÁ)
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -s -o /dev/null http://tax/tax/v3/api-docs
    
    # 2. allowed-client gọi faulty (Kiểm tra retry policy -> HTTP 500)
    kubectl exec -n mesh-demo allowed-client -c curl -- curl -s -o /dev/null http://faulty/status/500
    
    # 3. blocked-client gọi tax (Kiểm tra default-deny -> HTTP 403 -> Đường màu CAM/ĐỎ)
    kubectl exec -n mesh-demo blocked-client -c curl -- curl -s -o /dev/null http://tax/tax/v3/api-docs
    
    echo -n "."
    sleep 1
done
echo -e "\nĐã hoàn thành tạo traffic."
