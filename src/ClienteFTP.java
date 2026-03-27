import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClienteFTP extends JFrame {
    private JTextArea logArea;
    private JTextField ipField, usuarioField, senhaField;
    private JButton conectarBtn, listarBtn;
    private Socket socketComando;
    private BufferedReader entrada;
    private PrintWriter saida;
    private boolean conectado = false;

    private WatchService watchService;
    private Thread watchThread;
    private File[] arquivosSelecionados;
    private JPanel painelAcoes;

    public ClienteFTP() {
        super("Cliente FTP - Backup de Fotos");
        configurarGUI();
    }

    //Interface gráfica do cliente FTP
    private void configurarGUI() {
        setLayout(new BorderLayout());

        // Painel de conexão
        JPanel painelConexao = new JPanel(new GridBagLayout());
        painelConexao.setBorder(BorderFactory.createTitledBorder("Conexão"));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        painelConexao.add(new JLabel("IP do Servidor:"), gbc);
        gbc.gridx = 1;
        ipField = new JTextField("127.0.0.1", 15);
        painelConexao.add(ipField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painelConexao.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1;
        usuarioField = new JTextField("anonymous", 15);
        painelConexao.add(usuarioField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painelConexao.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        senhaField = new JPasswordField("guest", 15);
        painelConexao.add(senhaField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 3;
        conectarBtn = new JButton("Conectar");
        conectarBtn.addActionListener(e -> conectarDesconectar());
        painelConexao.add(conectarBtn, gbc);

        add(painelConexao, BorderLayout.NORTH);

        // Área de log
        logArea = new JTextArea(15, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        // Painel de ações dinâmico
        painelAcoes = new JPanel(new FlowLayout());
        add(painelAcoes, BorderLayout.SOUTH);

        // Botão para listar arquivos 
        listarBtn = new JButton("Listar Backup");
        listarBtn.addActionListener(e -> listarArquivos());
        listarBtn.setEnabled(false);

        // Inicializa os botões padrão
        atualizarBotoesPadrao();

        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Alterna entre conectar e desconectar
    private void conectarDesconectar() {
        if (!conectado) {
            conectar();
        } else {
            desconectar();
        }
    }

    //Conecta ao servidor FTP e realiza o login
    private void conectar() {
        new Thread(() -> {
            try {
                log("Conectando ao servidor " + ipField.getText() + ":12384...");
                socketComando = new Socket(ipField.getText().trim(), 12384);
                entrada = new BufferedReader(new InputStreamReader(socketComando.getInputStream()));
                saida = new PrintWriter(socketComando.getOutputStream(), true);

                String resposta = entrada.readLine();
                log("Servidor: " + resposta);

                if (resposta.startsWith("220")) {
                    // Login
                    String usuario = usuarioField.getText().trim();
                    String senha = senhaField.getText().trim();

                    saida.println("USER " + usuario);
                    resposta = entrada.readLine();
                    log("Servidor: " + resposta);

                    saida.println("PASS " + senha);
                    resposta = entrada.readLine();
                    log("Servidor: " + resposta);

                    if (resposta.startsWith("230")) {
                        conectado = true;
                        SwingUtilities.invokeLater(() -> {
                            conectarBtn.setText("Desconectar");
                            ipField.setEnabled(false);
                            usuarioField.setEnabled(false);
                            senhaField.setEnabled(false);
                            listarBtn.setEnabled(true);
                        });
                        log("✓ Conectado com sucesso!");

                        // Define tipo binário
                        saida.println("TYPE I");
                        resposta = entrada.readLine();
                        log("Servidor: " + resposta);
                    } else {
                        log("✗ Falha no login");
                    }
                } else {
                    log("✗ Falha na conexão");
                }

            } catch (IOException e) {
                log("✗ Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    // Desconecta do servidor FTP e limpa a interface
    private void desconectar() {
        try {
            if (saida != null) {
                saida.println("QUIT");
            }
            if (socketComando != null) {
                socketComando.close();
            }
            conectado = false;

            SwingUtilities.invokeLater(() -> {
                conectarBtn.setText("Conectar");
                ipField.setEnabled(true);
                usuarioField.setEnabled(true);
                senhaField.setEnabled(true);
                listarBtn.setEnabled(false);
            });

            log("✓ Desconectado");

        } catch (IOException e) {
            log("Erro ao desconectar: " + e.getMessage());
        }
    }

    // Abre um seletor de arquivos para escolher fotos
    private void selecionarFoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione fotos para backup");
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String nome = f.getName().toLowerCase();
                return nome.endsWith(".jpg") || nome.endsWith(".jpeg") ||
                        nome.endsWith(".png") || nome.endsWith(".gif") ||
                        nome.endsWith(".bmp") || nome.endsWith(".tiff");
            }
            @Override
            public String getDescription() {
                return "Arquivos de Imagem (*.jpg, *.png, *.gif, *.bmp, *.tiff)";
            }
        });
        chooser.setMultiSelectionEnabled(true);

        int resultado = chooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File[] arquivosSelecionados = chooser.getSelectedFiles();
            for (File arquivo : arquivosSelecionados) {
                log("📁 Foto selecionada: " + arquivo.getName() +
                        " (" + (arquivo.length() / 1024) + " KB)");
            }
            this.arquivosSelecionados = arquivosSelecionados;
        }
    }

    // Envia as fotos selecionadas para o servidor
    private void enviarFoto() {
        if (arquivosSelecionados == null || arquivosSelecionados.length == 0 || !conectado) {
            log("✗ Selecione uma ou mais fotos e conecte ao servidor primeiro");
            return;
        }
        new Thread(() -> {
            for (File arquivo : arquivosSelecionados) {
                enviarArquivoParaServidor(arquivo);
            }
        }).start();
    }

    // Envia o arquivo selecionado para o servidor FTP
    private void enviarArquivoParaServidor(File arquivo) {
        try {
            saida.println("PASV");
            String resposta = entrada.readLine();
            log("Servidor: " + resposta);

            if (!resposta.startsWith("227")) {
                log("✗ Erro ao entrar no modo passivo");
                return;
            }

            int porta = extrairPortaPassiva(resposta);
            if (porta == -1) {
                log("✗ Erro ao extrair porta passiva");
                return;
            }

            saida.println("STOR " + arquivo.getName());
            resposta = entrada.readLine();
            log("Servidor: " + resposta);

            if (resposta.startsWith("150")) {
                try (Socket socketDados = new Socket(ipField.getText().trim(), porta);
                     FileInputStream fis = new FileInputStream(arquivo);
                     OutputStream os = socketDados.getOutputStream()) {

                    log("📤 Enviando " + arquivo.getName() + "...");

                    byte[] buffer = new byte[8192];
                    int bytesLidos;
                    long totalEnviado = 0;

                    while ((bytesLidos = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesLidos);
                        totalEnviado += bytesLidos;
                    }

                    log("📤 " + totalEnviado + " bytes enviados");
                }

                resposta = entrada.readLine();
                log("Servidor: " + resposta);

                if (resposta.startsWith("226")) {
                    log("✅ Foto enviada com sucesso!");
                }
            }
        } catch (IOException ex) {
            log("✗ Erro ao enviar foto: " + ex.getMessage());
        }
    }

    // Extrai a porta passiva da resposta do servidor FTP
    private int extrairPortaPassiva(String resposta) {
        try {
            int inicio = resposta.indexOf('(');
            int fim = resposta.indexOf(')');
            String dados = resposta.substring(inicio + 1, fim);
            String[] partes = dados.split(",");

            int p1 = Integer.parseInt(partes[4]);
            int p2 = Integer.parseInt(partes[5]);

            return p1 * 256 + p2;
        } catch (Exception e) {
            return -1;
        }
    }

    // Limpa a lista de arquivos e exibe os disponíveis no servidor
    private void listarArquivos() {
        if (!conectado) {
            log("✗ Conecte ao servidor primeiro");
            return;
        }

        new Thread(() -> {
            try {
                saida.println("PASV");
                String resposta = entrada.readLine();
                log("Servidor: " + resposta);

                if (!resposta.startsWith("227")) {
                    log("✗ Erro ao entrar no modo passivo");
                    return;
                }

                int porta = extrairPortaPassiva(resposta);
                if (porta == -1) {
                    log("✗ Erro ao extrair porta passiva");
                    return;
                }

                saida.println("LIST");
                resposta = entrada.readLine();
                log("Servidor: " + resposta);

                if (resposta.startsWith("150")) {
                    try (Socket socketDados = new Socket(ipField.getText().trim(), porta);
                         BufferedReader dadosEntrada = new BufferedReader(
                                 new InputStreamReader(socketDados.getInputStream()))) {

                        log("📋 Arquivos no backup:");
                        String linha;
                        while ((linha = dadosEntrada.readLine()) != null) {
                            log("   " + linha);
                        }
                    }

                    resposta = entrada.readLine();
                    log("Servidor: " + resposta);
                }

            } catch (IOException ex) {
                log("✗ Erro ao listar arquivos: " + ex.getMessage());
            }
        }).start();
    }

    // Monitora uma pasta para novos arquivos e envia automaticamente
    private void iniciarBackupAutomatico(File pasta) {
        try {
            Path path = pasta.toPath();
            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            watchThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                File novoArquivo = new File(pasta, event.context().toString());
                                if (isImagem(novoArquivo)) {
                                    log("Backup automático: " + novoArquivo.getName());
                                    enviarFotoAutomatico(novoArquivo);
                                }
                            }
                        }
                        key.reset();
                    } catch (Exception ex) {
                        log("Erro no backup automático: " + ex.getMessage());
                    }
                }
            });
            watchThread.start();
            log("Backup automático iniciado para a pasta: " + pasta.getAbsolutePath());
        } catch (IOException ex) {
            log("Erro ao iniciar backup automático: " + ex.getMessage());
        }
    }

    // Verifica se o arquivo é uma imagem com base na extensão
    private boolean isImagem(File arquivo) {
        String nome = arquivo.getName().toLowerCase();
        return nome.endsWith(".jpg") || nome.endsWith(".jpeg") || nome.endsWith(".png") ||
                nome.endsWith(".gif") || nome.endsWith(".bmp") || nome.endsWith(".tiff");
    }

    // Envia automaticamente a foto detectada pelo monitoramento de pasta
    private void enviarFotoAutomatico(File arquivo) {
        if (conectado) {
            enviarArquivoParaServidor(arquivo);
        } else {
            log("✗ Não conectado ao servidor. Backup automático pausado.");
        }
    }

    // Escreve uma mensagem no log com timestamp
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // Inicia o programa
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteFTP());
    }

    // Abre o seletor de pasta para escolher onde armazenar o backup automático
    private void selecionarPastaBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione a pasta para backup automático");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int resultado = chooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File pasta = chooser.getSelectedFile();
            iniciarBackupAutomatico(pasta);
        }
    }

    // Método para atualizar os botões de ação padrão
    private void atualizarBotoesPadrao() {
        painelAcoes.removeAll();

        JButton btnManual = new JButton("Enviar Fotos Manualmente");
        btnManual.addActionListener(e -> {
            selecionarFoto();
            mostrarBotoesEnviarCancelar("manual");
        });

        JButton btnAutomatico = new JButton("Selecionar Pasta para Backup Automático");
        btnAutomatico.addActionListener(e -> {
            selecionarPastaBackup();
            mostrarBotoesEnviarCancelar("automatico");
        });

        painelAcoes.add(btnManual);
        painelAcoes.add(btnAutomatico);
        painelAcoes.add(listarBtn);
        painelAcoes.revalidate();
        painelAcoes.repaint();
    }

    // Método para exibir os botões de enviar e cancelar
    private void mostrarBotoesEnviarCancelar(String modo) {
        painelAcoes.removeAll();

        JButton btnEnviar = new JButton("Enviar Fotos");
        btnEnviar.addActionListener(e -> {
            if ("manual".equals(modo)) {
                enviarFoto();
            } else if ("automatico".equals(modo)) {
                log("Backup automático já está monitorando a pasta.");
            }
            atualizarBotoesPadrao();
        });

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> atualizarBotoesPadrao());

        painelAcoes.add(btnEnviar);
        painelAcoes.add(btnCancelar);
        painelAcoes.revalidate();
        painelAcoes.repaint();
    }
}