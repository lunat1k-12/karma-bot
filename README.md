# Local Run

### Pre requirements
Java - AdoptOpenJDK-11 <br/>
Docker <br/>
Registered Telegram bot token

### Docker

You need to create a DB with the next command:

`docker run --name chat -p5434:5432 -e POSTGRES_PASSWORD=1234 -e POSTGRES_USER=chat -e POSTGRES_DB=chat -d postgres`

### System variables

System variable with telegram bot token needed:
`TEST_KARMA_BOT_TOKEN`

### Run command

`./gradlew bootRun --args='--spring.profiles.active=local`