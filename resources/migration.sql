CREATE TABLE player (
  p_id INTEGER PRIMARY KEY,
  first_name VARCHAR (50) NOT NULL,
  last_name VARCHAR (50) NOT NULL,
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE game (
  g_id SERIAL PRIMARY KEY,
  status VARCHAR (10) NOT NULL DEFAULT 'active',
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_game ON game (g_id, status);

CREATE TABLE turn (
  t_id SERIAL PRIMARY KEY,
  p_id INTEGER NOT NULL,
  g_id INTEGER NOT NULL,
  status VARCHAR (10) NOT NULL DEFAULT 'unplayed',
  text_turn BOOLEAN NOT NULL,
  m_id INTEGER,  -- ID of telegram message
  text VARCHAR (255),
  filename VARCHAR (40),
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT turn_p_id_fkey FOREIGN KEY (p_id)
  REFERENCES player MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT turn_g_id_fkey FOREIGN KEY (g_id)
  REFERENCES game MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX idx_turn ON turn (t_id, status);
CREATE INDEX idx_game_turn ON turn (g_id);
CREATE INDEX idx_player_turn ON turn (p_id, status);

CREATE OR REPLACE FUNCTION update_row_modified_function_()
  RETURNS TRIGGER
AS
$$
  BEGIN
    -- ASSUMES the table has a column named exactly "m_at".
    -- Fetch date-time of actual current moment from clock, rather than start of statement or start of transaction.
    NEW.m_at = clock_timestamp();
    RETURN NEW;
  END;
$$
LANGUAGE 'plpgsql';

CREATE TRIGGER row_mod_on_player_trigger_
  BEFORE UPDATE
  ON player
  FOR EACH ROW
    EXECUTE PROCEDURE update_row_modified_function_();

CREATE TRIGGER row_mod_on_game_trigger_
  BEFORE UPDATE
  ON game
  FOR EACH ROW
    EXECUTE PROCEDURE update_row_modified_function_();

CREATE TRIGGER row_mod_on_turn_trigger_
  BEFORE UPDATE
  ON turn
  FOR EACH ROW
    EXECUTE PROCEDURE update_row_modified_function_();
