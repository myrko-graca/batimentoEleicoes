/*


select
	sum(qtd_total) as qtd_total,
	sum(qtd_validos) as qtd_validos,
	sum(qtd_votos_pt) as qtd_votos_pt,
	sum(qtd_votos_pl) as qtd_votos_pl,
	sum(qtd_votos_pt) * 100.00 / sum(qtd_total) as perc_pt_total,
	sum(qtd_votos_pt) * 100.00 / sum(qtd_validos) as perc_pt_validos,
	sum(qtd_votos_pl) * 100.00 / sum(qtd_total) as perc_pl_total,
	sum(qtd_votos_pl) * 100.00 / sum(qtd_validos) as perc_pl_validos
from temp.LOC_PARTIDO;

select count(*)
from temp.LOC_PARTIDO
where perc_pl_validos > 98.00;

SELECT 
	AVG((t.row - sub.a) * (t.row - sub.a)) as var 
from t, (SELECT AVG(row) AS a FROM t) AS sub;
*/
select
	tipoUrna,
	case when dif_perc_pt_pl < 0 then "PL" else "PT" end as partido,
	uf,
	nome,
	codigo,
	zona,
	qtd,
	replace(format("%.2f%", media), ".", ",") as media,
	replace(format("%.2f%", dv), ".", ",") as dv,
	replace(format("%.2f%", perc_min), ".", ",") as perc_min,
	replace(format("%.2f%", perc_max), ".", ",") as perc_max,
	replace(format("%.2f%", dif_perc_pt_pl), ".", ",") as dif_perc_pt_pl,
	qtd_votos_pt,
	qtd_votos_pl
from (
	select 
		lm.*,
		lm.media - lm.qtd_dv*lm.dv as perc_min,
		lm.media + lm.qtd_dv*lm.dv as perc_max,
		l.tipoUrna,
		l.dif_perc_pt_pl,
		l.qtd_votos_pt,
		l.qtd_votos_pl
	from (
		select 
			*,
			case when ts.valor is null then 3.29 else ts.valor end as qtd_dv
		from (
			select 
				l.uf,
				l.nome,
				l.codigo,
				l.zona,
				count(*) as qtd,
				max(media.valor) as media,
				sqrt(AVG(power(l.dif_perc_pt_pl - media.valor, 2))) as dv
			from temp.LOC_PARTIDO l
			join (
				select 
					codigo,
					zona,
					AVG(dif_perc_pt_pl) as valor
				from temp.LOC_PARTIDO
				group by codigo, zona
			) media on l.codigo = media.codigo and l.zona = media.zona
			--where tipoUrna='2020'
			group by l.uf, l.nome, l.codigo, l.zona
		) x
		left join TStudent ts on ts.espacoAmostral = x.qtd-1
	) lm
	join temp.LOC_PARTIDO l on lm.codigo = l.codigo and lm.zona = l.zona
	where 
		--tipoUrna='2020' and
		--lm.uf = "MA" and 
		(l.dif_perc_pt_pl < perc_min or l.dif_perc_pt_pl > perc_max)
);