-- V4 seeded PUBLISHED, but TournamentStatus uses REGISTRATION_OPEN.
-- Keep V4 unchanged to avoid Flyway checksum issues on existing databases.
update tournaments
set status = 'REGISTRATION_OPEN'
where status = 'PUBLISHED';
