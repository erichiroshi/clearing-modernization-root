CREATE TABLE compradores (
    id                VARCHAR(64)     PRIMARY KEY,
    saldo_disponivel  NUMERIC(18,4)   NOT NULL CHECK (saldo_disponivel >= 0)
);

CREATE TABLE vendedores (
    id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE vendedor_posicoes (
    vendedor_id VARCHAR(64)     NOT NULL REFERENCES vendedores (id),
    ticker      VARCHAR(20)     NOT NULL,
    quantidade  NUMERIC(18,8)   NOT NULL CHECK (quantidade >= 0),
    PRIMARY KEY (vendedor_id, ticker)
);

CREATE TABLE trades (
    id             VARCHAR(36)      PRIMARY KEY,
    comprador_id   VARCHAR(64)      NOT NULL REFERENCES compradores (id),
    vendedor_id    VARCHAR(64)      NOT NULL REFERENCES vendedores (id),
    ativo_ticker   VARCHAR(20)      NOT NULL,
    ativo_nome     VARCHAR(120)     NOT NULL,
    quantidade     NUMERIC(18,8)    NOT NULL CHECK (quantidade > 0),
    preco          NUMERIC(18,4)    NOT NULL CHECK (preco > 0),
    valor_total    NUMERIC(18,4)    NOT NULL,
    status         VARCHAR(20)      NOT NULL CHECK (status IN ('PENDENTE', 'VALIDADO', 'LIQUIDADO', 'REJEITADO')),
    registrado_em  TIMESTAMPTZ      NOT NULL,
    liquidado_em   TIMESTAMPTZ
);

CREATE INDEX idx_trades_comprador ON trades (comprador_id);
CREATE INDEX idx_trades_vendedor ON trades (vendedor_id);
CREATE INDEX idx_trades_status ON trades (status);
