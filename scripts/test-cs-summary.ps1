$body = @{
    counselText = "고객이 배송 지연으로 문의했고 오늘 중 연락을 요청했습니다."
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8081/counsels/summary" `
    -ContentType "application/json" `
    -Body $body
