alter table persons
  add column if not exists current_address varchar(255),
  add column if not exists emergency_contact_name varchar(180),
  add column if not exists emergency_contact_phone varchar(60);

alter table organization_members
  add column if not exists is_student boolean not null default false,
  add column if not exists attendance_view_enabled boolean not null default true,
  add column if not exists tuition_status varchar(40) not null default 'PENDING',
  add column if not exists tuition_paid_amount numeric(12,2) not null default 0,
  add column if not exists other_fee_status varchar(40) not null default 'PENDING',
  add column if not exists other_fee_paid_amount numeric(12,2) not null default 0,
  add column if not exists payment_note varchar(500),
  add column if not exists member_note varchar(500);

update persons
set current_address = coalesce(current_address, 'Chưa cập nhật địa chỉ')
where current_address is null;

update organization_members
set tuition_status = 'PAID',
    tuition_paid_amount = 650000,
    other_fee_status = 'PENDING',
    other_fee_paid_amount = 0,
    is_student = role_code = 'ATHLETE'
where id in (
  '10000000-0000-0000-0000-000000000301',
  '10000000-0000-0000-0000-000000000302',
  '10000000-0000-0000-0000-000000000304',
  '10000000-0000-0000-0000-000000000305',
  '10000000-0000-0000-0000-000000000307',
  '10000000-0000-0000-0000-000000000309'
);
