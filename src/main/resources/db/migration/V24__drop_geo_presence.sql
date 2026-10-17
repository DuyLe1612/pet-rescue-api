-- The realtime geo-presence feature (/api/v1/geo) was removed; drop its table.
DROP TABLE IF EXISTS user_geo_locations;
