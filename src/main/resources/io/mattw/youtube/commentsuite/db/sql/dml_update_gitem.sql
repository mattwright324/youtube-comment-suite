UPDATE gitem_list
SET title = ?,
    channel_title = ?,
    published = ?,
    last_checked = ?,
    thumb_url = ?
WHERE gitem_id = ?;