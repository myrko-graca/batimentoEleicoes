drop view if exists V_LOC_PARTIDO;
create view if not exists V_LOC_PARTIDO (
	regiao,
	uf,
	codigo,
	nome,
	numero,
	zona,
	secao,
	numeroUrna,
	tipoUrna,
	qtdAptos,
	dtEncerramento,
	dtRecebimento,
	qtd_total,
	qtd_validos,
	qtd_votos_pt,
	qtd_votos_pl,
	perc_pt_total,
	perc_pt_validos,
	perc_pl_total,
	perc_pl_validos,
	dif_perc_pt_pl
) as
select 
	*,
	perc_pt_validos - perc_pl_validos
from (
	select 
		u.regiao,
		m.uf,
		m.codigo,
		m.nome,
		l.numero,
		l.zona,
		l.secao,
		l.numeroUrna,
		l.tipoUrna,
		l.qtdAptos,
		l.dtEncerramento,
		l.dtRecebimento,
		v.qtd_total,
		v.qtd_total - v.qtd_bn as qtd_validos,
		v.qtd_votos_pt,
		v.qtd_votos_pl,
		v.qtd_votos_pt * 100.00 / v.qtd_total as perc_pt_total,
		v.qtd_votos_pt * 100.00 / (v.qtd_total - v.qtd_bn) as perc_pt_validos,
		v.qtd_votos_pl * 100.00 / v.qtd_total as perc_pl_total,
		v.qtd_votos_pl * 100.00 / (v.qtd_total - v.qtd_bn) as perc_pl_validos
	from loc l
	join mun m on m.codigo=l.codMun
	join uf u on m.uf=u.sigla
	join (
		select 
			v.codMun,
			v.zona,
			v.secao,
			sum(v.qtd) as qtd_total,
			sum(case when c.partido = "PT" then v.qtd else 0 end) as qtd_votos_pt,
			sum(case when c.partido = "PL" then v.qtd else 0 end) as qtd_votos_pl,
			sum(case when c.partido = "#NULO#" then v.qtd else 0 end) as qtd_bn
		from VOT v
		join CAN c on v.numCand = c.numero and v.cargo = c.cargo
		where
			v.cargo = "Presidente"
		group by
			v.codMun,
			v.zona,
			v.secao
	) v on l.codMun = v.codMun and l.zona = v.zona and l.secao = v.secao
)