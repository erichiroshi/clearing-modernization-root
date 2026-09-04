CREATE TABLE trade_events_outbox (
    id                    BIGSERIAL       PRIMARY KEY,
    aggregate_id          VARCHAR(36)     NOT NULL,
    tipo_evento           VARCHAR(50)     NOT NULL,
    payload               TEXT            NOT NULL,
    status                VARCHAR(20)     NOT NULL CHECK (status IN ('PENDENTE', 'PUBLICADO', 'FALHA')),
    tentativas             INT            NOT NULL DEFAULT 0,
    criado_em             TIMESTAMPTZ     NOT NULL,
    publicado_em          TIMESTAMPTZ,
    ultima_tentativa_em   TIMESTAMPTZ,
    ultimo_erro           TEXT
);

-- usada pelo poller para buscar o próximo lote pendente, em ordem de chegada
CREATE INDEX idx_outbox_status_criado_em ON trade_events_outbox (status, criado_em);
