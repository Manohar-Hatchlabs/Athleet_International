# Acadware Admin – Feature Inventory (working notes)

Source: https://acadware.net/aw-admin (Angular SPA, Angular Material UI). Captured 2026-11-19 while signed in to the
"Athleet Junior Sports Development Program" organization. Only structure is recorded — no student/staff personal data.

Goal: rebuild this admin like-for-like (same screens, fields and behaviour), then add Athleet-specific features.

---

## 0. Entry flow

1. **Login** (`#/login`): single field "Email / Mobile *" → Submit (then password / OTP step). Footer: "By continuing, you agree to
   our Terms of Use", "Request data deletion" link (`#/data-deletion-req`).
   Also: `#/forgot`, `#/resetPassword`, `#/register`, `#/terms-of-use`. Captcha verification endpoint exists.
   White-label login routes exist for other customers (sos, dataStream, mahd, sportz-village, al-hilal, al-Shabab, yalla-goal,
   sports-pad) — same login, different branding.
2. **Select Academy** (`#/organization_selector`): searchable dropdown of organizations the user can access → GO.
3. **Organization selector** for a primary org: shows *Primary Organization* card and a searchable grid of
   *Affiliated Organizations* (14 schools: TIGPS - Kolaghat, Jalpaiguri, APD, BOL, COB, DGP, FLK, HGH, Midnapore, NBD, RAI,
   Siliguri; TIGWS - Siliguri; RNJ - NJSS), each with logo + "City, Country". Floating **+** button to add an organization.
4. Clicking the primary org opens a dialog: "Every Primary Organization can also operate as a self contained academy."
   → **Primary Organization (Primary)** or **Academy Admin (Affiliate)**.

### Hierarchy model
- **Primary (parent) organization** → many **affiliated (child) academies** → **centers/locations** → **batches/groups**
  (in AJSDP a batch = a school class: "Class - III" … "Class - X") → students.
- Players can be transferred between academies (`parent_child_academy/GetOrganizationListToTransferPlayer`, `player/TransferPlayer`).

## 1. Shell (all pages)
- Top bar: org logo + org name; academy switcher (icon with count "14"); current org name; **settings** gear; **notifications** bell;
  user avatar menu (Logout).
- Left sidebar (Primary view): DASHBOARD · STUDENT · STAFF · REPORTS · REQUEST · EXPENSE · POSTS · ASSESSMENT.

## 2. Dashboard (`#/dashboard`)
Tabs: **GENERAL · ATTENDANCE · PERFORMANCE**

### 2.1 General → sub-tabs HEADCOUNT · UTILIZATION
**Headcount**
- KPI tiles: *Affiliated Academies* (14), *Current Headcount* (12,299), *Month over Month* (%).
- *Headcount Trend* line chart (last 4 months) + "Academy wise trend ›" link.
- *Age Wise Breakup* bar chart (buckets 1-5, 6-10, 11-15, 16-20, 20+).
- *Gender Ratio* (male % / female % pictograms).
- *Locations* map (Google Maps markers per academy).
- *Academy Details* table: Academy Name (+city) · No. of Players (MoM) · Staff:Player.

**Utilization** → sub-tabs *Current Utilization* · *Utilization Trends*
- "Real Time Utilization" with **Export** and Search.
- Facet filters: UTILIZATION (<50%, 50–75%, >75%) · SPORT · STATE · CITY · ACADEMY.
- Summary: Available Capacity · Used Capacity · Utilization % · Staff (coaches).
- Table: Academy Name · Location · Batches · Capacity · Student · Staff · Staff:Player · Utilization — paginated (10/page).

### 2.2 Attendance → sub-tabs LIVE ATTENDANCE · ATTENDANCE TRACKER
- **Live Attendance**: Academy filter; per academy a card listing Staff Member · Status · In · Out, with batch name (today's coach
  check-in/out).
- **Attendance Tracker** (`#/attendance-tracker`): monthly attendance view, select Staff / Student.

### 2.3 Performance
- (to capture) uses `dashboard/GetPlayerPerformanceStatistics`, `GetAssessmentConsolidatedRankingDetails`.

## 3. Student (`#/student`)
- Primary level: **Import** (bulk student upload). Full list/profile is at academy level (see §10).

## 4. Staff (`#/staff`)
- **+** Add staff. Table: Staff Member · Role (Role Type); Search; paginator.
- API: user/AddUser, AssignRole, DeleteStaffMember, DeregisterStaffMember, GetAllUserList, GetCenterUserList,
  GetQualificationDetails, SearchUser.

## 5. Reports (`#/report`) — launcher grouped in 4 sections
| Student | Attendance | Fees | Performance |
|---|---|---|---|
| Deregistered Student | Attendance Tracker | Total Revenue Report | Assessment Report |
| Kit Size Report | In/Out Report | Cheque Tracking | Student Tags Report |
| Student Refferal Report | Session Tracker | Sales Order | Compliance Tracking |
| Batch Allocation Report | Student Session Report | Equated Monthly Income | Athlete Daily Activities |
| | Session Insight Report | Student Fee Report | |
| | | Payment Extension Report | |
| | | Fee Exemption Report | |
| | | Write-Off Report | |
| | | Expense Report | |
| | | Revenue Share Report | |

## 6. Request (`#/request`)
- Delete requests and expense requests (approve/reject). API: request/AddDeleteRequest, GetDeleteRequestList,
  UpdateDeleteRequestStatus, GetExpenseRequestList, GetExpenseRequestDetails, GetPendingExpenseDetails, UpdateExpenseRequestStatus.

## 7. Expense (`#/expense`)
- Tiles: Pending Expense · Approved Expense · Rejected Expense, with Pending / Approved / Rejected Amount.
- Filters: Staff · Status (All/…) · From Date · To Date.

## 8. Posts (`#/posts`)
- **+** create post (news/announcement to parents/staff). API: news/AddAcademyNewsNotification, Update…, Delete…,
  GetAcademyNewsNotification(Details), AddAcademyNewsAttendees (RSVP), SendEmailNewsNotification.

## 9. Assessment (`#/Assessment`)
Two cards:
- **CREATE/VIEW TEST** — "Create your tests and group them into a test bank".
  - Test bank list (+ add, edit, delete). Example: *Health-Related Fitness Test* — 10 parameters.
  - Test parameters screen: filter by Area ("All Areas"); each parameter shows Name · Area · Description · *Measured As*
    (unit + type Numeric/Dropdown) · *Comments are* (Optional/Mandatory) · edit/delete; **+** add parameter.
  - AJSDP parameters: Weight (Anthropometrics, kg), Height (Anthropometrics, cm), Beep Test (Endurance, level),
    30m Dash (Speed, s), 20m Dash (Speed, s), T-Test (agility, s), BESS (balance, stage dropdown),
    Seated Medicine Ball Chest Pass (upper body, m), Standing Broad Jump (lower body, m), Sit & Reach (flexibility, cm).
- **REPORTS / LEADERBOARDS** — "Generate Reports of Student based on overall performance".

## 10. Settings (gear icon → `#/academySettings`) — same list at primary and academy level
| Item | Route | Contents |
|---|---|---|
| Branding Preference | academySettings | Currency (INR), Date format (d/m/Y), Font family (Inter), "Center Name Title" (label used for a center – AJSDP uses "Class"); colour pickers for Registration page (background, text, hint text), Main page (background, text, page header bg/text, card header bg/text, button bg/text, selected-tab underline), Side navigation (header bg, header font, header menu, background) |
| Mobile Apps | | Play Store link (Android), App Store link (iOS) → Submit |
| Notifications | | Toggles + message text for: Birthday wishes · Payment reminder (outstanding 5 days after due) · Payment received · Download invites (email on new registration) · Post notification · Cheque status · Attendance (as soon as captured) · Calendar tasks · Deregistration · Batch capacity exceeded (to academy email) · Payment due date extension · Staff rating · Self-registration |
| Social Media Handles | | "Get Social Media Handles of the Athlete" toggle (Instagram, Facebook, LinkedIn) |
| Terms & Conditions | | Org T&C editor with versions/history and publish; parents must accept (acceptance report) |
| Dashboard Configuration | | Entitlements for *Student Dashboard* and *Academy Dashboard* |
| Attendance Configuration | attendance-configuration | Mandatory photo capture (selfie) · Capture tasks accomplished with Out punch · Display device ID / phone type in reports · Allow backdated/forward-dated attendance entry → Save |
| Fee Settings | tax-setting | Enable online payment of fees · Allow partial payments · Tax settings |
| Cascade Activities | cascade-activities | Select Activity Type + Activity and cascade it to child academies |
| Activity Categories | activity-categories | Category tree (e.g. Age, Dribble) with Active flag, scope Staff / Center / Academy; + add |
| Competition / Rank Master | competition-rank-master | Per sport (Multi Sports) competition levels and ranks; + add |
| Player Unique ID Auto-generation | player-id-generator | Toggle + prefix; Unique ID = prefix + system player ID |
| Student Rating Categories | overall-rating | Overall player rating – setup scale for 5-star ratings (categories + labels) |

## 11. Academy Admin (Affiliate) view — e.g. "TIGPS - Kolaghat"
Sidebar: DASHBOARD · STUDENT · **CLASS** · **ROLES** · STAFF · REPORTS · REQUEST · EXPENSE · **MESSAGING** · POSTS · ASSESSMENT ·
**COACHING DRILLS**. (Fees menus — FeePlans, payment-tracker, cheque-tracker — are hidden for this school because online fees are off.)

### 11.1 Dashboard
- Headcount tile (current headcount; "Total Deregistration : n | New Inquiries : n"), Month over Month %.
- Headcount Trend (+ "Center wise trend"), HeadCount Analysis, Age Wise Breakup (+ "Center wise Age Breakup"),
  Center Headcount list (per class, sorted desc), Gender Ratio. Each widget has a copy/export icon.

### 11.2 Student (`#/student`)
- Tabs: **Active (n)** · **Submitted (n)** (self-registrations awaiting approval).
- Toolbar: Search · *Select Bulk Action* (enabled after row selection; bulk deactivate/deregister/delete) · **+** add · settings.
- Table: photo · Student Name · ID · Class Name · Batch · Age · Grade · Email Address · Actions — paginated 5/10/20.
- Row actions (*Select Action*): **ID Card · Attendance · Deregistration · Batches · Tag Student · Injury Log ·
  Transfer Student · Student History**.
- **Add Student** (multi-step dialog, step 1 → Next): Class · Batch · First Name · Last Name · Email Id · Gender (Male/Female) ·
  Date of Birth · Mobile (country code + number) · Unique ID · Blood Group · Country · State · City · Address · Pin Code ·
  School Name / Address · Dominant Side · Medical Condition. Later steps (from API): parent/secondary contacts, fee plan, kit size.
- Student profile (API): profile details, kit details, ID card, secondary contacts, training days, transfer history,
  assessment/performance sets, ranking, LTAD daily activities, competition tracker, payment history, tags.

### 11.3 Class (`#/center`) — a "center" relabelled via Branding → Center Name Title
- Table: photo · Class name · Status · Location · Batches · Student · Capacity · Action. Row actions: **Add Batches · Delete Class**.
- **Add Class**: Class Name · Point Contact Name · Code + Point Contact Number · Point Contact Email · Sport ·
  Court/Ground/Pitch Type · Number of Courts/Grounds/Pitches · Status · Country · State · City · Address 1 · Address 2 · Zipcode ·
  View In Map (GPS pin).
- **Add Batch**: Batch Name · Start Date · End Date · Training Days (M T W T F S S) · Timings per day (from–to) · Coach(s) ·
  Batch Capacity · Status · Batch Description.

### 11.4 Roles (`#/role`)
- Table: Role Name · Role Type · Actions (edit, delete). Existing: Owner, Admin, Manager, Cluster Manager (Head Office);
  Center Manager (Class); Student (Student).
- **Add/Edit Role**: Role Type (*Head Office* – administrative; *Class* – premise coaching roles) · Role Name.
  Per-role module permissions come from `entitlement_granularization` (module question list per role).

### 11.5 Staff
- Table: Staff Member · Role (Role Type).
- **Add Staff**: Role Type · Role · Email Address · Mobile (code + number) · First Name · Last Name · Date Of Joining ·
  Qualifications · Profile.

### 11.6 Messaging (`#/messaging`)
- Inbox ("Notification not available" when empty) + compose (edit icon).
- **New Message** wizard: *Send to*: Academy / Class / Batch / Student-Staff → Next → template + mode (push / SMS / WhatsApp /
  email per `messaging/GetModeOfNotifyList`, `GetSMSTemplateList`, `SendWhatsAppNotification`).

### 11.7 Posts
- **Create Posts**: Post Title · Author (logged-in user) · Posted Date · Photo / Video · Post Content · Post Status · Post To (audience) → send.

### 11.8 Coaching Drills (`#/coaching-drill`)
- Categories list + "Add Drills". **Create Coaching Drill**: Name · Objective · Setup · Add file/image/video · Instructions/Description ·
  Coaching Points · Category (from Activity Categories, e.g. Age, Dribble) → Save.
- Related routes: lesson-plans, session-tracker.

### 11.9 Reports (academy level)
| Student | Attendance | Performance |
|---|---|---|
| Deregistered Student | Attendance Tracker | Assessment Report |
| User Login Stats Report | In/Out Report | Student Tags Report |
| Kit Size Report | Session Tracker | Activity Status Report |
| Student Refferal Report | Student Session Report | Compliance Tracking |
| Renewal Report | Session Insight Report | Athlete Daily Activities |
| Communication History Report | | |
| Survey Report | | |
| Batch Allocation Report | | |

Report filters / columns captured:
- **Attendance Tracker** (`#/attendance-tracker`): Select Staff / Student · Class · Status · From Date · To Date → Go (monthly grid).
- **In/Out Report** (`#/Reports?report_type=Attendance Report`): Staff/Student · Class · Status · From · To → Go.
- **Session Tracker** (`#/session-tracker`): Select User · Class · Month and Year.
- **Student Session Report** (`#/session-report`): filter "Remaining Session ≤ n", Search; columns Name · Contact · Class Name ·
  Batches · Billing · Attended Session · Per session Amount · Invoice Amount · Available Session · Remaining Session · Parent Name ·
  Parent Contact · Last Fee Paid Amt · Last Payment Date.
- **Assessment Report** (`#/Assessment/reports?type=1`): searchable list of assessments → per-student scores / leaderboard.
- **Compliance Tracking** (`#/LTADReports?type=compliance`): Search Student · Month and Year (LTAD plan compliance).
- **Athlete Daily Activities** (`#/LTADReports?type=daily`): Go To Date · Search Student.

---

## API surface (from the web bundle; controller/action, ~280 endpoints)
attendance: AddSingleAttendance, GetMonthlyAttendanceView, TodaysLiveAttendance, UnsetSingleAttendance
authentication: AuthenticateUser, ForgotPassword, GetAccessToken, Session, SetPassword, UserAuthenticate, VerifyCaptcha
authorization: Authorization
batch: GetAddBatchFormDetails, GetBatchDetails, GetBatchsByAcademy, GetOrganizationCenterBatches, SetDefaultBatch, UpdateBatchDetails
center: AddCenter, CheckCenterName, DeleteCenter, GetCenterDetails, GetCenterListByName, GetCenterListWithGroups, IsAllowDeleteCenter, UpdateCenter
competition_tracker: GetCompetitionsGraphDetails
dashboard: GetAcademyAgeCount, GetAcademyAllCenterAttendance, GetAcademyAllCenterPaymentDetails(New), GetAcademyAllCenterStudentAgeCount,
  GetAcademyAllCenterStudentHeadCount(Donut/Table), GetAcademyAllCenterUtilization, GetAcademyAttedance, GetAcademyCenterUtilization,
  GetAcademyStudentHeadCount, GetAssessmentConsolidatedRankingDetails, GetAttendanceReport(ParentLevel), GetCenterUtiizationGraphDetails,
  GetChildAcademyAgeCount, GetChildAcademyDetailsForDashboard, GetChildAcademyStudentHeadCount(Average), GetChildAcademyUtilizationDetails,
  GetChildAcademyUtilizationFacetNevigation, GetPlayerPerformanceStatistics, GetSessionsAttendedReport
delete_organization: DeleteAcademyData, DeleteAllRequest, DeleteAttendanceAndSession, DeleteCalendarRelatedActivities, DeleteCoachingDrill,
  DeleteCompetitionData, DeleteDashboard, DeleteInjuryLog, DeleteLessonPlanMaster, DeleteMaster, DeleteParentChildMappingRelation,
  DeletePaymentsAndInvoicesAndFeePlans, DeletePerformanceMaster, DeletePlayerData, DeletePost, DeleteRankingAndTaggingData, DeleteStaff,
  DeleteVideoConfMaster, DeleteWorkoutMaster
entitlement: AccessControlQuestionsByRoleType, AddAccessControlDetails, GetAcadmeyDashboardEntitlement, GetAccessControlDetailsByRoleID,
  GetAttendanceConfiguration, GetPaymentFlagDetails, SetPaymentFlagDetails, UpdateAccessControlDetails
entitlement_granularization: AddAccessControlDetails, GetAccessControlDetailsByRoleID, GetOrganizationModulesQuestionsList,
  GetOrganizationModulesStatus, GetRoleEntitlementDetails, UpdateAccessControlDetails
feesetup: DeleteFeePlan, GetFeeSetup, GetOrganizationFeeSetup, IsAllowDeleteFeePlan, SetFeeSetup, UpdateFeePlanStatus
general: GetCities, GetCountries, GetScoreType, GetSportType, GetStates
groups: GetGroupDetailsByOrg
messaging: GetCustomerUnreadMsgCount, GetMessageTemplateList, GetModeOfNotifyList, GetRecepientTypeList, GetSMSTemplateList,
  SendWhatsAppNotification, UpdateCustomerNotificationFlag
mobile: AddOrganizationFormData, AddPerformance, AddPlayerForm2Data, AddRoleEntitlementDetails, GetAcademyAndPlayerKitDetails,
  GetEditPlayerFeesDetails, GetEditPlayerGroupFormData, GetPlayerPaymentHistory, SendAddPlayerForm2Data, SendNotification,
  UpdateStaffProfile, UpdateStudentProfile, ViewPerformanceSet
news: AddAcademyNewsAttendees, AddAcademyNewsNotification, DeleteAcademyNewsNotification, GetAcademyNewsNotification(Details),
  SendEmailNewsNotification, UpdateAcademyNewsNotification
org_terms_and_conditions: Add/Update/Publish OrgTermsAndConditionsDetails, GetOrgCurrentTermsAndConditionsDetails,
  GetOrgTermsAndConditionsHistory, UpdateOrgTermsAndConditionsFlagDetails
organization: AddOrganization, GetOrganizationDetails, GetOrganizationsList, UpdateOrganization
parent_child_academy: GetChildOrganizations, GetOrganizationListToTransferPlayer
payment: AcceptPlayerPayment, AddTaxDetails, CreateNewPaymentLink, GenerateSubscriptionDates, GetAcademyTaxDetails,
  GetParentOrg(Partial)PaymentReportDetails, Get(Partial)PaymentReportDetails, GetSalesOrderDetails, ReceiptDownload, RevertWriteoff,
  SendPaymentLink, SetAcademyInvoiceTemplateFromParent, SetAcademyTaxDetailsFromParentAcademy, UpdateNextPaymentDueDate, UpdatePlayerFeesDetails
performance: AddPerformanceParameterScore, AddPerformanceSet, AddPerformanceSetParameter, DeletePerformanceParameterAllDetails,
  DeletePerformanceSet, DeleteScoreDetails, GetAssesmentPerformanceSetScore, GetCalculatedParameterMaster, GetCascadedAssessmentList,
  GetParameterScoreList, GetParameterScoreSetDetails, GetPerformanceAssessment(ForDropdown), GetPerformanceParameterForAssessment,
  GetPerformanceParameterList, GetPerformanceSetList, IsAllowDelete…, SearchAreaName, UpdateFitnessQuotient, UpdateParameterScore,
  UpdatePerformanceParameter, UpdatePerformanceSet
player: AddPlayerDetails, AddPlayerFeesSetup, AddPlayerSecondaryContacts, BulkDeActivePlayer, BulkDeRegisterPlayer, ChangePlayerCenterAndGroup,
  DeRegisterPlayer, Delete(Organization)(Bulk)Player, DeletePlayerExistingFeePlan, GetAssessmentProgressReportAtParentLevel,
  GetDeregisteredPlayersDetails, GetKitDetails, GetOrganizationPlayerList(New), GetPlayerAssessment(…), GetPlayerDeRegisterFormDetails,
  GetPlayerDetails, GetPlayerIdenticardDetails (ID card), GetPlayerKitDetails, GetPlayerLTADDailyActivities, GetPlayerList(…),
  GetPlayerPerformanceRanking, GetPlayerPerformanceSet, GetPlayerProfileDetails, GetPlayerSecondaryContacts, GetPlayerTrainingDays,
  GetPlayerTransferHistory, GetPrivilagePlayerDetails, PlayerProfileCompetitionTracker, Searchplayer, TransferPlayer, TransferSinglePlayer,
  UpdatePlayerDetails, UpdatePlayerGroup, UpdatePlayerInvoiceDate, UpdatePlayerPerformanceConsoliatedRanking, UpdatePlayerTrainingDays
player_rating: GetRankingLabelAndRatingLabelAndCategory, SetPlayerOverallRating
preferences: Getpreferences, Setpreferences
ranking_tracker: GetPlayerRankingSummaryDetails
report: GetActivityAssessmentReportList, GetChildLevelMonthlyStudentCount, GetCompetitionTracker, GetPlayerAssessmentReport,
  GetPlayerConsolidatedRankingAssesmentReport, GetPlayerDeregistrationReport, GetPlayerReport, GetReportPublishList
request: AddDeleteRequest, GetDeleteRequestList, GetExpenseRequestDetails, GetExpenseRequestList, GetPendingExpenseDetails,
  UpdateDeleteRequestStatus, UpdateExpenseRequestStatus
role: CheckRoleDetails, DeleteRole, GetRole, IsAllowDeleteRole, SetRole
tag: AddPlayersTagsDetails, DeletePlayerAllTag, DeletePlayerTag, GetTagListByTagName, GetTagsDetailsByPlayer
termsofuse: GetTermsOfUse
user: AddUser, AssignRole, DeleteStaffMember, DeregisterStaffMember, GetAllUserList, GetCenterUserList, GetQualificationDetails, IsAllowDeleteUser, SearchUser
web_bulkupload: GetOrganizationPlayerBulkUploadFilesDetails, UploadBulkPlayerData, UploadBulkPlayers
webadmin: AddBulkPlayerDataValidation, ComparePlayerAssessmentScore, DeRegisterBulkStudents, GetAssessmentListWithCommonSet,
  GetForeignKeyDetails, GetPlayerScoringForeignKeyDetails, ImportBulkPlayerPerformanceScore, ImportBulkPlayerTempTable

## Front-end modules (73 lazy-loaded feature modules)
attendance-tracker, batch-allocation-report, carry-over-report, academy-profile, academy-survey-report, activity-status-report,
equated-monthly-income-report, initial-survey-report, semester-renewal-report, communication-history-report, fee-exemption-report,
payment-extension-report, ranking-tracker, test-bank, academy-set-parent-setting, lesson-plans, academy-settings, assessment,
student-profile, cheque-tracker, messaging, news, request, cascade-activities, deregister-student, report-card, student-ranking,
chart (dashboard), student-list, center, coaching-drill, dashboard-entitlement, activity-categories, competition-rank-master,
overall-player-rating, set-report-criteria, terms-conditions, competition-tracker-report, assessment-report-card, assessment-scores,
coaching-progress-report, nsa-assessment-report, progress-report, performance-summery-report, login, ltad-reports, kit-size,
revenue-registration-date-report, role, session-tracker, staff, in-out-report, fees-reports, tag-report, bulk-file-upload,
revenue-report, scoring, payment-tracker, report-engine, expense, fee-plans, grade-level-report, player-refferal-report,
publish-reports-list, ranking-tracker-report, report-page-dashboard, sales-order-report, setuptournament, social-media-integration,
student-fees-plan-report, tax-setting, terms-and-condition-acceptance-report, user-login-stats-report

## Top-level routes
login (+ white-label logins), student, academyDetails, center, role, staff, FeePlans, Assessment, Reports, dashboard, posts, messaging,
academySettings, cheque-tracker, request, expense, DeregisterList, FeesReports, LTADReports, attendance-tracker, report,
payment-tracker, competition-tracker, lesson-plans, coaching-drill, session-tracker, session-report, logout, forgot, resetPassword,
organization_selector(/:organization_id), register, individual-report, trend-report, terms-of-use, data-deletion-req,
deletion-request-listing, individual-report-nsa, tournaments, asignCoachList

## Deferred: Fees (decided 2026-11-19)
Everything fee-related is built **last**: fee plans, fee setup per student, payments (online + manual), payment links, partial
payments, receipts/invoices, tax settings, cheque tracker, write-off, payment extension, fee exemption, sales order, EMI,
revenue share, session-based billing and all Fees reports. Before starting it, enable "Enable Online Payments of Fees" on one
academy in Acadware and capture those screens. Until then, no fee code, tables or endpoints; leave a `fees` package empty.

## Not yet captured (need a follow-up pass)
- Screens hidden for this tenant because the module is off: **Fee Plans, Payment Tracker, Cheque Tracker, fee reports**
  (visible only at primary level as report names). Need an academy with online fees enabled, or enable it temporarily.
- Student profile page (opens on row click; layout not captured — API list above shows its sections).
- Add Student steps 2+, Bulk import file format, Request filter options, Dashboard → Performance tab,
  Assessment → Reports/Leaderboards detail, Lesson plans, Tournaments, Competition tracker, Expense detail/approval dialog.
- Staff mobile app & parent app (separate apps; not part of aw-admin).

---

## Mapping to the Athleet backend modules
Acadware's model is **organization → affiliated academy → center ("Class") → batch → student**. Ours is
**AJSDP: zone → cluster → school → batch** and **UKSC: academy location → batch**. Mapping:

| Acadware feature | Athleet module | Status |
|---|---|---|
| Login (email/mobile + password/OTP), forgot/reset, captcha | core/auth | planned |
| Org selector, primary vs affiliate mode, academy switcher | core/program + iam scopes | **gap** – add org/academy switch + "act as academy" mode |
| Roles (Head Office / Class types), staff, entitlements per module | core/iam | planned; add role-type + module-permission matrix |
| Branding preference (colours, currency, date format, center label) | core/program settings | **gap** – add white-label branding settings |
| Notification settings (13 types) | core/notification | partly planned (absence, fees, OTP); **add** birthday, post, cheque, calendar, deregistration, batch-capacity, staff-rating, self-registration |
| Social media handles, mobile app links | core/program settings | **gap** |
| Terms & Conditions with versions + acceptance report | core/consent | planned (consent); add versioned T&C + acceptance report |
| Data deletion request + listing | core/consent | planned |
| Dashboard: headcount, MoM, age/gender, locations map, utilization, live attendance | ajsdp/uksc dashboard | planned; add **utilization (capacity vs used)** and **live staff in/out** |
| Students: list, active/submitted, add, bulk import, bulk actions, ID card, tags, injury log, transfer, history, deregistration | ajsdp/student, registration; uksc/player, admission | planned; **add** ID card, tags, injury log, transfer between academies, student history, unique-ID generator |
| Class/center + batches (days, timings, coaches, capacity) | ajsdp/org + schedule; uksc/academy + batch | planned |
| Attendance (staff selfie in/out, student attendance, back-dating rule, device ID) | ajsdp/uksc attendance | planned; add selfie + device-ID + backdate config |
| Session tracker / student session report (session-based billing) | fees + attendance | **gap** – session-pack billing not in our spec |
| Assessment test bank (areas, parameters, unit, numeric/dropdown, comments rule), scores, report cards, leaderboards, fitness quotient | ajsdp/uksc assessment + reportcard | planned; mirror test-bank → parameters model |
| Overall 5-star player rating, rank labels | uksc/observation | planned (UKSC); add to AJSDP if wanted |
| Competition / rank master, competition tracker, tournaments, ranking tracker | — | **gap** |
| LTAD daily activities + compliance tracking, cascade activities, activity categories | — | **gap** |
| Coaching drills, lesson plans | — | **gap** |
| Fee plans, payments, payment links, partial payments, write-off, cheque tracking, sales orders, EMI, extensions, exemptions, tax, revenue share | ajsdp/uksc fees | planned (Razorpay + manual); **add** cheque tracker, write-off, extension, exemption, revenue share, sales order |
| Expense (staff claims, approve/reject) + Request (delete & expense approvals) | — | **gap** – add `expense` + `request` modules |
| Posts (news with media, RSVP attendees) | ajsdp/uksc announcement | planned; add RSVP |
| Messaging (audience wizard, SMS/WhatsApp/push templates) | core/notification | planned; **add** WhatsApp + templates |
| Reports (≈30) incl. kit size, referral, renewal, login stats, communication history, survey | dashboards/reports | mostly **gap** |
| Kit sizes per student | student/player | **gap** |
