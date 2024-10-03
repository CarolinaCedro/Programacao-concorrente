package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
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
    private static Lock lock = new ReentrantLock();
    private static final Logger logger = Logger.getLogger(BancoServidor.class.getName());
    private static final String FILE_PATH = "contas.json";

    public static void main(String[] args) throws IOException {
        int portaServidor = 12345;

        // Configura o logger para gravar os logs em um arquivo
        setupLogger();

        // Carrega os dados de contas do arquivo JSON
        carregarContas();

        ServerSocket serverSocket = new ServerSocket(portaServidor);
        System.out.println("Servidor bancário em execução...");
        logger.info("Servidor iniciado na porta " + portaServidor);

        while (true) {
            Socket clienteSocket = serverSocket.accept();
            logger.info("Novo cliente conectado: " + clienteSocket.getInetAddress());

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
        Gson gson = new Gson();
        try (Reader reader = new FileReader(FILE_PATH)) {
            Type type = new TypeToken<Map<String, Double>>() {}.getType();
            contas = gson.fromJson(reader, type);
            if (contas == null) {
                contas = new HashMap<>();
            }
        } catch (IOException e) {
            logger.warning("Nenhum arquivo de contas encontrado. Criando novo arquivo.");
            contas = new HashMap<>();
        }
    }

    private static void salvarContas() {
        Gson gson = new Gson();
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(contas, writer);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar o arquivo de contas.", e);
        }
    }

    private static class ClienteHandler implements Runnable {
        private Socket clienteSocket;

        public ClienteHandler(Socket socket) {
            this.clienteSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true)) {

                String entrada;
                while ((entrada = in.readLine()) != null) {
                    String finalEntrada = entrada;
                    FutureTask<String> task = new FutureTask<>(() -> processarOperacao(finalEntrada, out));
                    new Thread(task).start();

                    try {
                        String resultado = task.get(5, TimeUnit.SECONDS); // Timeout de 5 segundos
                        out.println(resultado);
                    } catch (TimeoutException e) {
                        out.println("Operação demorou muito para ser concluída.");
                        logger.warning("Operação demorou mais de 5 segundos.");
                        task.cancel(true); // Cancela a operação se demorar muito
                    }
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "Erro de comunicação com o cliente: " + clienteSocket.getInetAddress(), e);
            }
        }

        private String processarOperacao(String entrada, PrintWriter out) {
            String[] partes = entrada.split(" ");
            String operacao = partes[0];

            logger.info("Operação recebida: " + operacao + " do cliente " + clienteSocket.getInetAddress());

            switch (operacao.toUpperCase()) {
                case "DEPOSITAR":
                    if (partes.length == 3) {
                        String conta = partes[1];
                        double valorDeposito = Double.parseDouble(partes[2]);
                        depositar(conta, valorDeposito);
                        logger.info("Depósito de R$" + valorDeposito + " realizado na conta " + conta);
                        return "Depósito de R$" + valorDeposito + " na conta " + conta + " realizado com sucesso.";
                    } else {
                        return "Comando inválido. Formato esperado: DEPOSITAR CONTA VALOR";
                    }
                case "SACAR":
                    if (partes.length == 3) {
                        String conta = partes[1];
                        double valorSaque = Double.parseDouble(partes[2]);
                        boolean sucesso = sacar(conta, valorSaque);
                        if (sucesso) {
                            logger.info("Saque de R$" + valorSaque + " realizado na conta " + conta);
                            return "Saque de R$" + valorSaque + " na conta " + conta + " realizado com sucesso.";
                        } else {
                            logger.warning("Tentativa de saque falhou. Saldo insuficiente.");
                            return "Saldo insuficiente para saque.";
                        }
                    } else {
                        return "Comando inválido. Formato esperado: SACAR CONTA VALOR";
                    }
                case "TRANSFERIR":
                    if (partes.length == 4) {
                        String contaOrigem = partes[1];
                        String contaDestino = partes[2];
                        double valorTransferencia = Double.parseDouble(partes[3]);
                        boolean sucesso = transferir(contaOrigem, contaDestino, valorTransferencia);
                        if (sucesso) {
                            logger.info("Transferência de R$" + valorTransferencia + " da conta " + contaOrigem + " para " + contaDestino + " realizada.");
                            return "Transferência de R$" + valorTransferencia + " realizada com sucesso.";
                        } else {
                            return "Saldo insuficiente para transferência.";
                        }
                    } else {
                        return "Comando inválido. Formato esperado: TRANSFERIR CONTA_ORIGEM CONTA_DESTINO VALOR";
                    }
                case "SALDO":
                    if (partes.length == 2) {
                        String conta = partes[1];
                        double saldo = getSaldo(conta);
                        logger.info("Consulta de saldo da conta " + conta + ": R$" + saldo);
                        return "Saldo da conta " + conta + ": R$" + saldo;
                    } else {
                        return "Comando inválido. Formato esperado: SALDO CONTA";
                    }
                case "SAIR":
                    try {
                        clienteSocket.close();
                        logger.info("Cliente desconectado: " + clienteSocket.getInetAddress());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Erro ao desconectar o cliente: " + clienteSocket.getInetAddress(), e);
                    }
                    return "Desconectando...";
                default:
                    return "Operação inválida. Use DEPOSITAR, SACAR, SALDO, TRANSFERIR ou SAIR.";
            }
        }

        private void depositar(String conta, double valor) {
            lock.lock();
            try {
                contas.putIfAbsent(conta, 0.0);
                double saldoAtual = contas.get(conta);
                contas.put(conta, saldoAtual + valor);
                salvarContas();
            } finally {
                lock.unlock();
            }
        }

        private boolean sacar(String conta, double valor) {
            lock.lock();
            try {
                contas.putIfAbsent(conta, 0.0);
                double saldoAtual = contas.get(conta);
                if (saldoAtual >= valor) {
                    contas.put(conta, saldoAtual - valor);
                    salvarContas();
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        private boolean transferir(String contaOrigem, String contaDestino, double valor) {
            lock.lock();
            try {
                if (contas.getOrDefault(contaOrigem, 0.0) >= valor) {
                    sacar(contaOrigem, valor);
                    depositar(contaDestino, valor);
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        private double getSaldo(String conta) {
            lock.lock();
            try {
                return contas.getOrDefault(conta, 0.0);
            } finally {
                lock.unlock();
            }
        }
    }
}
