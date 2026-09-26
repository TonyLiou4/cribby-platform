## Local Database Setup

The `dev` Spring profile (`src/main/resources/application-dev.yml`) expects a
local PostgreSQL database named `roommate_marketplace` on `localhost:5432`,
with `spring.jpa.hibernate.ddl-auto=update` doing incremental schema updates
from the JPA entities. If PostgreSQL is ever reinstalled and the local data
is wiped, rebuild it with the two scripts below.

**Prerequisites:** the `DATABASE_PASSWORD` and `JWT_SECRET` environment
variables must be set the same way you normally run this app locally
(shell profile, IntelliJ run config, etc.) — this rebuild doesn't change
that.

1. Create the database (run against the default `postgres` maintenance DB):

   ```bash
   psql postgres -f scripts/create-local-database.sql
   ```

2. Create all tables, keys, and indexes from the current entity model:

   ```bash
   psql -d roommate_marketplace -f src/main/resources/db/local-init.sql
   ```

3. Start the app as usual:

   ```bash
   ./gradlew bootRun
   ```

   With `ddl-auto=update`, Hibernate will see a schema that already matches
   the entities and start cleanly without altering anything.

Both scripts are safe to re-run — they only create what's missing.

If you ever add or change an `@Entity`, update
`src/main/resources/db/local-init.sql` to match, so a future rebuild stays
in sync with the code (this project has no Flyway/Liquibase migrations —
`ddl-auto=update` plus this script is the whole story for local dev).
