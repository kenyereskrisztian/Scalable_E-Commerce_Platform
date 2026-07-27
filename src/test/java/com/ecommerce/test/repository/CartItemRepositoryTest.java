package com.ecommerce.test.repository;

import com.ecommerce.domain.CartItem;
import com.ecommerce.domain.Category;
import com.ecommerce.domain.Product;
import com.ecommerce.domain.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("user@test.com").password("pass").firstName("A").lastName("B").active(true)
                .build());

        Category category = categoryRepository.save(Category.builder().name("Test").active(true).build());

        product = productRepository.save(Product.builder()
                .name("Item").price(BigDecimal.TEN).category(category).stock(5).active(true)
                .build());
    }

    @Test
    void findByUserId_shouldReturnUserCartItems() {
        cartItemRepository.save(CartItem.builder().user(user).product(product).quantity(2).build());

        List<CartItem> items = cartItemRepository.findByUserId(user.getId());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void findByUserIdAndProductId_shouldReturnExistingItem() {
        cartItemRepository.save(CartItem.builder().user(user).product(product).quantity(1).build());

        Optional<CartItem> found = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(1);
    }
}
