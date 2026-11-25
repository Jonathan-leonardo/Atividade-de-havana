import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Contrato comum para todos os canais de notificação.
interface CanalNotificacao {
    void enviar(String mensagem, Cliente cliente);
}

class SMS implements CanalNotificacao {
    @Override
    public void enviar(String mensagem, Cliente cliente) {
        System.out.println("SMS para " + cliente.getNome() + ": " + mensagem);
    }
}

class Email implements CanalNotificacao {
    @Override
    public void enviar(String mensagem, Cliente cliente) {
        System.out.println("E-mail para " + cliente.getNome() + ": " + mensagem);
    }
}

class NotificacaoApp implements CanalNotificacao {
    @Override
    public void enviar(String mensagem, Cliente cliente) {
        System.out.println("Aplicativo para " + cliente.getNome() + ": " + mensagem);
    }
}

class Cliente {
    private String nome;
    private String cpf;
    private String endereco;
    private double renda;
    private List<Conta> contas = new ArrayList<>();
    private CanalNotificacao canal;

    public Cliente(String nome, String cpf, String endereco,
                   double renda, CanalNotificacao canal) {
        this.nome = nome;
        this.cpf = cpf;
        this.endereco = endereco;
        this.renda = renda;
        this.canal = canal;
    }

    public void adicionarConta(Conta conta) {
        contas.add(conta);
    }

    public void notificar(String mensagem) {
        canal.enviar(mensagem, this);
    }

    public String getNome() {
        return nome;
    }
}

class SaqueException extends RuntimeException {
    public SaqueException(String mensagem) {
        super(mensagem);
    }
}

class ValorInvalidoException extends SaqueException {
    public ValorInvalidoException() {
        super("Valor inválido para saque!");
    }
}

class ContaBloqueadaException extends SaqueException {
    public ContaBloqueadaException() {
        super("A conta está bloqueada!");
    }
}

class SaldoInsuficienteException extends SaqueException {
    public SaldoInsuficienteException(double falta) {
        super(String.format("Saldo insuficiente! Falta R$ %.2f", falta));
    }
}

class LimiteDiarioExcedidoException extends SaqueException {
    public LimiteDiarioExcedidoException() {
        super("Limite diário de saque excedido!");
    }
}

class Conta {
    private String numero;
    private String agencia;
    private LocalDate dataAbertura;
    private double saldo;
    private boolean bloqueada;
    private double limiteDiario;
    private double totalSacadoHoje;
    private LocalDate dataControle;
    private Cliente cliente;

    public Conta(String numero, String agencia, double saldo,
                 double limiteDiario, Cliente cliente) {
        this.numero = numero;
        this.agencia = agencia;
        this.saldo = saldo;
        this.limiteDiario = limiteDiario;
        this.cliente = cliente;
        this.dataAbertura = LocalDate.now();
        this.bloqueada = false;
        this.totalSacadoHoje = 0;
        this.dataControle = LocalDate.now();
        cliente.adicionarConta(this);
    }

    public void sacar(double valor) {
        // Reinicia o total de saques quando muda o dia.
        if (!LocalDate.now().equals(dataControle)) {
            totalSacadoHoje = 0;
            dataControle = LocalDate.now();
        }

        if (valor <= 0) {
            throw new ValorInvalidoException();
        }
        if (bloqueada) {
            throw new ContaBloqueadaException();
        }
        if (valor > saldo) {
            throw new SaldoInsuficienteException(valor - saldo);
        }
        if (totalSacadoHoje + valor > limiteDiario) {
            throw new LimiteDiarioExcedidoException();
        }

        saldo -= valor;
        totalSacadoHoje += valor;
        System.out.println("Saque realizado com sucesso!");
        System.out.printf("Novo saldo: R$ %.2f%n", saldo);
    }

    public void depositar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("Depósito deve ser positivo.");
        }
        saldo += valor;
        System.out.println("Depósito realizado com sucesso!");
    }

    public double consultarSaldo() {
        return saldo;
    }

    public void bloquearConta() {
        bloqueada = true;
    }

    public void desbloquearConta() {
        bloqueada = false;
    }

    public Cliente getCliente() {
        return cliente;
    }
}

class BancoService {
    public void realizarSaque(Conta conta, double valor) {
        try {
            conta.sacar(valor);
        } catch (SaqueException erro) {
            System.out.println("ERRO: " + erro.getMessage());
            conta.getCliente().notificar(
                "Não foi possível realizar o saque: " + erro.getMessage()
            );
        }
    }
}

public class Main {
    public static void main(String[] args) {
        BancoService banco = new BancoService();

        Cliente joao = new Cliente(
            "João", "11111111111", "Rua A", 2500, new SMS()
        );
        Cliente maria = new Cliente(
            "Maria", "22222222222", "Rua B", 3500, new Email()
        );
        Cliente pedro = new Cliente(
            "Pedro", "33333333333", "Rua C", 4000, new NotificacaoApp()
        );

        System.out.println("\\n===== 1. VALOR INVÁLIDO =====");
        Conta c1 = new Conta("001", "123", 500, 300, joao);
        Conta c2 = new Conta("002", "123", 500, 300, maria);
        banco.realizarSaque(c1, 0);
        banco.realizarSaque(c2, -50);

        System.out.println("\\n===== 2. CONTA BLOQUEADA =====");
        Conta c3 = new Conta("003", "123", 500, 300, joao);
        Conta c4 = new Conta("004", "123", 500, 300, maria);
        c3.bloquearConta();
        c4.bloquearConta();
        banco.realizarSaque(c3, 100);
        banco.realizarSaque(c4, 50);

        System.out.println("\\n===== 3. SALDO INSUFICIENTE =====");
        Conta c5 = new Conta("005", "123", 100, 300, joao);
        Conta c6 = new Conta("006", "123", 200, 300, maria);
        banco.realizarSaque(c5, 150);
        banco.realizarSaque(c6, 250);

        System.out.println("\\n===== 4. LIMITE DIÁRIO EXCEDIDO =====");
        Conta c7 = new Conta("007", "123", 500, 100, maria);
        Conta c8 = new Conta("008", "123", 500, 50, pedro);
        banco.realizarSaque(c7, 80);
        banco.realizarSaque(c7, 30);
        banco.realizarSaque(c8, 40);
        banco.realizarSaque(c8, 20);

        System.out.println("\\n===== 5. SAQUES COM SUCESSO =====");
        Conta c9 = new Conta("009", "123", 1000, 500, joao);
        Conta c10 = new Conta("010", "123", 2000, 1000, pedro);
        banco.realizarSaque(c9, 200);
        banco.realizarSaque(c10, 500);

        System.out.println("\\n===== 6. DEPÓSITO E CONSULTA =====");
        c9.depositar(300);
        System.out.printf("Saldo final da conta 009: R$ %.2f%n",
                          c9.consultarSaldo());

        System.out.println("\\n===== FIM DOS TESTES =====");
    }
}