package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    /**
     * 영속성 컨텍스트가 자동 변경
     * @param itemId
     * @param name
     * @param price
     * @param stockQuantity
     */
    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        // 영속성 컨텍스트에 의해 관리
        Item findItem = itemRepository.findOne(itemId);
         findItem.changeItem(name, price, stockQuantity);
        // -> 업데이트 할 때도 setter 대신 의미 있는 메소드를 만들어 두자
//        findItem.setPrice(price);
//        findItem.setName(name);
//        findItem.setStockQuantity(stockQuantity);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }
}
