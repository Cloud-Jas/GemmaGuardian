# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "❌ This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Please right-click PowerShell and select 'Run as Administrator', then run this script again." -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host "Creating Windows Firewall rules for SurveillanceAgent..." -ForegroundColor Green

# Create UDP outbound rule for broadcasting notifications
try {
    New-NetFirewallRule -DisplayName "SurveillanceAgent UDP Broadcast" -Direction Outbound -Protocol UDP -LocalPort 9999 -Action Allow -Profile Any -ErrorAction Stop
    Write-Host "✅ Created UDP outbound rule for port 9999" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create UDP outbound rule: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Create TCP inbound rule for HTTP notification server
try {
    New-NetFirewallRule -DisplayName "SurveillanceAgent HTTP Server" -Direction Inbound -Protocol TCP -LocalPort 8888 -Action Allow -Profile Any -ErrorAction Stop
    Write-Host "✅ Created TCP inbound rule for port 8888" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create TCP inbound rule: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Create UDP inbound rule for potential responses
try {
    New-NetFirewallRule -DisplayName "SurveillanceAgent UDP Inbound" -Direction Inbound -Protocol UDP -LocalPort 9999 -Action Allow -Profile Any -ErrorAction Stop
    Write-Host "✅ Created UDP inbound rule for port 9999" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create UDP inbound rule: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "All firewall rules created successfully!" -ForegroundColor Yellow
Write-Host "You can now test the mobile notifications." -ForegroundColor Yellow
