# Getting Started

## Get the code

Prerequisites: Git, SBT, Scala, Docker

1. Pull the code from github

    `git pull blah blah blah`

<br>
<br>

## Check for dependency updates

$name$ uses SemVer versioning to keep the project up-to-date with patch releases for it's dependencies. Minor and/or major releases are not automatically fetched so in order to check if there are any updates available the `sbt-updates` plugin has been installed. In order to check if any updates are available run the `dependencyUpdates` task in sbt.


<br>
<br>
<br>

# Setup Digital Ocean Server

Digital Ocean is not the only platform you can deploy this web app on, any cloud infrastructure that runs on Docker will work.

1. Start a new docker droplet from the Digital Ocean market.

2. Once it has started SSH onto the newly running droplet.

3. Install Nginx by running `apt install nginx`.

4. Setup nginx to map port `80` to port `4000` (the port the server runs on). Open `/etc/nginx/sites-enabled/default` in a text editor (vi or nano) and replace the `location /` block to look like this:

    ```
    location / {
        proxy_set_header   X-Forwarded-For \$remote_addr;
        proxy_set_header   Host \$host;
        proxy_pass         "http://127.0.0.1:4000";
    }
    ```

7. Update the firewall to allow nginx. `sudo ufw allow 'Nginx Full'`.

8. Add all the environment variables to the bottom of `~/.bashrc`. You can use [this](https://www.guidgenerator.com/) website to generate very secure passwords. Here is an example of the variables that need to be set:

    ```
    # Environment variables for Open Science Institute
    export ACCESS_SECRET=<some guid>
    export REFRESH_SECRET=<some guid>
    export POSTGRES_DB=$name;format="camel"$
    export POSTGRES_HOST=db
    export POSTGRES_PORT=5432
    export POSTGRES_AUTO_MIGRATE=true
    export POSTGRES_MAX_CONN=10
    export POSTGRES_DEBUG=true
    export POSTGRES_USER=$name;format="camel"$_owner
    export POSTGRES_PASSWORD=<some guid>
    export POSTGRES_WORKER=$name;format="camel"$_worker
    export POSTGRES_WORKER_PASSWORD=<some guid>
    ```

<br>
<br>
<br>

# Ways of Running

|   |**Database**	|**Server**	|**Frontend**|
|---|-----------|-------|--------|
|**Local Dev**	|DB Docker	| SBT - app/reStart|  yarn serve (vue)|
|**Local Stage**	|DB Docker	|Web Docker	|Web Docker|
|**Remote**	|DB Docker	|Web Docker	|Web Docker|



# Run locally

## Database

The database can be run in two ways: either by manually setting up postgres on your local (host) machine, or by running it inside docker (recommended).

### Local Postgres Server

TODO: fill this in!

<br>

### Docker (recommended)

1. (optional) Build the latest version of the database.
    WINDOWS: MAKE SURE ./02-persistence-pg/src/main/resources/db/init-db.sh IS LF AND NOT CRLF!

    `docker build -f Dockerfile.db -t <image>:<tag> .`

    _suggested image: __$docker_repo$/$name$_db___

    _suggested tag: __latest___

2. Start a new container from the $name$_db image.

    `docker run -d --env-file .env -p 5432:5432 --name $name$_db <image>:<tag>`

The database is now running inside docker and has created a database along with two users (the database owner and a worker) as defined in the `.env` file used in the command above.

The database can be accessed from your host machine using `localhost:5432` and the credentials defined in the `.env` file.

<br>
<br>

## Server & UI

There are 2 ways of running the server & UI locally: either run the server via SBT and the UI via Vue (do this during development), or run the server & UI together using docker-compose (do this if testing a deployment because it's how production deployment works).

### SBT & Vue (dev)

Make sure that the database is running (see above)

1. (optional) Ensure the `POSTGRES_HOST` env var is pointing to the correct location (it should be `localhost`). SBT uses the `.env` file to load environment variables which should already be setup to point to `localhost`. 

2. Start the SBT shell using `sbt` in the root of the project.

3. Start the server by runnning `app/reStart`.

    _NOTE: to stop the server you can use `reStop`_.

4. In another terminal navigate to the `web_ui` folder in the root of the project.

5. Start the Vue server using `yarn serve`.

The server and UI are now running in separate processes and will automatically reload if any files change. Open a browser and goto `localhost:8080`.

<br>

### Docker compose (testing)

1. (optional) Build the latest version of the server.

    `docker build -f Dockerfile.web -t <image>:<tag> .`

    _suggested image: __$docker_repo$/$name$_web___

    _suggested tag: __latest___

2. Ensure the `POSTGRES_HOST` env var is pointing to the correct location (it should be `db` assuming the docker-compose.yml file hasn't been changed)

3. Deploy the system locally using docker-compose from the root of the project.

    `docker-compose up -d`

<br>
<br>
<br>

# Deploy to DigitalOcean

## Github Actions (recommended)

Github actions will take care of building and publishing all docker images to Docker Hub. The database and server images are handled by separate workflows so that they can be deployed independently.

They workflows are triggered by pushing a tag called `web_x.x.x` (for the web server) or `db_x.x.x` (for the database) to the Github repo.

Once the workflow has successfully completed you just have to SSH onto the server and run `docker-compose up -d`.

<br>
<br>

## Manually

1. Make sure you have Docker install on you machine.

2. Navigate to the project root.

3. Build a new image using one (or both) of the command(s) below:

    `docker build -f ./Dockerfile.web -t $docker_repo$/$name$_web:<tag> -t $docker_repo$/$name$_web:latest .`

    or

    `docker build -f ./Dockerfile.db -t $docker_repo$/$name$_db:<tag> -t $docker_repo$/$name$_db:latest .`

4. Push the image(s) to docker hub (you must be logged in to docker hub for this to work):

    `docker push $docker_repo$/$name$_web:<tag>`

    or

    `docker push $docker_repo$/$name$_db:<tag>`

6. SSH into the server and run:

    `docker-compose up -d`
