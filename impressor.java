package eleicoes;

import java.io.*;

public class Impressor {
	FileWriter arqSaida = null;

	public Impressor() {
	}
	public Impressor(FileWriter arq) {
		this.arqSaida = arq;
	}
	public void iniciaGravacaoArquivo(String nomeArq) {
		this.FecharArquivo();
		try {
			this.arqSaida = new FileWriter (nomeArq);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void imprimir(String str) {
		System.out.print(str);
		if (arqSaida != null) {
			try {
				arqSaida.write(str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	void imprimirNovaLinha() {
		this.imprimirNovaLinha("");
	}
	void imprimirNovaLinha(String str) {
		System.out.println(str);
		if (arqSaida != null) {
			try {
				arqSaida.write(str);
				arqSaida.write(System.lineSeparator());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	void JogaParaArquivo() {
		if (arqSaida != null) {
			try {
				arqSaida.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void FecharArquivo() {
		if (arqSaida != null) {
			try {
				arqSaida.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
