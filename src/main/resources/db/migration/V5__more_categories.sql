-- Nouveaux genres, et tri alphabétique de la liste (« Autre » reste en dernier).
insert into category (code, label, position) values
    ('ART',            'Art / Photo', 0),
    ('CLASSIC',        'Classique', 0),
    ('COOKING',        'Cuisine', 0),
    ('BUSINESS',       'Économie / Entreprise', 0),
    ('HORROR',         'Horreur', 0),
    ('HUMOR',          'Humour', 0),
    ('SHORT_STORIES',  'Nouvelles', 0),
    ('PHILOSOPHY',     'Philosophie', 0),
    ('RELIGION',       'Religion / Spiritualité', 0),
    ('ROMANCE',        'Romance', 0),
    ('HEALTH',         'Santé / Bien-être', 0),
    ('SOCIETY',        'Société / Politique', 0),
    ('TRAVEL',         'Voyage', 0);

update category set position = case code
    when 'ART' then 10
    when 'COMICS' then 20
    when 'BIOGRAPHY' then 30
    when 'CLASSIC' then 40
    when 'COOKING' then 50
    when 'SELF_HELP' then 60
    when 'BUSINESS' then 70
    when 'ESSAY' then 80
    when 'FANTASY' then 90
    when 'HISTORY' then 100
    when 'HORROR' then 110
    when 'HUMOR' then 120
    when 'YOUTH' then 130
    when 'SHORT_STORIES' then 140
    when 'PHILOSOPHY' then 150
    when 'POETRY' then 160
    when 'CRIME' then 170
    when 'RELIGION' then 180
    when 'NOVEL' then 190
    when 'ROMANCE' then 200
    when 'HEALTH' then 210
    when 'SCI_FI' then 220
    when 'SCIENCE' then 230
    when 'SOCIETY' then 240
    when 'TRAVEL' then 250
    else 999
end;
