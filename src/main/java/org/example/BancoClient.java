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
        String servidorIP = "127.0.0.1";
        int portaServidor = 12345;

        try (Socket socket = new Socket(servidorIP, portaServidor);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Cliente bancário conectado ao servidor. Digite comandos (SAIR para encerrar):");

            while (true) {
                String comando = sc.nextLine();
                out.println(comando);

                String resposta = in.readLine();
                System.out.println("Servidor: " + resposta);

                if (comando.equalsIgnoreCase("SAIR")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Erro de conexão com o servidor.");
            e.printStackTrace();
        }
    }
}





//package org.example;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.Socket;
//import java.util.Scanner;
//
//public class BancoClient {
//    private static Scanner sc = new Scanner(System.in);
//
//    public static void main(String[] args) throws IOException {
//        String servidorHost = "localhost";
//        int servidorPorta = 12345;
//
//        // Conexão com o servidor
//        Socket socket = new Socket(servidorHost, servidorPorta);
//        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//        System.out.println("Bem vindo ao Banco Lockstrike");
//
//        while (true) {
//            // Exibe o menu
//            System.out.println("---- Menu ----");
//            System.out.println("(1) Depositar");
//            System.out.println("(2) Sacar");
//            System.out.println("(3) Ver saldo");
//            System.out.println("(0) Sair");
//            System.out.print("Escolha uma opção: ");
//            int op = sc.nextInt();
//
//            switch (op) {
//                case 1:
//                    depositar(out, in);
//                    break;
//                case 2:
//                    sacar(out, in);
//                    break;
//                case 3:
//                    saldo(out, in);
//                    break;
//                case 0:
//                    sair(out);
//                    socket.close();
//                    return;
//                default:
//                    System.out.println("Opção inválida.");
//            }
//        }
//    }
//
//    public static void depositar(PrintWriter out, BufferedReader in) throws IOException {
//        System.out.print("Qual a quantia para o depósito? ");
//        double valor = sc.nextDouble();
//
//        // Envia comando ao servidor
//        out.println("DEPOSITAR " + valor);
//
//        // Recebe resposta do servidor
//        String resposta = in.readLine();
//        System.out.println("Servidor: " + resposta);
//    }
//
//    public static void sacar(PrintWriter out, BufferedReader in) throws IOException {
//        System.out.print("Qual a quantia para o saque? ");
//        double valor = sc.nextDouble();
//
//        // Envia comando ao servidor
//        out.println("SACAR " + valor);
//
//        // Recebe resposta do servidor
//        String resposta = in.readLine();
//        System.out.println("Servidor: " + resposta);
//    }
//
//    public static void saldo(PrintWriter out, BufferedReader in) throws IOException {
//        // Envia comando de saldo ao servidor sem valor
//        out.println("SALDO");
//
//        // Recebe resposta do servidor
//        String resposta = in.readLine();
//        System.out.println("Servidor: " + resposta);
//    }
//
//    public static void sair(PrintWriter out) {
//        // Envia comando de saída ao servidor
//        out.println("SAIR");
//        System.out.println("Desconectando...");
//    }
//}
