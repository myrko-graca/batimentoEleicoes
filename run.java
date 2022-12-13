package eleicoes;
import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.text.*;
import java.util.zip.*;

public class Run {
    public static void main(String[] args) throws Exception {
		//https://dadosabertos.tse.jus.br/dataset/resultados-2022-boletim-de-urna
		//https://www.descubraonline.com/eleicoes/locais-de-votacao/
		File dirDados;
		if (args.length > 0) {
			System.out.println(args[0]);
			dirDados = new File(args[0]);
		} else {
			dirDados = new File("dadosAux");
		}
		Leitor leitor = new Leitor(dirDados + ".db");
		for (String arq: dirDados.list()) {
			if (arq.endsWith(".zip")) {
				String fileName = dirDados + "/" + arq;
				ZipFile zipFile = new ZipFile(fileName);
				for (ZipEntry e: Collections.list(zipFile.entries())) {
					if (e.getName().endsWith(".csv")) {
						System.out.println("Importando '" + e.getName() + "'");
						InputStream is = zipFile.getInputStream(e);
						leitor.importar(is);
					}
				}
			}
		}
		String input = "";
		while (!input.equals("fim")) {
			System.out.print("> ");
			input = System.console().readLine();
			System.out.println(input);
			Run.executaComando(leitor, input);
		}
	}
	static String[] separaComando(String comando) {
		return comando.split("\\s+");
	}
	public static void executaComando(Leitor leitor, String comando) {
		String[] cmd = separaComando(comando);
		
		if (cmd.length > 0) {
			if (cmd[0].equalsIgnoreCase("fim")) {
				leitor.imp.FecharArquivo();
			} else if (cmd[0].equalsIgnoreCase("?")) {
				System.out.println("Lista de comandos:");
				System.out.println("arq: Inicia gravação em arquivo");
				System.out.println("	ex.: arq saida.txt");
				System.out.println("cand: Resultado do candidato");
				System.out.println("	ex.: cand presidente");
				System.out.println("	ex.: cand governador_rs");
				System.out.println("	ex.: cand presidente tu=2020 -> filtra por tipo de urna");
				System.out.println("apu: Apuração parcial");
				System.out.println("	ex.: ap 5 -> apuração de 5 em 5 minutos");
				System.out.println("	ex.: ap 5 tu=2022-> filtro por tipo de urna");
				System.out.println("	ex.: ap 5 uf=sp-> filtro por uf");
				System.out.println("	ex.: ap 5 rg=sul-> filtro por região");
				System.out.println("	ex.: ap 5 rg=sul&rg=sul-> filtro combinado");
				System.out.println("uf: Lista UFs");
				System.out.println("	ex.: uf presidente -> lista por uf com votos brancos/nulos para presidente");
				System.out.println("mun: Lista de municípios");
				System.out.println("	ex.: mun MA presidente -> lista por município da UF 'MA' com votos brancos/nulos para presidente");
				System.out.println("sec: Lista de seções de um município");
				System.out.println("	ex.: sec CE MADALENA presidente -> lista por seções do município com votos brancos/nulos para presidente");
				System.out.println("desv: Mostra seções com percentual maior que X% para um determinado partido (entre 90% e 100%)");
				System.out.println("	ex.: desv presidente 98");
				System.out.println("	ex.: desv presidente 98 pt;pl -> mostra somente desvio entre os partidos");
				System.out.println("urn: Mostra os modelos de urnas");
				System.out.println("	ex.: urn");
				System.out.println("	ex.: urn presidente -> mostra urnas com a votação para o cargo");
				System.out.println("	ex.: urn uf -> motrar urnas e dados de uf");
				System.out.println("	ex.: urn regiao -> motrar urnas e dados da região");
				System.out.println("fim: Termina a execução.");
			} else if (cmd[0].equalsIgnoreCase("cand")) {
				if (cmd.length == 2) {
					leitor.listarCandidatos(cmd[1]);
				} else if (cmd.length == 3) {
					leitor.listarCandidatos(cmd[1], cmd[2]);
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("apu")) {
				String[] partidos = {"PT", "PL"};
				String cargo = "Presidente";
				if (cmd.length == 1) {
					leitor.mostrarApuracaoParcial(cargo, partidos, 10);
				} else if (cmd.length == 2) {
					try {
						int intervalo = Integer.parseInt(cmd[1]);
						leitor.mostrarApuracaoParcial(cargo, partidos, intervalo);
					} catch(NumberFormatException e) {
						System.out.println("Parâmetro numérico inválido");
					}
				} else if (cmd.length == 3) {
					try {
						int intervalo = Integer.parseInt(cmd[1]);
						leitor.mostrarApuracaoParcial(cargo, partidos, intervalo, cmd[2]);
					} catch(NumberFormatException e) {
						System.out.println("Parâmetro numérico inválido");
					}
				}
			} else if (cmd[0].equalsIgnoreCase("uf")) {
				if (cmd.length == 1) {
					leitor.listaUFs();
				} else if (cmd.length == 2) {
					leitor.listaUFs(cmd[1]);
				} else if (cmd.length == 3) {
					leitor.listaUFs(cmd[1], cmd[2]);
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("mun")) {
				if (cmd.length == 1) {
					leitor.listaMunicipio();
				} else if (cmd.length == 2) {
					leitor.listaMunicipio(cmd[1]);
				} else if (cmd.length == 3) {
					leitor.listaMunicipio(cmd[1], cmd[2]);
				} else if (cmd.length == 4) {
					leitor.listaMunicipio(cmd[1], cmd[2], cmd[3]);
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("sec")) {
				if (cmd.length == 3) {
					leitor.listaSecoes(cmd[1], cmd[2]);
				} else if (cmd.length == 4) {
					leitor.listaSecoes(cmd[1], cmd[2], cmd[3]);
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("desv")) {
				if (cmd.length == 3) {
					try {
						leitor.mostrarDesvios(cmd[1], Integer.parseInt(cmd[2]));
					} catch(NumberFormatException e) {
						System.out.println("Número inválido");
					}
				} else if (cmd.length == 4) {
					try {
						leitor.mostrarDesvios(cmd[1], Integer.parseInt(cmd[2]), cmd[3]);
					} catch(NumberFormatException e) {
						System.out.println("Número inválido");
					}
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("urn")) {
				if (cmd.length == 1) {
					leitor.urnas();
				} else if (cmd.length == 2) {
					leitor.urnas(cmd[1]);
				} else {
					System.out.println("Quantidade de parâmetros inválida");
				}
			} else if (cmd[0].equalsIgnoreCase("arq")) {
				if (cmd.length == 2) {
					leitor.imp.iniciaGravacaoArquivo(cmd[1]);
				} else {
					System.out.println("Coloque o nome do arquivo");
				}
			}
		}
	}
}
