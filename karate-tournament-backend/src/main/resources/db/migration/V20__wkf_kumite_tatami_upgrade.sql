alter table kumite_match_state add column aka_category1_penalty varchar(40) not null default 'NONE';
alter table kumite_match_state add column ao_category1_penalty varchar(40) not null default 'NONE';
alter table kumite_match_state add column aka_category2_penalty varchar(40) not null default 'NONE';
alter table kumite_match_state add column ao_category2_penalty varchar(40) not null default 'NONE';
alter table kumite_match_state add column senshu_holder varchar(10);
alter table kumite_match_state add column senshu_awarded_at timestamp with time zone;
alter table kumite_match_state add column senshu_revoked boolean not null default false;
alter table kumite_match_state add column senshu_revoked_at timestamp with time zone;
alter table kumite_match_state add column senshu_revocation_reason_code varchar(80);
alter table kumite_match_state add column senshu_reaward_blocked boolean not null default false;
alter table kumite_match_state add column video_review_status varchar(40) not null default 'IDLE';
alter table kumite_match_state add column video_review_active_side varchar(10);
alter table kumite_match_state add column aka_video_review_card_available boolean not null default true;
alter table kumite_match_state add column ao_video_review_card_available boolean not null default true;
alter table kumite_match_state add column video_review_last_resolution varchar(40);
alter table kumite_match_state add column medical_status varchar(40) not null default 'IDLE';
alter table kumite_match_state add column medical_injured_side varchar(10);
alter table kumite_match_state add column medical_started_at timestamp with time zone;
alter table kumite_match_state add column medical_deadline_at timestamp with time zone;
alter table kumite_match_state add column medical_last_outcome varchar(40);
alter table kumite_match_state add column decision_winner_side varchar(10);
alter table kumite_match_state add column decision_win_type varchar(60);
alter table kumite_match_state add column decision_reason_code varchar(80);
alter table kumite_match_state add column decision_reason_text varchar(255);
alter table kumite_match_state add column decision_frozen boolean not null default false;
alter table kumite_match_state add column decision_confirmable boolean not null default false;
alter table kumite_match_state add column last_live_status varchar(40);

update kumite_match_state
set aka_category1_penalty = case
      when aka_hansoku then 'HANSOKU'
      when aka_hansoku_chui then 'HANSOKU_CHUI'
      when aka_chui >= 2 then 'KEIKOKU'
      when aka_chui = 1 then 'CHUKOKU'
      else 'NONE'
    end,
    ao_category1_penalty = case
      when ao_hansoku then 'HANSOKU'
      when ao_hansoku_chui then 'HANSOKU_CHUI'
      when ao_chui >= 2 then 'KEIKOKU'
      when ao_chui = 1 then 'CHUKOKU'
      else 'NONE'
    end,
    senshu_holder = case
      when aka_senshu then 'AKA'
      when ao_senshu then 'AO'
      else null
    end;

update kumite_match_state state
set last_live_status = case
      when match_row.status in ('READY', 'RUNNING', 'PAUSED', 'REVIEW') then match_row.status
      else null
    end
from matches match_row
where state.match_id = match_row.id;
