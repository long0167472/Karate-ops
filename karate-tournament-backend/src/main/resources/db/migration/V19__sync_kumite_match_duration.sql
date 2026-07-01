update kumite_match_state state
set duration_ms = greatest(coalesce(category.match_duration_seconds, 180), 30) * 1000,
    remaining_ms = least(state.remaining_ms, greatest(coalesce(category.match_duration_seconds, 180), 30) * 1000)
from matches match_row
join categories category on category.id = match_row.category_id
where state.match_id = match_row.id
  and match_row.deleted_at is null
  and category.deleted_at is null
  and match_row.mode in ('KUMITE', 'TEAM_KUMITE');
