ALTER TABLE books
    ADD COLUMN IF NOT EXISTS isbn VARCHAR(20),
    ADD COLUMN IF NOT EXISTS publisher VARCHAR(255),
    ADD COLUMN IF NOT EXISTS published_year INTEGER,
    ADD COLUMN IF NOT EXISTS page_count INTEGER,
    ADD COLUMN IF NOT EXISTS language VARCHAR(50);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

UPDATE books SET publisher = 'Prentice Hall', published_year = 2008, page_count = 464, language = 'English', isbn = '978-0132350884'
WHERE title = 'Clean Code' AND publisher IS NULL;

UPDATE books SET publisher = 'Avery', published_year = 2018, page_count = 320, language = 'English', isbn = '978-0735211292'
WHERE title = 'Atomic Habits' AND publisher IS NULL;

UPDATE books SET publisher = 'Chilton Books', published_year = 1965, page_count = 688, language = 'English', isbn = '978-0441172719'
WHERE title = 'Dune' AND publisher IS NULL;
