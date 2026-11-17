-- MySQL AUTO_INCREMENT 列显式插入 0 会被当作自增请求（除非 NO_AUTO_VALUE_ON_ZERO），
-- 导致 V6 的本地存储实际拿到 id=1；这里统一修正为约定的 id=0。
-- 对象存储 id 从自增序列继续分配，不受影响。
UPDATE storage_config SET id = 0 WHERE type = 'local' AND id <> 0;