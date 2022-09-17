CREATE TABLE brands
(
    uuid UUID PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE categories
(
    uuid UUID PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE items
(
    uuid        UUID PRIMARY KEY,
    name        VARCHAR UNIQUE NOT NULL,
    description VARCHAR        NOT NULL,
    price       NUMERIC        NOT NULL,
    brand_id    UUID           NOT NULL,
    category_id UUID           NOT NULL,
    CONSTRAINT brand_id_fkey FOREIGN KEY (brand_id)
        REFERENCES brands (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT category_id_fkey FOREIGN KEY (category_id)
        REFERENCES categories (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);