package jpabook.jpashop.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateItemDTO {
    String name;
    int price;
    int stockQuantity;
}
