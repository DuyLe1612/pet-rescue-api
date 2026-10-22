-- ============================================================
-- V25: Chat and social posts now live only in MongoDB (migrate.md).
-- Drops the old PostgreSQL tables WITH THEIR DATA — existing posts, comments, likes and chat
-- messages are not copied to MongoDB. Users, media_files, tags, rescue_cases, pets, adoptions
-- and friend_requests are untouched.
-- ============================================================

-- V7 counter / nesting triggers go with their tables; the functions are dropped explicitly
DROP TABLE IF EXISTS comment_likes CASCADE;
DROP TABLE IF EXISTS post_likes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS post_media CASCADE;
DROP TABLE IF EXISTS post_tags CASCADE;
DROP TABLE IF EXISTS posts CASCADE;

DROP FUNCTION IF EXISTS enforce_comment_nesting_level();
DROP FUNCTION IF EXISTS update_post_like_count();
DROP FUNCTION IF EXISTS update_comment_like_count();
DROP FUNCTION IF EXISTS update_post_comment_count();
DROP FUNCTION IF EXISTS update_comment_reply_count();

DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS conversation_participants CASCADE;
DROP TABLE IF EXISTS conversations CASCADE;
