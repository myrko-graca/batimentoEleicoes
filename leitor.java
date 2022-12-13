package eleicoes;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import java.sql.*;
import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.text.*;
import java.nio.charset.StandardCharsets;

public class Leitor {
	HashMap<String, Candidato> candidatos = new HashMap<String, Candidato>();
	HashMap<String, UF> ufs = new HashMap<String, UF>();
	HashMap<String, String> mapRegiaoUF = new HashMap<String, String>();
	private static DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	public Impressor imp = new Impressor();
	Connection conn = null;

	public Leitor(String nomeBd) {
		// Inicia BD
		String url = "jdbc:sqlite:" + nomeBd;
		if (nomeBd != null) {
			try {
				conn = DriverManager.getConnection(url);
				conn.setAutoCommit(false);
				System.out.println("Conexão com SQLite ok.");
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Carrega dados de estados br
		this.mapRegiaoUF.put("ZZ", "Exterior");
		try (CSVReader reader = new CSVReaderBuilder(new FileReader("estados.csv", StandardCharsets.UTF_8)).withSkipLines(1).build()) {
			for (String[] reg: reader.readAll()) {
				//System.out.println(Arrays.toString(reg));
				this.mapRegiaoUF.put(reg[1], reg[5]);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void importar(InputStream is) throws Exception {
		String sqlUf = "INSERT INTO UF(sigla, regiao) VALUES(?,?)";
		String sqlMun = "INSERT INTO MUN(codigo, uf, nome) VALUES(?,?,?)";
		String sqlCan = "INSERT INTO CAN(numero, cargo, nome, partido) VALUES(?,?,?,?)";
		String sqlLoc = "INSERT INTO LOC(codMun, zona, secao, numero, numeroUrna, tipoUrna, qtdAptos, dtEncerramento, dtRecebimento) VALUES(?,?,?,?,?,?,?,?,?)";
		String sqlVot = "INSERT INTO VOT(codMun, zona, secao, numCand, cargo, qtd) VALUES(?,?,?,?,?,?)";
		
		CSVParser csvParser = new CSVParserBuilder().withSeparator(';').build();
		BufferedReader breader = new BufferedReader(new InputStreamReader(is));
		try (CSVReader reader = new CSVReaderBuilder(breader).withCSVParser(csvParser).withSkipLines(1).build()) {
			LocalTRE local = new LocalTRE();
			String[] reg;
			while ((reg = reader.readNext()) != null) {
				UF uf = this.ufs.get(reg[10]);
				if (uf == null) {
					uf = new UF();
					uf.sigla = reg[10];
					String regiao = this.mapRegiaoUF.get(uf.sigla);
					if (regiao != null) {
						uf.regiao = regiao;
					} else {
						System.err.println("UF não encontrou região: " + local);
					}
					this.ufs.put(uf.sigla, uf);
					if (this.conn != null) {
						PreparedStatement pstmt = this.conn.prepareStatement(sqlUf);
						pstmt.setString(1, uf.sigla);
						pstmt.setString(2, uf.regiao);
						pstmt.executeUpdate();
					}
				}
				Municipio municipio = uf.municipios.get(reg[12]);
				if (municipio == null) {
					municipio = new Municipio();
					municipio.uf = uf;
					municipio.numero = reg[11];
					municipio.nome = reg[12];
					uf.municipios.put(municipio.nome, municipio);
					if (this.conn != null) {
						PreparedStatement pstmt = this.conn.prepareStatement(sqlMun);
						pstmt.setString(1, municipio.numero);
						pstmt.setString(2, municipio.uf.sigla);
						pstmt.setString(3, municipio.nome);
						pstmt.executeUpdate();
					}
				}
				String nomeCandidato = reg[30];
				if (reg[19].equals("#NULO#") && !reg[17].equals("Presidente")) {
					nomeCandidato += "_" + reg[17] + "_" + uf.sigla;
				}
				Candidato candidato = candidatos.get(nomeCandidato);
				if (candidato == null) {
					candidato = new Candidato();
					candidato.nome = reg[30];
					candidato.cargo = reg[17];
					if (!candidato.cargo.equals("Presidente")) {
						//TODO: ajustar para municípios
						candidato.cargo += "_" + uf.sigla;
					}
					candidato.numero = reg[29];
					candidato.partido = reg[19];
					candidatos.put(nomeCandidato, candidato);
					if (this.conn != null) {
						PreparedStatement pstmt = this.conn.prepareStatement(sqlCan);
						pstmt.setString(1, candidato.numero);
						pstmt.setString(2, candidato.cargo);
						pstmt.setString(3, candidato.nome);
						pstmt.setString(4, candidato.partido);
						pstmt.executeUpdate();
					}
				}
				LocalTRE localAux = new LocalTRE();
				localAux.municipio = municipio;
				localAux.zona = Integer.parseInt(reg[13]);
				localAux.secao = Integer.parseInt(reg[14]);
				if (!localAux.equals(local)) {
					local = localAux;
					local.numeroLocal = Integer.parseInt(reg[15]);
					local.qtdAptos = Integer.parseInt(reg[22]);
					local.numeroUrna = Integer.parseInt(reg[32]);
					try {
						local.dtEncerramento = LocalDateTime.parse(reg[40], formato);
					} catch(java.time.format.DateTimeParseException e) {
					}
					local.dtRecebimento = LocalDateTime.parse(reg[21], formato);
					municipio.locais.add(local);
					if (this.conn != null) {
						PreparedStatement pstmt = this.conn.prepareStatement(sqlLoc);
						pstmt.setString(1, local.municipio.numero);
						pstmt.setInt(2, local.zona);
						pstmt.setInt(3, local.secao);
						pstmt.setInt(4, local.numeroLocal);
						pstmt.setInt(5, local.numeroUrna);
						pstmt.setString(6, local.getModeloUrna());
						pstmt.setInt(7, local.qtdAptos);
						pstmt.setString(8, local.getDtEncerramentoAnsi());
						pstmt.setString(9, local.getDtRecebimentoAnsi());
						//pstmt.setLocalDateTime(8, local.dtEncerramento);
						//pstmt.setLocalDateTime(9, local.dtRecebimento);
						pstmt.executeUpdate();
					}

				}
				VotacaoTRE votacao = new VotacaoTRE();
				votacao.local = local;
				votacao.candidato = candidato;
				votacao.qtd = Integer.parseInt(reg[31]);
				candidato.lista.add(votacao);
				local.lista.add(votacao);
				if (this.conn != null) {
					PreparedStatement pstmt = this.conn.prepareStatement(sqlVot);
					pstmt.setString(1, local.municipio.numero);
					pstmt.setInt(2, local.zona);
					pstmt.setInt(3, local.secao);
					pstmt.setString(4, candidato.numero);
					pstmt.setString(5, candidato.cargo);
					pstmt.setLong(6, votacao.qtd);
					pstmt.executeUpdate();
				}
			}
			if (this.conn != null) {
				conn.commit();
			}
		}
	}
	public void listarCandidatos(String cargo) {
		this.listarCandidatos(cargo, null);
	}
	public void listarCandidatos(String cargo, String filtro) {
		imp.imprimirNovaLinha("Candidatos " + cargo);
		imp.imprimirNovaLinha("nome;cargo;partido;numero;votos;perc total;perc válidos");
		long totalVotos = 0;
		long totalValidos = 0;
		for (String key: this.candidatos.keySet()) {
			Candidato candidato = this.candidatos.get(key);
			if (cargo.equalsIgnoreCase(candidato.cargo)) {
				long votos = candidato.totalVotos(filtro);
				totalVotos += votos;
				if (!candidato.partido.equals("#NULO#")) {
					totalValidos += votos;
				}
			}
		}
		for (String key: this.candidatos.keySet()) {
			Candidato candidato = candidatos.get(key);
			if (cargo.equalsIgnoreCase(candidato.cargo)) {
				long votos = candidato.totalVotos(filtro);
				double perc = (votos * 100.00) / totalVotos;
				double percValidos = (votos * 100.00) / totalValidos;
				imp.imprimirNovaLinha(candidato + ";" + votos + ";" + new DecimalFormat("#,##0.00").format(perc) + "%;" + new DecimalFormat("#,##0.00").format(percValidos) + "%");
			}
		}
		imp.JogaParaArquivo();
	}
	public void listaUFs() {
		this.listaUFs(null, null);
	}
	public void listaUFs(String cargo) {
		this.listaUFs(cargo, null);
	}
	public void listaUFs(String cargo, String filtro) {
		String partidos[] = this.retornaPartidos(cargo);
		imp.imprimirNovaLinha("Lista de UFs: " + cargo + " - " + Arrays.toString(partidos));
		imp.imprimir("região;uf;qtd municípios;aptos");
		if (cargo != null) {
			imp.imprimir(";votos;perc votos;qtd branco/nulo;perc branco/nulo");
		}
		if (partidos != null) {
			for (String partido: partidos) {
				imp.imprimir(";votos " + partido + ";perc " + partido);
			}
		}
		imp.imprimirNovaLinha();
		for (String key1: this.ufs.keySet()) {
			UF uf = this.ufs.get(key1);
			Contagem cont = new Contagem(cargo, partidos, filtro);
			for (String key: uf.municipios.keySet()) {
				Municipio mun = uf.municipios.get(key);
				for (LocalTRE local: mun.locais) {
					Contagem contLocal = new Contagem(cargo, partidos, filtro);
					local.realizaContagem(contLocal, true);
					cont.somar(contLocal);
				}
			}
			imp.imprimir(uf + ";" + uf.municipios.size() + ";" + cont.aptos);
			if (cargo != null) {
				imp.imprimir(";" + cont.totalVotos + ";" + cont.getPercentualAptos() + ";" + cont.totalBrancoNulo + ";" + cont.getPercentualBrancoNulo());
			}
			if (partidos != null) {
				for (int i = 0; i < partidos.length; i++) {
					String partido = partidos[i];
					double perc = cont.votosPartido[i] * 100.00 / cont.totalVotos;
					imp.imprimir(";" + cont.votosPartido[i] + ";" + cont.getPercentualPartido(i));
				}
			}
			imp.imprimirNovaLinha();
		}
		this.imp.JogaParaArquivo();
	}
	public void listaMunicipio() {
		this.listaMunicipio(null, null, null);
	}
	public void listaMunicipio(String strUF) {
		this.listaMunicipio(strUF, null, null);
	}
	public void listaMunicipio(String strUF, String cargo) {
		this.listaMunicipio(strUF, cargo, null);
	}
	public void listaMunicipio(String strUF, String cargo, String filtro) {
		String partidos[] = this.retornaPartidos(cargo);
		imp.imprimirNovaLinha("Lista de municípios: " + strUF + " - " + cargo + " " + Arrays.toString(partidos));
		imp.imprimir("região;uf;nome;codigo;aptos;votos;perc votos");
		if (cargo != null) {
			imp.imprimir(";qtd branco/nulo;perc branco/nulo");
		}
		if (partidos != null) {
			for (String partido: partidos) {
				imp.imprimir(";votos " + partido + ";perc " + partido);
			}
		}
		imp.imprimirNovaLinha();
		for (String key1: this.ufs.keySet()) {
			if (strUF == null || key1.equalsIgnoreCase(strUF)) {
				UF uf = this.ufs.get(key1);
				for (String key: uf.municipios.keySet()) {
					Contagem cont = new Contagem(cargo, partidos, filtro);
					Municipio mun = uf.municipios.get(key);
					for (LocalTRE local: mun.locais) {
						Contagem contLocal = new Contagem(cargo, partidos, filtro);
						local.realizaContagem(contLocal);
						cont.somar(contLocal);
					}
					imp.imprimir(mun + ";" + cont.aptos + ";" + cont.totalVotos + ";" + cont.getPercentualAptos());
					if (cargo != null) {
						imp.imprimir(";" + cont.totalBrancoNulo + ";" + cont.getPercentualBrancoNulo());
					}
					if (partidos != null) {
						for (int i = 0; i < partidos.length; i++) {
							String partido = partidos[i];
							imp.imprimir(";" + cont.votosPartido[i] + ";" + cont.getPercentualPartido(i));
						}
					}
					imp.imprimirNovaLinha();
				}
			}
		}
		this.imp.JogaParaArquivo();
	}
	public void listaSecoes(String uf, String municipio) {
		this.listaSecoes(uf, municipio, null);
	}
	public void listaSecoes(String strUF, String municipio, String cargo) {
		String partidos[] = this.retornaPartidos(cargo);
		municipio = municipio.replaceAll("_", " ");
		imp.imprimirNovaLinha("Secões de uma determinada cidade: " + municipio + "(" + strUF + ") - " + cargo + " " + Arrays.toString(partidos));
		imp.imprimir("região;uf;município;código;data recebimento;modelo urna;local;zona;seção");
		if (cargo != null) {
			imp.imprimir(";qtd branco/nulo;perc branco/nulo");
		}
		if (partidos != null) {
			for (String partido: partidos) {
				imp.imprimir(";votos " + partido + ";perc " + partido);
			}
		}
		imp.imprimirNovaLinha();
		UF uf = this.ufs.get(strUF.toUpperCase());
		if (uf != null) {
			Municipio mun = uf.municipios.get(municipio.toUpperCase());
			if (mun != null) {
				for (LocalTRE local: mun.locais) {
					Contagem cont = new Contagem(cargo, partidos);
					local.realizaContagem(cont);
					imp.imprimir(local + ";" + cont.aptos + ";" + cont.totalVotos + ";" + cont.getPercentualAptos());
					if (cargo != null) {
						imp.imprimir(";" + cont.totalBrancoNulo + ";" + cont.getPercentualBrancoNulo());
					}
					if (partidos != null) {
						for (int i = 0; i < partidos.length; i++) {
							String partido = partidos[i];
							imp.imprimir(";" + cont.votosPartido[i] + ";" + cont.getPercentualPartido(i));
						}
					}
					imp.imprimirNovaLinha();
				}
			} else {
				System.out.println("Município não encontrado");
			}
		} else {
			System.out.println("UF não encontrada");
		}
		this.imp.JogaParaArquivo();
	}
	public void mostrarDesvios(String cargo, int valor) {
		this.mostrarDesvios(cargo, valor, null);
	}
	public void mostrarDesvios(String cargo, int valor, String strPartidos) {
		imp.imprimirNovaLinha("Secões com percentual alto para um partido: " + cargo + " > " + valor + "%");
		imp.imprimirNovaLinha("Obs.: Em uma distribuição normal, 3 ou mais desvios padrão de distância da média tem a chance de menor que 0,13% de ocorrer para um dos lados (4 dp < 0,003%, 5 dp < 0,00003%)");
		String partidos[];
		if (strPartidos == null) {
			partidos = this.retornaPartidos(cargo);
		} else {
			partidos = strPartidos.split(";");
		}
		imp.imprimir("partido;percentual;votos maior;votos outros;qtd urna mun;região;uf;cidade;codigo;dt envio tre;modelo urna;numero local votacao;zona;secao");
		if (partidos.length == 2) {
			imp.imprimir(";dif " + Arrays.toString(partidos) + ";média mun dif ;dp dif mun;qtd desv mun");
		}
		imp.imprimirNovaLinha();
		List<LocalTRE> locais = listaLocalTRE();
		for (LocalTRE local: locais) {
			Contagem cont = new Contagem(cargo, partidos);
			local.realizaContagem(cont);
			for (int i=0; i < cont.partidos.length; i++) {
				double perc = cont.votosPartido[i] * 100.00 / (cont.totalVotos - cont.totalBrancoNulo);
				if (perc >= valor) {
					imp.imprimir(cont.partidos[i] + ";" + cont.getPercentualValidosPartido(i) + ";" + cont.votosPartido[i] + ";"  + (cont.totalVotos - cont.totalBrancoNulo - cont.votosPartido[i]) + ";" + local.municipio.locais.size() + ";" + local);
					if (partidos.length == 2) {
						double dif = (perc - (100.00 - perc)) / 100.00;
						imp.imprimir(";" + new DecimalFormat("#,##0.00").format(dif*100) + "%");
						double[] aux = local.municipio.calculaMediaDp(cargo, partidos);
						imp.imprimir(";" + new DecimalFormat("#,##0.00").format(aux[0]*100) + "%;" + new DecimalFormat("#,##0.00").format(aux[1]*100) + "%");
						double qtdDesvios = (Math.abs(dif) - Math.abs(aux[0])) / aux[1];
						imp.imprimirNovaLinha(";" + new DecimalFormat("#,##0.00").format(qtdDesvios));
					}
				}
			}
		}
		this.imp.JogaParaArquivo();
	}
	String[] retornaPartidos(String cargo) {
		Set<String> partidos = new HashSet<String>();
		for (String key: this.candidatos.keySet()) {
			Candidato candidato = this.candidatos.get(key);
			if (candidato.cargo.equalsIgnoreCase(cargo) && !candidato.partido.equals("#NULO#")) {
				partidos.add(candidato.partido);
			}
		}
		String arrPartidos[] = new String[partidos.size()];
        partidos.toArray(arrPartidos);
		return arrPartidos;
	}
	public void mostrarApuracaoParcial(String cargo, String[] partidos, int tempoMin) {
		this.mostrarApuracaoParcial(cargo, partidos, tempoMin, null);
	}
	public void mostrarApuracaoParcial(String cargo, String[] partidos, int tempoMin, String filtro) {
		long totalVotosApuracao = 0;
		imp.imprimirNovaLinha("Apuracao parcial: " + tempoMin + " minutos, filtro: " + filtro);
		imp.imprimir("dt apuracao;perc apuracao");
		for (String partido: partidos) {
			imp.imprimir(";votos " + partido + ";perc " + partido); 
		}
		imp.imprimirNovaLinha();
		for (String key: this.candidatos.keySet()) {
			Candidato candidato = this.candidatos.get(key);
			if (candidato.cargo.equalsIgnoreCase(cargo)) {
				totalVotosApuracao += candidato.totalVotos(null, filtro);
			}
		}
		LocalDateTime dtInicio = LocalDateTime.MAX;
		LocalDateTime dtFim = LocalDateTime.MIN;
		List<LocalTRE> locais = listaLocalTRE();
		for (LocalTRE local: locais) {
			if (local.filtrar(filtro)) {
				if (dtInicio.isAfter(local.dtRecebimento)) {
					dtInicio = local.dtRecebimento;
				}
				if (dtFim.isBefore(local.dtRecebimento)) {
					dtFim = local.dtRecebimento;
				}
			}
		}
		dtInicio = dtInicio.plusMinutes(tempoMin);
		do {
			//Mostra dados dos candidatos
			Contagem cont = new Contagem(cargo, partidos);
			for (String key: this.candidatos.keySet()) {
				Candidato candidato = candidatos.get(key);
				if (candidato.cargo.equalsIgnoreCase(cargo)) {
					long votos = candidato.totalVotos(dtInicio, filtro);
					cont.totalVotos += votos;
					for (int i = 0; i < partidos.length; i++) {
						if (candidato.partido.equals(cont.partidos[i])) {
							cont.votosPartido[i] += votos;
						}
					}
				}
			}
			imp.imprimir(dtInicio.format(formato) + ";");
			imp.imprimir(new DecimalFormat("#,##0.00").format(cont.totalVotos * 100.00 / totalVotosApuracao) + "%");
			for (int i = 0; i < partidos.length; i++) {
				imp.imprimir(";" + cont.votosPartido[i] + ";" + cont.getPercentualPartido(i)); 
			}
			imp.imprimirNovaLinha();
			dtInicio = dtInicio.plusMinutes(tempoMin);
		} while (dtInicio.isBefore(dtFim.plusMinutes(tempoMin)));
		this.imp.JogaParaArquivo();
	}
	List<LocalTRE> listaLocalTRE() {
		List<LocalTRE> saida = new ArrayList<LocalTRE>();
		for (String key1: this.ufs.keySet()) {
			UF uf = this.ufs.get(key1);
			for (String key2: uf.municipios.keySet()) {
				Municipio municipio = uf.municipios.get(key2);
				for (LocalTRE local: municipio.locais) {
					saida.add(local);
				}
			}
		}
		return saida;
	}
	private class RegQtd {
		long qtdAptos = 0;
	}
	public void urnas() {
		this.urnas(null);
	}
	public void urnas(String cargo) {
		String partidos[] = this.retornaPartidos(cargo);
		imp.imprimirNovaLinha("Tipos de urnas");
		imp.imprimir("modelo;quantidade;aptos;qtd uf;qtd município");
		if (cargo != null && !cargo.equalsIgnoreCase("uf")) {
			imp.imprimir(";votos;perc votos;brancos/nulos;perc branco/nulo");
			for (int i=0; i<partidos.length; i++) {
				imp.imprimir(";votos " + partidos[i] + ";perc " + partidos[i]);
			}
		}
		imp.imprimirNovaLinha();
		HashMap<String, List<LocalTRE>> urnas = new HashMap<String, List<LocalTRE>>();
		List<LocalTRE> locais = listaLocalTRE();
		for (LocalTRE local: locais) {
			List<LocalTRE> locaisUrna = urnas.get(local.getModeloUrna());
			if (locaisUrna == null) {
				locaisUrna = new ArrayList<LocalTRE>();
				urnas.put(local.getModeloUrna(), locaisUrna);
			}
			locaisUrna.add(local);
		}
		for (String key: urnas.keySet()) {
			Contagem cont = new Contagem(cargo, partidos);
			List<LocalTRE> locaisUrna = urnas.get(key);
			Set<Municipio> munUrna = new HashSet<Municipio>();
			HashMap<UF, RegQtd> ufUrna = new HashMap<UF, RegQtd>();
			HashMap<String, RegQtd> regiaoUrna = new HashMap<String, RegQtd>();
			for (LocalTRE local: locaisUrna) {
				Municipio mun = local.municipio;
				munUrna.add(mun);
				Contagem contAux = new Contagem(cargo, partidos);
				local.realizaContagem(contAux);
				cont.somar(contAux);
				RegQtd regQtdUf = ufUrna.get(mun.uf);
				if (regQtdUf == null) {
					regQtdUf = new RegQtd();
					ufUrna.put(mun.uf, regQtdUf);
				}
				regQtdUf.qtdAptos += contAux.aptos;
				String regiao = mun.uf.regiao;
				RegQtd regQtdRegiao = regiaoUrna.get(regiao);
				if (regQtdRegiao == null) {
					regQtdRegiao = new RegQtd();
					regiaoUrna.put(regiao, regQtdRegiao);
				}
				regQtdRegiao.qtdAptos += contAux.aptos;
			}
			imp.imprimir(key + ";" + locaisUrna.size() + ";" + cont.aptos + ";" + ufUrna.size() + ";" + munUrna.size());
			if (cargo != null) {
				if (cargo.equalsIgnoreCase("regiao")) {
					imp.imprimirNovaLinha();
					for (String regiao: regiaoUrna.keySet()) {
						RegQtd regQtd = regiaoUrna.get(regiao);
						double perc = regQtd.qtdAptos * 100.00 / cont.aptos;
						imp.imprimirNovaLinha(regiao + ": " + regQtd.qtdAptos + " aptos (" + new DecimalFormat("#,##0.00").format(perc) + "%)");
					}
				} else if (cargo.equalsIgnoreCase("uf")) {
					imp.imprimirNovaLinha();
					for (UF uf: ufUrna.keySet()) {
						RegQtd regQtd = ufUrna.get(uf);
						imp.imprimirNovaLinha(uf + ": " + regQtd.qtdAptos + " aptos");
					}
				} else {
					imp.imprimir(";" + cont.totalVotos + ";" + cont.getPercentualAptos() + ";" + cont.totalBrancoNulo + ";" + cont.getPercentualBrancoNulo());
					for (int i=0; i<cont.partidos.length; i++) {
						imp.imprimir(";" + cont.votosPartido[i] + ";" + cont.getPercentualPartido(i));
					}
				}
			}
			imp.imprimirNovaLinha();
		}
		this.imp.JogaParaArquivo();
	}
}
