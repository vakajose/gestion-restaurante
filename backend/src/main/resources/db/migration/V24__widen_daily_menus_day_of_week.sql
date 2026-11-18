-- V24: Ampliar daily_menus.day_of_week para soportar múltiples días concatenados (ej. LUNES,MARTES,MIERCOLES,JUEVES,VIERNES)
ALTER TABLE daily_menus
    ALTER COLUMN day_of_week TYPE VARCHAR(120);
