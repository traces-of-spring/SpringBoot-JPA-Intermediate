package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XToOne(ManyToOne, OneToOne) 에서는 어떻게 성능 최적화를 할까?
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate%Module 모듈 등록, LAZY=null 처리
     * - 양방향 연관 관계 문제 발생 -> @JsonIgnore
     * @return
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // LAZY 강제 초기화
            order.getDelivery().getAddress(); // LAZY 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용X)
     * - 단점 : 지연로딩으로 쿼리 N번 호출
     * @return
     */
    @GetMapping("/api/v2/simple-orders")
    public Result orderV2() {
        // ORDER 2개
        // N + 1 -> 1 + 회원 N + 배송 N
        List<Order> orders = orderRepository.findAll(new OrderSearch());

        //
        List<SimpleOrderDTO> collect = orders.stream()
                .map(SimpleOrderDTO::new)
                .collect(Collectors.toList());

        return new Result(collect);
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 O)
     * - fetch join으로 쿼리 1번 호출
     * @return
     */
    @GetMapping("/api/v3/simple-orders")
    public Result orderV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        List<SimpleOrderDTO> collect = orders.stream()
                .map(SimpleOrderDTO::new)
                .collect(Collectors.toList());

        return new Result(collect);
    }

    /**
     * V4. JPA에서 DTO로 바로 조회
     * - 쿼리 1번 호출
     * - select 절에서 원하는 데이터만 선택해서 조회
     * - 재사용성이 낮다는 단점
     * @return
     */
    @GetMapping("/api/v4/simple-orders")
    public Result orderV4() {
        return new Result(orderRepository.findOrderDtos());
    }


    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }

    @Data
    static class SimpleOrderDTO {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDTO(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 (영속성 컨텍스트가 DB 쿼리)
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화 (영속성 컨텍스트가 DB 쿼리)
        }
    }
}
