alter table post
    add column search_vector tsvector generated always as
        (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))) stored;

create index idx_post_search_vector on post using gin (search_vector);
