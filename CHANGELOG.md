# Ændringslog

Alle væsentlige ændringer i dette projekt dokumenteres i denne fil.

Formatet er baseret på [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
og projektet følger [Calendar Versioning](https://calver.org/) i formatet `YYYY.R.PATCH`.

## [Unreleased]

### Opgraderingsnoter
- **Påkrævet:** Angiv `os2ilm.activeDirectoryConfig.targetOu` i din driftskonfiguration (f.eks. `OU=Staff,OU=Users,DC=example,DC=com`). Uden denne afvises oprettelse af AD-konti af v3-backenden.
- **Påkrævet:** Angiv `os2ilm.activeDirectoryConfig.rootGroup` i din driftskonfiguration (f.eks. `ilm-root`). Uden denne fejler synkronisering af AD-grupper.
- **Påkrævet:** Angiv `os2ilm.baseUrl` som applikationens offentlige base-URL (f.eks. `https://ilm.example.com`). Uden denne returnerer afsendelse af e-mailinvitationer en fejl.
- **Påkrævet, hvis AD-agenten begrænser skrivbare attributter:** tilføj `displayName` og `userPrincipalName` til agentens `AllowedWritableAttributes`, ellers afviser agenten enhver kontooprettelse.
- **Valgfrit:** Angiv `os2ilm.activeDirectoryConfig.upnDomain` (f.eks. `example.com`), hvis OS2ilm skal sætte `userPrincipalName` på nye AD-konti. Lad den være udeladt, hvis UPN håndteres andetsteds.
- **Valgfrit:** Angiv `os2ilm.activeDirectoryConfig.cprWithDash=true`, hvis dit AD-miljø kræver, at CPR-numre skrives med bindestreg (f.eks. `010190-1234`). Er som standard slået fra, hvilket bevarer den nuværende adfærd. Gælder kun for konti, der oprettes fra dette tidspunkt.
- **Valgfrit:** Angiv `os2ilm.customer.lockNameFromMitId=true` for at gøre navnefeltet på registreringsformularen skrivebeskyttet, når MitID leverer et navn. Er som standard slået fra, hvilket holder feltet redigerbart.
- **Påkrævet:** Angiv `os2ilm.customer.s3FileBasePrefix` (f.eks. `kommune/ilm/`) og `framework.s3fileshare.*`-indstillingerne (herunder `framework.s3fileshare.s3-file-share-api-key`). Vedhæftninger til mailskabeloner gemmes i S3 under dette prefix.
- **Automatisk:** De eksisterende CMS-mailtekster (`cms.employee.*`, `cms.admin.approved`, `cms.admin.invitation`, `cms.awaiting.approval.email*`) flyttes over i de nye mailskabeloner ved opstart, og `PLACEHOLDER_X`-pladsholdere konverteres til de nye `{x}`-pladsholdere. De gamle CMS-nøgler fjernes.

### Tilføjet
- Produktbeskrivelse (`Doc/Produktbeskrivelse.md`) til beslutningstagere og nye administratorer, der dækker business casen, oprettelsesflowene, integrationerne og livscyklussen
- Navn og CPR-nummer på registreringsformularen forudfyldes fra MitID; navnefeltet kan gøres skrivebeskyttet via konfiguration (`os2ilm.customer.lockNameFromMitId`). Hvis MitID rapporterer navne- og adressebeskyttelse, efterlades navnefeltet tomt og redigerbart uanset indstillingen
- CPR-numre kan skrives til Active Directory med bindestreg via konfiguration (`os2ilm.activeDirectoryConfig.cprWithDash`)
- Daglig e-mailpåmindelse om medarbejdere, der afventer godkendelse, sendt til ledere og til den generelle indbakke (`os2ilm.scheduled.sendAwaitingApprovalEmailNotifications*`)
- AD-konti får `displayName` (medarbejderens navn) og `userPrincipalName` (AD-brugernavn plus det konfigurerede UPN-domæne) sat ved oprettelse
- Ledere kan nu oprettes med et CPR-nummer og logge ind med MitID (personlig) ved hjælp af dette CPR-nummer
- E-mailinvitationsflow: administratorer kan invitere konsulenter via e-mail til selv at registrere sig med MitID på en ny landingsside
- Nye CMS-moduler til invitationens e-mailtekst (`cms.admin.invitation`) og landingssidens indhold (`cms.landingpage.*`)
- Logo vises på MitID-login-/discovery-landingssiden ud over i hovednavbaren
- Administratorer kan forsøge igen eller slette mislykkede Active Directory-køelementer direkte fra medarbejderoversigten
- Felt til årsag/begrundelse på registreringsformularen; årsagen vises for ledere og administratorer under godkendelsen
- Telefonnummer og/eller registreringsårsag kan gøres obligatorisk på registreringsformularen via konfiguration (`os2ilm.customer.require-phone`, `os2ilm.customer.require-registration-reason`); registreringsformularen og dens JS-validator håndhæver kravet dynamisk
- Det er muligt at slette en medarbejder i listen over medarbejdere ved hjælp af en slet-knap. Det medfører slettelse af deres AD-konto, hvis de havde et.
- Afviste medarbejder kan nu blive gen-godkendt og vises i listen over godkendte medarbejdere i stedet for listen over medarbejdere + en knap til at skjule dem.
- Listen over godkendte medarbejder viser nu virksomheden som medarbejderen er ansat for.
- Ny AD-konto navngivnings strategi per virksomhed.
- CVR-nummer kan nu blive redigeret på en eksisterende virksomhed.
- Mulighed for at bestille en mailkonto til medarbejderen i OS2sofd via OS2ilm.
- Ny administrationsside til mailskabeloner: emne og HTML-indhold på alle e-mail-/e-boks-notifikationer kan nu redigeres, og hver skabelon kan slås til og fra. Pladsholdere har danske navne (`{modtager}`, `{brugernavn}`, `{kodeord}`, `{dato}`, `{antal}`, `{link}`), og administratoren advares om HTML, der fjernes af sanitizeren
- Mailskabeloner kan have en vedhæftet fil (gemt i S3), som sendes med både e-mail og e-boks-beskeder. En erstattet vedhæftning gemmes i 7 dage, så e-boks-beskeder i køen stadig kan sendes, og fjernes derefter af en oprydningsopgave, der kører hver time (`os2ilm.scheduled.cleanUpEmailAttachmentsCron`)
- Administratorer kan sende en test-e-mail af en skabelon (med eksempelværdier i pladsholderne) til sig selv, før den gemmes. Der kan højst sendes én test-e-mail hvert 15. sekund

### Rettet
- CPR-numre med bogstaver blev afvist ved oprettelse eller redigering af en leder; nu kontrolleres kun længden
- Navnet på registreringsformularen hentes fra MitID i stedet for fra browseren, når feltet er låst, så låsen ikke kan omgås ved at poste direkte
- Mislykkede registreringer meldte succes og sendte brugeren til en blank side; der vises nu en fejl i stedet
- Feltet "Registreringsårsag" i registreringsflowet er ikke længere obligatorisk
- Autentificerede brugere, der besøger invitationssiden, omdirigeres nu til dashboardet i stedet for at se loginsiden
- Invitationssiden er nu tilgængelig uden autentificering
- Logout-knappen virkede ikke på sider med den tomme navbar (jQuery-afhængighed erstattet med vanilla JS)
- Ingen succesbesked blev vist efter afsendelse af en invitations-e-mail
- Manglende footer-scripts-fragment på invitationsformularen medførte, at toastr var udefineret 
- Stopdato blev ikke forudfyldt i formularen til redigering af medarbejdere
- Null pointer exception, når AD-svarets statusfelt mangler; falder tilbage til den gamle `isSuccess()`-kontrol
- Gentagne forsøg på AD-køelementer nulstiller nu forsøgstælleren korrekt, så elementer, der prøves igen, får hele deres kvote af forsøg
- Jackson-deserialiseringsfejl for `ADMessageRequest`-objekter, der blev læst tilbage fra køen
- CMS pladsholdere er ikke længere hardcoded, så nu vises der kun dem der rent faktisk bruges i skabelonen.

### Ændret
- Oprettelse af AD-konti sender nu `targetOU` fra konfigurationen (`os2ilm.activeDirectoryConfig.targetOu`) videre til backenden (kræves af framework-activedirectory v3)
- Synkronisering af AD-grupper sender nu `rootGroup` fra konfigurationen (`os2ilm.activeDirectoryConfig.rootGroup`) videre til backenden (kræves af framework-activedirectory v3)
- Opgraderet `framework-activedirectory` fra `2.1.2` til `3.0.0`
- Udførselstiden for planlagte opgaver logges som WARN, når den overstiger den konfigurerede tærskel (`os2ilm.scheduled.task.durationThreshold`, standard 1 minut)
- Alle AJAX-kald i browseren er migreret fra jQuery til den native `fetch`-API (ingen adfærdsændring)
- Ny styling på registreringssiden.
- Mailtekster redigeres ikke længere på CMS-siden, men på den nye side til mailskabeloner. Mailenes emner, som tidligere var hardcoded, kan nu også redigeres
- Emnet på skabeloner, der kan blive sendt via Digital Post, må ikke indeholde `:`
- Opgraderet `emailService` fra `1.1.1` til `1.2.0` og tilføjet `framework-s3fileshare` `2.0.0`

## [2026r1]

### Rettet
- Låsning af en medarbejder sætter nu straks stopdatoen i ILM og giver SOFD besked


## [2026r1-2026-06-19]

### Rettet
- eBoks-/Digital Post-beskeder blev stille droppet, fordi beskedemner indeholdt `:`, som ikke er tilladt i Digital Post — kolon er fjernet fra alle emner, og `EboksService` fjerner nu kolon og logger en advarsel, hvis et alligevel slipper igennem
- Flaget `inheritPrivileges` blev ikke sat fra virksomheden ved opdatering af en person via SOFD-synkronisering (kun ved oprettelse, ikke ved opdatering)
- Startdato blev blankt i redigeringsformularen for medarbejdere med en fremtidig startdato på grund af en JS-fejl i operatorprioritet i `startIsInPast`

## [2026r1-2026-05-08]

### Ændret
- Skiftet til udgivelsesbaseret versionering
- Tilføjet visning af version i navbaren med opdateringsnotifikation
