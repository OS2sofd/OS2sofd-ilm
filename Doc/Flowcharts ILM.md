### Flow 1 — Medarbejderen registrerer sig selv

```mermaid
graph LR
    A["Medarbejderen registrerer sig selv<br>med MitID og accepterer betingelserne"] --> B["Lederen godkender"]
    B --> C["Administratoren godkender og vælger<br>enhed, Arketype og start- og slutdato"]
    A -.->|administratoren kan godkende uden om lederen| C
    C --> D["Fælles integrationsflow<br>SOFD, AD og Rollekatalog"]
```

Status følger godkendelserne: *Afventer godkendelse* → *Leder har godkendt* → *Admin har godkendt +
Afventer at blive integreret*. Enten lederen eller administratoren kan afvise undervejs, og så ender
medarbejderen i status *Afvist*. Godkendte lederen ikke først, får virksomhedens ledere en mail om, at
administratoren har godkendt.

### Flow 2 — Administratoren opretter medarbejderen

```mermaid
graph LR
    A["Administratoren opretter medarbejderen<br>med virksomhed, enhed, Arketype<br>og start- og slutdato"] --> B["Medarbejderen logger ind<br>med MitID"]
    B --> C["Accepterer betingelserne"]
    C --> D["Fælles integrationsflow<br>SOFD, AD og Rollekatalog"]
```

Medarbejderen er godkendt fra start (*Admin har godkendt + Afventer at blive integreret*), men der sker
ingen integration ved oprettelsen. Det er accepten af betingelserne, der sætter den i gang.

### Flow 3 — Lederen opretter medarbejderen

```mermaid
graph LR
    A["Lederen opretter medarbejderen<br>på virksomhedens vegne"] --> B["Administratoren godkender og vælger<br>enhed, Arketype og start- og slutdato"]
    B --> C["Medarbejderen logger ind med MitID<br>og accepterer betingelserne"]
    C --> D["Fælles integrationsflow<br>SOFD, AD og Rollekatalog"]
```

Oprettelsen tæller som lederens egen godkendelse (*Leder har godkendt*), så medarbejderen lander direkte
på administratorens godkendelsesliste. Som i Flow 2 er det accepten af betingelserne, der sætter
integrationen i gang.