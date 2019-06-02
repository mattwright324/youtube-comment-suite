SELECT MAX(last_checked) AS checked FROM gitem_list
JOIN group_gitem USING (gitem_id)
WHERE group_id = ?;