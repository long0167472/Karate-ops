param(
  [string]$BaseUrl = "http://localhost:8080",
  [switch]$ResetDatabase,
  [switch]$StartBackend,
  [switch]$KeepBackendRunning
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Split-Path -Parent $scriptDir
$repoRoot = Split-Path -Parent $backendDir
$logPath = Join-Path $backendDir "target\\club-management-flow.log"
$startedBackend = $false
$backendProcess = $null

function Write-Step {
  param([string]$Message)
  Write-Host "==> $Message" -ForegroundColor Cyan
}

function Ensure-Java {
  if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\\Users\\hoang\\AppData\\Local\\Programs\\Android Studio\\jbr"
  }
  if (-not (Test-Path $env:JAVA_HOME)) {
    throw "JAVA_HOME does not exist: $env:JAVA_HOME"
  }
  if ($env:Path -notlike "*$($env:JAVA_HOME)\\bin*") {
    $env:Path = "$($env:JAVA_HOME)\\bin;$($env:Path)"
  }
}

function Wait-BackendHealthy {
  param([int]$TimeoutSeconds = 180)
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -UseBasicParsing "$BaseUrl/actuator/health" -TimeoutSec 5
      if ($response.StatusCode -eq 200) {
        return
      }
    } catch {
    }
    Start-Sleep -Seconds 2
  }
  throw "Backend did not become healthy within $TimeoutSeconds seconds."
}

function Start-BackendIfNeeded {
  $healthy = $false
  try {
    $response = Invoke-WebRequest -UseBasicParsing "$BaseUrl/actuator/health" -TimeoutSec 5
    $healthy = ($response.StatusCode -eq 200)
  } catch {
    $healthy = $false
  }

  if ($healthy) {
    Write-Step "Backend already healthy at $BaseUrl"
    return
  }

  if (-not $StartBackend) {
    throw "Backend is not running at $BaseUrl. Re-run with -StartBackend or start the app manually."
  }

  Ensure-Java
  Write-Step "Starting backend via mvn spring-boot:run"
  if (Test-Path $logPath) {
    Remove-Item -LiteralPath $logPath -Force
  }
  $command = "`$env:JAVA_HOME = '$env:JAVA_HOME'; `$env:Path = '$env:JAVA_HOME\\bin;' + `$env:Path; Set-Location '$backendDir'; mvn spring-boot:run *> '$logPath'"
  $script:backendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command -WindowStyle Hidden -PassThru
  $script:startedBackend = $true
  Wait-BackendHealthy
}

function Reset-DatabaseIfRequested {
  if (-not $ResetDatabase) {
    return
  }
  Write-Step "Resetting postgres volume"
  Push-Location $backendDir
  try {
    docker compose down -v | Out-Host
    docker compose up -d postgres | Out-Host
  } finally {
    Pop-Location
  }
}

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    $Body = $null,
    [string]$Token = $null,
    [int]$ExpectedStatus = 200
  )

  $uri = "$BaseUrl$Path"
  $headers = @{}
  if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
  }

  try {
    if ($null -ne $Body) {
      $json = $Body | ConvertTo-Json -Depth 12
      $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $uri -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json
    } else {
      $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri $uri -Headers $headers
    }
  } catch {
    if ($_.Exception.Response) {
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $bodyText = $reader.ReadToEnd()
      throw "HTTP error for ${Method} ${Path}: $bodyText"
    }
    throw
  }

  if ([int]$response.StatusCode -ne $ExpectedStatus) {
    throw "Unexpected status for ${Method} ${Path}: $($response.StatusCode)"
  }

  if ([string]::IsNullOrWhiteSpace($response.Content)) {
    return $null
  }

  $envelope = $response.Content | ConvertFrom-Json
  if (-not $envelope.success) {
    throw "API failure for ${Method} ${Path}: $($envelope.message)"
  }
  return $envelope.data
}

function Login {
  param(
    [string]$Email,
    [string]$Password
  )
  Invoke-Api -Method "POST" -Path "/api/auth/login" -Body @{
    email = $Email
    password = $Password
  }
}

function Find-SessionRow {
  param(
    $Attendance,
    [string]$SessionId
  )
  foreach ($row in $Attendance.sessionRows) {
    if ($row.id -eq $SessionId) {
      return $row
    }
  }
  throw "Session row not found: $SessionId"
}

try {
  Reset-DatabaseIfRequested
  Start-BackendIfNeeded

  $summary = [ordered]@{}

  Write-Step "Logging in as global admin"
  $global = Login -Email "admin@karate-ops.local" -Password "Admin@123456"
  $globalToken = $global.accessToken
  $summary.globalAdmin = [ordered]@{
    email = "admin@karate-ops.local"
    roles = @($global.user.roles)
  }

  Write-Step "Creating club TLKC"
  $club = Invoke-Api -Method "POST" -Path "/api/organizations" -Token $globalToken -ExpectedStatus 201 -Body @{
    name = "TLKC"
    shortName = "TLKC"
    code = "TLKC"
    type = "CLUB"
    country = "VN"
    province = "HN"
    address = "Ha Noi"
    contactEmail = "contact@tlkc.local"
    contactPhone = "0900000001"
  }
  $clubId = $club.id
  $summary.club = [ordered]@{
    id = $clubId
    code = $club.code
    name = $club.name
  }

  Write-Step "Creating club admin account"
  $clubAdminAccount = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/member-accounts" -Token $globalToken -ExpectedStatus 201 -Body @{
    displayName = "TLKC Club Admin"
    email = "admin.tlkc@example.test"
    phone = "0900000100"
    gender = "MALE"
    birthDate = "1990-01-01"
    currentAddress = "Ha Noi"
    role = "MANAGER"
    status = "ACTIVE"
    student = $false
    attendanceViewEnabled = $true
    memberNote = "Seed admin club"
  }
  $clubAdminUserId = $clubAdminAccount.member.userId
  Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/users/$clubAdminUserId/club-manager-role" -Token $globalToken | Out-Null
  $clubAdminLogin = Login -Email "admin.tlkc@example.test" -Password "123456"
  $clubAdminToken = $clubAdminLogin.accessToken
  $summary.clubAdmin = [ordered]@{
    email = "admin.tlkc@example.test"
    userId = $clubAdminUserId
    memberId = $clubAdminAccount.member.id
    temporaryPassword = $clubAdminAccount.temporaryPassword
    roles = @($clubAdminLogin.user.roles)
  }

  Write-Step "Creating 100 member accounts"
  $createdMembers = New-Object System.Collections.Generic.List[object]
  for ($i = 1; $i -le 100; $i++) {
    $index = "{0:D3}" -f $i
    $account = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/member-accounts" -Token $clubAdminToken -ExpectedStatus 201 -Body @{
      displayName = "TLKC Member $index"
      email = "member$index.tlkc@example.test"
      phone = ("0901{0:D6}" -f $i)
      gender = "MALE"
      birthDate = "2010-01-01"
      currentAddress = "Ha Noi"
      role = "ATHLETE"
      status = "ACTIVE"
      student = ($i % 2 -eq 0)
      attendanceViewEnabled = $true
      memberNote = "bulk seed"
    }
    if ($i -le 3 -or $i -gt 97) {
      $createdMembers.Add([ordered]@{
        displayName = $account.member.personName
        email = "member$index.tlkc@example.test"
        memberId = $account.member.id
      }) | Out-Null
    }
  }
  $membersAfterBulk = Invoke-Api -Method "GET" -Path "/api/organizations/$clubId/members" -Token $clubAdminToken
  $summary.bulkMembers = [ordered]@{
    created = 100
    sample = $createdMembers
    memberCountAfterBulk = @($membersAfterBulk).Count
  }

  Write-Step "Running public account request and approving it"
  $lookup = Invoke-Api -Method "GET" -Path "/api/public/clubs/lookup?code=TLKC"
  $requestEmail = "requester.tlkc@example.test"
  $accountRequest = Invoke-Api -Method "POST" -Path "/api/account-requests" -ExpectedStatus 201 -Body @{
    organizationCode = "TLKC"
    displayName = "Requested User"
    email = $requestEmail
    phone = "0900999999"
    gender = "FEMALE"
    birthDate = "2008-05-01"
    currentAddress = "Ha Noi"
  }
  $pendingRequests = Invoke-Api -Method "GET" -Path "/api/organizations/$clubId/account-requests?status=PENDING" -Token $clubAdminToken
  $approvedRequest = Invoke-Api -Method "PATCH" -Path "/api/organizations/$clubId/account-requests/$($accountRequest.id)/decision" -Token $clubAdminToken -Body @{
    status = "APPROVED"
    decisionNote = "Approved by club admin"
  }
  $memberLogin = Login -Email $requestEmail -Password "123456"
  $memberToken = $memberLogin.accessToken
  $memberProfile = Invoke-Api -Method "GET" -Path "/api/me/club-profile" -Token $memberToken
  $approvedMemberId = $approvedRequest.member.id
  $summary.accountRequest = [ordered]@{
    lookupClubId = $lookup.id
    requestId = $accountRequest.id
    pendingCountBeforeApprove = @($pendingRequests).Count
    approvedMemberId = $approvedMemberId
    approvedUsername = $approvedRequest.username
    approvedTempPassword = $approvedRequest.temporaryPassword
    meMembershipCount = @($memberProfile.memberships).Count
    joinedClubIds = @($memberProfile.memberships | ForEach-Object { $_.organizationId })
  }

  Write-Step "Creating session and running leave approval flow"
  $session = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/attendance-sessions" -Token $clubAdminToken -ExpectedStatus 201 -Body @{
    name = "TLKC Integration Session"
    type = "TRAINING"
    scheduledAt = ([DateTime]::UtcNow.AddDays(1).ToString("yyyy-MM-ddTHH:mm:ssZ"))
    notes = "backend integration flow"
  }
  $leaveRequest = Invoke-Api -Method "POST" -Path "/api/me/attendance/leave-requests" -Token $memberToken -ExpectedStatus 201 -Body @{
    requestType = "LEAVE_SESSION"
    sessionId = $session.id
    reason = "Family trip"
  }
  $orgLeaveRequests = Invoke-Api -Method "GET" -Path "/api/organizations/$clubId/attendance-leave-requests" -Token $clubAdminToken
  $approvedLeave = Invoke-Api -Method "PATCH" -Path "/api/attendance-leave-requests/$($leaveRequest.id)/decision" -Token $clubAdminToken -Body @{
    status = "APPROVED"
    decisionNote = "Approved by admin"
  }
  $memberAttendance = Invoke-Api -Method "GET" -Path "/api/me/attendance" -Token $memberToken
  $attendanceRow = Find-SessionRow -Attendance $memberAttendance -SessionId $session.id
  $summary.leaveFlow = [ordered]@{
    sessionId = $session.id
    requestId = $leaveRequest.id
    orgLeaveCount = @($orgLeaveRequests).Count
    approvedStatus = $approvedLeave.status
    memberPendingLeaveRequests = $memberAttendance.pendingLeaveRequests
    memberExcusedCount = $memberAttendance.excused
    sessionRecordStatus = $attendanceRow.record.status
    sessionLeaveStatus = $attendanceRow.leaveRequest.status
  }

  Write-Step "Creating fee role, fee item, applying fee, and marking it paid"
  $feeRole = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/fee-roles" -Token $clubAdminToken -ExpectedStatus 201 -Body @{
    code = "REGULAR"
    name = "Regular Athlete"
    description = "Regular athlete fee role"
    priority = 100
    active = $true
  }
  $memberFeeRole = Invoke-Api -Method "PUT" -Path "/api/organizations/$clubId/members/$approvedMemberId/fee-roles" -Token $clubAdminToken -Body @{
    feeRoleIds = @($feeRole.id)
  }
  $feeItem = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/fee-items" -Token $clubAdminToken -ExpectedStatus 201 -Body @{
    name = "TLKC Monthly Tuition"
    feeType = "TUITION"
    feeKind = "MONTHLY_TUITION_DEFAULT"
    billingCycle = "MONTHLY"
    status = "ACTIVE"
    defaultAmount = 500000
    dueDay = 10
    description = "Monthly tuition for regular athlete"
    roleAmounts = @(
      @{
        feeRoleId = $feeRole.id
        amount = 350000
        exempt = $false
      }
    )
  }
  $assignments = Invoke-Api -Method "POST" -Path "/api/organizations/$clubId/fee-items/$($feeItem.id)/apply" -Token $clubAdminToken -Body @{
    memberIds = @($approvedMemberId)
    feeRoleIds = @($feeRole.id)
    dueDate = ([DateTime]::UtcNow.Date.AddDays(7).ToString("yyyy-MM-dd"))
    note = "Apply monthly tuition to approved user"
  }
  $assignment = $assignments | Select-Object -First 1
  $paidAssignment = Invoke-Api -Method "PATCH" -Path "/api/organizations/$clubId/fee-assignments/$($assignment.id)" -Token $clubAdminToken -Body @{
    paidAmount = $assignment.amountDue
    status = "PAID"
    note = "Paid in full"
  }
  $memberFees = Invoke-Api -Method "GET" -Path "/api/me/fees" -Token $memberToken
  $financeOverview = Invoke-Api -Method "GET" -Path "/api/organizations/$clubId/finance/overview" -Token $clubAdminToken
  $summary.feeFlow = [ordered]@{
    feeRoleId = $feeRole.id
    memberRoleCount = @($memberFeeRole.roles).Count
    feeItemId = $feeItem.id
    assignmentId = $assignment.id
    assignmentAmountDue = $assignment.amountDue
    paidStatus = $paidAssignment.status
    paidAmount = $paidAssignment.paidAmount
    memberTotalDue = $memberFees.totalDue
    memberTotalPaid = $memberFees.totalPaid
    memberTotalRemaining = $memberFees.totalRemaining
    financeTotalPaid = $financeOverview.summary.totalPaid
  }

  $membersFinal = Invoke-Api -Method "GET" -Path "/api/organizations/$clubId/members" -Token $clubAdminToken
  $summary.finalState = [ordered]@{
    totalClubMembers = @($membersFinal).Count
    adminSessionUser = $clubAdminLogin.user.displayName
    memberSessionUser = $memberLogin.user.displayName
  }

  Write-Step "Flow completed"
  $summary | ConvertTo-Json -Depth 12
} finally {
  if ($startedBackend -and -not $KeepBackendRunning -and $backendProcess -and -not $backendProcess.HasExited) {
    Write-Step "Stopping backend process started by script"
    Stop-Process -Id $backendProcess.Id -Force
  }
}
