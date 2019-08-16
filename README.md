# epyc

Administers games of [Eat Poop You Cat](https://boardgamegeek.com/boardgame/30618/eat-poop-you-cat) over the Telegram messaging service. You can try it [here](https://telegram.me/epyc-bot).

Includes an `app.json` and `Procfile` for use with Heroku.

## Installation

Create a database: `psql postgres -c"CREATE DATABASE epyc"`

Create a `.lein-env` in the project root, e.g.:
```
{:telegramtoken "Get one of these from @BotFather!"
 :dbspec        "postgresql://localhost:5432/epyc"
 :dbtestspec    "postgresql://localhost:5432/epyctest"
 :turnspergame  "10"}
```

## Usage

`lein run`

## Running Tests

Create a test db: `psql postgres -c"CREATE DATABASE epyctest"`

Then: `lein test`
