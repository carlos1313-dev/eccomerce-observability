package com.ecommerce;

import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import com.ecommerce.events.EventPublisher;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * PRUEBA DE CONCURRENCIA — Evidencia de prevención de sobreventa
 *
 * EventPublisher se mockea para no depender de RabbitMQ en el entorno de pruebas.
 * La lógica crítica (bloqueo pesimista, validación de stock) se prueba íntegramente.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** Mock del EventPublisher para no requerir RabbitMQ en tests */
    @MockBean private EventPublisher eventPublisher;

    private Product testProduct;
    private List<User> testUsers;

    private static final int STOCK_INICIAL          = 5;
    private static final int COMPRADORES_CONCURRENTES = 20;

    @BeforeEach
    void setUp() {
        doNothing().when(eventPublisher).publishOrderCreated(any());

        productRepository.deleteAll();
        userRepository.deleteAll();

        testProduct = productRepository.save(Product.builder()
                .name("Producto Test Concurrencia")
                .price(new BigDecimal("99.99"))
                .stock(STOCK_INICIAL).active(true).build());

        testUsers = new ArrayList<>();
        for (int i = 0; i < COMPRADORES_CONCURRENTES; i++) {
            testUsers.add(userRepository.save(User.builder()
                    .name("Cliente " + i)
                    .email("cliente" + i + "@test.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.CLIENTE).active(true).build()));
        }
    }

    @Test
    @DisplayName("CONCURRENCIA: stock nunca negativo bajo 20 compradores simultáneos")
    void debePrevenirSobreventa() throws InterruptedException {
        AtomicInteger exitosas = new AtomicInteger(0);
        AtomicInteger fallidas = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(COMPRADORES_CONCURRENTES);
        ExecutorService executor = Executors.newFixedThreadPool(COMPRADORES_CONCURRENTES);

        for (int i = 0; i < COMPRADORES_CONCURRENTES; i++) {
            final String email = testUsers.get(i).getEmail();
            final Long   pid   = testProduct.getId();
            executor.submit(() -> {
                try {
                    latch.await();
                    orderService.createOrder(
                            new Dtos.CreateOrderRequest(List.of(new Dtos.OrderItemRequest(pid, 1))),
                            email);
                    exitosas.incrementAndGet();
                } catch (Exception e) {
                    fallidas.incrementAndGet();
                } finally { done.countDown(); }
            });
        }

        latch.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Product final_ = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("=== RESULTADO PRUEBA CONCURRENCIA ===");
        System.out.println("Stock inicial:      " + STOCK_INICIAL);
        System.out.println("Compradores:        " + COMPRADORES_CONCURRENTES);
        System.out.println("Compras exitosas:   " + exitosas.get());
        System.out.println("Compras fallidas:   " + fallidas.get());
        System.out.println("Stock final en BD:  " + final_.getStock());
        System.out.println("=====================================");

        assertThat(final_.getStock()).as("Stock nunca negativo").isGreaterThanOrEqualTo(0);
        assertThat(exitosas.get()).as("Exitosas no superan stock").isLessThanOrEqualTo(STOCK_INICIAL);
        assertThat(final_.getStock() + exitosas.get()).as("Stock final + vendidos = stock inicial")
                .isEqualTo(STOCK_INICIAL);
    }
}
