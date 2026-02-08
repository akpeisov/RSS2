select * from public.controllers rc 

select * from public.rc_outputs ro 

select * from public.rc_controllers rc 

select * from public.rc_modbus_config rmc 

select * from public.users u 

select * from public.rc_inputs ri 
where ri.uuid = 'c48a6543-cb71-47b7-a909-403659541f9a'

select * from public.rc_actions ra 

select * from public.network_config nc 

select * from public.wifi_config wc 

select * from public.eth_config ec 

select * from public.cloud_config cc 

select * from public.rc_scheduler rs 

select * from public.rc_tasks rt 

select * from public.rc_task_actions rta 

select * from public.rc_task_dow rtd 

select * from public.rc_outputs ro  where ro.relay_controller_uuid = 'd9a3c221-0e7d-4a6f-819a-2d0ea37e3ef7'

select gen_random_uuid () 

alter table "users" add column "uuid" uuid 

alter table "controllers" add constraint "FKbtmwwdnqu5aaxu3u3p2fwos3v" foreign key ("user_uuid") references "users"

alter table users add CONSTRAINT users_pkey PRIMARY KEY (uuid)

select * from public.sessions s 

uk_285wqhcvl3u00b5ps8ykvxrcv

b97fffe5-db3a-403d-a098-3404bc226c93
6cd4fad0-263d-424a-a1f3-374f74f4fb14

insert into public.rc_outputs (uuid, id, "name", relay_controller_uuid)
values (gen_random_uuid (), 0, 'Out 0', 'b97fffe5-db3a-403d-a098-3404bc226c93')


select * from public.rc_inputs ri where uuid = '654435bc-c5ef-40cd-9c68-37ee2aad5cf0'

select * from public.rc_inputs ri
--left join public.rc_events re on re.input_uuid = ri.uuid 
--left join public.rc_actions ra on ra.event_uuid = re.uuid 
where ri.relay_controller_uuid = '76e6aca9-5b04-4b59-af1b-f6e7633cea0c'


SELECT *--c.column_name, c.data_type
FROM information_schema.table_constraints tc 
--JOIN information_schema.constraint_column_usage AS ccu USING (constraint_schema, constraint_name) 
JOIN information_schema.columns AS c ON c.table_schema = tc.constraint_schema
  AND tc.table_name = c.table_name --AND ccu.column_name = c.column_name
WHERE 1=1 --tc.table_name like 'rc_%' 
--and constraint_type = 'FOREIGN KEY'
and constraint_name = 'uk_285wqhcvl3u00b5ps8ykvxrcv'
 
select * from public.rc_inputs where uuid = 'd9e2cad6-acd9-4c12-9dc1-d1a1ce43df02' 

select * from public.rc_events re 
left join public.rc_actions ra on ra.event_uuid = re.uuid 
where re.input_uuid = 'd9e2cad6-acd9-4c12-9dc1-d1a1ce43df02'

select * from public.rc_actions ra where ra.event_uuid in ( select uuid from public.rc_events re where re.input_uuid = 'd9e2cad6-acd9-4c12-9dc1-d1a1ce43df02')

update public.rc_actions ra 
   set output_uuid = rr.uuid
 where ra.uuid = (
select uuid from public.rc_outputs ro 
 where ro.relay_controller_uuid = (select ri.relay_controller_uuid from public.rc_inputs ri, public.rc_events re where ri.uuid = re.input_uuid and re.uuid = ra.event_uuid)
  and ro.id = ra."output" 
  and coalesce(ro.slave_id, 0) = coalesce(ra.slave_id, 0)) rr

UPDATE public.rc_actions ra
SET output_uuid = (
    SELECT ro.uuid
    FROM public.rc_outputs ro
    WHERE ro.relay_controller_uuid = (
        SELECT ri.relay_controller_uuid
        FROM public.rc_inputs ri
        JOIN public.rc_events re ON ri.uuid = re.input_uuid
        WHERE re.uuid = ra.event_uuid
    )
    AND ro.id = ra."output"
    AND COALESCE(ro.slave_id, 0) = COALESCE(ra.slave_id, 0)
)
WHERE EXISTS (
    SELECT 1
    FROM public.rc_outputs ro
    WHERE ro.relay_controller_uuid = (
        SELECT ri.relay_controller_uuid
        FROM public.rc_inputs ri
        JOIN public.rc_events re ON ri.uuid = re.input_uuid
        WHERE re.uuid = ra.event_uuid
    )
    AND ro.id = ra."output"
    AND COALESCE(ro.slave_id, 0) = COALESCE(ra.slave_id, 0)
);
  
  
 
select * from public.rc_events re 
left join public.rc_actions ra on ra.event_uuid = re.uuid 
where re.input_uuid = 'd9e2cad6-acd9-4c12-9dc1-d1a1ce43df02'



d9e2cad6-acd9-4c12-9dc1-d1a1ce43df02

select * from public.rc_actions ra 
where ra.event_uuid in (select uuid from public.rc_events re where re.input_uuid = '654435bc-c5ef-40cd-9c68-37ee2aad5cf0')
