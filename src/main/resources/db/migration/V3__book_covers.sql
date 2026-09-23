-- Couvertures : table séparée pour ne pas charger les images avec la liste des livres
alter table book add column cover_updated_at timestamptz;

create table book_cover (
    book_id bigint primary key references book (id) on delete cascade,
    data    bytea  not null check (octet_length(data) <= 2097152) -- JPEG réencodé par le serveur
);
