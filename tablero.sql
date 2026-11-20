-- Trackly: script de la base de datos (PostgreSQL)
-- Se corre una vez sobre la base vacía. Hibernate solo revisa (validate).

CREATE TABLE usuarios (
    id              SERIAL PRIMARY KEY,
    nombre          VARCHAR(80)   NOT NULL,
    correo          VARCHAR(120)  NOT NULL UNIQUE,
    hash_clave      VARCHAR(60)   NOT NULL,
    presupuesto_mes NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (presupuesto_mes >= 0)
);

CREATE TABLE categorias (
    id     SERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE clases (
    id          SERIAL PRIMARY KEY,
    id_usuario  INT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    dia         INT          NOT NULL CHECK (dia BETWEEN 1 AND 7),
    hora_inicio TIME         NOT NULL,
    hora_fin    TIME         NOT NULL,
    materia     VARCHAR(80)  NOT NULL,
    aula        VARCHAR(30),
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE movimientos (
    id           SERIAL PRIMARY KEY,
    id_usuario   INT           NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    id_categoria INT           NOT NULL REFERENCES categorias(id),
    id_clase     INT           REFERENCES clases(id) ON DELETE SET NULL,
    tipo         VARCHAR(7)    NOT NULL CHECK (tipo IN ('GASTO', 'INGRESO')),
    monto        NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha        DATE          NOT NULL,
    nota         VARCHAR(150)
);

CREATE INDEX idx_movimientos_usuario_fecha ON movimientos (id_usuario, fecha);

CREATE TABLE recordatorios (
    id            SERIAL PRIMARY KEY,
    id_usuario    INT           NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo        VARCHAR(100)  NOT NULL,
    fecha_hora    TIMESTAMP     NOT NULL,
    hecho         BOOLEAN       NOT NULL DEFAULT FALSE,
    monto         NUMERIC(10,2) CHECK (monto > 0),
    id_categoria  INT           REFERENCES categorias(id),
    mensual       BOOLEAN       NOT NULL DEFAULT FALSE,
    id_movimiento INT           UNIQUE REFERENCES movimientos(id) ON DELETE SET NULL,
    CHECK ((monto IS NULL) = (id_categoria IS NULL))
);

INSERT INTO categorias (nombre) VALUES
    ('Comida'), ('Transporte'), ('Estudio'), ('Salidas'), ('Servicios'), ('Salario'), ('Otros');
