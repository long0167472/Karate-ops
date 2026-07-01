alter table kumite_match_state add column aka_penalty_level varchar(40) not null default 'NONE';
alter table kumite_match_state add column ao_penalty_level varchar(40) not null default 'NONE';
alter table kumite_match_state add column aka_penalty_reason_code varchar(80);
alter table kumite_match_state add column ao_penalty_reason_code varchar(80);

update kumite_match_state
set aka_penalty_level = case
      when aka_hansoku then 'HANSOKU'
      when aka_hansoku_chui then 'HANSOKU_CHUI'
      when aka_chui >= 3 then 'CHUI_3'
      when aka_chui = 2 then 'CHUI_2'
      when aka_chui = 1 then 'CHUI_1'
      else 'NONE'
    end,
    ao_penalty_level = case
      when ao_hansoku then 'HANSOKU'
      when ao_hansoku_chui then 'HANSOKU_CHUI'
      when ao_chui >= 3 then 'CHUI_3'
      when ao_chui = 2 then 'CHUI_2'
      when ao_chui = 1 then 'CHUI_1'
      else 'NONE'
    end;

update kumite_match_state
set aka_category1_penalty = aka_penalty_level,
    ao_category1_penalty = ao_penalty_level,
    aka_category2_penalty = 'NONE',
    ao_category2_penalty = 'NONE';
