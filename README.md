# Project Overview
- **Project Name**: OS2ilm
- **Description**: OS2ilm is a municipal solution for managing non-salaried employees / consultants. The purpose of this solution is to support secure, practical and fast management of consultants access to municipal it-infrastructure (e.g. active directory accounts).
---

# Developer Guide
This section will help you get the application running locally for development and testing purposes. It assumes basic familiarity with Java, Docker, and Maven.

## Prerequisites
Before you begin, ensure you have the following installed:
- Java JDK 21
- Docker
- Maven

## Development Guidelines
- **Branch Naming**: Branch names follow the Jira ID naming convention: `KON-XXXX`.
- **Pull Requests**: Once a branch is moved to the "to be reviewed" column in Jira, a pull request should be created for it.

---
## Hosts File Configuration
Ensure your `/etc/hosts` (Linux & macOS) or `C:\Windows\System32\drivers\etc\hosts` (Windows) file includes an alias for your local environment. Example alias:
```
127.0.0.1   os2ilm.test
```
This allows the application to resolve the local alias `os2ilm.test` properly.

## Database Setup with Docker
The application requires a MariaDB database with a schema named `ilm`.

### Step 1: Start the Docker Container
You can make a database using Docker. Here's a command you can use for MariaDB:
```
docker run --name OS2ILM_CONTAINER \
  -e MYSQL_ROOT_PASSWORD=YOUR_PASSWORD \
  -p 3306:3306 \
  -d mariadb:10.11
```
This creates and starts a MariaDB container:
- Accessible at `localhost:5432`
- Container name: `OS2ILM_CONTAINER`
- Password: `YOUR_PASSWORD`

Now we can go ahead and create a database in the container. Exec into the container using the following command:
```
docker exec -it OS2ILM_CONTAINER bash
```
When you're in bash you can use the command:
```
mariadb -u root -p
```
Now you're in a standard SQL environment, where basic SQL can be used to create a new database:
```
CREATE DATABASE ilm;
```
You now have a running container with a database that you can use for development.

## Integration Setup
The application has a few integrations, i.e., other applications that it communicates with through api calls.

### Application settings
The below described integrations require an ApiKey to properly run. Without an ApiKey they won't work as you won't have access to the APIs we call in each of the integrations.
The ApiKeys should be placed in a file called `application.development.properties`. This file is excluded from version control and overwrites values in the standard
`application.properties` to ensure that our ApiKeys stay hidden for security reasons. After you have created the `application.development.properties` file you need to insert the following code:
```
### Sofd integration ###
os2ilm.sofd.url=https://demo.sofd.io
os2ilm.sofd.apiKey=

### RoleCatalog integration ###
os2ilm.rc.url=https://demo.rollekatalog.dk
os2ilm.rc.apiKey=
os2ilm.rc.allowAutomaticUserRoles=true

### Datafordeler ###
os2ilm.integrations.cvr.apiKey=

### Active Directory ###
#framework.ad.devMode=false
#framework.ad.url=http://localhost:8833/
#framework.ad.cvr=
#framework.ad.apiKey=

### Developer settings ###
logging.level.dk.digitalidentity=DEBUG

### Scheduled tasks ###
os2ilm.scheduled.updateActiveDirectoryEnabled=true
v.scheduled.updateActiveDirectoryCron=0 0/1 * * * *
#v.scheduled.cleanUpOldUsersEnabled=true
#os2ilm.scheduled.cleanUpOldUsersCron=0 0/1 * * * *
os2ilm.scheduled.sofdSyncCron=0 0/1 * * * *
os2ilm.scheduled.rcSyncCron=0 0/1 * * * *
os2ilm.scheduled.sofdAffiliationCron= 0 0/1 * * * *
os2ilm.scheduled.rcAssignCron= 0 0/1 * * * *
```

The scheduled tasks are as the name suggests tasks that run based on a given schedule. You can configure the values to fit your desire.

#### ApiKeys
As mentioned it's unsafe to expose our ApiKeys, so they have been omitted in the code above. You can make your own ApiKeys for OS2sofd and OS2rollekatalog
by going to their demo websites:

Sofd: https://demo.sofd.io/  
Rollekatalog: https://demo.rollekatalog.dk/

On there you can create new clients with given access rights. The client in sofd needs write access and the client in rollekatalog needs administrator access. The receive an ApiKey for
Active Directory and Datafordeler you need to contact one of our managers:

Brian Storm Graversen: **bsg@digital-identity.dk**  
Malthe Plenge Overgaard: **mpo@digital-identity.dk**

#### OS2sofd
We have a sofd integration running to read accessible organisation units into OS2ilm and also write our consultants into OS2sofd.
This is necessary to correctly place our consultants during creation so we know which unit/leader is responsible for the consultant in the municipality.

#### OS2rollekatalog
We have a rollekatalog integration running to read accessible roles/rights into OS2ilm. Municipal administrator can give these roles
to consultants during creation or when approving them.

#### Active Directory
We have an active directory integration to ensure that for each consultant in OS2ilm we create an appropriate account with the correct credentials in AD. This also allows for
correct enabling and disabling of active directory accounts whenever necessary (for example when their period expires).

#### Datafordeler
We have a datafordeler integration running to ensure that users and administrators don't accidentally enter a cvr-number that does not exist.



## Running the Application
With a working database you can now run the application with the following command:
```
mvn clean install spring-boot:run
```
If you wish to run the application without tests you can use this command:
```
mvn clean install -DskipTests spring-boot:run
```
---
