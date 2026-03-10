create extension if not exists pgcrypto;

create table if not exists clientes (
    id varchar(50) primary key,
    activo boolean not null
);

create table if not exists zonas (
    id varchar(50) primary key,
    soporte_refrigeracion boolean not null
);

create table if not exists pedidos (
    id uuid primary key default gen_random_uuid(),
    numero_pedido varchar(50) not null,
    cliente_id varchar(50) not null,
    zona_id varchar(50) not null,
    fecha_entrega date not null,
    estado varchar(20) not null,
    requiere_refrigeracion boolean not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_pedidos_numero_pedido unique (numero_pedido),
    constraint chk_pedidos_estado check (estado in ('PENDIENTE', 'CONFIRMADO', 'ENTREGADO'))
);

create index if not exists idx_pedidos_estado_fecha_entrega on pedidos (estado, fecha_entrega);

create table if not exists cargas_idempotencia (
    id uuid primary key default gen_random_uuid(),
    idempotency_key varchar(120) not null,
    archivo_hash varchar(64) not null,
    status varchar(20) not null,
    response_payload text,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_cargas_idempotencia_key_hash unique (idempotency_key, archivo_hash)
);
