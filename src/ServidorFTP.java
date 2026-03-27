import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class ServidorFTP extends JFrame {
    private JTextArea logArea;
    private ServerSocket servidorComando;
    private ExecutorService pool;
    private boolean rodando = false;
    private static final int PORTA_COMANDO = 12384; // Porta na faixa permitida (12382-12392)
    
    public ServidorFTP() {
        super("Servidor FTP - Backup de Fotos");
        configurarGUI();
        new Thread(this::iniciarServidor).start();
    }
    
    // Inicializa a interface gráfica do usuário (GUI)
    private void configurarGUI() {
        setLayout(new BorderLayout());
        
        // Área de log
        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroll, BorderLayout.CENTER);
        
        // Painel de controle
        JPanel controle = new JPanel(new FlowLayout());
        JButton pararBtn = new JButton("Parar Servidor");
        pararBtn.addActionListener(e -> pararServidor());
        controle.add(pararBtn);
        add(controle, BorderLayout.SOUTH);
        
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
        
        // Cria diretório de backup
        File backup = new File("backup");
        if (!backup.exists()) {
            backup.mkdirs();
            log("Diretório de backup criado: " + backup.getAbsolutePath());
        }
    }
    
    // Inicia o servidor FTP e aceita conexões de clientes
    private void iniciarServidor() {
        pool = Executors.newCachedThreadPool();
        try {
            servidorComando = new ServerSocket(PORTA_COMANDO);
            rodando = true;
            log("=== SERVIDOR FTP INICIADO ===");
            log("Porta de comando: " + PORTA_COMANDO);
            log("Aguardando conexões...");
            
            while (rodando) {
                try {
                    Socket cliente = servidorComando.accept();
                    log("Nova conexão de: " + cliente.getInetAddress().getHostAddress());
                    pool.execute(new ClientHandler(cliente, this));
                } catch (SocketException e) {
                    if (rodando) {
                        log("Erro na conexão: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("ERRO ao iniciar servidor: " + e.getMessage());
        }
    }
    
    // Para o servidor e encerra todas as conexões
    private void pararServidor() {
        try {
            rodando = false;
            if (servidorComando != null && !servidorComando.isClosed()) {
                servidorComando.close();
            }
            if (pool != null) {
                pool.shutdown();
            }
            log("=== SERVIDOR PARADO ===");
        } catch (IOException e) {
            log("Erro ao parar servidor: " + e.getMessage());
        }
    }
    
    // Escreve mensagens no log da GUI
    public synchronized void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    // Inicia o programa do servidor
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServidorFTP());
    }
}