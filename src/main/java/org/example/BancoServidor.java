package org.example;

// Imports de bibliotecas necessárias
// import com.google.gson.Gson;
// import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class BancoServidor {

    private static Map<String, Double> contas = new HashMap<>();

    // Lock para controlar acesso concorrente às contas
    private static Lock lock = new ReentrantLock();

    // Semáforo para limitar o número de operações simultâneas
    private static Semaphore semaforo = new Semaphore(5); // Limite de 5 operações simultâneas

    // Logger para registrar eventos e erros
    private static final Logger logger = Logger.getLogger(BancoServidor.class.getName());
    private static final String FILE_PATH = "contas.json";

    public static void main(String[] args) throws IOException {
        int portaServidor = 12345;

        setupLogger(); // Configura o logger
        carregarContas(); // Carrega contas do arquivo JSON

        // Cria um ServerSocket para escutar conexões de clientes
        ServerSocket serverSocket = new ServerSocket(portaServidor);
        System.out.println("Servidor bancário em execução...");
        logger.info("Servidor iniciado na porta " + portaServidor);

        // Loop infinito para aceitar conexões de clientes
        while (true) {
            Socket clienteSocket = serverSocket.accept(); // Aceita uma nova conexão
            logger.info("Novo cliente conectado: " + clienteSocket.getInetAddress());

            // Cria uma nova thread para tratar a comunicação com o cliente
            new Thread(new ClienteHandler(clienteSocket)).start();
        }
    }


    private static void setupLogger() throws IOException {
        FileHandler fileHandler = new FileHandler("banco_servidor.log", true);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.INFO);
    }


    private static void carregarContas() {
        // Gson gson = new Gson(); // Instancia Gson para manipulação de JSON
        try (Reader reader = new FileReader(FILE_PATH)) {
            // Type type = new TypeToken<Map<String, Double>>() {}.getType();
            // contas = gson.fromJson(reader, type);
            if (contas == null) {
                contas = new HashMap<>();
            }
        } catch (IOException e) {
            logger.warning("Nenhum arquivo de contas encontrado. Criando novo arquivo.");
            contas = new HashMap<>();
        }
    }

    // Método para salvar contas no arquivo
    private static void salvarContas() {
        // Gson gson = new Gson();
        try (Writer writer = new FileWriter(FILE_PATH)) {
            // gson.toJson(contas, writer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar o arquivo de contas.", e);
        }
    }

    // Classe interna que trata a comunicação com o cliente
    private static class ClienteHandler implements Runnable {
        private Socket clienteSocket; // Socket do cliente

        public ClienteHandler(Socket socket) {
            this.clienteSocket = socket; // Inicializa o socket do cliente
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true)) {

                String entrada; // Variável para armazenar a entrada do cliente
                while ((entrada = in.readLine()) != null) { // Lê as entradas do cliente

                    //O Semaphore é configurado para permitir um máximo de 5 operações simultâneas,
                    // limitando o número de threads que podem executar operações bancárias ao mesmo tempo.
                    //Coberto: A implementação de Lock e o uso do semáforo ajudam a evitar condições de corrida.
                    // O lock garante que apenas uma operação por vez possa ser executada nas contas, evitando assim que duas operações tentem modificar o mesmo saldo simultaneamente.
                    semaforo.acquire();  // Adquire permissão do semáforo antes de processar a operação

                    String finalEntrada = entrada;
                    FutureTask<String> task = new FutureTask<>(() -> processarOperacao(finalEntrada, out)); // Cria uma nova tarefa para processar a operação
                    new Thread(task).start(); // Inicia a tarefa em uma nova thread

                    try {
                        String resultado = task.get(5, TimeUnit.SECONDS); // Timeout de 5 segundos
                        out.println(resultado);
                    } catch (TimeoutException e) {
                        out.println("Operação demorou muito para ser concluída.");
                        logger.warning("Operação demorou mais de 5 segundos.");
                        task.cancel(true);
                    } finally {
                        semaforo.release();  // Libera a permissão após a conclusão da operação
                    }
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "Erro de comunicação com o cliente: " + clienteSocket.getInetAddress(), e);
            }
        }

        // Método para processar a operação recebida do cliente
        private String processarOperacao(String entrada, PrintWriter out) {

            //O uso de Lock (ReentrantLock) protege o acesso a operações críticas nas contas, garantindo que apenas uma thread
            // possa modificar uma conta por vez.

            lock.lock(); // Adquire o lock para garantir acesso exclusivo
            try {
                String[] tokens = entrada.split(" "); // Divide a entrada em tokens
                String comando = tokens[0]; // Primeiro token é o comando

                // Executa o comando apropriado
                switch (comando) {
                    case "DEPOSITAR":
                        return depositar(tokens[1], Double.parseDouble(tokens[2])); // Chama o método de depósito
                    case "SACAR":
                        return sacar(tokens[1], Double.parseDouble(tokens[2])); // Chama o método de saque
                    case "SALDO":
                        return consultarSaldo(tokens[1]); // Chama o método de consulta de saldo
                    case "TRANSFERIR":
                        return transferir(tokens[1], tokens[2], Double.parseDouble(tokens[3])); // Chama o método de transferência
                    case "SAIR":
                        return "Desconectando do servidor...";
                    default:
                        return "Comando inválido.";
                }
            } finally {
                lock.unlock(); // Libera o lock após o processamento
            }
        }

        // Método para depositar valor na conta
        private String depositar(String conta, double valor) {
            contas.put(conta, contas.getOrDefault(conta, 0.0) + valor);
            salvarContas(); // Salva as contas no arquivo
            logger.info("Depósito realizado na conta " + conta + ": R$" + valor);
            return "Depósito de R$" + valor + " realizado com sucesso na conta " + conta + ".";
        }

        // Método para sacar valor da conta
        private String sacar(String conta, double valor) {
            double saldoAtual = contas.getOrDefault(conta, 0.0);
            if (saldoAtual >= valor) { // Verifica se o saldo é suficiente
                contas.put(conta, saldoAtual - valor); // Atualiza o saldo da conta
                salvarContas(); // Salva as contas no arquivo
                logger.info("Saque realizado da conta " + conta + ": R$" + valor);
                return "Saque de R$" + valor + " realizado com sucesso da conta " + conta + ".";
            } else {
                return "Saldo insuficiente na conta " + conta + ".";
            }
        }

        // Método para consultar o saldo da conta
        private String consultarSaldo(String conta) {
            double saldo = contas.getOrDefault(conta, 0.0);
            return "Saldo da conta " + conta + ": R$" + saldo;
        }

        // Método para transferir valor entre contas
        private String transferir(String contaOrigem, String contaDestino, double valor) {
            double saldoOrigem = contas.getOrDefault(contaOrigem, 0.0);
            if (saldoOrigem >= valor) {
                contas.put(contaOrigem, saldoOrigem - valor);
                contas.put(contaDestino, contas.getOrDefault(contaDestino, 0.0) + valor); // Atualiza o saldo da conta de destino
                salvarContas(); // Salva as contas no arquivo
                logger.info("Transferência de R$" + valor + " da conta " + contaOrigem + " para a conta " + contaDestino);
                return "Transferência de R$" + valor + " da conta " + contaOrigem + " para a conta " + contaDestino + " realizada com sucesso.";
            } else {
                return "Saldo insuficiente na conta " + contaOrigem + ".";
            }
        }
    }
}
