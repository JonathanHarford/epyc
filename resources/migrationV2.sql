CREATE TABLE player (
  p_id SERIAL PRIMARY KEY,
  email VARCHAR (50) UNIQUE NOT NULL,
  handle VARCHAR (50) NOT NULL,
  del BOOLEAN NOT NULL DEFAULT FALSE,
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_player ON player (p_id, del);
CREATE INDEX idx_email ON player (email, del);

CREATE TABLE game (
  g_id SERIAL PRIMARY KEY,
  del BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR (10) NOT NULL DEFAULT 'active',
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_game ON game (g_id, del);

CREATE TABLE turn (
  t_id SERIAL PRIMARY KEY,
  p_id INTEGER NOT NULL,
  g_id INTEGER NOT NULL,
  status VARCHAR (10) NOT NULL DEFAULT '',
  turn_type BOOLEAN NOT NULL, 
  del BOOLEAN NOT NULL DEFAULT FALSE,
  c_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  m_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT turn_p_id_fkey FOREIGN KEY (p_id)
  REFERENCES player MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT turn_g_id_fkey FOREIGN KEY (g_id)
  REFERENCES game MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX idx_turn ON turn (t_id, del);

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
