
--DROP TABLE If EXISTS temp.presidente;

--CREATE TEMPORARY TABLE temp.presidente AS
select 
	"PT" as partido,
	cargo,
	uf,
	qtd_total,
	qtd_votos,
	qtd_bn,
	replace(format("%.2f%", qtd_votos * 100.00 / qtd_total), ".", ",") as perc_total,
	replace(format("%.2f%", qtd_votos * 100.00 / (qtd_total - qtd_bn)), ".", ",") as perc_validos,
	qtd_total - qtd_bn - qtd_votos as qtd_outro,
	replace(format("%.2f%", (qtd_total - qtd_bn - qtd_votos) * 100.00 / qtd_total), ".", ",") as perc_total_outro,
	replace(format("%.2f%", (qtd_total - qtd_bn - qtd_votos) * 100.00 / (qtd_total - qtd_bn)), ".", ",") as perc_validos_outro
from (
	select 
		c.cargo,
		m.uf,
		sum(v.qtd) as qtd_total,
		sum(case when c.partido = "PT" then v.qtd else 0 end) as qtd_votos,
		sum(case when c.partido = "#NULO#" then v.qtd else 0 end) as qtd_bn
	from VOT v
	join LOC l on l.codMun = v.codMun and l.zona = v.zona and l.secao = v.secao
	join MUN m on m.codigo = l.codMun
	join UF u on u.sigla = m.uf
	join CAN c on v.numCand = c.numero and v.cargo = c.cargo
	where
		v.cargo = "Presidente"
	group by
		c.cargo,
		m.uf
) p;
