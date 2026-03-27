import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket socketComando;
    private ServidorFTP servidor;
    private BufferedReader entrada;
    private PrintWriter saida;
    private String clienteIP;

    // Construtor: inicializa o handler para um cliente
    public ClientHandler(Socket socket, ServidorFTP servidor) {
        this.socketComando = socket;
        this.servidor = servidor;
        this.clienteIP = socket.getInetAddress().getHostAddress();
    }

    // Thread principal do handler: processa comandos do cliente
    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socketComando.getInputStream()));
            saida = new PrintWriter(socketComando.getOutputStream(), true);

            // Mensagem de boas-vindas
            saida.println("220 🖼️ Servidor FTP de Backup de Fotos pronto! Bem-vindo(a)!");
            servidor.log("Cliente " + clienteIP + " conectado");

            String comando;
            while ((comando = entrada.readLine()) != null) {
                servidor.log("Cliente " + clienteIP + " >> " + comando);
                processarComando(comando.trim());
            }

        } catch (IOException e) {
            servidor.log("Erro com cliente " + clienteIP + ": " + e.getMessage());
        } finally {
            try {
                if (socketComando != null) socketComando.close();
                servidor.log("Cliente " + clienteIP + " desconectado");
            } catch (IOException e) {
                servidor.log("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    // Processa cada comando FTP recebido do cliente
    private void processarComando(String comando) {
        String[] partes = comando.split(" ", 2);
        String cmd = partes[0].toUpperCase();
        String parametro = partes.length > 1 ? partes[1] : "";

        switch (cmd) {
            case "USER":
                saida.println("331 👤 Usuário recebido, por favor informe a senha.");
                break;
            case "PASS":
                saida.println("230 ✅ Login realizado com sucesso!");
                break;
            case "PWD":
                saida.println("257 📁 Diretório atual: \"/backup\"");
                break;
            case "TYPE":
                saida.println("200 📦 Tipo de transferência definido para " + parametro);
                break;
            case "PASV":
                iniciarModoPassivo();
                break;
            case "STOR":
                if (!parametro.isEmpty()) {
                    receberArquivo(parametro);
                } else {
                    saida.println("501 ⚠️ Sintaxe incorreta. Informe o nome do arquivo.");
                }
                break;
            case "LIST":
                listarArquivos();
                break;
            case "QUIT":
                saida.println("221 👋 Conexão encerrada. Até logo!");
                try {
                    socketComando.close();
                } catch (IOException e) {
                    servidor.log("Erro ao fechar socket: " + e.getMessage());
                }
                break;
            default:
                saida.println("502 ❌ Comando não reconhecido ou não implementado.");
        }
    }

    private ServerSocket socketPassivo;
    private int portaPassiva;

    // Inicia o modo passivo para transferência de dados
    private void iniciarModoPassivo() {
        try {
            socketPassivo = new ServerSocket(0); // Porta automática
            portaPassiva = socketPassivo.getLocalPort();

            String ip = socketComando.getLocalAddress().getHostAddress();
            String[] partes = ip.split("\\.");
            int p1 = portaPassiva / 256;
            int p2 = portaPassiva % 256;

            String resposta = String.format(
                "227 🟢 Modo passivo ativado em (%s,%s,%s,%s,%d,%d)",
                partes[0], partes[1], partes[2], partes[3], p1, p2
            );
            saida.println(resposta);

            servidor.log("Modo passivo iniciado na porta " + portaPassiva);

        } catch (IOException e) {
            saida.println("425 🚫 Não foi possível abrir conexão de dados.");
            servidor.log("Erro no modo passivo: " + e.getMessage());
        }
    }

    // Recebe e salva um arquivo enviado pelo cliente
    private void receberArquivo(String nomeArquivo) {
        if (socketPassivo == null) {
            saida.println("425 ⚠️ Use PASV antes de enviar arquivos.");
            return;
        }

        try {
            saida.println("150 ⏳ Preparando para receber o arquivo: " + nomeArquivo);

            Socket socketDados = socketPassivo.accept();
            servidor.log("Conexão de dados estabelecida para " + nomeArquivo);

            if (!isImageFile(nomeArquivo)) {
                servidor.log("Arquivo rejeitado (não é imagem): " + nomeArquivo);
                saida.println("550 ❌ Apenas arquivos de imagem são permitidos.");
                socketDados.close();
                socketPassivo.close();
                socketPassivo = null;
                return;
            }

            String nomeSeguro = sanitizarNomeArquivo(nomeArquivo);
            File arquivoDestino = new File("backup", nomeSeguro);

            if (arquivoDestino.exists()) {
                nomeSeguro = criarNomeUnico(nomeSeguro);
                arquivoDestino = new File("backup", nomeSeguro);
            }

            try (InputStream inputStream = socketDados.getInputStream();
                 FileOutputStream fos = new FileOutputStream(arquivoDestino)) {

                byte[] buffer = new byte[8192];
                int bytesLidos;
                long totalBytes = 0;

                while ((bytesLidos = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesLidos);
                    totalBytes += bytesLidos;
                }

                servidor.log("Arquivo recebido: " + nomeSeguro + " (" + totalBytes + " bytes)");
                saida.println("226 ✅ Arquivo \"" + nomeSeguro + "\" salvo com sucesso! (" + totalBytes + " bytes)");

            } catch (IOException e) {
                servidor.log("Erro ao salvar arquivo: " + e.getMessage());
                saida.println("550 ❌ Erro ao salvar arquivo.");
            }

            socketDados.close();
            socketPassivo.close();
            socketPassivo = null;

        } catch (IOException e) {
            servidor.log("Erro na transferência: " + e.getMessage());
            saida.println("426 🚫 Conexão fechada; transferência abortada.");
        }
    }

    // Verifica se o arquivo tem extensão de imagem
    private boolean isImageFile(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
            return false;
        }
        String nome = nomeArquivo.toLowerCase().trim();
        String[] extensoesValidas = {
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".tiff", ".tif", ".webp", ".svg", ".ico",
            ".raw", ".cr2", ".nef", ".arw", ".dng"
        };
        for (String ext : extensoesValidas) {
            if (nome.endsWith(ext)) {
                servidor.log("Arquivo aceito como imagem: " + nomeArquivo + " (extensão: " + ext + ")");
                return true;
            }
        }
        servidor.log("Arquivo rejeitado - extensão não reconhecida: " + nomeArquivo);
        return false;
    }

    // Remove caracteres problemáticos do nome do arquivo
    private String sanitizarNomeArquivo(String nome) {
        if (nome == null) return "arquivo_sem_nome.jpg";
        String nomeLimpo = nome.replaceAll("[<>:\"/\\\\|?*]", "_");
        nomeLimpo = nomeLimpo.trim();
        if (nomeLimpo.isEmpty()) {
            nomeLimpo = "foto_" + System.currentTimeMillis() + ".jpg";
        }
        if (!nomeLimpo.contains(".")) {
            nomeLimpo += ".jpg";
        }
        return nomeLimpo;
    }

    // Gera um nome único se já existir arquivo com o mesmo nome
    private String criarNomeUnico(String nomeBase) {
        String nome = nomeBase;
        String extensao = "";
        int pontoPos = nomeBase.lastIndexOf('.');
        if (pontoPos > 0) {
            nome = nomeBase.substring(0, pontoPos);
            extensao = nomeBase.substring(pontoPos);
        }
        int contador = 1;
        String novoNome;
        File arquivo;
        do {
            novoNome = nome + "_" + contador + extensao;
            arquivo = new File("backup", novoNome);
            contador++;
        } while (arquivo.exists() && contador < 1000);
        return novoNome;
    }

    // Lista os arquivos do diretório de backup para o cliente
    private void listarArquivos() {
        if (socketPassivo == null) {
            saida.println("425 ⚠️ Use PASV antes de listar arquivos.");
            return;
        }

        try {
            saida.println("150 📋 Listando arquivos do backup...");
            Socket socketDados = socketPassivo.accept();
            PrintWriter dadosSaida = new PrintWriter(socketDados.getOutputStream(), true);

            File backup = new File("backup");
            File[] arquivos = backup.listFiles();

            if (arquivos != null && arquivos.length > 0) {
                for (File arquivo : arquivos) {
                    if (arquivo.isFile()) {
                        String linha = String.format(
                            "- %s | %10d bytes | %s",
                            arquivo.getName(),
                            arquivo.length(),
                            new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(arquivo.lastModified()))
                        );
                        dadosSaida.println(linha);
                    }
                }
                servidor.log("Listagem enviada: " + arquivos.length + " arquivos");
            } else {
                dadosSaida.println("Nenhum arquivo encontrado no backup.");
                servidor.log("Pasta backup vazia");
            }

            dadosSaida.close();
            socketDados.close();
            socketPassivo.close();
            socketPassivo = null;

            saida.println("226 ✅ Listagem concluída.");

        } catch (IOException e) {
            servidor.log("Erro ao listar arquivos: " + e.getMessage());
            saida.println("550 ❌ Erro ao listar arquivos.");
        }
    }
}