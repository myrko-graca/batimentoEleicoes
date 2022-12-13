drop TABLE if exists temp.LOC_PARTIDO;
CREATE TEMPORARY TABLE temp.LOC_PARTIDO AS
select * from V_LOC_PARTIDO;

.headers on 
.separator ";" "\n"
.timer on