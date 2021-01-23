SELECT * FROM gitem_list
JOIN group_gitem USING (gitem_id)
WHERE group_id = ?
ORDER BY title;