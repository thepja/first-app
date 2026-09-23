-- Catégories : liste de référence proposée dans la liste déroulante.
-- Pour en ajouter une, créer une nouvelle migration (les codes sont stables, les libellés peuvent changer).
create table category (
    code     varchar(30) primary key,
    label    varchar(60) not null unique,
    position smallint    not null
);

insert into category (code, label, position) values
    ('NOVEL',       'Roman',                    10),
    ('SCI_FI',      'Science-fiction',          20),
    ('FANTASY',     'Fantasy',                  30),
    ('CRIME',       'Policier / Thriller',      40),
    ('COMICS',      'BD / Manga',               50),
    ('BIOGRAPHY',   'Biographie / Mémoires',    60),
    ('HISTORY',     'Histoire',                 70),
    ('ESSAY',       'Essai',                    80),
    ('SCIENCE',     'Sciences',                 90),
    ('SELF_HELP',   'Développement personnel', 100),
    ('POETRY',      'Poésie / Théâtre',        110),
    ('YOUTH',       'Jeunesse',                120),
    ('OTHER',       'Autre',                   999);

alter table book add column category_code varchar(30) references category (code);
