package eleicoes;

import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.text.*;

class Contagem {
	String cargo;
	String[] partidos;
	String filtro;
	long aptos = 0;
	long totalVotos = 0;
	long totalBrancoNulo = 0;
	long[] votosPartido = new long[50];
	
	Contagem(String cargo, String[] partidos) {
		this.cargo = cargo;
		this.partidos = partidos;
	}
	Contagem(String cargo, String[] partidos, String filtro) {
		this.cargo = cargo;
		this.partidos = partidos;
		this.filtro = filtro;
	}
	void somar(Contagem cont) {
		this.aptos += cont.aptos;
		this.totalVotos += cont.totalVotos;
		this.totalBrancoNulo += cont.totalBrancoNulo;
		for (int i=0; i < this.votosPartido.length; i++) {
			this.votosPartido[i] += cont.votosPartido[i];
		}
	}
	String getPercentualAptos() {
		double perc = this.totalVotos * 100.00 / this.aptos;
		return new DecimalFormat("#,##0.00").format(perc) + "%";
	}
	String getPercentualBrancoNulo() {
		double perc = this.totalBrancoNulo * 100.00 / this.totalVotos;
		return new DecimalFormat("#,##0.00").format(perc) + "%";
	}
	String getPercentualPartido(int i) {
		double perc = this.votosPartido[i] * 100.00 / this.totalVotos;
		return new DecimalFormat("#,##0.00").format(perc) + "%";
	}
	String getPercentualValidosPartido(int i) {
		double perc = this.votosPartido[i] * 100.00 / (this.totalVotos -  this.totalBrancoNulo);
		return new DecimalFormat("#,##0.00").format(perc) + "%";
	}
}
class UF {
	String sigla;
	String regiao;
	HashMap<String, Municipio> municipios = new HashMap<String, Municipio>();

	public String toString() {
		return regiao + ";" + this.sigla;
	}
}
class Municipio {
	UF uf;
	String nome;
	String numero;
	List<LocalTRE> locais = new ArrayList<LocalTRE>();

	double[] calculaMediaDp(String cargo, String[] partidos) {
		return calculaMediaDp(cargo, partidos, null);
	}
	double[] calculaMediaDp(String cargo, String[] partidos, Integer zona) {
		double[] saida = {0, 0};
		double soma = 0;
		List<Double> valores = new ArrayList<Double>();
		for (LocalTRE local: this.locais) {
			if (zona == null || zona == local.zona) {
				long votos1 = local.getVotos(cargo, partidos[0]);
				long votos2 = local.getVotos(cargo, partidos[1]);
				double perc1 = (double) votos1 / (votos1 + votos2);
				double perc2 = (double) votos2 / (votos1 + votos2);
				double dif = perc1 - perc2;
				valores.add(dif);
				soma += dif;
			}
		}
		double media = soma / valores.size();
		double var = 0;
		for (Double valor: valores) {
			double aux = valor - media;
			var += aux * aux;
		}
		double dp = Math.sqrt(var / (valores.size()-1));
		saida[0] = media;
		saida[1] = dp;
		return saida;
	}
	public String toString() {
		return this.uf + ";" + this.nome + ";" + numero;
	}
}
class LocalTRE {
	Municipio municipio;
	int numeroLocal;
	int zona;
	int secao;
	int qtdAptos;
	int numeroUrna;
	LocalDateTime dtEncerramento;
	LocalDateTime dtRecebimento;
	List<VotacaoTRE> lista = new ArrayList<VotacaoTRE>();
	
	long getVotos(String cargo, String partido) {
		long votos = 0;
		for (VotacaoTRE vot: this.lista) {
			if (cargo.equalsIgnoreCase(vot.candidato.cargo)) {
				if (partido.equalsIgnoreCase(vot.candidato.partido)) {
					votos += vot.qtd;
				}
			}
		}
		return votos;
	}
	void realizaContagem(Contagem cont) {
		this.realizaContagem(cont, false);
	}
	void realizaContagem(Contagem cont, boolean ignoraUf) {
		if (cont.filtro == null || this.filtrar(cont.filtro)) {
			cont.aptos += this.qtdAptos;
			for (VotacaoTRE vot: this.lista) {
				String cargo = vot.candidato.cargo;
				if (ignoraUf) {
					int ind = cargo.indexOf("_");
					if (ind != -1) {
						cargo = cargo.substring(0, ind);
					}
				}
				if (cont.cargo != null && cont.cargo.equalsIgnoreCase(cargo)) {
					cont.totalVotos += vot.qtd;
					if (vot.candidato.partido.equalsIgnoreCase("#NULO#")) {
						cont.totalBrancoNulo += vot.qtd;
					}
					if (cont.partidos != null) {
						for (int i = 0; i < cont.partidos.length; i++) {
							String partido = cont.partidos[i];
							if (vot.candidato.partido.equalsIgnoreCase(partido)) {
								cont.votosPartido[i] += vot.qtd;
							}
						}
					}
				}
			}
		}
	}
	String getModeloUrna() {
		if (this.numeroUrna >= 999500 && this.numeroUrna <= 1220500) {
			return "2009";
		} else if (this.numeroUrna >= 1220501 && this.numeroUrna <= 1345500) {
			return "2010";
		} else if (this.numeroUrna >= 1368501 && this.numeroUrna <= 1370500) {
			return "2011";
		} else if (this.numeroUrna >= 1600000 && this.numeroUrna <= 1650000) {
			return "2011";
		} else if (this.numeroUrna >= 1650001 && this.numeroUrna <= 1701000) {
			return "2013";
		} else if (this.numeroUrna >= 1750000 && this.numeroUrna <= 1950000) {
			return "2015";
		} else if (this.numeroUrna >= 2000000 && this.numeroUrna <= 2250000) {
			return "2020";
		} else {
			return "Não sei";
		}
	}
	boolean filtrar(String filtroCompleto) {
		if (filtroCompleto == null) {
			return true;
		} else {
			String[] filtros = filtroCompleto.split("&");
			for (String filtro: filtros) {
				String[] partes = filtro.split("=");
				if (partes[0].equalsIgnoreCase("tu")) { //tipo de urna
					if (!partes[1].equals(this.getModeloUrna())) {
						return false;
					}
				} else if (partes[0].equalsIgnoreCase("uf")) { //sigla da uf
					if (!partes[1].equalsIgnoreCase(this.municipio.uf.sigla)) {
						return false;
					}
				} else if (partes[0].equalsIgnoreCase("rg")) { //região
					if (!partes[1].equalsIgnoreCase(this.municipio.uf.regiao)) {
						return false;
					}
				}
			}
			return true;
		}
	}
	public String getDtEncerramentoAnsi() {
		if (this.dtEncerramento == null) return null;
		return this.dtEncerramento.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}
	public String getDtRecebimentoAnsi() {
		if (this.dtRecebimento == null) return null;
		return this.dtRecebimento.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}
	public String toString() {
		return this.municipio + ";" + this.dtRecebimento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + ";" + this.getModeloUrna() + ";" + this.numeroLocal + ";" + this.zona + ";" + this.secao;
	}
	public boolean equals(LocalTRE local) {
		if (local == null) {
			return false;
		} else {
			return this.municipio.equals(local.municipio) && this.zona == local.zona && this.secao == local.secao;
		}
	}
}
class VotacaoTRE {
	LocalTRE local;
	Candidato candidato;
	long qtd;

	public String toString() {
		return this.local + " - " + this.candidato + ": " + this.qtd;
	}
}
class Candidato {
	String nome;
	String numero;
	String cargo;
	String partido;
	List<VotacaoTRE> lista = new ArrayList<VotacaoTRE>();
	
	public String toString() {
		return this.nome + ";" + this.cargo + ";" + this.partido + ";" + this.numero;
	}
	long totalVotos() {
		return totalVotos(null, null);
	}
	long totalVotos(String filtro) {
		return totalVotos(null, filtro);
	}
	long totalVotos(LocalDateTime dt) {
		return this.totalVotos(dt, null);
	}
	long totalVotos(LocalDateTime dt, String filtro) {
		long qtd = 0;
		for (VotacaoTRE votacao: this.lista) {
			if (dt == null || votacao.local.dtRecebimento.isBefore(dt)) {
				if (votacao.local.filtrar(filtro)) {
					qtd += votacao.qtd;
				}
			}
		};
		return qtd;
	}

}
