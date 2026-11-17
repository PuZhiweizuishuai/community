-- ===========================================================================
-- V4: 关键字搜索升级 FULLTEXT(ngram)，替代 LIKE '%kw%' 全表扫描
-- ngram 解析器支持中文（默认 ngram_token_size=2，单字搜索不命中属预期行为）
-- 搜索 SQL 使用 MATCH ... AGAINST(... IN BOOLEAN MODE)，关键字在服务端清洗布尔操作符
-- ===========================================================================

ALTER TABLE `Questions` ADD FULLTEXT INDEX `ft_questions_title_tag` (`title`, `tag`) WITH PARSER ngram;

ALTER TABLE `users` ADD FULLTEXT INDEX `ft_users_search` (`userName`, `userId`, `simpleSelfIntroduction`, `selfIntroduction`) WITH PARSER ngram;

ANALYZE TABLE `Questions`, `users`;