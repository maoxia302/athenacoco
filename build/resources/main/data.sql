drop table RAW_PARTIES;
create table if not exists RAW_PARTIES
(id integer not null auto_increment,
 party_name varchar(255),
 authentication varchar(255),
 origin varchar(255),
 directory integer,
 sign_date datetime,
 sign_time numeric,
 token integer,
 details varchar(255),
 authorization integer);