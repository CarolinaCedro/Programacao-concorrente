package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BancoClient {
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        String servidorIP = "127.0.0.1"; // IP do servidor
        int portaServidor = 12345; // Porta do servidor

        try (Socket socket = new Socket(servidorIP, portaServidor);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("=== Bem-vindo ao Banco Lockstrike ===");
            System.out.println("Conectado ao servidor bancário.");

            boolean sair = false;

            while (!sair) {
                // Exibe o menu de opções
                System.out.println("\n---- Menu ----");
                System.out.println("(1) Depositar");
                System.out.println("(2) Sacar");
                System.out.println("(3) Ver saldo");
                System.out.println("(4) Transferir");
                System.out.println("(0) Sair");
                System.out.print("Escolha uma opção: ");
                int opcao = sc.nextInt();

                switch (opcao) {
                    case 1:
                        depositar(out, in);
                        break;
                    case 2:
                        sacar(out, in);
                        break;
                    case 3:
                        consultarSaldo(out, in);
                        break;
                    case 4:
                        transferir(out, in);
                        break;
                    case 0:
                        sair(out);
                        sair = true;
                        break;
                    default:
                        System.out.println("Opção inválida, tente novamente.");
                }
            }
        } catch (IOException e) {
            System.out.println("Erro de conexão com o servidor.");
            e.printStackTrace();
        }
    }

    private static void depositar(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Digite a conta para depósito: ");
        String conta = sc.next();
        System.out.print("Digite o valor do depósito: ");
        double valor = sc.nextDouble();

        // Envia comando ao servidor
        out.println("DEPOSITAR " + conta + " " + valor);

        // Recebe resposta do servidor
        String resposta = in.readLine();
        System.out.println("Servidor: " + resposta);
    }

    private static void sacar(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Digite a conta para saque: ");
        String conta = sc.next();
        System.out.print("Digite o valor do saque: ");
        double valor = sc.nextDouble();

        // Envia comando ao servidor
        out.println("SACAR " + conta + " " + valor);

        // Recebe resposta do servidor
        String resposta = in.readLine();
        System.out.println("Servidor: " + resposta);
    }

    private static void consultarSaldo(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Digite a conta para consulta de saldo: ");
        String conta = sc.next();

        // Envia comando ao servidor
        out.println("SALDO " + conta);

        // Recebe resposta do servidor
        String resposta = in.readLine();
        System.out.println("Servidor: " + resposta);
    }

    private static void transferir(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Digite a conta de origem: ");
        String contaOrigem = sc.next();
        System.out.print("Digite a conta de destino: ");
        String contaDestino = sc.next();
        System.out.print("Digite o valor da transferência: ");
        double valor = sc.nextDouble();

        // Envia comando ao servidor
        out.println("TRANSFERIR " + contaOrigem + " " + contaDestino + " " + valor);

        // Recebe resposta do servidor
        String resposta = in.readLine();
        System.out.println("Servidor: " + resposta);
    }

    private static void sair(PrintWriter out) {
        out.println("SAIR");
        System.out.println("Desconectando...");
    }
}
