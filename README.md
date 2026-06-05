# FTP Photo Backup

Sistema cliente-servidor FTP para backup de fotos desenvolvido em Java com interface gráfica Swing.

## Sobre o Projeto

Trabalho acadêmico da disciplina de Redes de Computadores que implementa um servidor e cliente FTP simplificado, especializado em backup de imagens.

### Funcionalidades

**Servidor:**
- Aceita múltiplas conexões simultâneas (multithreaded)
- Recebe e armazena arquivos de imagem
- Valida extensões de arquivos (apenas imagens)
- Interface gráfica com log em tempo real
- Modo passivo para transferência de dados

**Cliente:**
- Interface gráfica intuitiva
- Seleção manual de fotos para upload
- Backup automático monitorando uma pasta
- Listagem de arquivos no servidor
- Suporte a múltiplos formatos de imagem

### Formatos Suportados

JPG, JPEG, PNG, GIF, BMP, TIFF, TIF, WEBP, SVG, ICO, RAW, CR2, NEF, ARW, DNG

## Como Executar

### Pré-requisitos

- Java JDK 8 ou superior

### Executando o Servidor

```bash
java -jar Servidor.jar
```

O servidor iniciará na porta **12384** e criará automaticamente um diretório `backup/` para armazenar as fotos.

### Executando o Cliente

```bash
java -jar Cliente.jar
```

1. Insira o IP do servidor (padrão: 127.0.0.1)
2. Clique em "Conectar"
3. Selecione fotos manualmente ou configure backup automático
4. Clique em "Enviar Fotos"

## Estrutura do Projeto

```
TrabalhoRedes/
├── src/
│   ├── ServidorFTP.java    # Servidor FTP com GUI
│   ├── ClienteFTP.java     # Cliente FTP com GUI
│   └── ClientHandler.java  # Handler de conexão do cliente
├── bin/                    # Classes compiladas
├── backup/                 # Diretório de armazenamento (criado automaticamente)
├── Cliente.jar             # Cliente executável
└── Servidor.jar            # Servidor executável
```

## Comandos FTP Implementados

| Comando | Descrição |
|---------|-----------|
| USER    | Identificação do usuário |
| PASS    | Senha do usuário |
| PWD     | Diretório atual |
| TYPE    | Tipo de transferência (ASCII/Binary) |
| PASV    | Modo passivo |
| STOR    | Upload de arquivo |
| LIST    | Listar arquivos |
| QUIT    | Encerrar conexão |

## Compilando a partir do Código Fonte

```bash
# Compilar
javac -d bin src/*.java

# Criar JAR do Servidor
jar cfe Servidor.jar ServidorFTP -C bin .

# Criar JAR do Cliente
jar cfe Cliente.jar ClienteFTP -C bin .
```

## Tecnologias

- **Java SE** - Linguagem de programação
- **Swing** - Interface gráfica
- **Sockets TCP** - Comunicação de rede
- **WatchService** - Monitoramento de diretórios

## Licença

Projeto acadêmico - Uso educacional

