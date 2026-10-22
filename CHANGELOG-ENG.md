# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Calendar Versioning](https://calver.org/) in the format `YYYY.R.PATCH`.

## [Unreleased]

### Upgrade Notes
- **Required:** Set `os2ilm.activeDirectoryConfig.targetOu` in your deployment config (e.g. `OU=Staff,OU=Users,DC=example,DC=com`). Without it, AD account creation will be rejected by the v3 backend.
- **Required:** Set `os2ilm.activeDirectoryConfig.rootGroup` in your deployment config (e.g. `ilm-root`). Without it, AD group sync will fail.
- **Required:** Set `os2ilm.baseUrl` to the public base URL of the application (e.g. `https://ilm.example.com`). Without it, sending email invitations returns an error.
- **Optional:** Set `os2ilm.activeDirectoryConfig.upnDomain` (e.g. `example.com`) if OS2ilm should set `userPrincipalName` on new AD accounts. Leave it unset if the UPN is handled elsewhere.
- **Required if the AD agent restricts writable attributes:** add `displayName` and `userPrincipalName` to the agent's `AllowedWritableAttributes`, otherwise the agent rejects every account creation.
- **Optional:** Set `os2ilm.activeDirectoryConfig.cprWithDash=true` if your AD environment requires CPR numbers to be written with a dash (e.g. `010190-1234`). Defaults to off, which keeps the existing behaviour. Only applies to accounts created from this point on.
- **Optional:** Set `os2ilm.customer.lockNameFromMitId=true` to make the name field on the registration form read-only when MitID supplies a name. Defaults to off, which keeps the field editable.

### Fixed
- CPR numbers containing letters were rejected when creating or editing a leader; only the length is checked now
- Name on the registration form is taken from MitID rather than from the browser when the field is locked, so the lock cannot be bypassed by posting directly
- Failed registrations reported success and sent the user to a blank page; an error is now shown instead
- "Registreringsårsag" field in the registration flow no longer required
- Authenticated users visiting the invitation landing page are now redirected to the dashboard instead of seeing the login page
- Invitation landing page is now accessible without authentication
- Logout button non-functional on pages using the empty navbar (jQuery dependency replaced with vanilla JS)
- No success feedback shown after sending an invitation email
- Missing footer scripts fragment on invitation form page causing toastr to be undefined

### Added
- Product description (`Doc/Produktbeskrivelse.md`) for decision makers and new administrators, covering the business case, the creation flows, the integrations and the lifecycle
- Name and CPR number on the registration form are pre-filled from MitID; the name field can be made read-only via configuration (`os2ilm.customer.lockNameFromMitId`). If MitID reports name and address protection, the name field is left empty and editable regardless of the setting
- CPR numbers can be written to Active Directory with a dash via configuration (`os2ilm.activeDirectoryConfig.cprWithDash`)
- Daily email reminder about employees awaiting approval, sent to leaders and to the general inbox (`os2ilm.scheduled.sendAwaitingApprovalEmailNotifications*`)
- AD accounts get `displayName` (the employee's name) and `userPrincipalName` (AD username plus the configured UPN domain) set on creation
- Leaders can now be created with a CPR number and log in with MitID (personal) using that CPR number
- Email invitation flow: admins can invite consultants by email to self-register via MitID on a new landing page
- New CMS modules for invitation email body (`cms.admin.invitation`) and landing page content (`cms.landingpage.*`)
- Logo displayed on the MitID login/discovery landing page in addition to the main navbar
- Admins can retry or delete failed Active Directory queue items directly from the employee overview
- Reason/justification field on the registration form; reason is shown to leaders and admins during approval
- Phone number and/or registration reason can be made required on the registration form via configuration (`os2ilm.customer.require-phone`, `os2ilm.customer.require-registration-reason`); the registration form and its JS validator enforce the requirement dynamically

### Fixed
- Stop date was not pre-populated in the edit employee form
- Null pointer exception when AD response status field is absent; falls back to legacy `isSuccess()` check
- AD queue retry now correctly resets the attempt counter so retried items get the full retry quota
- Jackson deserialization error for `ADMessageRequest` objects read back from the queue

### Changed
- AD account creation now passes `targetOU` from config (`os2ilm.activeDirectoryConfig.targetOu`) to the backend (required by framework-activedirectory v3)
- AD group sync now passes `rootGroup` from config (`os2ilm.activeDirectoryConfig.rootGroup`) to the backend (required by framework-activedirectory v3)
- Upgraded `framework-activedirectory` from `2.1.2` to `3.0.0`
- Scheduled task execution time is logged as WARN when it exceeds the configured threshold (`os2ilm.scheduled.task.durationThreshold`, default 1 minute)
- All browser-side AJAX calls migrated from jQuery to native `fetch` API (no behavior change)

## [2026r1]

### Fixed
- Locking an employee now sets the stop date in ILM immediately and notifies SOFD


## [2026r1-2026-06-19]

### Fixed
- eBoks/Digital Post messages were silently dropped because message subjects contained `:`, which is not allowed by Digital Post — colons removed from all subjects and `EboksService` now strips and warns if a colon slips through
- `inheritPrivileges` flag was not being set from the business when updating a person via SOFD sync (only set on create, not update)
- Start date was blanked in the edit form for employees with a future start date due to a JS operator precedence bug in `startIsInPast`

## [2026r1-2026-05-08]

### Changed
- Switched to release-based versioning
- Added version display in navbar with update notification
