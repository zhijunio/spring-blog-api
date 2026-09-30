create index idx_comment_post_id on comment (post_id);

create index idx_post_category_id on post (category_id);

create index idx_post_created_by on post (created_by);

create index idx_post_updated_by on post (updated_by);
