insert into clientes (id, activo) values
('CLI-123', true),
('CLI-999', true),
('CLI-555', true),
('CLI-000', false)
on conflict (id) do nothing;

insert into zonas (id, soporte_refrigeracion) values
('ZONA1', true),
('ZONA2', false),
('ZONA3', true),
('ZONA5', false)
on conflict (id) do nothing;
