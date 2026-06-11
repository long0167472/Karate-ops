UPDATE club_fee_roles
SET name = CASE code
    WHEN 'NORMAL' THEN 'Học viên thường'
    WHEN 'STUDENT' THEN 'Sinh viên'
    WHEN 'ATHLETE' THEN 'VĐV đội tuyển'
    WHEN 'CLUB_STAFF' THEN 'Ban cán sự CLB'
    ELSE name
  END,
  description = CASE code
    WHEN 'NORMAL' THEN 'Mức phí tiêu chuẩn cho học viên CLB.'
    WHEN 'STUDENT' THEN 'Mức phí ưu đãi cho sinh viên.'
    WHEN 'ATHLETE' THEN 'Mức phí ưu đãi cho VĐV thi đấu.'
    WHEN 'CLUB_STAFF' THEN 'Mức phí ưu tiên cho ban cán sự/cộng tác viên.'
    ELSE description
  END,
  updated_at = now()
WHERE code IN ('NORMAL', 'STUDENT', 'ATHLETE', 'CLUB_STAFF')
  AND deleted_at IS NULL;
