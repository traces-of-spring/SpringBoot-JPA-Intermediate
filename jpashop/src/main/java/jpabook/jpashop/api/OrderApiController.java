package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.query.OrderQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1. 엔티티 직접 노출
     * - 엔티티가 변하면 API 스펙이 변한다.
     * - 트랜잭션 안에서 지연 로딩 필요
     * - 양방향 연관관계 문제
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     * @return
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // LAZY 강제 초기화
            order.getDelivery().getAddress(); // LAZY 강제 초기화
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream()
                    .forEach(o -> o.getItem().getName()); // LAZY 강제 초기화
        }

        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 X)
     * - 트랜잭션 안에서 지연 로딩 필요
     * - 지연 로딩으로 너무 많은 SQL 실행
     * @return
     */
    @GetMapping("/api/v2/orders")
    public Result ordersV2() {
        List<Order> all = orderRepository.findAll(new OrderSearch());

        List<OrderDTO> collect = all.stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());

        return new Result(collect);
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 O)
     * - 페이징 시에는 N 부분을 포기해야함 (대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능)
     * - 페치 조인으로 SQL이 1번만 실행됨
     * @return
     */
    @GetMapping("/api/v3/orders")
    public Result ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDTO> collect = orders.stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
        return new Result(collect);
    }

    /**
     * V3.1 엔티티를 조회해서 DTO로 변환 (fetch join 사용 O)
     * - 1. ToOne 관계를 모두 페치조인한다 (ToOne 관계는 row 수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다)
     * - 2. 컬렉션은 지연 로딩으로 조회한다
     * - 3. 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize 를 적용한다
     * - 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼이나 IN 쿼리로 조회한다
     * @param <T>
     */
    @GetMapping("/api/v3.1/orders")
    public Result ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDTO> collect = orders.stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
        return new Result(collect);
    }


    @GetMapping("/api/v4/orders")
    public Result ordersV4() {
        return new Result(orderQueryRepository.findOrderQueryDtos());
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }

    @Data
    static class OrderDTO {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDTO> orderItems;

        public OrderDTO(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY
            orderItems = order.getOrderItems().stream() // LAZY
                    .map(OrderItemDTO::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    @AllArgsConstructor
    static class OrderItemDTO {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDTO(OrderItem orderItem) {
            itemName = orderItem.getItem().getName(); // LAZY
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }


    }
}
